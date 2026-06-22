![](https://github.com/nayasis/exposed-crud/actions/workflows/build.yaml/badge.svg)
# Exposed (JetBrains) CRUD repository generator. 

This is a KSP processor to simplify working with [Jetbrains Exposed](https://github.com/JetBrains/Exposed).
Loosely inspired by JPA annotations.

You define a Kotlin `data class` holding your data and use annotations to mark Primary Keys, Relationships and others.

Based on your annotations this library will generate:
 - Exposed table DSL.
 - A `CrudRepository` class with strongly-typed CRUD methods based on your data class.

[Sample data classes](https://github.com/nayasis/exposed-crud/tree/main/sample/src/main/kotlin/com/dshatz/exposed_crud/models)

[Sample usage (test class)](https://github.com/nayasis/exposed-crud/blob/main/sample/src/test/kotlin/com/dshatz/exposed_crud/TestDB.kt)

## Installation
![](https://img.shields.io/maven-central/v/io.github.nayasis/exposed-crud)

```kotlin
plugins {
  id("com.google.devtools.ksp") version "..."
}

dependencies {
  ksp("io.github.nayasis:exposed-crud-processor:0.1.0")
  implementation("io.github.nayasis:exposed-crud:0.1.0")
  // also add exposed-core
}
```

Optional KSP arg for default field-to-column naming:
```kotlin
ksp {
  arg("exposedCrud.defaultFieldNameStrategy", "snake_case_lower") // default
  // arg("exposedCrud.defaultFieldNameStrategy", "snake_case_upper")
  // arg("exposedCrud.defaultFieldNameStrategy", "camel_case")
  // arg("exposedCrud.defaultFieldNameStrategy", "none") // use field name as-is
}
```

## Usage
### 1. Define your entity with `@Entity`
Making a property nullable (e.g. `originalTitle: String?`) will make it nullable in the database as well.

```kotlin
@Entity
data class Movie(
    val id: Long,
    val title: String,
    val createdAt: String,
    val originalTitle: String?,
    val directorId: Long,
    val categoryId: Long,
)
```

You can use `@Text`, `@MediumText`, `@LargeText` to specify what type of column to use for your String property. If none are applied, simple exposed `text` will be used.

When `@Column(name = "...")` is set, that name is used as-is.
When it is blank, the default field naming strategy is applied (`snake_case_lower` by default).
The same strategy is also used for default table names when both `@Table(name)` and `@Entity(name)` are blank.

`@Collate` annotation is also available.

### 2. Specify the primary key with @Id
Set optional parameter `autoGenerate = true` to mark this column as auto-incrementing. Default `false`.

For composite primary keys, annotate multiple properties with `@Id`. Note that in that case auto-incrementing is not supported.
```kotlin
@Entity
data class Movie(
    @Id(autoGenerate = true) val id: Long,
    val title: String,
    val createdAt: String,
    val originalTitle: String?,
    val directorId: Long,
    val categoryId: Long,
)
```

### 3. Specify default values
To specify a default value, use `@DefaultText(text: String)` for Strings and `@Default(literal: String)` for numbers and booleans.

Additionally, specify the default values using Kotlin property initializer for convenience.

```kotlin
@Entity
data class Movie(
    @Id(autoGenerate = true) val id: Long,
    val title: String,
    @DefaultText("01-01-1970")
    val createdAt: String = "01-01-1970",
    val originalTitle: String?,
    val directorId: Long,
    val categoryId: Long,
)
```

### 4. Foreign keys

Annotate the foreign key columns with `@ForeignKey(<your other entity data class>::class)`. 

By default the foreign key will reference the primary key of the given entity. In case the other entity has a composite key, or you want to specify another target column, pass another parameter to `@ForeignKey` with the name of the target column.

```kotlin
@Entity
data class Movie(
    @Id(autoGenerate = true) val id: Long,
    val title: String,
    @DefaultText("01-01-1970")
    val createdAt: String = "01-01-1970",
    val originalTitle: String?,
    @ForeignKey(Director::class)
    val directorId: Long,
)
```

### 5. Relationships
Once the foreign key is defined, you can optionally bind the related entity to a local property with `@References`.

```kotlin
annotation class References(val related: KClass<*>, vararg val fkColumns: String)
```

Pass the related entity class and the names of the local columns annotated with `@ForeignKey`.

```kotlin
@Entity
data class Movie(
    @Id(autoGenerate = true) val id: Long,
    val title: String,
    @DefaultText("01-01-1970")
    val createdAt: String = "01-01-1970",
    val originalTitle: String?,
    @ForeignKey(Director::class)
    val directorId: Long,
    @References(Director::class, "directorId")
    val director: Director? = null,
)
```

#### Reverse relationships
Now that you have annotated `Movie.director` with `@References(Director::class)`, we can optionally add a reverse relationship on `Director`.

```kotlin
@Entity
data class Director(
   @Id(autoGenerate = true) val id: Long,

   @BackReference(Movie::class)
   val movies: List<Movie>? = null
)
```


## Working with the CRUD repository
This is the whole purpose of this library. Simply perform CRUD operations using your existing data class instances.

The generated `CrudRepository` is is immutable and stateless. It is also not bound to any Exposed transaction or DB instance.

**Note: **When calling `CrudRepository` methods you should supply your own transaction with `transaction {}` block.

**First, get the repository:**
 - From the generated table class: `MovieTable.repo`.
 - From the original data class: `Movie::class.repo`.

### Find By Primary Key
```kotlin
val movie: Movie = repo.findById(1L)
```

### Select all
```kotlin
val allMovies: List<Movie> = repo.selectAll()
// Or selectAllLazy (uses Exposed mapLazy under the hood).
```

### Normal select
```kotlin
val moviesFromDirector: Iterable<Movie> = repo.select().where(MovieTable.directorId eq 1)
// Call .first(), .map() or any other iterable function.
```

### Select with related
```kotlin
val moviesWithDirectors: List<Movie> = repo.withRelated(DirectorTable).selectAll()
// Now each `Movie` instance will have a non-null `director: Director` prop.
```

### Insert
`repo.create()` automatically determines the appropriate insert strategy based on the table type:

- **For tables with auto-incrementing IDs** (IntIdTable, UIntIdTable, LongIdTable, ULongIdTable, UUIDTable):
  - Inserts excluding auto-incrementing columns
  - Database generates the ID automatically
  - Useful when receiving a newly created object from the frontend where the ID is not known and is set to some arbitrary value like 0 or -1

- **For tables without auto-incrementing IDs** (e.g., String IDs, composite keys):
  - Inserts all columns including the ID
  - The provided ID value is used as-is

```kotlin
// Auto-incrementing ID (LongIdTable)
val movie = Movie(
   id = -1,  // This value is ignored
   title = "Die Hard",
   originalTitle = null,
   directorId = 1 
)
val inserted = repo.create(movie) // Movie is inserted with an auto-generated id.

// Non-auto-incrementing ID (String)
val language = Language("lv")
val insertedLang = repo.create(language) // Language is inserted with code = "lv"
```

### Insert with related
Same as for selects, you can create a variation of your repo that will include the related entities.

```kotlin
val movieWithDirectorRepo = repo.withRelated(DirectorTable)

val movie = Movie(
   id = -1,
   title = "Die Hard",
   originalTitle = null,
   directorId = 1,
   director = Director(name = "Alfred")
)

val inserted = movieWithDirectorRepo.createWithRelated(movie)
```

So what is happening here? Since we passed a non-null `director` value, this will happen:
 1. Director will be inserted using `create()`.
 2. `Movie.directorId` will be set to the ID of the inserted `Director`.
 3. Movie will be inserted.

In this case, `directorId = 1` is ignored. If `director` is null, Movie will be inseted with `directorId = 1`.

*Please note that the returned Movie will not have the Director set. Currently this is a limitation that will be addressed in the future. As a workaround, you can retrieve it yourself using:* `movieWithDirectorRepo.findById(inserted.id)`.

### Update
Updating an existing entity is straightforward:
```kotlin
repo.update(movie)
```

This will issue an `UPDATE` on all fields with a `WHERE` clause derived from the primary key.

Of course you may still use raw Exposed way of updating if you need to do an `UPDATE` with a different `WHERE`.

### Deleting
```kotlin
repo.delete(movie) 
```

Same as with `UPDATE`, this will delete based on the primary key.


## Other things
All generated tables will have a couple of helper functions that you can also call directly if needed.
- `MovieTable.toEntity(resultRow: ResultRow): E`


This is still in early stages so let me know what functionality essential to you is missing.
Not everything will be possible to implement however. 






