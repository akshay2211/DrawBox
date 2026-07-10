import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.spotless)
    alias(libs.plugins.dokka)
    alias(libs.plugins.vanniktech.maven.publish)
}
kotlin {
    android {
        namespace = "io.ak1.drawbox.ui"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }
    js {
        browser()
        binaries.executable()
    }

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    val xcfName = "DrawBoxUI"
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = xcfName
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.drawBox)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.material.icons.core)
            implementation(libs.jetbrains.material.icons.extended)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.rang.vikalp)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "io.ak1.drawbox.ui.resources"
    generateResClass = auto
}

spotless {
    kotlin {
        target("src/**/*.kt")
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

signing {
    sign(publishing.publications)
}

mavenPublishing {
    pom {
        name.set("DrawBox UI")
        description.set(
            "Reusable Compose Multiplatform toolbars and controls for DrawBox — " +
            "context bar, controls bar, and floating toolbar building blocks. " +
            "Runs on Android, iOS, Web (WASM), and JVM from a single shared codebase."
        )
        inceptionYear.set("2026")
        url.set("https://github.com/akshay2211/DrawBox")

        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("akshay2211")
                name.set("Akshay Sharma")
                email.set("fxn769@gmail.com")
                url.set("https://akshay2211.github.io/")
            }
        }

        scm {
            url.set("https://github.com/akshay2211/DrawBox")
            connection.set("scm:git:git://github.com/akshay2211/DrawBox.git")
            developerConnection.set("scm:git:git@github.com:akshay2211/DrawBox.git")
        }

        issueManagement {
            system.set("Github")
            url.set("https://github.com/akshay2211/DrawBox/issues")
        }
    }
}