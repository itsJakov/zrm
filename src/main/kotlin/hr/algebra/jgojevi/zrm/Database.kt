package hr.algebra.jgojevi.zrm

import java.sql.DriverManager
import java.util.LinkedList
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
                val entityType = it.returnType.arguments[0].type!!.classifier as KClass<*>
                val store = EntityStore(entityType, this)
                it.setter.call(this, store)
                entityStores[entityType] = store
            }

        this.entityStores = entityStores
    }


    private val trackedEntities = mutableListOf<Any>()
    private val insertQueue = LinkedList<Any>()

    fun add(entity: Any) {
        trackedEntities.add(entity)
        insertQueue.add(entity)
    }

    fun save() {
        while (insertQueue.isNotEmpty()) {
            val entity = insertQueue.removeFirst()

            println("Inserting $entity")
            DMLExec.insert(entity, connection)
        }
    }

}