package hr.algebra.jgojevi.zrm

import hr.algebra.jgojevi.zrm.exec.DQLExec
import hr.algebra.jgojevi.zrm.schema.DBColumn
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

    fun include(property: KProperty1<*, *>): Query<E> {
        val table = DBTable.of(property.getter.parameters.first().type.classifier as KClass<*>)

        if (join == null) join = ""

        val clazz = property.returnType.classifier as KClass<*>

        val other: DBTable<*>
        val foreignKey: DBColumn<*, *>

        if (clazz == List::class) { // To many
            other = DBTable.of(property.returnType.arguments.first().type?.classifier as KClass<*>)
            foreignKey = other.foreignKeyByTable[table] ?: throw Exception("${property.name} is not a navigation property")

            join += "left join \"${other.name}\" on ${table.primaryKey.qualifiedName} = ${foreignKey.qualifiedName} "
        } else { // To one
            other = DBTable.of(clazz)
            foreignKey = table.navigationProperties[property] ?: throw Exception("${property.name} is not a navigation property")

            join += "left join \"${other.name}\" on ${foreignKey.qualifiedName} = ${other.primaryKey.qualifiedName} "
        }

        select += ", ${other.columns.joinToString { it.qualifiedName }}"

        return this
    }

    fun orderBy(vararg columns: OrderedColumn): Query<E> {
        orderBy = columns.joinToString(", ") {
            val order = if (it.desc) "desc" else "asc"
            "${it.column.qualifiedName} $order"
        }
        return this
    }

    fun _orderByRandom(): Query<E> {
        orderBy = "random()"
        return this
    }

    // - Execution
    fun all(): List<E> = DQLExec.all(table, buildSQL(), buildParams(), database.connection)
    fun one(): E? = DQLExec.one(table, buildSQL(), buildParams(), database.connection)

}