# Installation Guide

## Prerequisites

DrawBox requires:
- **Kotlin 1.9.0+** for Kotlin Multiplatform support
- **Gradle 8.0+** for building multiplatform projects
- **Compose Multiplatform 1.5.0+**

## Step 1: Add Maven Central Repository

In your project's `build.gradle.kts` or `settings.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
    // other repositories
}
```

## Step 2: Add DrawBox Dependency

### Kotlin DSL (Recommended)
```kotlin
dependencies {
    implementation("io.ak1:drawbox:2.0.0")
}
```

### Groovy DSL
```groovy
dependencies {
    implementation 'io.ak1:drawbox:2.0.0'
}
```

### Maven
```xml
<dependency>
  <groupId>io.ak1</groupId>
  <artifactId>drawbox</artifactId>
  <version>2.0.0</version>
</dependency>
```

### Ivy
```xml
<dependency org='io.ak1' name='drawbox' rev='2.0.0'>
  <artifact name='drawbox' ext='pom' ></artifact>
</dependency>
```

## Platform-Specific Setup

### Android

No additional setup required! DrawBox automatically includes the Android implementation.

```kotlin
// In your android {} block
android {
    compileSdk = 34  // or higher
    
    defaultConfig {
        minSdk = 21  // DrawBox supports API 21+
        targetSdk = 34
    }
}
```

### iOS

DrawBox is automatically included via the shared library framework.

```kotlin
// In your shared module build.gradle.kts
kotlin {
    iosArm64()
    iosSimulatorArm64()
}
```

### Desktop (JVM)

Add JVM target to your `build.gradle.kts`:

```kotlin
kotlin {
    jvm()
}
```

### Web (WebAssembly)

Add WASM target to your `build.gradle.kts`:

```kotlin
kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
}
```

## Verification

To verify DrawBox is properly installed:

```kotlin
import io.ak1.drawbox.DrawBox
import io.ak1.drawbox.presentation.viewmodel.DrawBoxController

// If these imports work, DrawBox is installed correctly!
```

## Troubleshooting

### Import Errors
If you get "Unresolved reference" errors:
1. Run `./gradlew clean`
2. Sync Gradle: In Android Studio, go to File > Sync Now
3. Invalidate cache: File > Invalidate Caches

### Version Conflicts
If you have dependency version conflicts:
```kotlin
dependencies {
    implementation("io.ak1:drawbox:2.0.0") {
        exclude(group = "androidx.compose.runtime", module = "runtime")
    }
}
```

### Platform-Specific Issues

**Android: Missing resources**
- Ensure `compileSdk` is 34 or higher
- Clean build: `./gradlew clean assembleDebug`

**iOS: Framework not found**
- Run `./gradlew build` from shared module
- Rebuild Xcode project

**Web: WASM compilation error**
- Ensure Kotlin version is 1.9.0+
- Check browser compatibility (modern browsers required)

## Next Steps

After installation:
1. ✅ [Quick Start](quickstart.md) - Create your first drawing in 5 minutes
2. 📚 [Core Concepts](../core-concepts/architecture.md) - Understand how DrawBox works
3. 🎨 [Examples](../examples/basic-drawing.md) - See real-world usage patterns

## Support

Having installation issues?
- Check [FAQ](../faq.md) for common problems
- Open an issue on [GitHub](https://github.com/akshay2211/DrawBox)
- Read the [troubleshooting guide](../guides/troubleshooting.md)