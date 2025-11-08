package hr.algebra.jgojevi.zrm.test

import hr.algebra.jgojevi.zrm.*
import hr.algebra.jgojevi.zrm.schema.Column
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

@Table("things")
data class Things(
    @Key
    @Column("thing_id")
    var id: Int,

    @Column("thing_name")
    var thingName: String?,

    @Column("student_id")
    var studentId: Int?,
) {
    var student: Student? = null
}

class AppDatabase : Database("jdbc:postgresql://localhost/pepeka?user=postgres&password=Pa55w.rd") {
    lateinit var students: EntityStore<Student>
    lateinit var things: EntityStore<Things>
}

fun main() {
    val database = AppDatabase()

    val student = Student(
        firstName = "John",
        lastName = "Doe",
        enrollmentYear = 1000,
    )

    database.students._add(student)
    database.students._detectChanges()
    database.students._saveChanges()

    student.lastName = "Pork"
    database.students._detectChanges()
    database.students._saveChanges()

    database.students._remove(student)
    database.students._detectChanges()
    database.students._saveChanges()

    println()
}