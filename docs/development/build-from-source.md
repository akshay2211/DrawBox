# Building from Source

Build DrawBox locally for development and contributions.

## Prerequisites

- JDK 11+
- Gradle 8.0+
- Kotlin 1.9.0+

## Clone Repository

```bash
git clone https://github.com/akshay2211/DrawBox.git
cd DrawBox
```

## Build All Modules

```bash
./gradlew build
```

## Build Specific Modules

```bash
# Build DrawBox library
./gradlew :DrawBox:build

# Build sample app
./gradlew :shared:build
```

## Run Tests

```bash
./gradlew test
```

## Format Code

```bash
./gradlew spotlessApply
```

## Generate Documentation

```bash
# Dokka API docs
./gradlew dokkaHtml

# View at: DrawBox/build/dokka/html/index.html
```

## Troubleshooting

### Build fails

```bash
./gradlew clean build
```

### Gradle sync issues

- Invalidate cache in Android Studio (File → Invalidate Caches)
- Delete `.gradle` folder

---

See [Contributing](contributing.md) for more details.
