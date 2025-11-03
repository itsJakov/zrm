package hr.algebra.jgojevi.zrm

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class EntityStore<E : Any> internal constructor(private val entityClass: KClass<E>, private val database: Database) {

    internal val table = EntityTable(entityClass)

    fun query() = Query(database.connection, table)

    // - Convenience methods (to prevent code like this: query().where().fetchAll()
    fun all(): List<E> = query().fetchAll()
    fun where(expr: BoolConvertableExpr): Query<E> = query().where(expr)
    fun include(property: KProperty1<E, *>): Query<E> = query().include(property)

    // - Raw SQL Calls
    // TODO: Allow prepared statements for raw SQL
    fun fetchAll(sql: String) = QueryExec.all(database.connection, sql, entityClass)
    fun fetchOne(sql: String) = QueryExec.one(database.connection, sql, entityClass)


}