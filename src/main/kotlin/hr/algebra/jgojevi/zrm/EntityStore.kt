package hr.algebra.jgojevi.zrm

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.instanceParameter

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

    // - Change Tracking
    class TrackedEntity<E : Any>(val entity: E) {

        // TODO: bad idea lmao
        fun copyViaReflection(): E {
            val kclass = entity::class
            val copyMethod = kclass.members.find { it.name == "copy" }!!
            val instanceParam = copyMethod.instanceParameter!!
            return copyMethod.callBy(mapOf(instanceParam to entity)) as E
        }

        private var og = copyViaReflection()

        fun update() {
            state = State.UNCHANGED
            og = copyViaReflection()
        }

        enum class State {
            UNCHANGED, INSERT, UPDATED, DELETED
        }
        internal var state = State.UNCHANGED

        var changedColumns = listOf<EntityColumn>()

        fun detectChanges() {
            if (state != State.UNCHANGED) { return }

            val changedColumns = mutableListOf<EntityColumn>()
            for (column in EntityTable(og::class).columns) {
                val ogValue = column.property.getter.call(og)
                val currentValue = column.property.getter.call(entity)

                if (currentValue != ogValue) {
                    state = State.UPDATED
                    changedColumns.add(column)
                }
            }
            this.changedColumns = changedColumns
        }

    }

    private val trackedEntities = mutableListOf<TrackedEntity<E>>()

    fun detectChanges() {
        trackedEntities.forEach { it.detectChanges() }
    }

    fun attach(entity: E) {
        trackedEntities.add(TrackedEntity(entity))
    }

    fun add(entity: E) {
        val te = TrackedEntity(entity)
        te.state = TrackedEntity.State.INSERT
        trackedEntities.add(te)
        //database.add(entity)
    }

    fun remove(entity: E) {
        val te = trackedEntities.firstOrNull { it.entity === entity }
        assert(te != null) { "Cannot remove an untracked entity: $entity" }
        te?.state = TrackedEntity.State.DELETED
    }

    fun save() {
        println("Saving...")
        detectChanges()

        for (entity in trackedEntities) {
            when (entity.state) {
                TrackedEntity.State.UNCHANGED -> { continue }
                TrackedEntity.State.INSERT -> {
                    println("Inserting entity ${entity.entity}")
                    DMLExec.insert(entity.entity, database.connection)
                    entity.update()
                }
                TrackedEntity.State.UPDATED -> {
                    println("Updating entity ${entity.entity} changed: ${entity.changedColumns.joinToString { it.name }}")
                    DMLExec.update(entity, database.connection)
                    entity.update()
                }
                TrackedEntity.State.DELETED -> {
                    println("Deleting entity ${entity.entity}")
                    DMLExec.delete(entity.entity, database.connection)
                    trackedEntities.removeIf { it.entity === entity } // detach entity
                }
            }
        }
    }

}