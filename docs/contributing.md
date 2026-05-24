# Contributing to DrawBox

We love contributions! DrawBox is an open-source project and we welcome help from the community.

## Getting Started

1. **Fork** the repository on GitHub
2. **Clone** your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/DrawBox.git
   cd DrawBox
   ```
3. **Create** a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```

## Development Setup

### Prerequisites
- JDK 11+
- Android Studio Flamingo+
- Kotlin 1.9.0+

### Building

```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :DrawBox:build

# Run tests
./gradlew test

# Format code
./gradlew spotlessApply
```

## Code Style

DrawBox follows the [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html).

- Use `spotlessApply` to auto-format code
- 4 spaces for indentation (not tabs)
- Max line length: 120 characters
- Meaningful variable and function names

### Commit Messages

Write clear, descriptive commit messages:

```
feat: add new drawing mode for star shapes
fix: resolve canvas rendering bug on iOS
docs: improve API documentation
refactor: simplify shape calculation logic
```

Start with:
- `feat:` for new features
- `fix:` for bug fixes
- `docs:` for documentation
- `refactor:` for code improvements
- `test:` for test additions
- `ci:` for CI/CD changes

## Making Changes

### Before You Start
1. Check existing [issues](https://github.com/akshay2211/DrawBox/issues)
2. Create an issue for your feature/bug fix
3. Wait for feedback before starting large changes

### Code Guidelines

#### Module Structure
DrawBox follows a clean architecture with clear separation of concerns:

```
DrawBox/
├── src/commonMain/
│   ├── kotlin/io/ak1/drawbox/
│   │   ├── domain/        # Business logic, entities, use cases
│   │   ├── presentation/  # ViewModels, Controllers, Reducers
│   │   ├── ui/           # Compose UI components
│   │   └── Helper.kt
│   └── composeResources/  # Assets, icons, resources
└── src/[platformMain]/    # Platform-specific implementations
```

#### Naming Conventions
- **Classes/Interfaces**: PascalCase (e.g., `DrawBoxController`)
- **Functions/Variables**: camelCase (e.g., `setStrokeColor()`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `DEFAULT_STROKE_WIDTH`)
- **Files**: Match public class name

#### Documentation
Add Kotlin documentation comments for public APIs:

```kotlin
/**
 * Sets the stroke color for new drawings.
 *
 * This method updates the current stroke color and applies it
 * to all subsequently drawn elements.
 *
 * @param color The color to set for drawing
 * @see State.strokeColor
 */
fun setColor(color: Color) {
    // implementation
}
```

### Testing

Write tests for new features:

```kotlin
class DrawBoxControllerTest {
    @Test
    fun `setColor updates state correctly`() {
        val controller = DrawBoxController()
        val testColor = Color.Blue
        
        controller.setColor(testColor)
        
        val state = controller.state.value
        assertEquals(state.strokeColor, testColor)
    }
}
```

## Submitting Changes

1. **Push** to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```

2. **Create** a Pull Request on GitHub with:
   - Clear title and description
   - Reference to related issues (#123)
   - Summary of changes
   - Screenshots for UI changes

3. **Address** feedback from code review

4. **Celebrate** when merged! 🎉

## Pull Request Guidelines

- Keep PRs focused on a single feature/fix
- Add tests for new code
- Update documentation
- Ensure CI passes
- Request review from maintainers

## Reporting Bugs

Create an issue with:
- Clear description of the problem
- Steps to reproduce
- Expected behavior
- Actual behavior
- Your environment:
  - Platform (Android/iOS/Desktop/Web)
  - DrawBox version
  - Kotlin version
  - Relevant dependencies

## Requesting Features

Suggest features by opening an issue with:
- Clear use case
- Why it would be useful
- Possible implementation approach
- Any relevant references

## Documentation

Help improve documentation:

- Fix typos and clarify explanations
- Add examples
- Improve code snippets
- Update API documentation

Edit files in the `docs/` folder and submit a PR.

## Code Review Process

1. **Automated checks**: Tests, formatting, linting
2. **Manual review**: Code quality and architecture
3. **Feedback**: Maintainers may request changes
4. **Approval**: Usually 2 maintainers
5. **Merge**: When approved and CI passes

## Areas We Need Help With

- **Documentation**: Improve guides and API docs
- **Examples**: Create example projects
- **Testing**: Add tests for edge cases
- **Accessibility**: Improve accessibility features
- **Performance**: Optimize algorithms
- **Platforms**: Improve iOS/Web support
- **Translations**: Help with i18n

## Recognized Contributors

All contributors are recognized in:
- GitHub contributors page
- Release notes
- Special mentions in documentation

## License

By contributing, you agree that your contributions will be licensed under DrawBox's Apache 2.0 license.

## Questions?

Feel free to ask questions in:
- [GitHub Issues](https://github.com/akshay2211/DrawBox/issues)
- [GitHub Discussions](https://github.com/akshay2211/DrawBox/discussions)

---

**Thank you for contributing to DrawBox!** 🙌