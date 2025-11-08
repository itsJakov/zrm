package hr.algebra.jgojevi.zrm

import hr.algebra.jgojevi.zrm.exec.DQLExec
import hr.algebra.jgojevi.zrm.schema.DBTable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class EntityStore<E : Any> internal constructor(entityClass: KClass<E>, private val database: Database) {

    internal val table = DBTable.of(entityClass)

    fun query() = Query(table, database)

    // - Convenience methods (to prevent code like this: query().where().fetchAll()
    fun all() = query().fetchAll()
    fun where(expr: BoolConvertableExpr) = query().where(expr)
    fun include(property: KProperty1<E, *>) = query().include(property)

    fun find(key: Any?): E?
        = where(EqualExpr(ColumnExpr(table.primaryKey), ConstExpr(key)))
        .fetchOne()

    // - SQL Calls
    fun fetchAll(sql: String, params: Sequence<Any> = emptySequence())
        = DQLExec.all(table, sql, params, database.connection)

    fun fetchOne(sql: String, params: Sequence<Any> = emptySequence())
            = DQLExec.one(table, sql, params, database.connection)

}