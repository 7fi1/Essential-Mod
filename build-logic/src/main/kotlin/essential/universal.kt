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
package essential

import gg.essential.gradle.multiversion.StripReferencesTransform
import gg.essential.gradle.util.KotlinVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.attributes.Attribute
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

fun Project.universalLibs() {
    val versionCatalogs = extensions.getByType<VersionCatalogsExtension>()
    val catalog = versionCatalogs.named("libs")

    fun getVersion(name: String) = catalog.findVersion(name).orElseThrow()

    dependencies {
        val compileOnly = "compileOnly"

        val universalAttr = Attribute.of("universal", Boolean::class.javaObjectType)

        registerTransform(StripReferencesTransform::class.java) {
            from.attribute(universalAttr, false)
            to.attribute(universalAttr, true)
            parameters {
                excludes.add("net.minecraft")
            }
        }

        artifactTypes.all {
            attributes.attribute(universalAttr, false)
        }

        val kotlin = KotlinVersion.minimal
        compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlin.stdlib}")
        compileOnly("org.jetbrains.kotlin:kotlin-reflect:${kotlin.stdlib}")
        compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:${kotlin.coroutines}")
        compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${kotlin.coroutines}")
        compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-core:${kotlin.serialization}")
        compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:${kotlin.serialization}")
        compileOnly("org.jetbrains:annotations:23.0.0")

        // Provided by MC on 1.17+, and by `:slf4j-to-log4j` on older versions
        compileOnly("org.slf4j:slf4j-api:1.7.36")
        // Provided by MC (should ideally migrate away from this as MC itself is migrating to slf4j)
        compileOnly("org.apache.logging.log4j:log4j-api:2.0-beta9")
        // Depending on LWJGL3 instead of 2 so we can choose opengl bindings only
        // Note that this will still include some methods that are not available on LWJGL2. If these are used by mistake,
        // it should be obvious when the code is tested as long as our main version is still 1.12.2. If we change that,
        // we should maybe considering changing this to lwjgl2 or building a small transformer that produces a jar
        // containing only methods and classes that are in both versions.
        compileOnly("org.lwjgl:lwjgl-opengl:3.3.1")
        // Depending on 1.8.9 for all of these because that's the oldest version we support
        compileOnly("com.google.code.gson:gson:2.2.4")
        compileOnly("commons-codec:commons-codec:1.9")
        compileOnly("org.apache.httpcomponents:httpclient:4.3.3") // TODO ideally switch to one of the libs we bundle
        // These versions are configured in gradle/libs.versions.toml
        compileOnly("gg.essential:vigilance-1.8.9-forge:${getVersion("vigilance")}") {
            attributes { attribute(universalAttr, true) }
            isTransitive = false
        }
        compileOnly("gg.essential:universalcraft-1.8.9-forge:${getVersion("universalcraft")}") {
            attributes { attribute(universalAttr, true) }
            isTransitive = false
        }
        compileOnly("gg.essential:elementa-1.8.9-forge:${getVersion("elementa")}") {
            attributes { attribute(universalAttr, true) }
            isTransitive = false
        }
    }
}
