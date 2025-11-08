package hr.algebra.jgojevi.zrm

import hr.algebra.jgojevi.zrm.schema.DBColumn
import kotlin.reflect.KProperty1

fun Expr._debugPrint() {
    println(this.sql())
    println(this.params().joinToString())
}

interface Expr {
    fun sql(): String
    fun params(): Sequence<Any> = emptySequence()
}

interface BoolConvertableExpr : Expr

class NotExpr(private val expr: Expr) : BoolConvertableExpr {
    override fun sql() = "not ${expr.sql()}"
    override fun params() = expr.params()
}

class BinaryExpr(private val operator: String, private val lhs: Expr, private val rhs: Expr) : BoolConvertableExpr {
    override fun sql(): String
        = "(${lhs.sql()}) $operator (${rhs.sql()})"

    override fun params(): Sequence<Any>
        = lhs.params() + rhs.params()
}

class EqualExpr(private val lhs: Expr, private val rhs: Expr, private val negated: Boolean = false) : BoolConvertableExpr {
    override fun sql(): String {
        val lhs = lhs.sql()
        val rhs = rhs.sql()
        assert(lhs != "null") { "Left side of the equality expression cannot be NULL!" }

        return if (rhs == "null") {
            val op = if (negated) "is not" else "is"
            "$lhs $op null"
        } else {
            val op = if (negated) "!=" else "="
            "$lhs $op $rhs"
        }
    }

    override fun params(): Sequence<Any>
        = lhs.params() + rhs.params()
}

class ConstExpr(private val value: Any?) : Expr {
    override fun sql() = if (value == null) "null" else "?"
    override fun params() = if (value == null) emptySequence() else sequenceOf(value)
}

class ColumnExpr <E : Any, T> (private val column: DBColumn<E, T>): Expr {
    constructor (property: KProperty1<E, T>) : this(DBColumn.of(property))
    override fun sql() = column.qualifiedName
}

infix fun <E : Any> KProperty1<E, Boolean?>.eq(value: Boolean?)
        = EqualExpr(ColumnExpr(this), ConstExpr(value))

infix fun <E : Any> KProperty1<E, Number?>.eq(value: Number?)
        = EqualExpr(ColumnExpr(this), ConstExpr(value))

infix fun <E : Any> KProperty1<E, String?>.eq(value: String?)
        = EqualExpr(ColumnExpr(this), ConstExpr(value))


infix fun <E : Any> KProperty1<E, Boolean?>.neq(value: Boolean?)
        = EqualExpr(ColumnExpr(this), ConstExpr(value), negated = true)

infix fun <E : Any> KProperty1<E, Number?>.neq(value: Number?)
        = EqualExpr(ColumnExpr(this), ConstExpr(value), negated = true)

infix fun <E : Any> KProperty1<E, String?>.neq(value: String?)
        = EqualExpr(ColumnExpr(this), ConstExpr(value), negated = true)


infix fun <E : Any> KProperty1<E, Number?>.gt(value: Number?)
        = BinaryExpr(">", ColumnExpr(this), ConstExpr(value))

infix fun <E : Any> KProperty1<E, Number?>.gte(value: Number?)
        = BinaryExpr(">=", ColumnExpr(this), ConstExpr(value))

infix fun <E : Any> KProperty1<E, Number?>.lt(value: Number?)
        = BinaryExpr("<", ColumnExpr(this), ConstExpr(value))

infix fun <E : Any> KProperty1<E, Number?>.lte(value: Number?)
        = BinaryExpr("<=", ColumnExpr(this), ConstExpr(value))

infix fun Expr.and(other: Expr) = BinaryExpr("and", this, other)
infix fun Expr.or(other: Expr) = BinaryExpr("or", this, other)
fun not(expr: Expr) = NotExpr(expr)