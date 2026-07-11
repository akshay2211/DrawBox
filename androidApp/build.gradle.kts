import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "io.ak1.drawboxsample"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.ak1.drawboxsample"
        minSdk = libs.versions.android.minSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

tasks.register<Copy>("copyComposeResources") {
    delete("src/main/assets/composeResources/drawboxsample.shared.generated.resources")
    from("../shared/src/commonMain/composeResources")
    into("src/main/assets/composeResources/drawboxsample.shared.generated.resources")
}

// drawbox-ui's Compose Resources package is "io.ak1.drawbox.ui.resources" (see
// its build.gradle.kts). The KMP-Android library plugin doesn't auto-bundle its
// compose resources into the consuming Android app's assets, so mirror them by
// hand — same workaround as the shared task above.
tasks.register<Copy>("copyDrawBoxUiComposeResources") {
    delete("src/main/assets/composeResources/io.ak1.drawbox.ui.resources")
    from("../drawbox-ui/src/commonMain/composeResources")
    into("src/main/assets/composeResources/io.ak1.drawbox.ui.resources")
}

tasks.named("preBuild") {
    dependsOn("copyComposeResources", "copyDrawBoxUiComposeResources")
}
