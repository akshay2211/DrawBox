import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.roborazzi)
}

// Skiko (the Skia bindings Compose/Roborazzi render JVM screenshots through)
// resolves its native runtime by the *publishing* host's classifier for a
// jvm()-only module, not the host running the tests. On Linux CI that means the
// linux-x64 .so is never on the classpath and every screenshot test dies at
// class-init with skiko LibraryLoadException ("proper native dependency
// missing"). Pin the runtime to the host actually executing the tests.
val skikoVersion = "0.144.6"
val skikoTarget = run {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val isArm = arch.contains("aarch64") || arch.contains("arm")
    when {
        os.contains("mac") || os.contains("darwin") -> if (isArm) "macos-arm64" else "macos-x64"
        os.contains("win") -> "windows-x64"
        else -> if (isArm) "linux-arm64" else "linux-x64"
    }
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    js {
        browser()
        binaries.executable()
    }

    android {
        namespace = "io.ak1.drawboxsample.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.roborazzi.compose.desktop)
            implementation(libs.compose.ui.test.junit4.desktop)
            // Native Skiko runtime for the host executing the tests (see note above).
            runtimeOnly("org.jetbrains.skiko:skiko-awt-runtime-$skikoTarget:$skikoVersion")
        }
        commonMain.dependencies {
            implementation(projects.drawBox)
            implementation(projects.drawboxUi)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.material.icons.core)
            implementation(libs.jetbrains.material.icons.extended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.navigation3.ui)
            implementation(libs.material3.adaptive.navigation3)
            implementation(libs.lifecycle.viewmodel.navigation3)
            implementation(libs.rang.vikalp)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
