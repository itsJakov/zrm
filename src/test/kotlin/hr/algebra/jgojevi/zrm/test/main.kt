package hr.algebra.jgojevi.zrm.test

import hr.algebra.jgojevi.zrm.*
import hr.algebra.jgojevi.zrm.schema.Column
import hr.algebra.jgojevi.zrm.schema.Key
import hr.algebra.jgojevi.zrm.schema.Table

@Table("students")
data class Student(
    @Key
    @Column("student_id")
    var id: Int,

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

    val allThings = database.things.include(Things::student).fetchAll()

    val allStudents = database.students.all()

    val ivys = database.students
        .where((Student::firstName eq "Ivy") and (Student::lastName eq "Brown"))
        .fetchAll()

    val noEnrollment = database.students
        .where(Student::enrollmentYear eq null)
        .fetchOne()

    val after2022 = database.students
        .where(Student::enrollmentYear gt 2022)
        .fetchAll()

    val e1 = BinaryExpr(
        "or",
        BinaryExpr(
            "and",
            BinaryExpr(
                ">=",
                ColumnExpr(Student::enrollmentYear),
                ConstExpr(2020)
            ),
            EqualExpr(
                ColumnExpr(Student::lastName),
                ConstExpr(null),
                negated = true
            )
        ),
        NotExpr(
            EqualExpr(
                ColumnExpr(Student::id),
                ConstExpr(12)
            )
        )
    )

    e1._debugPrint()

    println()
}