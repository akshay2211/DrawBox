plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidLint) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.vanniktech.maven.publish) apply false
    alias(libs.plugins.roborazzi) apply false
}

tasks.register("updateVersion") {
    description = "Update version in all documentation files from DrawBox/gradle.properties"
    group = "release"
    notCompatibleWithConfigurationCache("Modifies source files outside build directory")

    doLast {
        // Read version from DrawBox/gradle.properties
        val propsFile = File(rootDir, "DrawBox/gradle.properties")
        val properties = java.util.Properties().apply {
            load(propsFile.inputStream())
        }
        val version = properties.getProperty("VERSION_NAME")
            ?: throw Exception("❌ VERSION_NAME not found in DrawBox/gradle.properties")

        println("📝 Updating version references to: $version")

        // Files to update
        val filesToUpdate = mapOf(
            "README.md" to listOf("markdown"),
            "docs/index.md" to listOf("markdown"),
            "docs/getting-started/installation.md" to listOf("markdown"),
            "docs/getting-started/quickstart.md" to listOf("markdown"),
            "gradle/libs.versions.toml" to listOf("toml")
        )

        var updatedFiles = 0
        filesToUpdate.forEach { (filePath, _) ->
            val file = File(rootDir, filePath)
            if (!file.exists()) {
                println("⚠️  File not found: $filePath")
                return@forEach
            }

            val content = file.readText()
            val updatedContent = when {
                filePath.endsWith(".md") -> {
                    // Update version in gradle examples
                    content
                        .replace(Regex("""implementation\("io\.ak1:drawbox:[^"]+"\)""")) {
                            """implementation("io.ak1:drawbox:$version")"""
                        }
                        .replace(Regex("""implementation 'io\.ak1:drawbox:[^']+'""")) {
                            """implementation 'io.ak1:drawbox:$version'"""
                        }
                        .replace(Regex("""<version>.+?</version>""")) {
                            "<version>$version</version>"
                        }
                        .replace(Regex("""rev='.+?'>""")) {
                            "rev='$version'>"
                        }
                        .replace(Regex("""Latest Version: \*\*.+?\*\*""")) {
                            """Latest Version: **$version**"""
                        }
                        .replace(Regex("""\| \*\*Latest Version\*\* \| .+ \|""")) {
                            "| **Latest Version** | $version |"
                        }
                }
                filePath.endsWith("libs.versions.toml") -> {
                    // Update version in TOML
                    content.replace(Regex("""drawbox\s*=\s*"[^"]+" """)) {
                        """drawbox = "$version" """
                    }
                }
                else -> content
            }

            if (updatedContent != content) {
                file.writeText(updatedContent)
                println("✅ Updated: $filePath")
                updatedFiles++
            } else {
                println("⏭️  No changes needed: $filePath")
            }
        }

        println("\n✨ Version update complete! Updated $updatedFiles files")
    }
}