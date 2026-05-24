import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class UpdateVersionTask : DefaultTask() {
    init {
        description = "Update version in all documentation files"
        group = "release"
    }

    @TaskAction
    fun updateVersion() {
        // Read version from gradle.properties
        val propsFile = File(project.rootDir, "DrawBox/gradle.properties")
        val properties = java.util.Properties().apply {
            load(propsFile.inputStream())
        }
        val version = properties.getProperty("VERSION_NAME")
            ?: throw Exception("VERSION_NAME not found in DrawBox/gradle.properties")

        println("📝 Updating version references to: $version")

        // Files to update
        val filesToUpdate = listOf(
            "README.md",
            "docs/index.md",
            "docs/getting-started/installation.md",
            "docs/getting-started/quickstart.md"
        )

        var updatedFiles = 0
        filesToUpdate.forEach { filePath ->
            val file = File(project.rootDir, filePath)
            if (!file.exists()) {
                println("⚠️  File not found: $filePath")
                return@forEach
            }

            val content = file.readText()

            // For markdown files, update implementation examples
            val updatedContent = when {
                filePath.endsWith(".md") -> {
                    // Update version in gradle examples
                    content
                        .replace(Regex("""implementation\("io\.ak1:drawbox:\S+"\)""")) {
                            """implementation("io.ak1:drawbox:$version")"""
                        }
                        .replace(Regex("""implementation 'io\.ak1:drawbox:\S+'""")) {
                            """implementation 'io.ak1:drawbox:$version'"""
                        }
                        .replace(Regex("""<version>\S+</version>""")) {
                            "<version>$version</version>"
                        }
                        .replace(Regex("""rev='drawbox:?\s+\S+'""")) {
                            """rev='drawbox:$version'"""
                        }
                        .replace(Regex("""Latest Version: \*\*\S+\*\*""")) {
                            """Latest Version: **$version**"""
                        }
                }
                filePath.endsWith("libs.versions.toml") -> {
                    // Update version in TOML
                    content.replace(Regex("""drawbox\s*=\s*"\S+"""")) {
                        """drawbox = "$version""""
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
