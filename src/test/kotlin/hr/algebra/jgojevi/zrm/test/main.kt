package hr.algebra.jgojevi.zrm.test

import hr.algebra.jgojevi.zrm.Database
import hr.algebra.jgojevi.zrm.EntityStore
import hr.algebra.jgojevi.zrm.schema.Column
import hr.algebra.jgojevi.zrm.schema.ForeignKey
import hr.algebra.jgojevi.zrm.schema.Key
import hr.algebra.jgojevi.zrm.schema.Table

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
    @ForeignKey("album")
    var albumId: Int,

    @Column("title")
    var title: String,

    @Column("length")
    var length: Int
) {
    var album: Album? = null
}

class AppDatabase : Database("jdbc:postgresql://localhost/pepeka?user=postgres&password=Pa55w.rd") {
    lateinit var students: EntityStore<Student>
    lateinit var artists: EntityStore<Artist>
    lateinit var albums: EntityStore<Album>
    lateinit var songs: EntityStore<Song>
}

fun main() {
    val database = AppDatabase()

    val song = database.songs.find(23)!!
    database.saveChanges()

    song.title = "Borderline"

    database.saveChanges()
    database.saveChanges()

    println()
}