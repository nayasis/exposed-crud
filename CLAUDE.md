# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a KSP (Kotlin Symbol Processing) annotation processor that generates Exposed CRUD repositories from annotated Kotlin data classes. It automates creation of Exposed table DSL and type-safe repository methods.

## Common Commands

### Build and Test
```bash
# Run all tests (via sample module)
./gradlew :sample:check

# Run tests with full diagnostics
./gradlew :sample:check --stacktrace --build-cache

# Build all modules
./gradlew build

# Clean and rebuild
./gradlew clean build
```

### Running Individual Tests
```bash
# Run a specific test class
./gradlew :sample:test --tests "com.dshatz.exposed_crud.TestDB"

# Run a single test method
./gradlew :sample:test --tests "com.dshatz.exposed_crud.TestDB.testInsertAndSelect"
```

### Publishing (for maintainers)
```bash
# Publish to Maven Local for testing
./gradlew publishToMavenLocal

# Publish to Maven Central (requires credentials and mavenReleaseVersion property)
./gradlew publish -PmavenReleaseVersion=X.Y.Z
```

### Development Workflow
```bash
# After making changes to processor, rebuild sample to see generated code
./gradlew :sample:clean :sample:build

# Check generated code location
# Generated files are in: sample/build/generated/ksp/main/kotlin/
```

## Code Conventions

### Language and Documentation
- **All code comments must be written in English**
- **All commit messages must be written in English**
- Use clear, concise English for documentation and inline comments

### Commit Messages
- **Do NOT include AI attribution** in commit messages
- Do NOT add "Co-Authored-By: Claude" or similar AI assistance mentions
- Write commit messages that describe the change directly and professionally
- Follow conventional commit format when appropriate (e.g., `feat:`, `fix:`, `refactor:`)

## Architecture

### Module Structure

The project has three modules with distinct responsibilities:

**annotations/** - Runtime annotation library
- Pure annotation definitions (`@Entity`, `@Id`, `@ForeignKey`, etc.)
- Runtime interfaces: `IEntityTable`, `AttributeConverter`, `IdGenerator`
- No dependencies on KSP or code generation

**processor/** - KSP annotation processor
- Runs at compile time to generate code
- Main classes: `KspProcessor`, `Generator`, `TypedQueriesGenerator`
- Uses KotlinPoet for code generation
- Depends on: KSP API, annotations module, Exposed core

**sample/** - Integration tests and examples
- Demonstrates all features with realistic entity models
- Tests generated code functionality
- Uses KSP to process its own entities during compilation
- Located in: `sample/src/main/kotlin/com/dshatz/exposed_crud/models/`

### Annotation Processing Flow

1. **Entry Point**: `KspProcessorProvider` → `KspProcessor`
2. **Discovery**: Find all `@Entity` annotated classes
3. **Analysis**: Parse properties, extract metadata into `EntityModel`
   - Identifies primary keys (`@Id`), foreign keys (`@ForeignKey`)
   - Processes relationships (`@References`, `@BackReference`)
   - Handles converters (`@Convert`), timestamps, text types
4. **Model Building**: Create `EntityModel` with `ColumnModel` instances
5. **Code Generation**: `Generator` produces Exposed table DSL
   - Table object inheriting from appropriate IdTable type
   - `toEntity(ResultRow)` conversion functions
   - `write()` and `writeExceptAutoIncrementing()` methods
6. **Repository Generation**: `TypedQueriesGenerator` creates repository accessors
   - `MovieTable.repo` property
   - `Movie::class.repo` extension
   - Typed CRUD methods

### Key Classes and Responsibilities

**KspProcessor** (`processor/src/.../KspProcessor.kt`)
- Main KSP entry point
- Orchestrates entity discovery and validation
- Delegates to Generator and TypedQueriesGenerator

**EntityModel** (`processor/src/.../models/EntityModel.kt`)
- Intermediate representation of entity metadata
- Contains: table name, columns, primary key, relationships, indexes
- Used as input for code generation

**ColumnModel** (`processor/src/.../models/ColumnModel.kt`)
- Represents a single database column
- Tracks: property name, column name, type, nullable, converters, FK info
- Handles complex type mapping logic

**Generator** (`processor/src/.../Generator.kt`)
- Core code generation engine
- Generates table objects with Exposed column definitions
- Creates `toEntity()` conversion logic
- Handles relationship loading, timestamps, converters

**TypedQueriesGenerator** (`processor/src/.../TypedQueriesGenerator.kt`)
- Generates `CrudRepository` extension methods
- Creates `.repo` accessor properties
- Implements typed query methods

### Generated Code Pattern

For each `@Entity` class, the processor generates:

```kotlin
object MovieTable : LongIdTable("movies"), IEntityTable<Movie, Long> {
    val id = integer("id").autoIncrement().entityId()
    val title = text("title")
    // ... other columns

    override fun toEntity(row: ResultRow, related: List<ColumnSet>): Movie
    override fun write(update: UpdateBuilder<Number>, data: Movie)
    override fun writeExceptAutoIncrementing(...)
    override fun makePK(data: Movie): EntityID<Long>
    override fun setId(data: Movie, id: Long): Movie
}

val MovieTable.repo: CrudRepository<MovieTable, Long, Movie> by lazy { ... }
val KClass<Movie>.repo: CrudRepository<...> get() = table.repo
```

### Primary Key Handling

The processor distinguishes between:
- **Simple primary key**: Single `@Id` property → generates `LongIdTable`, `IntIdTable`, `UUIDTable`, etc.
- **Composite primary key**: Multiple `@Id` properties → generates custom `IdTable` with composite PK
- **Auto-increment**: `@Id(autoGenerate = true)` → enables database auto-generation

Table type selection logic is in `Generator.getTableType()`.

### Insert Strategy

`CrudRepository.create()` behavior depends on table type:
- **Auto-increment tables** (Int/Long/UUID IdTable): Uses `writeExceptAutoIncrementing()` to exclude ID column
- **Non-auto-increment tables**: Uses `write()` to include all columns

This is determined by the `autoGenerate` flag on the table.

### Relationship System

**@ForeignKey** - Declares FK column
```kotlin
@ForeignKey(Director::class)
val directorId: Long
```

**@References** - Forward relationship (one-to-one/many-to-one)
```kotlin
@References(Director::class, "directorId")
val director: Director? = null
```

**@BackReference** - Reverse relationship (one-to-many)
```kotlin
@BackReference(Movie::class)
val movies: List<Movie>? = null
```

Relationships are loaded when using `repo.withRelated(DirectorTable)`, which generates LEFT JOINs.

### Converter System

**@Convert** - Type conversion between entity and database
```kotlin
@Convert(ColorConverter::class)
val color: Color  // Stored as String in DB
```

Converters implement `AttributeConverter<EntityType, DbType>`:
```kotlin
interface AttributeConverter<ENTITY, DB> {
    fun convertToDatabaseColumn(attribute: ENTITY?): DB?
    fun convertToEntityAttribute(dbData: DB?): ENTITY?
}
```

The processor determines the final column type from the converter's `DB` type parameter. When the converter target type is nullable, special nullability logic applies (see `Generator.getColumnType()`).

### Text Column Types

String properties can use different SQL text types:
- `@Column(length=10)` → `VARCHAR(10)`
- `@Text` → `TEXT`
- `@MediumText` → `MEDIUMTEXT`
- `@LargeText` → `LONGTEXT`
- Default (no annotation) → Exposed `text()`

Recent addition: Text-based annotations now work with properties that have String converter target types.

### Timestamp Handling

**@CreationTimestamp** - Set once on insert
**@UpdateTimestamp** - Updated on every modification

Supported types: `Date`, `LocalDate`, `LocalDateTime`, `Instant`, kotlinx-datetime variants.

The generator creates appropriate `now()` calls in `write()` methods.

## Development Notes

### Testing Changes

After modifying the processor, test by:

1. Clean and rebuild sample: `./gradlew :sample:clean :sample:build`
2. Inspect generated code in `sample/build/generated/ksp/main/kotlin/`
3. Run tests: `./gradlew :sample:test`

The sample module tests all features through actual compilation and execution.

### Test Coverage for Important Features

When you add an important feature, if there is no related test case, create one.
After adding such a feature and its tests, you must run the full test suite
(`./gradlew :sample:check`) to ensure end-to-end validation.

### Adding New Annotations

1. Define annotation in `annotations/src/main/kotlin/com/dshatz/exposed_crud/annotations/`
2. Update `EntityModel` or `ColumnModel` to capture the annotation data
3. Modify `KspProcessor.processEntity()` to parse the annotation
4. Update `Generator` to handle the annotation during code generation
5. Add test cases in `sample/src/test/kotlin/`

### Code Generation with KotlinPoet

The processor uses KotlinPoet extensively. Key patterns:

- `TypeSpec.objectBuilder()` for table objects
- `FunSpec.builder()` for methods
- `PropertySpec.builder()` for columns
- `CodeBlock.builder()` for complex logic

See `Generator.generateTable()` for the main generation flow.

### KSP Symbol Processing

The processor works with KSP symbols:
- `KSClassDeclaration` - Entity class being processed
- `KSPropertyDeclaration` - Entity properties (become columns)
- `KSAnnotation` - Annotation instances on classes/properties

Use `KSAnnotation.toAnnotationInfo()` extension to parse annotation values.

### Common Patterns

**Nullable handling**: The processor carefully tracks nullability through converters and FK references. When a converter has a nullable target type, the column becomes nullable regardless of the entity property nullability.

**EntityID wrapping**: Foreign key columns are typed as `Column<EntityID<T>>` in the generated table, but the entity class uses plain `T` (e.g., `Long`). Conversion happens in `toEntity()` and `write()`.

**Relationship resolution**: In `toEntity()`, when related tables are included via `withRelated()`, the processor generates recursive `toEntity()` calls on the joined ResultRow.

**ID generation**: Custom `IdGenerator` implementations can be specified with `@Id(generator = MyGenerator::class)`. The generator is invoked before insert in `create()`.

## CI/CD

The project uses GitHub Actions (`.github/workflows/build.yaml`):
- Runs on: pushes to main/develop, PRs, releases
- Tests: `./gradlew :sample:check` with test reporting
- Publishing: Triggered on release creation, publishes to Maven Central

## Claude Code Skills

This project provides custom Claude Code skills to streamline common development workflows. Skills are loaded automatically when you use Claude Code in this repository.

### Available Skills

**Test KSP Processor** (`/test-ksp`)
- Rebuilds the sample module to regenerate code
- Inspects generated code in `sample/build/generated/ksp/main/kotlin/`
- Runs the test suite with diagnostics
- Use when: testing annotation processor changes, debugging code generation

**Check Generated Code** (`/check-generated`)
- Quickly displays recently generated table files
- Shows generated repository code
- Use when: verifying code generation output without running full tests

**Gradle Build** (`/gradle-build`)
- Common Gradle tasks (clean, build, test, check)
- Includes useful flags (--stacktrace, --build-cache)
- Use when: running builds, troubleshooting Gradle issues

### Using Skills

Skills can be invoked in two ways:

1. **Manual invocation**: Type `/test-ksp` or `/check-generated` directly
2. **Automatic triggering**: Ask naturally (e.g., "test the processor", "show me the generated code")

### General-Purpose Skills

The following skills from your personal ~/.claude/skills/ are also useful for this project:

- **agent-development**: When creating custom Claude Code agents
- **skill-development**: When creating new project-specific skills
- **plugin-structure**: Understanding Claude Code plugin architecture
- **hook-development**: Creating event hooks for automation

## Troubleshooting

### Windows IntelliJ + WSL Path Issues

When running tests from Windows IntelliJ on a WSL project, you may encounter incremental compilation errors:

```
Expected absolute path but found relative path: \mnt\c\project_ref\exposed-crud\sample\src\test\kotlin\...
```

This occurs because Windows and WSL use different path formats, confusing Kotlin's incremental compilation cache.

**Solutions:**

1. **IntelliJ Cache Invalidation** (most effective)
   - `File` → `Invalidate Caches...` → `Invalidate and Restart`

2. **Clear Kotlin Compilation Caches**
   ```bash
   ./gradlew clean
   rm -rf .gradle/kotlin sample/build/.kotlin
   ```

3. **Stop Kotlin Daemon**
   - IntelliJ: `Tools` → `Kotlin` → `Stop Kotlin Compiler Daemon`
   - Command line: `./gradlew --stop`

4. **Disable Precise Java Tracking** (already configured in `gradle.properties`)
   ```properties
   kotlin.incremental.usePreciseJavaTracking=false
   ```

Note: Tests will still run successfully even with this warning, as Kotlin falls back to non-incremental compilation.

## Requirements

- JDK 17
- Gradle 8.x (uses wrapper)
- Exposed 0.50.x or compatible
