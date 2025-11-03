package hr.algebra.jgojevi.zrm

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

interface Expr {
    fun eval(): String
}

interface BoolConvertableExpr : Expr

class NotExpr(private val expr: Expr) : BoolConvertableExpr {
    override fun eval(): String
            = "not ${expr.eval()}"
}

class BinaryExpr(private val operator: String, private val lhs: Expr, private val rhs: Expr) : BoolConvertableExpr {
    override fun eval(): String
        = "(${lhs.eval()}) $operator (${rhs.eval()})"
}

class EqualExpr(private val lhs: Expr, private val rhs: Expr, private val negated: Boolean = false) : BoolConvertableExpr {
    override fun eval(): String {
        val lhs = lhs.eval()
        val rhs = rhs.eval()
        assert(lhs != "null") { "Left side of the equality expression cannot be NULL!" }

        return if (rhs == "null") {
            val op = if (negated) "is not" else "is"
            "$lhs $op null"
        } else {
            val op = if (negated) "!=" else "="
            "$lhs $op $rhs"
        }
    }
}

class BoolConst(private val value: Boolean) : BoolConvertableExpr {
    override fun eval(): String
        = value.toString()
}

class StringConst(private val value: String?) : Expr {
    override fun eval(): String
        = if (value == null) "null" else "'$value'"
}

class NumberConst(private val value: Number?) : Expr {
    override fun eval(): String
        = value?.toString() ?: "null"
}

// TODO: This all kinda sucks
class ColumnExpr(private val entityType: KClass<*>, private val property: KProperty<*>) : Expr {
    override fun eval(): String
        = "\"${EntityTable(entityType).name}\".\"${property.name}\""
}


inline infix fun <reified E> KProperty1<E, Boolean>.eq(value: Boolean)
    = EqualExpr(ColumnExpr(E::class, this), BoolConst(value))

inline infix fun <reified E> KProperty1<E, String?>.eq(value: String?)
    = EqualExpr(ColumnExpr(E::class, this), StringConst(value))

inline infix fun <reified E> KProperty1<E, Number?>.eq(value: Number?)
    = EqualExpr(ColumnExpr(E::class, this), NumberConst(value))


inline infix fun <reified E> KProperty1<E, Boolean>.neq(value: Boolean)
    = EqualExpr(ColumnExpr(E::class, this), BoolConst(value), negated = true)

inline infix fun <reified E> KProperty1<E, String?>.neq(value: String?)
    = EqualExpr(ColumnExpr(E::class, this), StringConst(value), negated = true)

inline infix fun <reified E> KProperty1<E, Number?>.neq(value: Number?)
    = EqualExpr(ColumnExpr(E::class, this), NumberConst(value), negated = true)


inline infix fun <reified E> KProperty1<E, Number?>.gt(value: Number?)
    = BinaryExpr(">", ColumnExpr(E::class, this), NumberConst(value))

inline infix fun <reified E> KProperty1<E, Number?>.gte(value: Number?)
    = BinaryExpr(">=", ColumnExpr(E::class, this), NumberConst(value))

inline infix fun <reified E> KProperty1<E, Number?>.lt(value: Number?)
    = BinaryExpr("<", ColumnExpr(E::class, this), NumberConst(value))

inline infix fun <reified E> KProperty1<E, Number?>.lte(value: Number?)
    = BinaryExpr("<=", ColumnExpr(E::class, this), NumberConst(value))


infix fun Expr.and(other: Expr) = BinaryExpr("and", this, other)

infix fun Expr.or(other: Expr) = BinaryExpr("or", this, other)

fun not(expr: Expr) = NotExpr(expr)