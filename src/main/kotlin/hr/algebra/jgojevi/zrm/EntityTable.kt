package hr.algebra.jgojevi.zrm

import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

class EntityColumn(internal val property: KMutableProperty<*>, private val table: EntityTable<*>) {

    val isPrimaryKey: Boolean by lazy { property.hasAnnotation<Key>() }

    val name: String by lazy { property.name }
    val qualifiedName: String by lazy { "\"${table.name}\".\"$name\"" } // TODO: Should this be the job of the SQL builder?

}

class EntityTable<E : Any>(internal val entityClass: KClass<E>) {

    init {
        assert(entityClass.isData) { "$entityClass must be a data class." }
        assert(entityClass.primaryConstructor!!.parameters.isNotEmpty()) { "$entityClass must have at least one column." }
    }

    val name: String by lazy { entityClass.findAnnotation<Table>()?.name ?: entityClass.simpleName!! }

    val columns: List<EntityColumn> by lazy {
        // Only data class properties defined in the constructor are valid as properties in the table
        val constructorProperties = entityClass.primaryConstructor!!.parameters

        entityClass.memberProperties
            .filter { prop -> constructorProperties.any { it.name == prop.name } }
            .map { it as KMutableProperty<*> }
            .map { EntityColumn(it, this) }
    }

    val primaryKey: EntityColumn by lazy {
        val pk = columns.firstOrNull { it.isPrimaryKey }
        assert(pk != null) { "$entityClass must be have one @Key property." }
        return@lazy pk!!
    }

}