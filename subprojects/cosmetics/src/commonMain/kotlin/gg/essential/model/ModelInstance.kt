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
package gg.essential.model

import gg.essential.cosmetics.CosmeticsState
import gg.essential.cosmetics.events.AnimationTarget
import gg.essential.cosmetics.state.EssentialAnimationSystem
import gg.essential.cosmetics.state.TextureAnimationSync
import gg.essential.cosmetics.state.WearableLocator
import gg.essential.model.EnumPart.Companion.fromBoneName
import gg.essential.model.backend.PlayerPose
import gg.essential.model.backend.RenderBackend
import gg.essential.model.molang.MolangQueryEntity
import gg.essential.model.util.UMatrixStack
import gg.essential.network.cosmetics.Cosmetic

class ModelInstance(
    var model: BedrockModel,
    val entity: MolangQueryEntity,
    val animationTargets: Set<AnimationTarget>,
    val onAnimation: (String) -> Unit,
) {
    var locator = WearableLocator(entity.locator)
    var animationState = ModelAnimationState(entity, locator)
    var textureAnimationSync = TextureAnimationSync(model.textureFrameCount)
    var essentialAnimationSystem = EssentialAnimationSystem(model, entity, animationState, textureAnimationSync, animationTargets, onAnimation)

    fun switchModel(newModel: BedrockModel) {
        val newTextureAnimation = model.textureFrameCount != newModel.textureFrameCount
        val newAnimations = model.animations != newModel.animations || model.animationEvents != newModel.animationEvents

        model = newModel

        if (newTextureAnimation) {
            textureAnimationSync = TextureAnimationSync(model.textureFrameCount)
        }
        if (newAnimations) {
            locator.isValid = false
            locator = WearableLocator(entity.locator)
            animationState = ModelAnimationState(entity, locator)
        }
        if (newAnimations || newTextureAnimation) {
            essentialAnimationSystem = EssentialAnimationSystem(model, entity, animationState, textureAnimationSync, animationTargets, onAnimation)
        }
    }

    val cosmetic: Cosmetic
        get() = model.cosmetic

    fun computePose(basePose: PlayerPose): PlayerPose {
        return model.computePose(basePose, animationState, entity)
    }

    /**
     * Updates all locators bound to this model instance.
     *
     * Note that this method must called for all models, even those that were not actually rendered (e.g. because
     * the corresponding player was frustum culled), so that particles bound to locators (which may be visible even
     * when the player entity that spawned them is not) are update correctly.
     * In such cases where no reliable pose information is available, pass `null`.
     */
    fun updateLocators(renderedPose: PlayerPose?, state: CosmeticsState) {
        // Locators are fairly expensive to update, so only do it if we need to
        if (animationState.locatorsNeedUpdating()) {
            val pose = renderedPose
                // No way for us to get the real pose if we didn't actually render, let's just use the neutral pose
                ?: PlayerPose.neutral().copy(
                    // Also no way to know if cape/elytra/etc. are visible (not if you consider modded items anyway),
                    // so we'll move those far away so any events they spawn won't be visible.
                    rightShoulderEntity = PlayerPose.Part.MISSING,
                    leftShoulderEntity = PlayerPose.Part.MISSING,
                    rightWing = PlayerPose.Part.MISSING,
                    leftWing = PlayerPose.Part.MISSING,
                    cape = PlayerPose.Part.MISSING,
                )
            val rootBone = model.rootBone
            animationState.apply(rootBone, false)
            model.applyPose(rootBone, pose, entity)

            // process visibility and sided-ness from cosmetic state for Locator.isVisible update
            model.propagateVisibilityToRootBone(
                state.sides[cosmetic.id],
                rootBone,
                state.hiddenBones[cosmetic.id] ?: emptySet(),
                EnumPart.values().toSet() - state.hiddenParts.getOrDefault(cosmetic.id, emptySet()),
            )

            animationState.updateLocators(rootBone, 1 / 16f)
        }
    }

    fun render(
        matrixStack: UMatrixStack,
        vertexConsumerProvider: RenderBackend.VertexConsumerProvider,
        rootBone: Bone,
        renderMetadata: RenderMetadata,
    ) {
        animationState.apply(rootBone, false)

        for (bone in model.getBones(rootBone)) {
            if (fromBoneName(bone.boxName) != null) {
                bone.userOffsetX = renderMetadata.positionAdjustment.x
                bone.userOffsetY = renderMetadata.positionAdjustment.y
                bone.userOffsetZ = renderMetadata.positionAdjustment.z
            }
        }

        model.render(
            matrixStack,
            vertexConsumerProvider,
            rootBone,
            entity,
            renderMetadata,
            textureAnimationSync.getAdjustedLifetime(entity.lifeTime),
        )
    }
}