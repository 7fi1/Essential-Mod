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
import gg.essential.gradle.util.RelaxFabricLoaderDependencyTransform
import gg.essential.gradle.util.SlimKotlinForForgeTransform
import gg.essential.gradle.util.kotlinVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.kotlin.dsl.*
import essential.modrinth
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.api.tasks.bundling.Jar

data class Configurations(
    /**
     * Dependencies which will be exploded into our jar (and ideally, though not necessarily, relocated)
     */
    val bundle: Configuration,
    /**
     * Dependencies which will be packed into our jar without exploding (i.e. Jar-in-Jar) and then be unpacked
     * by our loader (or if supported by the platform loader)
     */
    val jij: Configuration,
)

open class BundlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val platform = project.extensions.getByType<Platform>()

        val configurations = project.createConfigurations(platform)
        project.createBundleJarTask(platform, configurations)
        when {
            platform.isFabric -> project.configureForFabricLoader(configurations, platform)
            platform.isModLauncher -> project.configureForModLauncher(configurations, platform)
        }
    }
}

private fun Project.createConfigurations(platform: Platform): Configurations {
    val bundle by configurations.creating {
        exclude(module = "fabric-loader") // specifying module only, so the yarn-mapped version in excluded as well
        exclude(group = "net.minecraftforge", module = "forge")
        if (platform.mcVersion >= 11700) {
            exclude(group = "org.slf4j", module = "slf4j-api")
        }
    }

    val jij by configurations.creating {
    }

    return Configurations(bundle, jij)
}

private fun Project.createBundleJarTask(platform: Platform, configurations: Configurations) = with(configurations) {
    val jar by tasks.existing(Jar::class)
    val remapJar by tasks.existing(RemapJarTask::class) {
        archiveClassifier.set("mapped")
        destinationDirectory.set(buildDir.resolve("devlibs"))
    }

    tasks.register<Jar>("bundleJar") {
        archiveClassifier.set("bundle")
        destinationDirectory.set(buildDir.resolve("devlibs"))

        manifest = jar.get().manifest
        from(remapJar.flatMap { it.archiveFile }.map { zipTree(it) })

        dependsOn(bundle)
        from({ bundle.map { if (it.isDirectory) it else zipTree(it) } }) {
            exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
            exclude("META-INF/services/javax.annotation.processing.Processor")

            // TODO these should not be published in the first place (did I already fix that?)
            exclude("gg.essential.vigilance.example.ExampleMod")
            exclude("dummyThing")

            // TODO figure out where these are coming from and stop that (content is an unhelpful "examplemod")
            exclude("pack.mcmeta")

            exclude("*LICENSE*")
            exclude("*license*")
            exclude("README.md") // FIXME why are these in UC suddenly? and why is that not an issue on release/1.2?

            // TODO I don't know how Java's module system works and if we need to somehow merge these.
            //      For the time being, just removing them should be fine as LaunchWrapper only supports Java 8 anyway.
            exclude("**/module-info.class")

            if (platform.isLegacyForge) {
                // Legacy Forge chokes on these (and they are useless for it anyway cause it only supports Java 8)
                exclude("**/module-info.class")
                exclude("META-INF/versions/9/**")
                // Same with these coming from Mixin for ModLauncher9 support
                exclude("org/spongepowered/asm/launch/MixinLaunchPlugin.class")
                exclude("org/spongepowered/asm/launch/MixinTransformationService.class")
                exclude("org/spongepowered/asm/launch/platform/container/ContainerHandleModLauncherEx*")
            }

            var i = 0
            filesMatching("META-INF/NOTICE*") { name += ".${i++}" }
            i = 0
            filesMatching("META-INF/LICENSE*") { name += ".${i++}" }

            // FIXME should ideally use JiJ but that gets tricky with loading at runtime
            exclude("fabric.mod.json")
        }

        from(jij) {
            rename { "META-INF/jars/$it" }
        }
    }
}

// On fabric-loader we use its Jar-in-Jar mechanism (indirectly via essential-loader) to load our libs. This allows
// everything to work well even if a third-party mod ships an older version of one of our libs thanks to fabric-loader
// always picking the most recent one of all JiJ jars.
private fun Project.configureForFabricLoader(configurations: Configurations, platform: Platform) = with(configurations) {
    val include by project.configurations
    include.exclude(group = "net.fabricmc", module = "fabric-loader") // can't upgrade this one via JiJ (unfortunately)

    val relaxedFabricLoaderDependency = Attribute.of("relaxed-fabric-loader-dependency", Boolean::class.javaObjectType)
    dependencies.registerTransform(RelaxFabricLoaderDependencyTransform::class.java) {
        from.attribute(relaxedFabricLoaderDependency, false)
        to.attribute(relaxedFabricLoaderDependency, true)
    }
    dependencies.artifactTypes.all {
        attributes.attribute(relaxedFabricLoaderDependency, false)
    }

    fun Configuration.excludeKotlin() {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains.kotlinx")
        exclude(group = "org.jetbrains", module = "annotations")
    }

    // For Kotlin, we instead bundle the official fabric-language-kotlin mod
    bundle.excludeKotlin()
    dependencies {
        val kotlin = platform.kotlinVersion
        include("net.fabricmc:fabric-language-kotlin:${kotlin.mod}+kotlin.${kotlin.stdlib}") {
            attributes {
                attribute(relaxedFabricLoaderDependency, true)
            }
        }
        // FLK doesn't include this but we need it for our commands API
        include("org.jetbrains:annotations:13.0")
    }

    afterEvaluate { // need to delay so repos and deps are all set up
        // Then we need to find all our mod jars
        project.configurations
            // declared in the `modApi` configuration of the corresponding api project
            .detachedConfiguration(dependencies.project(":api:" + project.name, configuration = "modApi"))
            .apply { excludeKotlin() } // kotlin is already taken care of
            .resolvedConfiguration
            .resolvedArtifacts
            .map { it.moduleVersion.id }
            .forEach {
                // exclude them from the bundled configuration
                bundle.exclude(module = it.name) // name-only so we get the remapped one as well
                // and instead add them to loom's include configuration
                dependencies {
                    include(group = it.group, name = it.name, version = it.version)
                }
            }
    }
}

// ModLauncher does not allow one package to be present in two different jars, and it does not give us any way to
// tell whether a package is already taken. The only way for us to not die at boot when another (e.g.) Kotlin mod is
// present, is to Jar-in-Jar (use a custom scheme, cause ModLauncher doesn't yet support that either) the most
// popular one (Java will pick the jar with the higher implementation-version. I hope. at least it seems to be fine with
// two jars declaring the same module) and then hope that everyone sticks with it.
private fun Project.configureForModLauncher(configurations: Configurations, platform: Platform) = with(configurations) {

    val slimKFF = Attribute.of("slim-kotlin-for-forge", Boolean::class.javaObjectType)
    dependencies.registerTransform(SlimKotlinForForgeTransform::class.java) {
        from.attribute(slimKFF, false)
        to.attribute(slimKFF, true)
    }
    dependencies.artifactTypes.all {
        attributes.attribute(slimKFF, false)
    }

    bundle.exclude(group = "org.jetbrains.kotlin")
    bundle.exclude(group = "org.jetbrains.kotlinx")
    bundle.exclude(group = "org.jetbrains", module = "annotations")
    repositories {
        modrinth()
    }
    dependencies {
        val kotlin = platform.kotlinVersion
        jij("maven.modrinth:kotlin-for-forge:${kotlin.mod}") {
            attributes {
                // Given we are about to JiJ updated Kotlin libraries, we can strip the old ones from KFF to reduce our
                // bundled jar size a bit (just need to make sure we bundle at least the same libs as KFF so we don't
                // break third-party mods that depend on those).
                attribute(slimKFF, true)
            }
        }

        jij("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlin.stdlib}")
        jij("org.jetbrains.kotlin:kotlin-reflect:${kotlin.stdlib}")
        jij("org.jetbrains.kotlinx:kotlinx-coroutines-core:${kotlin.coroutines}")
        jij("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${kotlin.coroutines}")
        jij("org.jetbrains.kotlinx:kotlinx-serialization-core:${kotlin.serialization}")
        jij("org.jetbrains.kotlinx:kotlinx-serialization-json:${kotlin.serialization}")
        jij.exclude(group = "org.jetbrains", module = "annotations")
    }
}
