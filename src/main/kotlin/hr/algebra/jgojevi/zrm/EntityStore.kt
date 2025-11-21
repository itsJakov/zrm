package hr.algebra.jgojevi.zrm

import hr.algebra.jgojevi.zrm.exec.DQLExec
import hr.algebra.jgojevi.zrm.schema.DBTable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class EntityStore<E : Any> internal constructor(entityClass: KClass<E>, private val database: Database) {

    internal val table = DBTable.of(entityClass)

    fun query() = Query(table, database)

    // - Convenience methods (to prevent code like this: query().where().fetchAll()
    fun all() = query().all()
    fun where(expr: BoolConvertableExpr) = query().where(expr)
    fun include(property: KProperty1<*, *>) = query().include(property)
    fun <T : Any> orderBy(vararg columns: OrderedColumn) = query().orderBy(*columns)

    fun find(key: Any?): E? =
        where(EqualExpr(ColumnExpr(table.primaryKey), ConstExpr(key)))
            .one()

    // - SQL Calls
    fun fetchAll(sql: String, vararg params: Any) =
        DQLExec.all(table, sql, params.asSequence(), database.connection)

    fun fetchOne(sql: String, vararg params: Any) =
        DQLExec.one(table, sql, params.asSequence(), database.connection)

}