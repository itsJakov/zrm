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
    fun all() = query().all()
    fun where(expr: BoolConvertableExpr) = query().where(expr)
    fun include(property: KProperty1<E, *>) = query().include(property)
    fun <T : Any> orderBy(vararg columns: OrderedColumn) = query().orderBy(*columns)

    fun find(key: Any?): E?
        = where(EqualExpr(ColumnExpr(table.primaryKey), ConstExpr(key)))
        .one()

    // - Change Tracking demo
    private val attached = mutableListOf<Entry>()

    fun _add(e: E) {
        val entry = _attach(e)
        entry.state = Entry.State.INSERTED
    }

    fun _attach(e: E): Entry {
        val entry = Entry(e)
        attached.add(entry)
        return entry
    }

    fun _remove(e: E) {
        // This method should probably attach the entity if it wasn't already
        attached.firstOrNull { it.entity === e }?.state = Entry.State.DELETED
    }

    fun _detectChanges() {
        attached.forEach { it.detectChanges() }
    }

    fun _saveChanges() {
        for (entry in attached) {
            when (entry.state) {
                Entry.State.UNCHANGED -> continue
                Entry.State.INSERTED -> {
                    DMLExec.insert(entry.entity, database.connection)
                    entry.reset()
                }
                Entry.State.UPDATED -> {
                    DMLExec.update(entry.entity, entry.changedColumns, database.connection)
                    entry.reset()
                }
                Entry.State.DELETED -> {
                    DMLExec.delete(entry.entity, database.connection)
                    // probably needs to detach the entity
                }
            }
        }
    }

    // - SQL Calls
    fun fetchAll(sql: String, vararg params: Any)
        = DQLExec.all(table, sql, params.asSequence(), database.connection)

    fun fetchOne(sql: String, vararg params: Any)
        = DQLExec.one(table, sql, params.asSequence(), database.connection)

}