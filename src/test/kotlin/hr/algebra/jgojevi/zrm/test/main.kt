package hr.algebra.jgojevi.zrm.test

import hr.algebra.jgojevi.zrm.*

@Table("students")
data class Student(
    @Key var student_id: Int,
    var first_name: String?,
    var last_name: String?,
    var enrollment_year: Int?,
)

@Table("things")
data class Things(
    @Key var thing_id: Int,
    var thing_name: String?,
    var student_id: Int?,
) {
    @ForeignKey("student_id")
    var student: Student? = null
}

class AppDatabase : Database("jdbc:postgresql://localhost/pepeka?user=postgres&password=Pa55w.rd") {

    lateinit var students: EntityStore<Student>
    lateinit var things: EntityStore<Things>

}

fun main() {
    val database = AppDatabase()


    val newStudent = Student(
        student_id = 0,
        first_name = "Jakov",
        last_name = "GZ",
        enrollment_year = 2023,
    )

    database.students.add(newStudent)
    database.students.save()

    newStudent.last_name = "Spojnica"
    database.students.save()

    database.students.remove(newStudent)
    database.students.save()

//    database.save()
//    val allStudents = database.students.all()

    println()
}