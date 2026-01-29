package hr.algebra.jgojevi.zrm.test

import hr.algebra.jgojevi.zrm.*
import hr.algebra.jgojevi.zrm.changes.ChangeTracker
import hr.algebra.jgojevi.zrm.schema.Column
import hr.algebra.jgojevi.zrm.schema.ForeignKey
import hr.algebra.jgojevi.zrm.schema.Key
import hr.algebra.jgojevi.zrm.schema.Table
import org.junit.jupiter.api.assertThrows

@Table("students")
data class Student(
    @Key
    @Column("student_id")
    var id: Int = 0,

    @Column("first_name")
    var firstName: String?,

    @Column("last_name")
    var lastName: String?,

    @Column("enrollment_year")
    var enrollmentYear: Int?,
)

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
    var artist: Artist? = null // For now, navigation properties have to be defined outside the primary constructor. Kinda ugly
    var songs: MutableList<Song>? = null
}

@Table("songs")
data class Song(
    @Key
    @Column("song_id")
    var id: Int = 0,

    @Column("album_id")
    @ForeignKey("album", onUpdate = ForeignKey.Action.CASCADE, onDelete = ForeignKey.Action.RESTRICT)
    var albumId: Int,

    @Column("title")
    var title: String,

    @Column("length")
    var length: Int
) {
    var album: Album? = null
}

class AppDatabase : Database("jdbc:postgresql://localhost/obrana?user=postgres&password=Pa55w.rd") {
    lateinit var students: EntityStore<Student>
    lateinit var artists: EntityStore<Artist>
    lateinit var albums: EntityStore<Album>
    lateinit var songs: EntityStore<Song>
}

fun testDQL() {
    val database = AppDatabase()

    val studentsAfter2020 = database.students
        .where(Student::enrollmentYear gt 2020)
        .orderBy(+Student::enrollmentYear, -Student::lastName)
        .detached()
        .all()

    val ivyBrowns = database.students
        .where((Student::firstName eq "Ivy") and (Student::lastName eq "Brown"))
        .detached()
        .all()

    val expr = ((Student::enrollmentYear gte 2020) and (Student::lastName neq null)) or not(Student::id eq 12)
    expr._debugPrint()

    // Doing this before allArtistsWithDetails will cause an interesting crash in the next query
//    val allArtists = database.artists.all()

    val allArtistsWithDetails = database.artists
        .include(Artist::albums)
        .include(Album::songs)
        .where(Song::title neq "Parklife")
//        .where(Song::title eq 10)  // Won't compile!
        .all()

    val trackedEntities = database.changeTracker.entries.map { it.entity }

    println()
}

fun testDML() {
    val database = AppDatabase()

    // Add a student
    val newStudent = Student(
        firstName = "John",
        lastName = "Doe",
        enrollmentYear = 2023
    )
    database.add(newStudent)
    database.saveChanges()
    println("newStudent ID = ${newStudent.id}")

    // Find that student by ID, should be the same instance in memory
    val trackedStudent = database.students.find(newStudent.id)
    assert(newStudent === trackedStudent)

    // Use a query to find that student, unattached so should be a different instance in memory
    val detachedStudent = database.students
        .where((Student::firstName eq "John") and (Student::lastName eq "Doe"))
        .detached()
        .one()!!
    assert(detachedStudent !== trackedStudent)
    assert(detachedStudent.id == newStudent.id)
    assertThrows<ChangeTracker.DuplicatePrimaryKeyException> { database.attach(detachedStudent) }

    // Delete all students with the last name Miller
    // zrm can't do this without loading everything into memory
    val allStudents = database.students.all()
    allStudents
        .filter { it.lastName == "Miller" }
        .forEach { database.remove(it) }

    // Before saving change the new student's name too (same transaction)
    newStudent.lastName = null
    database.saveChanges()

    // Remove the new student
    database.remove(newStudent)
    database.saveChanges()

    println()
}

fun main() {
    testDQL()
    testDML()

    println()
}