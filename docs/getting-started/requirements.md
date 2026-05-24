# System Requirements

## Minimum Requirements

### Development Environment
- **Kotlin**: 1.9.0 or higher
- **Gradle**: 8.0 or higher
- **Java/JDK**: 11 or higher

### Compile-Time
- **Compose Multiplatform**: 1.5.0 or higher
- **Android Gradle Plugin**: 8.0 or higher

## Platform-Specific Requirements

### Android
- **minSdk**: API 21 (Android 5.0)
- **compileSdk**: API 34 or higher (recommended)
- **targetSdk**: API 34 or higher (recommended)

### iOS
- **Minimum iOS Version**: iOS 14.0
- **Xcode**: 14.0 or higher
- **CocoaPods**: For dependency management

### Desktop (JVM)
- **Java Runtime**: JDK 11 or higher
- **Operating Systems**: macOS, Windows, Linux
- **RAM**: 512 MB minimum

### Web (WASM)
- **Browser Support**:
  - Chrome 91+
  - Firefox 89+
  - Safari 15+
  - Edge 91+
- **WASM Support**: Must be enabled in browser
- **Memory**: 512 MB minimum

## IDE/Editor Support
- **Android Studio**: Flamingo (2022.2.1) or higher
- **IntelliJ IDEA**: 2023.1 or higher
- **Visual Studio Code**: With Kotlin extension
- **Xcode**: 14.0+ for iOS development

## Network Requirements
- Internet connection for downloading dependencies
- Access to Maven Central for library downloads

## Optional but Recommended

### Development Tools
- **Git**: For version control
- **Gradle Wrapper**: Included in projects (recommended)
- **Kotlin Multiplatform Mobile (KMM) Plugin**: For IDE integration

### Testing Tools
- **JUnit 4+**: For unit testing
- **Mockk**: For mocking in tests

## Hardware Recommendations

| Device Type | Minimum | Recommended |
|------------|---------|-------------|
| **Desktop (Development)** | 4GB RAM, SSD | 8GB+ RAM, SSD |
| **Android Device** | API 21+, 512MB | API 28+, 2GB+ |
| **iOS Device** | iOS 14+, 1GB | iOS 15+, 2GB+ |
| **Web Browser** | 1GB RAM | 2GB+ RAM |

## Troubleshooting

### Too old Kotlin version
- Update Gradle: `gradle wrapper --gradle-version 8.0`
- Update Kotlin: Check `gradle/libs.versions.toml`

### Insufficient memory errors
- Increase Gradle heap size in `gradle.properties`:
  ```
  org.gradle.jvmargs=-Xmx2g
  ```

### Missing dependencies
- Run `./gradlew clean` to refresh cache
- Delete `.gradle` folder and resync

For more help, see the [Troubleshooting Guide](../guides/troubleshooting.md).