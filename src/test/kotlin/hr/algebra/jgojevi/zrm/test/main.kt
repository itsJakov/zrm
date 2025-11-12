package hr.algebra.jgojevi.zrm.test

import hr.algebra.jgojevi.zrm.*
import hr.algebra.jgojevi.zrm.schema.Column
import hr.algebra.jgojevi.zrm.schema.DBTable
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
)

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
}

class AppDatabase : Database("jdbc:postgresql://localhost/pepeka?user=postgres&password=Pa55w.rd") {
    lateinit var students: EntityStore<Student>
    lateinit var artists: EntityStore<Artist>
    lateinit var albums: EntityStore<Album>
}

fun main() {
    val database = AppDatabase()

    val allAlbums = database.albums
        .include(Album::artist)
        .fetchAll()

    val ok = DBTable.of(Album::class).navigationProperties

    println()
}