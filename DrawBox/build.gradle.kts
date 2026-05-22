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
    androidLibrary {
        namespace = "io.ak1.drawbox"
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

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    val xcfName = "DrawBox"
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
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
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
