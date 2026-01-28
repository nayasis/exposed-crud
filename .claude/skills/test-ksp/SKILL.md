---
name: test-ksp
description: This skill should be used when the user asks to "test the processor", "run KSP tests", "test annotation processing", "verify code generation", "check if processor works", or wants to validate changes to the KSP annotation processor.
version: 0.1.0
---

# Test KSP Processor

This skill automates the workflow for testing changes to the KSP annotation processor. Use this when verifying that processor changes correctly generate Exposed table DSL and repository code.

## Purpose

The KSP processor generates code at compile time. To test changes:
1. Clean previous generated code
2. Rebuild the sample module (triggers KSP processing)
3. Inspect the generated code
4. Run tests to verify functionality

## Testing Workflow

### Step 1: Clean and Rebuild

Clean the sample module and rebuild to regenerate code:

```bash
./gradlew :sample:clean :sample:build
```

This removes old generated code and runs KSP processor on sample entities.

### Step 2: Inspect Generated Code

Generated code location: `sample/build/generated/ksp/main/kotlin/`

Key files to check:
- Table objects (e.g., `MovieTable.kt`, `DirectorTable.kt`)
- Repository extensions (`TypedQueries.kt`)

Display recently modified generated files:

```bash
find sample/build/generated/ksp/main/kotlin/ -type f -name "*.kt" -mmin -5 | head -10
```

Read key generated files to verify correct code generation.

### Step 3: Run Tests

Run the full test suite:

```bash
./gradlew :sample:check
```

For detailed diagnostics on failure:

```bash
./gradlew :sample:check --stacktrace --build-cache
```

To run specific test class:

```bash
./gradlew :sample:test --tests "com.dshatz.exposed_crud.TestDB"
```

### Step 4: Report Results

Summarize the test results:
- Build status (success/failure)
- Generated code verification (correct table structure, proper types, etc.)
- Test results (passing/failing tests)
- Any compilation errors or warnings

## Common Test Scenarios

**After adding new annotation:**
1. Clean and rebuild sample
2. Check that new annotation generates expected code
3. Run tests to verify runtime behavior

**After modifying code generation:**
1. Clean and rebuild sample
2. Compare generated code before/after changes
3. Verify all tests still pass

**Debugging generation issues:**
1. Run build with `--stacktrace` flag
2. Check KSP error messages in build output
3. Inspect generated code for syntax errors

## Quick Commands Reference

```bash
# Full rebuild and test
./gradlew :sample:clean :sample:build :sample:test

# Just run tests (no rebuild)
./gradlew :sample:test

# Full check with diagnostics
./gradlew :sample:check --stacktrace

# Find generated files
ls -lh sample/build/generated/ksp/main/kotlin/

# Watch for compilation errors
./gradlew :sample:build 2>&1 | grep -i error
```

## Notes

- Always clean before testing annotation processor changes
- Generated code is not checked into git
- Sample module serves as integration test suite
- KSP runs during Gradle's compilation phase
