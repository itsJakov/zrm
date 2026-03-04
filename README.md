# zrm

A **Object-Relational Mapping (ORM)** library built in Kotlin, developed as an university project at [Algebra Bernays University](https://www.algebra.hr/).

The goal was to bring an EF Core-like API experience to Kotlin, with change tracking and a query builder.

> [!WARNING]
> This is **not production ready**. It has edge cases, missing features, and rough spots. Use it for learning only.

## Project Structure

```
src/main/kotlin/hr/algebra/jgojevi/zrm/
├── Database.kt       # Database connection
├── EntityStore.kt    # Entity persistence
├── Expression.kt     # Query expressions
├── Query.kt          # Query builder
├── changes/          # Change tracking
├── exec/             # Query execution
├── migration/        # Database migrations (Proof-of-Concept)
└── schema/           # Schema definitions
```

## Usage

### 1. Define your entities

Annotate your data classes to map them to database tables and columns.

```kotlin
@Table("artists")
data class Artist(
    @Key
    @Column("artist_id")
    var id: Int = 0,

    @Column("name")
    var name: String
) {
    var albums: MutableList<Album>? = null
}

@Table("albums")
data class Album(
    @Key
    @Column("album_id")
    var id: Int = 0,

    @Column("title")
    var name: String,

    @Column("artist_id")
    @ForeignKey("artist")
    var artistId: Int
) {
    var artist: Artist? = null
    var songs: MutableList<Song>? = null
}
```

### 2. Set up your database

Extend `Database` and declare your entity stores.

```kotlin
class AppDatabase : Database("jdbc:postgresql://localhost/mydb?user=postgres&password=secret") {
    lateinit var artists: EntityStore<Artist>
    lateinit var albums: EntityStore<Album>
    lateinit var songs: EntityStore<Song>
}
```

### 3. Query data (DQL)

Build type-safe queries with filtering, ordering, and eager loading.

```kotlin
val database = AppDatabase()

// Filter with ordering
val studentsAfter2020 = database.students
    .where(Student::enrollmentYear gt 2020)
    .orderBy(+Student::enrollmentYear, -Student::lastName)
    .all()

// Compound expressions
val ivyBrowns = database.students
    .where((Student::firstName eq "Ivy") and (Student::lastName eq "Brown"))
    .all()

// Eager load related entities
val allArtistsWithDetails = database.artists
    .include(Artist::albums)
    .include(Album::songs)
    .where(Song::title neq "Parklife")
    .all()
```

### 4. Modify data (DML)

Add, update, and remove entities with automatic change tracking.

```kotlin
// Insert
val newStudent = Student(firstName = "John", lastName = "Doe", enrollmentYear = 2023)
database.add(newStudent)
database.saveChanges()

// Update
newStudent.lastName = null
database.saveChanges()

// Delete
database.remove(newStudent)
database.saveChanges()
```

## 👇 See Also

[gas-orm](https://github.com/Antony1060/gas-orm) by [@Antony1060](https://github.com/Antony1060): a more feature complete ORM project, built in Rust! 🦀
