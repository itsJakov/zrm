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
        for (entry in changeTracker.entries) {
            when (entry.state) {
                Entry.State.UNCHANGED -> continue
                Entry.State.INSERTED -> {
                    DMLExec.insert(entry.entity, connection)
                    entry.reset()
                }
                Entry.State.UPDATED -> {
                    DMLExec.update(entry.entity, entry.changedColumns, connection)
                    entry.reset()
                }
                Entry.State.DELETED -> {
                    DMLExec.delete(entry.entity, connection)
                    changeTracker.detach(entry.entity)
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
        val entry = attach(entity)
        entry.state = Entry.State.DELETED
    }

}