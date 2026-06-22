---
name: gradle-build
description: This skill should be used when the user asks to "run gradle build", "build the project", "clean build", "run gradle tasks", "publish to maven", "check gradle", or needs help with Gradle commands for this KSP annotation processor project.
version: 0.1.0
---

# Gradle Build Helper

This skill provides quick access to common Gradle tasks for the exposed-crud KSP annotation processor project. Use this for building, testing, and publishing the project.

## Purpose

This project uses Gradle with three modules (annotations, processor, sample). The Gradle wrapper is configured, so all commands use `./gradlew`.

## Common Build Tasks

### Clean Build

Remove all build artifacts and rebuild from scratch:

```bash
./gradlew clean build
```

Use when:
- Starting fresh after major changes
- Troubleshooting build cache issues
- Preparing for release

### Build All Modules

Build annotations, processor, and sample modules:

```bash
./gradlew build
```

This compiles all modules and runs KSP on the sample module.

### Quick Build (Skip Tests)

Build without running tests (faster):

```bash
./gradlew build -x test
```

Use for quick compilation checks.

## Testing Tasks

### Run All Tests

Run the full test suite through sample module:

```bash
./gradlew :sample:check
```

This includes:
- Compilation
- KSP code generation
- Unit tests
- Integration tests

### Run Tests with Diagnostics

Full diagnostics on test failures:

```bash
./gradlew :sample:check --stacktrace --build-cache
```

### Run Specific Test Class

```bash
./gradlew :sample:test --tests "com.dshatz.exposed_crud.TestDB"
```

### Run Single Test Method

```bash
./gradlew :sample:test --tests "com.dshatz.exposed_crud.TestDB.testInsertAndSelect"
```

## Module-Specific Tasks

### Build Only Processor

```bash
./gradlew :processor:build
```

### Build Only Annotations

```bash
./gradlew :annotations:build
```

### Rebuild Sample (Triggers KSP)

Clean and rebuild sample to regenerate code:

```bash
./gradlew :sample:clean :sample:build
```

## Publishing Tasks

### Publish to Maven Local

For local testing:

```bash
./gradlew publishToMavenLocal
```

Published to: `~/.m2/repository/`

### Publish to Maven Central

Requires credentials and version property:

```bash
./gradlew :processor:publishAndReleaseToMavenCentral :annotations:publishAndReleaseToMavenCentral -Pversion=X.Y.Z
```

**Prerequisites:**
- Maven Central credentials configured
- Signing keys set up
- Version number specified

## Troubleshooting Tasks

### Show Gradle Version

```bash
./gradlew --version
```

### List All Tasks

```bash
./gradlew tasks --all
```

### Show Dependencies

```bash
./gradlew :processor:dependencies
```

### Clear Gradle Cache

```bash
./gradlew clean --no-build-cache
rm -rf .gradle/
```

### Refresh Dependencies

Force dependency refresh:

```bash
./gradlew build --refresh-dependencies
```

## Useful Flags

**--stacktrace**: Show full stack traces on errors

```bash
./gradlew build --stacktrace
```

**--info**: Detailed build logging

```bash
./gradlew build --info
```

**--debug**: Debug-level logging

```bash
./gradlew build --debug
```

**--build-cache**: Use build cache for faster builds

```bash
./gradlew build --build-cache
```

**--offline**: Build without network access

```bash
./gradlew build --offline
```

**--parallel**: Enable parallel execution

```bash
./gradlew build --parallel
```

## Quick Commands Reference

```bash
# Full clean rebuild with tests
./gradlew clean build

# Quick check (no clean)
./gradlew :sample:check

# Fast build (skip tests)
./gradlew build -x test

# Rebuild and regenerate code
./gradlew :sample:clean :sample:build

# Publish locally
./gradlew publishToMavenLocal

# Show tasks
./gradlew tasks

# Clear everything and rebuild
./gradlew clean build --no-build-cache
```

## Module Structure

**annotations/** - Runtime annotations
- Pure annotation definitions
- No KSP dependencies
- Published to Maven Central

**processor/** - KSP annotation processor
- Code generation logic
- KotlinPoet for code generation
- Published to Maven Central

**sample/** - Integration tests
- Example entities
- Generated code tests
- Not published

## Development Workflow

**After processor changes:**
```bash
# Rebuild processor
./gradlew :processor:build

# Regenerate sample code
./gradlew :sample:clean :sample:build

# Run tests
./gradlew :sample:test
```

**Before committing:**
```bash
# Run full check
./gradlew clean build

# Or just sample check
./gradlew :sample:clean :sample:check
```

**Before release:**
```bash
# Clean build with tests
./gradlew clean build

# Test local publishing
./gradlew publishToMavenLocal

# Publish to Maven Central (with version)
./gradlew :processor:publishAndReleaseToMavenCentral :annotations:publishAndReleaseToMavenCentral -Pversion=1.2.3
```

## Notes

- Always use `./gradlew` (not `gradle`)
- Gradle wrapper version: 8.x
- JDK 17 required
- KSP runs during compilation phase
- Generated code is in `sample/build/generated/ksp/`
