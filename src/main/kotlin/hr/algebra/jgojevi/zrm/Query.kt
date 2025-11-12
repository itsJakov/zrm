package hr.algebra.jgojevi.zrm

import hr.algebra.jgojevi.zrm.exec.DQLExec
import hr.algebra.jgojevi.zrm.schema.DBTable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class Query<E : Any> internal constructor(val table: DBTable<E>, val database: Database) {

    // - SQL
    private var select: String? = null
    private var from: String? = null
    private var join: String? = null
    private var where: String? = null
    private var orderBy: String? = null

    private var whereParams: Sequence<Any> = emptySequence()

    init {
        select = table.columns.joinToString(", ") { it.qualifiedName }
        from = "\"${table.name}\""
    }

    fun buildSQL(): String {
        val sql = StringBuilder()
        if (select != null) { sql.append("select $select ") }
        if (from != null) { sql.append("from $from ") }
        if (join != null) { sql.append("$join ") }
        if (where != null) { sql.append("where $where ") }
        if (orderBy != null) { sql.append("order by $orderBy ") }

        return sql.toString()
    }

    fun buildParams(): Sequence<Any>
        = whereParams

    // - Query building
    fun where(expr: BoolConvertableExpr): Query<E> {
        where = expr.sql()
        whereParams = expr.params()
        return this
    }

    fun include(property: KProperty1<E, *>): Query<E> {
        val other = DBTable.of(property.returnType.classifier as KClass<*>)

        val foreignKeyColumnName = table.navigationProperties[property]?.name ?: throw Exception("${property.name} is not a navigation property")

        // TODO: This only supports one join!
        select += ", ${other.columns.joinToString() { it.qualifiedName }}"
        join = "left join \"${other.name}\" on \"${table.name}\".\"$foreignKeyColumnName\" = ${other.primaryKey.qualifiedName}"

        return this
    }

    fun orderBy(vararg columns: OrderedColumn): Query<E> {
        orderBy = columns.joinToString(", ") {
            val order = if (it.desc) "desc" else "asc"
            "${it.column.qualifiedName} $order"
        }
        return this
    }

    // - Execution
    fun fetchAll(): List<E> = DQLExec.all(table, buildSQL(), buildParams(), database.connection)
    fun fetchOne(): E? = DQLExec.one(table, buildSQL(), buildParams(), database.connection)

}