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
package gg.essential.gradle

import gg.essential.gradle.multiversion.Platform
import essential.mixin
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources

open class MixinPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val platform = project.extensions.getByType<Platform>()

        project.configureMixin(platform)
    }
}

private fun Project.configureMixin(platform: Platform) {
    // For versions which use mojmap at runtime, there is no need for refmap files and therefore the mixin AP
    val usesMojmapAtRuntime = platform.isNeoForge || (platform.isForge && platform.mcVersion >= 12006) || platform.isUnobfuscated
    if (usesMojmapAtRuntime) {
        tasks.named<ProcessResources>("processResources") {
            filesMatching("mixins.*.json") {
                filter { line -> if ("\"refmap\":" in line) "" else line }
            }
        }
        return
    }

    configureLoomMixin()

    if (!platform.isFabric) {
        addMixinDependency(platform)
    }
}

private fun Project.configureLoomMixin() {
    extensions.configure<LoomGradleExtensionAPI> {
        mixin {
            useLegacyMixinAp.set(true) // TODO ideally migrate away from this
            defaultRefmapName.set("mixins.essential.refmap.json")
        }
    }
}

private fun Project.addMixinDependency(platform: Platform) {
    repositories {
        mixin()
    }

    dependencies {
        if (platform.mcVersion < 11400) {
            // Our special mixin which has its Guava 21 dependency relocated, so it can run alongside Guava 17
            "jij"("gg.essential:mixin")

            if (!System.getProperty("idea.sync.active", "false").toBoolean()) {
                "implementation"("gg.essential:mixin")
            } else {
                // IntelliJ by default doesn't use the actual patched mixin jar which our project produces
                // but instead includes its dependencies (unpatched mixin) and source folders (the patches).
                // The patches are usually applied via a task at build time, but IntelliJ circumvents that.
                // To trick IntelliJ into using the patched mixin, we'll, instead of directly depending
                // on `gg.essential:mixin` (which would be resolved to a subproject), manually resolve that
                // dependency to a file (the patched jar), and then depend on that file.
                val patchedFile = configurations.detachedConfiguration(
                    // FIXME this currently needs `isTransitive = false` because the mixin project inappropriately
                    //       exposes the unpatched mixin jar via its `apiElements` as well.
                    dependencies.create("gg.essential:mixin") { isTransitive = false }
                ).incoming.artifacts.artifactFiles
                "implementation"(patchedFile)
                // On a fresh checkout, the patched jar doesn't exist yet (and on an updated checkout, it may be
                // outdated), so we'll additionally ask for it to be built by Gradle on any IDEA sync.
                val prepareEssentialLoader by tasks.registering {
                    inputs.files(patchedFile)
                    outputs.upToDateWhen { false }
                }
                // the `ideaSyncTask` task is provided by Loom and automatically ran on sync
                tasks.named("ideaSyncTask") { dependsOn(prepareEssentialLoader) }
            }
        }

        // Use more recent mixin AP so we get reproducible refmaps (and hopefully less bugs in general)
        if (!System.getProperty("idea.sync.active", "false").toBoolean()) {
            "annotationProcessor"("net.fabricmc:sponge-mixin:0.12.5+mixin.0.8.5")
        }
    }
}
