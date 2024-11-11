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
package gg.essential.gui.elementa.state.v2.collections

import gg.essential.elementa.state.v2.ReferenceHolder
import gg.essential.gui.elementa.state.v2.ListState
import gg.essential.gui.elementa.state.v2.effect

// FIXME this is assuming there are no duplicate keys (good enough for now)
fun <T, K, V> ListState<T>.asMap(owner: ReferenceHolder, block: (T) -> Pair<K, V>): Map<K, V> {
    var oldList = get()
    val map = oldList.associateTo(mutableMapOf(), block)
    val keys = map.keys.toMutableList()
    onSetValue(owner) { newList ->
        val changes = newList.getChangesSince(oldList).also { oldList = newList }
        for (change in changes) {
            when (change) {
                is TrackedList.Add -> {
                    val (k, v) = block(change.element.value)
                    keys.add(change.element.index, k)
                    map[k] = v
                }
                is TrackedList.Remove -> {
                    map.remove(keys.removeAt(change.element.index))
                }
                is TrackedList.Clear -> {
                    map.clear()
                    keys.clear()
                }
            }
        }
    }
    return map
}

fun <T> ListState<T>.effectOnChange(
    referenceHolder: ReferenceHolder,
    onChange: (TrackedList.Change<T>) -> Unit,
) {
    var oldList = trackedListOf<T>()
    effect(referenceHolder) {
        val newList = this@effectOnChange()
        val changes = newList.getChangesSince(oldList).also { oldList = newList }
        for (change in changes) {
            onChange(change)
        }
    }
}

fun <T> ListState<T>.effectOnChange(
    referenceHolder: ReferenceHolder,
    add: (IndexedValue<T>) -> Unit,
    remove: (IndexedValue<T>) -> Unit,
    clear: (List<T>) -> Unit = { list -> list.forEach { remove(IndexedValue(0, it)) } },
) {
    effectOnChange(referenceHolder) { change ->
        when (change) {
            is TrackedList.Add -> add(change.element)
            is TrackedList.Remove -> remove(change.element)
            is TrackedList.Clear -> clear(change.oldElements)
        }
    }
}

fun <T> trackedListOf(vararg elements: T) : TrackedList<T> = MutableTrackedList(elements.toMutableList())

fun <T> mutableTrackedListOf(vararg elements: T): MutableTrackedList<T> = MutableTrackedList(elements.toMutableList())