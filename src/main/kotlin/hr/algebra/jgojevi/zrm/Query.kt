package hr.algebra.jgojevi.zrm

import hr.algebra.jgojevi.zrm.exec.DQLExec
import hr.algebra.jgojevi.zrm.schema.DBTable

class Query<E : Any> internal constructor(val table: DBTable<E>, val database: Database) {

    // - SQL
    private var select: String? = null
    private var from: String? = null
    private var join: String? = null
    private var where: String? = null

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

    // - Execution
    fun fetchAll(): List<E> = DQLExec.all(table, buildSQL(), buildParams(), database.connection)
    fun fetchOne(): E? = DQLExec.one(table, buildSQL(), buildParams(), database.connection)

}