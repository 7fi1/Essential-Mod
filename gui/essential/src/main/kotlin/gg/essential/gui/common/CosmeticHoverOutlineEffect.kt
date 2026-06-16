/*
 * Copyright (c) 2024 ModCore Inc. All rights reserved.
 *
 * This code is part of ModCore Inc.'s Essential Mod repository and is protected
 * under copyright registration # TX0009138511. For the full license, see:
 * https://github.com/EssentialGG/Essential/blob/main/LICENSE
 *
 * You may not use, copy, reproduce, modify, sell, license, distribute,
 * commercialize, or otherwise exploit, or create derivative works based
 * upon, this file or any other in this repository, all of which is reserved by Essential.
 */
package gg.essential.gui.common

import gg.essential.cosmetics.CosmeticId
import gg.essential.elementa.effects.Effect
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.model.util.ResourceCleaner
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UMinecraft
import gg.essential.universal.UMouse
import gg.essential.universal.UResolution
import gg.essential.universal.render.DrawCallBuilder
import gg.essential.universal.render.UGpuSampler
import gg.essential.universal.render.URenderPipeline
import gg.essential.universal.shader.BlendState
import gg.essential.universal.vertex.UBufferBuilder
import gg.essential.util.GlFrameBuffer
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.NEAREST
import gg.essential.util.image.GpuTexture
import kotlin.math.roundToInt

class CosmeticHoverOutlineEffect(
    private val outlineCosmetic: State<List<CosmeticId>>,
) : Effect() {

    private var previousScissorEffectState: ScissorEffect.ScissorState? = null
    private var previousFrameBuffer: () -> Unit = {}

    private val mutableHoveredCosmetic = mutableStateOf<CosmeticId?>(null)
    val hoveredCosmetic: State<CosmeticId?> = mutableHoveredCosmetic

    private lateinit var resources: Resources
    private val renderTargetColor: GpuTexture
        get() = platform.outputColorTextureOverride ?: resources.fallbackFrameBuffer?.texture ?: platform.mcFrameBufferColorTexture
    private val renderTargetDepth: GpuTexture
        get() = platform.outputDepthTextureOverride ?: resources.fallbackFrameBuffer?.depthStencil ?: platform.mcFrameBufferDepthTexture!!
    private val mainTextureCopy: GpuTexture
        get() = resources.mainTextureCopy
    private val compositeRenderResult: RenderResult
        get() = resources.compositeRenderResult
    private val renderResults: MutableMap<CosmeticId, RenderResult>
        get() = resources.renderResults

    override fun beforeDraw(matrixStack: UMatrixStack) {
        check(active == null) { "Outline effects cannot be nested." }
        active = this

        previousScissorEffectState = ScissorEffect.currentScissorState
        ScissorEffect.currentScissorState = null // required for Mc12106ScissorHandler to behave correctly
        UGraphics.disableScissor()

        val viewportWidth = UResolution.viewportWidth
        val viewportHeight = UResolution.viewportHeight
        if (!::resources.isInitialized || resources.viewportWidth != viewportWidth || resources.viewportHeight != viewportHeight) {
            if (::resources.isInitialized) resources.close()
            resourceCleaner.runCleanups()
            resources = Resources(viewportWidth, viewportHeight)
            resourceCleaner.register(this, resources)
        }

        resources.fallbackFrameBuffer?.let { fallbackFrameBuffer ->
            previousFrameBuffer = fallbackFrameBuffer.bind()
        }

        mainTextureCopy.copyFrom(renderTargetColor)
        renderTargetColor.clearColor()
        renderTargetDepth.clearDepth()
    }

    override fun afterDraw(matrixStack: UMatrixStack) {
        compositeRenderResult.color.copyFrom(renderTargetColor)
        compositeRenderResult.depth.copyFrom(renderTargetDepth)
        renderTargetColor.copyFrom(mainTextureCopy)
        renderTargetDepth.clearDepth()

        previousFrameBuffer()

        previousScissorEffectState?.let { (x, y, width, height) ->
            UGraphics.enableScissor(x, y, width, height)
        }
        ScissorEffect.currentScissorState = previousScissorEffectState

        renderFullScreenQuad(COMPOSITE_PIPELINE) {
            texture("ColorSampler", compositeRenderResult.color.ucView, UGpuSampler.NEAREST)
            texture("DepthSampler", compositeRenderResult.depth.ucView, UGpuSampler.NEAREST)
        }

        mutableHoveredCosmetic.set(computeHoveredCosmetic())

        outlineCosmetic.get().forEach { cosmetic ->
            val renderResult = renderResults[cosmetic]
            if (renderResult != null) {
                doDrawOutline(renderResult)
            }
        }

        resources.freeRenderResults()

        active = null
    }

    fun beginOutlineRender(cosmetic: CosmeticId) {
        compositeRenderResult.color.copyFrom(renderTargetColor)
        compositeRenderResult.depth.copyFrom(renderTargetDepth)

        val renderResult = renderResults[cosmetic]
        if (renderResult != null) {
            renderTargetColor.copyFrom(renderResult.color)
            renderTargetDepth.copyFrom(renderResult.depth)
        } else {
            renderTargetColor.clearColor()
            renderTargetDepth.clearDepth()
        }
    }

    fun endOutlineRender(cosmetic: CosmeticId) {
        val renderResult = renderResults[cosmetic] ?: resources.createRenderResult()
        renderResult.color.copyFrom(renderTargetColor)
        renderResult.depth.copyFrom(renderTargetDepth)
        renderResults[cosmetic] = renderResult

        renderTargetColor.copyFrom(compositeRenderResult.color)
        renderTargetDepth.copyFrom(compositeRenderResult.depth)
    }

    private fun computeHoveredCosmetic(): CosmeticId? {
        val scissor = ScissorEffect.currentScissorState
        if (scissor != null && !scissor.contains(UMouse.Scaled.x, UMouse.Scaled.y)) {
            return null
        }

        val (hoveredCosmetic, hoveredDepth) = renderResults.entries.associate {
            it.key to it.value.depth.readHoveredDepth()
        }.minByOrNull { it.value } ?: return null

        val compositeDepth = compositeRenderResult.depth.readHoveredDepth()
        if (hoveredDepth - 0.0001f >= compositeDepth.coerceAtMost(0.999f)) {
            return null // player is obstructing the cosmetic
        }

        return hoveredCosmetic
    }

    private fun doDrawOutline(renderResult: RenderResult) {
        renderFullScreenQuad(OUTLINE_PIPELINE) {
            texture("CompositeSampler", compositeRenderResult.depth.ucView, UGpuSampler.NEAREST)
            texture("TargetSampler", renderResult.depth.ucView, UGpuSampler.NEAREST)
            uniform("OneTexel", 1f / renderResult.color.width, 1f / renderResult.color.height)
            uniform("OutlineWidth", UMinecraft.guiScale * 2)
        }
    }

    private fun renderFullScreenQuad(pipeline: URenderPipeline, configure: DrawCallBuilder.() -> Unit) {
        UBufferBuilder.create(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_TEXTURE).apply {
            pos(UMatrixStack.UNIT, 0.0, 0.0, 0.0).tex(0.0, 0.0).endVertex()
            pos(UMatrixStack.UNIT, 1.0, 0.0, 0.0).tex(1.0, 0.0).endVertex()
            pos(UMatrixStack.UNIT, 1.0, 1.0, 0.0).tex(1.0, 1.0).endVertex()
            pos(UMatrixStack.UNIT, 0.0, 1.0, 0.0).tex(0.0, 1.0).endVertex()
        }.build()?.drawAndClose(pipeline, configure)
    }

    private fun ScissorEffect.ScissorState.contains(testX: Double, testY: Double): Boolean {
        val scaleFactor = UResolution.scaleFactor.toInt()
        val tx = (testX * scaleFactor).roundToInt()
        val ty = UResolution.viewportHeight - (testY * scaleFactor).roundToInt()
        return x <= tx && tx < x + width && y <= ty && ty < y + height
    }

    private class RenderResult(viewportWidth: Int, viewportHeight: Int) : AutoCloseable {
        val color: GpuTexture = GpuTexture(viewportWidth, viewportHeight, GpuTexture.Format.RGBA8)
        val depth: GpuTexture = GpuTexture(viewportWidth, viewportHeight, GpuTexture.Format.DEPTH32)

        override fun close() {
            color.close()
            depth.close()
        }
    }

    private class Resources(val viewportWidth: Int, val viewportHeight: Int) : AutoCloseable {
        val mainTextureCopy = GpuTexture(viewportWidth, viewportHeight, GpuTexture.Format.RGBA8)
        val compositeRenderResult = RenderResult(viewportWidth, viewportHeight)
        val renderResults = mutableMapOf<CosmeticId, RenderResult>()
        val unusedRenderResults = mutableListOf<RenderResult>()

        val fallbackFrameBuffer: GlFrameBuffer? =
            // MC prior to 1.16 does not use a depth texture with its framebuffer, so we need to use a framebuffer of
            // our own on those versions
            if (platform.mcFrameBufferDepthTexture != null) null
            else GlFrameBuffer(viewportWidth, viewportHeight, depthFormat = GpuTexture.Format.DEPTH32)

        fun createRenderResult() =
            unusedRenderResults.removeLastOrNull() ?: RenderResult(viewportWidth, viewportHeight)

        fun freeRenderResults() {
            unusedRenderResults.addAll(renderResults.values)
            renderResults.clear()
        }

        override fun close() {
            mainTextureCopy.close()
            compositeRenderResult.close()
            renderResults.forEach { it.value.close() }
            unusedRenderResults.forEach { it.close() }
            fallbackFrameBuffer?.close()
        }
    }

    companion object {
        var active: CosmeticHoverOutlineEffect? = null
            private set

        private val resourceCleaner = ResourceCleaner<CosmeticHoverOutlineEffect>()

        private fun GpuTexture.readHoveredDepth(): Float = readPixelDepth(
            (UMouse.Scaled.x * UResolution.scaleFactor).toInt(),
            UResolution.viewportHeight - (UMouse.Scaled.y * UResolution.scaleFactor).toInt(),
        ).let { if (platform.usesReversedZ) 1 - it else it }

        private val vertexShaderSource = """
            #version 120
            varying vec2 texCoord;
            void main(){
                gl_Position = vec4(gl_Vertex.xy * 2.0 - vec2(1.0), 0.5, 1.0);
                texCoord = gl_Vertex.xy;
            }
        """.trimIndent()

        private val compositeFragmentShaderSource = """
            #version 120
            uniform sampler2D ColorSampler;
            uniform sampler2D DepthSampler;
            varying vec2 texCoord;
            void main() {
                vec4 color = texture2D(ColorSampler, texCoord);
                if (color.a == 0.0) {
                    discard;
                }
                gl_FragColor = color;
                gl_FragDepth = texture2D(DepthSampler, texCoord).r;
            }
        """.trimIndent()

        private val COMPOSITE_PIPELINE = URenderPipeline.builderWithLegacyShader(
            "essential:cosmetic_hover_outline_composite",
            UGraphics.DrawMode.QUADS,
            UGraphics.CommonVertexFormats.POSITION_TEXTURE,
            vertexShaderSource,
            compositeFragmentShaderSource,
        ).apply {
            blendState = BlendState.PREMULTIPLIED_ALPHA
            depthTest = if (platform.usesReversedZ) URenderPipeline.DepthTest.GreaterOrEqual else URenderPipeline.DepthTest.LessOrEqual
        }.build()

        private val outlineFragmentShaderSource = """
            #version 120
            uniform sampler2D CompositeSampler;
            uniform sampler2D TargetSampler;
            uniform vec2 OneTexel;
            uniform int OutlineWidth;
            varying vec2 texCoord;
            
            float depth2D(sampler2D s, vec2 coord) {
                return ${if (platform.usesReversedZ) "1 -" else ""} texture2D(s, coord).r;
            }
            
            vec4 query(vec2 offset) {
                float composite = depth2D(CompositeSampler, texCoord + offset);
                float depth = depth2D(TargetSampler, texCoord + offset);
                if (depth > 0.99 || composite < depth) {
                    return vec4(0, 0, 0, 0);
                } else {
                    return vec4(1, 1, 1, 1);
                }
            }
            void main() {
                vec4 fragColor;
                
                bool isInside = false;
                bool shouldRender = false;
                
                for (int x = -OutlineWidth; x < OutlineWidth; x++) {
                    for (int y = -OutlineWidth; y < OutlineWidth; y++) {
                        vec2 d = vec2(float(x) * OneTexel.x, float(y) * OneTexel.y);
                        float value = query(d).a;
                        if (x == 0 && y == 0 && value == 1) {
                            isInside = true;
                        }
                        if (value == 1) {
                            shouldRender = true;
                        }
                    }
                }
                if (shouldRender && !isInside) {
                    fragColor = vec4(1, 1, 1, 1);
                } else {
                    fragColor = vec4(0, 0, 0, 0);
                }
                
                float fragDepth;
                {
                    float center = depth2D(TargetSampler, texCoord);
                    float left = depth2D(TargetSampler, texCoord - vec2(OneTexel.x, 0.0));
                    float right = depth2D(TargetSampler, texCoord + vec2(OneTexel.x, 0.0));
                    float up = depth2D(TargetSampler, texCoord - vec2(0.0, OneTexel.y));
                    float down = depth2D(TargetSampler, texCoord + vec2(0.0, OneTexel.y));
                    fragDepth = min(center, min(min(left, right), min(up, down)));
                }
                
                gl_FragColor = fragColor;
                gl_FragDepth = fragDepth;
            }
        """.trimIndent()

        private val OUTLINE_PIPELINE = URenderPipeline.builderWithLegacyShader(
            "essential:cosmetic_hover_outline",
            UGraphics.DrawMode.QUADS,
            UGraphics.CommonVertexFormats.POSITION_TEXTURE,
            vertexShaderSource,
            outlineFragmentShaderSource,
        ).apply {
            blendState = BlendState.NORMAL
            depthTest = URenderPipeline.DepthTest.Always
        }.build()
    }
}
