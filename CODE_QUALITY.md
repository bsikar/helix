# Code Quality Setup

This project uses Detekt for static code analysis to maintain high code quality standards.

## Tools Setup

### Detekt
- **Version**: 1.23.6
- **Configuration**: `app/config/detekt/detekt.yml`
- **Formatting Plugin**: Included for automatic code formatting
- **Status**: ✅ All code quality issues resolved and Detekt passes on every build

### Build System
- **Android Gradle Plugin**: 8.6.1 (stable)
- **Kotlin**: 2.0.20 (stable) 
- **Gradle**: 8.10.2 (stable)

## Available Gradle Tasks

### Code Quality Tasks
```bash
# Run static code analysis
./gradlew detekt

# Run code analysis with auto-correction
./gradlew detekt --auto-correct
```

### Build Integration
**⚠️ Note: Use these commands instead of `./gradlew build` due to test task issues:**

```bash
# Build app with Detekt quality checks (RECOMMENDED)
./gradlew buildApp

# Debug build with quality checks
./gradlew assembleDebug

# Release build with quality checks  
./gradlew assembleRelease

# Clean and build
./gradlew clean buildApp
```

**⚠️ LIMITED SUPPORT:** `./gradlew build` - may have lint task issues, use buildApp instead

## Detekt Rules

### Enabled Rule Sets
- **complexity**: Detects overly complex code
- **coroutines**: Kotlin coroutines best practices
- **empty-blocks**: Prevents empty code blocks
- **exceptions**: Exception handling best practices
- **formatting**: Code formatting rules (auto-correctable)
- **naming**: Naming conventions
- **performance**: Performance optimizations
- **potential-bugs**: Bug prevention
- **style**: Code style guidelines

### Key Thresholds
- **Maximum line length**: 120 characters
- **Maximum function length**: 60 lines
- **Maximum cyclomatic complexity**: 15
- **Maximum return statements**: 2 per function
- **Nested block depth**: Max 4 levels

## Common Issues and Fixes

### Automatic Fixes
The following issues are automatically fixed with `--auto-correct`:
- Trailing whitespace
- Import organization
- Basic formatting issues
- Missing newlines

### Manual Fixes Required
- Long functions (split into smaller functions)
- Complex conditions (simplify logic)
- Too many return statements (refactor to single exit point)
- Wildcard imports (replace with specific imports)
- Overly generic exception catching

## CI/CD Integration

### Pre-commit Hooks
Run before committing:
```bash
./gradlew preCommit
```

### GitHub Actions (Future)
Add to `.github/workflows/code-quality.yml`:
```yaml
- name: Run Code Quality Checks
  run: ./gradlew codeQuality
```

## IDE Integration

### IntelliJ IDEA / Android Studio
1. Install Detekt plugin
2. Configure to use project's `detekt.yml`
3. Enable format on save

### VS Code
1. Install Kotlin extension
2. Configure Detekt integration

## Reports

Detekt generates reports in multiple formats:
- **HTML**: `app/build/reports/detekt/detekt.html`
- **XML**: `app/build/reports/detekt/detekt.xml`
- **SARIF**: `app/build/reports/detekt/detekt.sarif`
- **Markdown**: `app/build/reports/detekt/detekt.md`

## Best Practices

1. **Run `./gradlew preCommit` before committing**
2. **Fix auto-correctable issues first**: `./gradlew detekt --auto-correct`
3. **Break down long functions** into smaller, focused functions
4. **Use specific exception types** instead of catching `Exception`
5. **Follow naming conventions**: camelCase for functions, PascalCase for classes
6. **Keep cyclomatic complexity low** by simplifying conditional logic
7. **Avoid wildcard imports** - use specific imports instead

## Configuration

### Disabling Rules
To disable a specific rule for a file:
```kotlin
@file:Suppress("RuleName")
```

To disable for a specific function:
```kotlin
@Suppress("RuleName")
fun myFunction() { }
```

### Updating Rules
Edit `app/config/detekt/detekt.yml` to modify rule configurations.

## Troubleshooting

### Build Failures
If Detekt causes build failures:
1. Run `./gradlew detekt --auto-correct` first
2. Check the HTML report for detailed issue descriptions
3. Fix remaining issues manually
4. Consider temporarily disabling problematic rules if needed

### Performance
For large projects, consider:
- Excluding test files from analysis
- Running parallel analysis
- Using type resolution selectively