package hr.algebra.jgojevi.zrm

import hr.algebra.jgojevi.zrm.changes.ChangeTracker
import hr.algebra.jgojevi.zrm.changes.Entry
import hr.algebra.jgojevi.zrm.exec.DMLExec
import hr.algebra.jgojevi.zrm.schema.DBTable
import java.sql.DriverManager
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

open class Database(connectionString: String) {

    internal val connection = DriverManager.getConnection(connectionString)!!
    private val entityStores: Map<KClass<*>, EntityStore<*>>

    init {
        val entityStores = mutableMapOf<KClass<*>, EntityStore<*>>()
        this::class.memberProperties
            .filter { it.returnType.classifier == EntityStore::class }
            .mapNotNull { it as? KMutableProperty<*> }
            .forEach {
                val entityType = it.returnType.arguments[0].type?.classifier as? KClass<*> ?: return@forEach
                val store = EntityStore(entityType, this)
                it.setter.call(this, store)
                entityStores[entityType] = store
            }

        this.entityStores = entityStores
    }

    internal fun getAllTables() =
        entityStores.keys.asSequence()
            .map { DBTable.of(it) }


    val changeTracker = ChangeTracker()

    fun detectChanges() = changeTracker.detectChanges()

    fun saveChanges() {
        detectChanges()
        DMLExec.inTransaction(connection) { dml ->
            for (entry in changeTracker.entries) {
                when (entry.state) {
                    Entry.State.UNCHANGED -> continue
                    Entry.State.INSERTED -> {
                        // This detach/attach dance is here as a quick hack
                        // Because DMLExec.insert will modify the primary key of the entity,
                        // The Change Tracker hashmap will use the old primary key value (probably 0)
                        changeTracker.detach(entry.entity)
                        dml.insert(entry.entity)
                        changeTracker.attach(entry.entity)
                    }
                    Entry.State.UPDATED -> {
                        dml.update(entry.entity, entry.changedColumns)
                        entry.reset()
                    }
                    Entry.State.DELETED -> {
                        dml.delete(entry.entity)
                        changeTracker.detach(entry.entity)
                    }
                }
            }
        }
    }

    fun attach(entity: Any) = changeTracker.attach(entity)

    fun add(entity: Any) {
        val entry = attach(entity)
        entry.state = Entry.State.INSERTED
    }

    fun remove(entity: Any) {
        val entry = changeTracker.attachIfNeeded(entity)
        entry.state = Entry.State.DELETED
    }

}