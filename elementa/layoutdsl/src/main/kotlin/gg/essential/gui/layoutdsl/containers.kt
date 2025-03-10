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
@file:OptIn(ExperimentalContracts::class)

package gg.essential.gui.layoutdsl

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.WidthConstraint
import gg.essential.elementa.dsl.boundTo
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.coerceAtLeast
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixels
import gg.essential.gui.common.HollowUIContainer
import gg.essential.gui.common.constraints.AlternateConstraint
import gg.essential.gui.common.constraints.SpacedCramSiblingConstraint
import gg.essential.gui.elementa.state.v2.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun LayoutScope.box(modifier: Modifier = Modifier, block: LayoutScope.() -> Unit = {}): UIComponent {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val container = TransparentBlock().apply {
        automaticComponentName("box")
        setWidth(ChildBasedSizeConstraint())
        setHeight(ChildBasedSizeConstraint())
    }
    container.addChildModifier(Modifier.alignHorizontal(Alignment.Center).alignVertical(Alignment.Center))
    return container(modifier = modifier, block = block)
}

fun LayoutScope.row(horizontalArrangement: Arrangement = Arrangement.spacedBy(), verticalAlignment: Alignment = Alignment.Center, block: LayoutScope.() -> Unit): UIComponent {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return row(Modifier, horizontalArrangement, verticalAlignment, block)
}
fun LayoutScope.row(modifier: Modifier, horizontalArrangement: Arrangement = Arrangement.spacedBy(), verticalAlignment: Alignment = Alignment.Center, block: LayoutScope.() -> Unit): UIComponent {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val rowContainer = TransparentBlock().apply {
        automaticComponentName("row")
        setWidth(ChildBasedSizeConstraint())
        setHeight(ChildBasedMaxSizeConstraint())
    }

    rowContainer.addChildModifier(Modifier.alignVertical(verticalAlignment))

    rowContainer(modifier = modifier, block = block)
    horizontalArrangement.initialize(rowContainer, Axis.HORIZONTAL)

    return rowContainer
}

fun LayoutScope.column(verticalArrangement: Arrangement = Arrangement.spacedBy(), horizontalAlignment: Alignment = Alignment.Center, block: LayoutScope.() -> Unit): UIComponent {
    return column(Modifier, verticalArrangement, horizontalAlignment, block)
}
fun LayoutScope.column(modifier: Modifier, verticalArrangement: Arrangement = Arrangement.spacedBy(), horizontalAlignment: Alignment = Alignment.Center, block: LayoutScope.() -> Unit): UIComponent {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val columnContainer = TransparentBlock().apply {
        automaticComponentName("column")
        setWidth(ChildBasedMaxSizeConstraint())
        setHeight(ChildBasedSizeConstraint())
    }

    columnContainer.addChildModifier(Modifier.alignHorizontal(horizontalAlignment))

    columnContainer(modifier = modifier, block = block)
    verticalArrangement.initialize(columnContainer, Axis.VERTICAL)

    return columnContainer
}

fun LayoutScope.flowContainer(
    modifier: Modifier = Modifier,
    // TODO ideally we can make this use Arrangement on a per-row basis, currently it's just always SpaceBetween
    minSeparation: () -> WidthConstraint = { 0.pixels },
    verticalSeparation: () -> WidthConstraint = { 0.pixels },
    block: LayoutScope.() -> Unit = {},
): UIComponent {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val flowContainer = TransparentBlock().apply {
        automaticComponentName("flowContainer")
        setHeight(ChildBasedSizeConstraint())
    }

    val childModifier = Modifier
        .then(BasicXModifier { SpacedCramSiblingConstraint(minSeparation(), 0.pixels) })
        .then(BasicYModifier { SpacedCramSiblingConstraint(minSeparation(), 0.pixels, verticalSeparation()) })
    flowContainer.addChildModifier(childModifier)

    flowContainer(modifier = modifier, block = block)

    return flowContainer
}

fun LayoutScope.scrollable(
    modifier: Modifier = Modifier,
    horizontal: Boolean = false,
    vertical: Boolean = false,
    pixelsPerScroll: Float = 15f,
    block: LayoutScope.() -> Unit = {},
): ScrollComponent {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    if (!horizontal && !vertical) {
        throw IllegalArgumentException("Either `horizontal` or `vertical` or both must be `true`.")
    }

    val outer = ScrollComponent(
        horizontalScrollEnabled = horizontal,
        verticalScrollEnabled = vertical,
        pixelsPerScroll = pixelsPerScroll,
    )
    val inner = outer.children.first()
    // Need an extra wrapper because ScrollComponent does stupid things which breaks padding in the inner component
    val content = HollowUIContainer() childOf outer // actually adds to `inner` because ScrollComponent redirects it

    outer.apply {
        automaticComponentName("scrollable")
        setWidth(ChildBasedSizeConstraint() boundTo content)
        setHeight(ChildBasedSizeConstraint() boundTo content)
    }
    inner.apply {
        componentName = "scrollableInternal"
        setWidth(100.percent boundTo content)
        setHeight(100.percent boundTo content)
    }
    content.apply {
        componentName = "scrollableContent"
        setWidth(AlternateConstraint(ChildBasedSizeConstraint(), 100.percent boundTo outer).coerceAtLeast(AlternateConstraint(100.percent boundTo outer, 0.pixels)))
        setHeight(AlternateConstraint(ChildBasedSizeConstraint(), 100.percent boundTo outer).coerceAtLeast(AlternateConstraint(100.percent boundTo outer, 0.pixels)))
        addChildModifier(Modifier.alignBoth(Alignment.Center))
    }

    outer(modifier = modifier)

    block(LayoutScope(content, this, content))

    return outer
}

fun LayoutScope.floatingBox(
    modifier: Modifier = Modifier,
    floating: State<Boolean> = stateOf(true),
    block: LayoutScope.() -> Unit = {},
): UIComponent {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val box = box(modifier, block)
    box.automaticComponentName("floatingBox")
    effect(box) {
        box.isFloating = floating()
    }
    return box
}
