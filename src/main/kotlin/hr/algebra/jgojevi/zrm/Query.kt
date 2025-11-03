package hr.algebra.jgojevi.zrm

import java.sql.Connection
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation

// TODO: Fix SQL injection (use prepared statements)
class Query<E : Any> internal constructor(private val conn: Connection, private val table: EntityTable<E>) {

    // - SQL string building
    private var select: String? = null
    private var from: String? = null
    private var join: String? = null
    private var where: String? = null

    init {
        select = table.columns.joinToString(", ") { it.qualifiedName }
        from = "\"${table.name}\""
    }

    internal fun buildSQL(): String {
        val sql = StringBuilder()
        if (select != null) { sql.append("select $select ") }
        if (from != null) { sql.append("from $from ") }
        if (join != null) { sql.append("$join ") }
        if (where != null) { sql.append("where $where ") }

        return sql.toString()
    }

    // - Query modifiers
    fun where(expr: BoolConvertableExpr): Query<E> {
        where = expr.eval()
        return this
    }

    fun include(property: KProperty1<E, *>): Query<E> {
        val other = EntityTable(property.returnType.classifier as KClass<*>)

        val foreignKeyColumnName = property.findAnnotation<ForeignKey>()?.column
        assert(foreignKeyColumnName != null) { "Navigation Property doesn't have a @ForeignKey annotation" }

        val joinType = if (property.returnType.isMarkedNullable) { "left" } else { "inner" }

        // TODO: This only supports one join!
        select += ", ${other.columns.joinToString { it.qualifiedName }}"
        join = "$joinType join ${other.name} on ${table.name}.$foreignKeyColumnName = ${other.primaryKey.qualifiedName}"

        return this
    }

    // - Execution
    fun fetchAll(): List<E> = QueryExec.all(conn, buildSQL(), table.entityClass)
    fun fetchOne(): E? = QueryExec.one(conn, buildSQL(), table.entityClass)

}