package hr.algebra.jgojevi.zrm.test

import hr.algebra.jgojevi.zrm.*
import hr.algebra.jgojevi.zrm.schema.Key
import hr.algebra.jgojevi.zrm.schema.Table

@Table("students")
data class Student(
    @Key var student_id: Int,
    var first_name: String?,
    var last_name: String?,
    var enrollment_year: Int?,
)

class AppDatabase : Database("jdbc:postgresql://localhost/pepeka?user=postgres&password=Pa55w.rd") {
    lateinit var students: EntityStore<Student>
}

fun main() {
    val database = AppDatabase()

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

    val e1 = BinaryExpr(
        "or",
        BinaryExpr(
            "and",
            BinaryExpr(
                ">=",
                ColumnExpr(Student::enrollment_year),
                ConstExpr(2020)
            ),
            EqualExpr(
                ColumnExpr(Student::last_name),
                ConstExpr(null),
                negated = true
            )
        ),
        NotExpr(
            EqualExpr(
                ColumnExpr(Student::student_id),
                ConstExpr(12)
            )
        )
    )

    e1._debugPrint()

    println()
}