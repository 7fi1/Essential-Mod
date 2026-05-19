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
package gg.essential.network.connectionmanager.cosmetics

import gg.essential.cosmetics.CosmeticBundleId
import gg.essential.cosmetics.CosmeticCategoryId
import gg.essential.cosmetics.CosmeticId
import gg.essential.cosmetics.FeaturedPageCollectionId
import gg.essential.cosmetics.ImplicitOwnership
import gg.essential.cosmetics.ImplicitOwnershipId
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.collections.asMap
import gg.essential.gui.elementa.state.v2.combinators.letState
import gg.essential.mod.cosmetics.CosmeticBundle
import gg.essential.mod.cosmetics.CosmeticCategory
import gg.essential.mod.cosmetics.featured.FeaturedPageCollection
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.util.logExceptions
import java.util.concurrent.CompletableFuture

class CosmeticsDataWithChanges(
    val inner: CosmeticsData,
) : CosmeticsData {

    private val updatedCosmetics = mutableStateOf(mapOf<CosmeticId, Cosmetic?>())
    private val updatedCategories = mutableStateOf(mapOf<CosmeticCategoryId, CosmeticCategory?>())
    private val updatedBundles = mutableStateOf(mapOf<CosmeticBundleId, CosmeticBundle?>())
    private val updatedFeaturedPageCollections = mutableStateOf(mapOf<FeaturedPageCollectionId, FeaturedPageCollection?>())
    private val updatedImplicitOwnerships = mutableStateOf(mapOf<ImplicitOwnershipId, ImplicitOwnership?>())

    override val cosmetics: ListState<Cosmetic> = stateBy {
        val originals = inner.cosmetics().associateBy { it.id }
        val updates = updatedCosmetics()
        (originals + updates).mapNotNull { it.value }
    }.toListState()

    override val categories: ListState<CosmeticCategory> = stateBy {
        val originals = inner.categories().associateBy { it.id }
        val updates = updatedCategories()
        (originals + updates).mapNotNull { it.value }
    }.toListState()

    override val bundles: ListState<CosmeticBundle> = stateBy {
        val originals = inner.bundles().associateBy { it.id }
        val updates = updatedBundles()
        (originals + updates).mapNotNull { it.value }
    }.toListState()

    override val featuredPageCollections: ListState<FeaturedPageCollection> = stateBy {
        val originals = inner.featuredPageCollections().associateBy { it.id }
        val updates = updatedFeaturedPageCollections()
        (originals + updates).mapNotNull { it.value }
    }.toListState()

    override val implicitOwnerships: ListState<ImplicitOwnership> = stateBy {
        val originals = inner.implicitOwnerships().associateBy { it.id }
        val updates = updatedImplicitOwnerships()
        (originals + updates).mapNotNull { it.value }
    }.toListState()

    private val refHolder = ReferenceHolderImpl()
    private val categoriesMap = categories.asMap(refHolder) { it.id to it }
    private val bundlesMap = bundles.asMap(refHolder) { it.id to it }
    private val featuredPageCollectionsMap = featuredPageCollections.asMap(refHolder) { it.id to it }
    private val implicitOwnershipsMap = implicitOwnerships.asMap(refHolder) { it.id to it }
    private val cosmeticsMap = cosmetics.asMap(refHolder) { it.id to it }

    fun updateCosmetic(id: CosmeticId, cosmetic: Cosmetic?) {
        if (inner.getCosmetic(id) != cosmetic) {
            updatedCosmetics.set { it + (id to cosmetic) }
        } else {
            updatedCosmetics.set { it - id }
        }
    }

    fun updateCategory(id: CosmeticCategoryId, category: CosmeticCategory?) {
        if (inner.getCategory(id) != category) {
            updatedCategories.set { it + (id to category) }
        } else {
            updatedCategories.set { it - id }
        }
    }

    fun updateBundle(id: CosmeticBundleId, bundle: CosmeticBundle?) {
        if (inner.getCosmeticBundle(id) != bundle) {
            updatedBundles.set { it + (id to bundle) }
        } else {
            updatedBundles.set { it - id }
        }
    }

    fun updateFeaturedPageCollection(id: FeaturedPageCollectionId, featuredPageCollection: FeaturedPageCollection?) {
        if (inner.getFeaturedPageCollection(id) != featuredPageCollection) {
            updatedFeaturedPageCollections.set { it + (id to featuredPageCollection) }
        } else {
            updatedFeaturedPageCollections.set { it - id }
        }
    }

    fun updateImplicitOwnership(id: ImplicitOwnershipId, implicitOwnership: ImplicitOwnership?) {
        if (inner.getImplicitOwnership(id) != implicitOwnership) {
            updatedImplicitOwnerships.set { it + (id to implicitOwnership) }
        } else {
            updatedImplicitOwnerships.set { it - id }
        }
    }

    fun writeChangesToLocalCosmeticData(localCosmeticsData: LocalCosmeticsData): CompletableFuture<Unit> {
        return localCosmeticsData.writeChanges(
            categories = categoriesMap + updatedCategories.get().filterValues { it == null },
            bundles = bundlesMap + updatedBundles.get().filterValues { it == null },
            featuredPageCollections = featuredPageCollectionsMap + updatedFeaturedPageCollections.get().filterValues { it == null },
            implicitOwnerships = implicitOwnershipsMap + updatedImplicitOwnerships.getUntracked().filterValues { it == null },
            cosmetics = cosmeticsMap + updatedCosmetics.get().filterValues { it == null },
        ).thenApply {
            resetLocalChanges()
        }.logExceptions()
    }

    fun resetLocalChanges() {
        updatedCosmetics.set(emptyMap())
        updatedCategories.set(emptyMap())
        updatedBundles.set(emptyMap())
        updatedFeaturedPageCollections.set(emptyMap())
        updatedImplicitOwnerships.set(emptyMap())
    }

    fun getUpdatesSummary() = memo {
        val updatedCosmetics = updatedCosmetics()
        val updatedCategories = updatedCategories()
        val updatedBundles = updatedBundles()
        val updatedFeaturedPageCollections = updatedFeaturedPageCollections()
        if(updatedCosmetics.isEmpty() && updatedCategories.isEmpty() && updatedBundles.isEmpty() && updatedFeaturedPageCollections.isEmpty()) {
            return@memo null
        }
        val cosmetics = updatedCosmetics.entries.joinToString("\n") { "${it.key} - " + if (it.value == null) "Removed" else "Changed" }
        val categories = updatedCategories.entries.joinToString("\n") { "${it.key} - " + if (it.value == null) "Removed" else "Changed" }
        val bundles = updatedBundles.entries.joinToString("\n") { "${it.key} - " + if (it.value == null) "Removed" else "Changed" }
        val featuredPageCollections = updatedFeaturedPageCollections.entries.joinToString("\n") { "${it.key} - " + if (it.value == null) "Removed" else "Changed" }
        val cosmeticsText = if (cosmetics.isNotBlank()) "\nCosmetics:\n$cosmetics" else ""
        val categoriesText = if (categories.isNotBlank()) "\nCategories:\n$categories" else ""
        val bundlesText = if (bundles.isNotBlank()) "\nBundles:\n$bundles" else ""
        val featuredPageCollectionsText = if (featuredPageCollections.isNotBlank()) "\nFeatured Pages:\n$featuredPageCollections" else ""
        "Changes:$cosmeticsText$categoriesText$bundlesText$featuredPageCollectionsText"
    }

    override fun getCosmetic(id: CosmeticId): Cosmetic? = cosmeticsMap[id]
    override fun getCategory(id: CosmeticCategoryId): CosmeticCategory? = categoriesMap[id]
    override fun getCosmeticBundle(id: CosmeticBundleId): CosmeticBundle? = bundlesMap[id]
    override fun getFeaturedPageCollection(id: FeaturedPageCollectionId): FeaturedPageCollection? = featuredPageCollectionsMap[id]
    override fun getImplicitOwnership(id: ImplicitOwnershipId): ImplicitOwnership? = implicitOwnershipsMap[id]

    override fun cosmetic(id: CosmeticId): State<Cosmetic?> =
        cosmetics.letState { cosmetics -> cosmetics.find { it.id == id } ?: inner.cosmetic(id)() }

}
