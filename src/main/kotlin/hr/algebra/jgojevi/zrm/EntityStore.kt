package hr.algebra.jgojevi.zrm

import hr.algebra.jgojevi.zrm.exec.DMLExec
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

    // - Change Tracking demo
    private val attached = mutableListOf<Entry>()

    fun _attach(e: E): Entry {
        val entry = Entry(e)
        attached.add(entry)
        return entry
    }

    fun _detectChanges() {
        attached.forEach { it.detectChanges() }
    }

    fun _saveChanges() {
        for (entry in attached) {
            when (entry.state) {
                Entry.State.UNCHANGED -> continue
                Entry.State.INSERT -> TODO()
                Entry.State.UPDATED -> {
                    DMLExec.update(entry.entity, entry.changedColumns, database.connection)
                    entry.reset()
                }
                Entry.State.DELETED -> TODO()
            }
        }
    }

    // - SQL Calls
    fun fetchAll(sql: String, params: Sequence<Any> = emptySequence())
        = DQLExec.all(table, sql, params, database.connection)

    fun fetchOne(sql: String, params: Sequence<Any> = emptySequence())
            = DQLExec.one(table, sql, params, database.connection)

}