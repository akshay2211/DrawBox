# Kotlin Multiplatform

DrawBox leverages Kotlin Multiplatform (KMP) to run on multiple platforms with a shared codebase.

## Supported Platforms

| Platform | Status | Min Version |
|----------|--------|-------------|
| Android | ✅ Full Support | API 21 |
| iOS | ✅ Full Support | iOS 14.0 |
| Desktop (JVM) | ✅ Full Support | JDK 11 |
| Web (WASM) | ✅ Full Support | Modern browsers |

## Shared Code

All core drawing logic is shared:
- State management
- Drawing algorithms
- Export functionality
- MVI architecture

## Platform-Specific Code

Each platform has optimized implementations for:
- Image saving
- File I/O
- Platform APIs
- Native performance

---

Learn more: [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
