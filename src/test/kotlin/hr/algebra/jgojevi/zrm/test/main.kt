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

    val allThings = database.things.include(Things::student).fetchAll()

    val allStudents = database.students.all()

    val ivys = database.students
        .where((Student::first_name eq "Ivy") and (Student::last_name eq "Brown"))
        .fetchAll()

    val noEnrollment = database.students
        .where(Student::enrollment_year eq null)
        .fetchOne()

    val after2022 = database.students
        .where(Student::enrollment_year gt 2022)
        .fetchAll()



    println()

    /*val expr1 =
        BinaryExpr(
            "or",
            BinaryExpr(
                "and",
                BinaryExpr(
                    ">=",
                    ColumnExpr(Student::class, Student::enrollment_year),
                    NumberConst(2020)
                ),
                EqualExpr(
                    ColumnExpr(Student::class, Student::last_name),
                    StringConst(null),
                    negated = true
                )
            ),
            not(
                EqualExpr(
                    ColumnExpr(Student::class, Student::student_id),
                    NumberConst(12)
                )
            )
        )

    val expr2 =
        ((Student::enrollment_year gte 2020) and (Student::last_name neq null)) or not((Student::student_id eq 12))*/
}