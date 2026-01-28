---
name: check-generated
description: This skill should be used when the user asks to "show generated code", "check generated tables", "see what was generated", "inspect generated files", "view generated repositories", or wants to quickly review KSP-generated output without running tests.
version: 0.1.0
---

# Check Generated Code

This skill provides a quick way to inspect KSP-generated code without running the full test suite. Use this to verify that the annotation processor generated the expected Exposed table DSL and repository code.

## Purpose

After making changes to the processor or sample entities, quickly review the generated code to ensure:
- Tables are generated with correct structure
- Column types match entity properties
- Relationships are properly handled
- Repository methods are generated

## Generated Code Location

All KSP-generated files are in: `sample/build/generated/ksp/main/kotlin/`

## Quick Inspection Workflow

### Step 1: List Generated Files

Show all generated Kotlin files:

```bash
find sample/build/generated/ksp/main/kotlin/ -name "*.kt" -type f
```

Group by type (Tables vs TypedQueries):

```bash
ls -lh sample/build/generated/ksp/main/kotlin/com/dshatz/exposed_crud/generated/
```

### Step 2: Display Recent Changes

Show recently modified generated files (last 10 minutes):

```bash
find sample/build/generated/ksp/main/kotlin/ -name "*.kt" -type f -mmin -10 -exec ls -lh {} \;
```

### Step 3: Read Key Generated Files

**Table objects:**
Read generated table files to verify:
- Correct table name
- Proper column definitions
- Primary key configuration
- Foreign key relationships
- `toEntity()` and `write()` methods

**Repository code:**
Read `TypedQueries.kt` to verify:
- `.repo` property generation
- CRUD method signatures
- Relationship loading support

### Step 4: Highlight Key Aspects

When reviewing generated code, focus on:

**For Table Objects:**
- Table inheritance (LongIdTable, IntIdTable, etc.)
- Column definitions match entity properties
- Nullable vs non-nullable columns
- Foreign key columns use `EntityID<T>`
- Converters applied correctly
- Timestamp columns (creation/update)

**For Repository Code:**
- Type parameters (Table, ID type, Entity type)
- Repository accessor properties
- Extension methods on KClass

## Common Checks

**After adding entity annotation:**
Verify the new entity generates a table file:

```bash
find sample/build/generated/ksp/main/kotlin/ -name "*Table.kt" | grep -i "YourEntity"
```

**After modifying column annotation:**
Check the column definition in generated table:

```bash
grep -A 2 "val yourColumn" sample/build/generated/ksp/main/kotlin/path/to/YourEntityTable.kt
```

**After changing relationship:**
Verify foreign key and reference handling in `toEntity()` method.

## Quick Commands Reference

```bash
# Count generated files
find sample/build/generated/ksp/main/kotlin/ -name "*.kt" | wc -l

# Show all table files
find sample/build/generated/ksp/main/kotlin/ -name "*Table.kt"

# Show TypedQueries file
find sample/build/generated/ksp/main/kotlin/ -name "TypedQueries.kt"

# Search for specific pattern in generated code
grep -r "pattern" sample/build/generated/ksp/main/kotlin/

# Show file sizes
du -h sample/build/generated/ksp/main/kotlin/
```

## Notes

- Generated code is in `build/` directory (not tracked by git)
- If directory is empty, run `./gradlew :sample:build` first
- Changes to processor require rebuild to see new generated code
- This skill focuses on quick inspection, not testing functionality
