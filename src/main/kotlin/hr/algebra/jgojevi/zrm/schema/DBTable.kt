package hr.algebra.jgojevi.zrm.schema

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

class DBTable<E : Any> private constructor(internal val tableClass: KClass<E>) {

    companion object {
        private val tables = mutableMapOf<KClass<*>, DBTable<*>>()

        fun <E : Any> of(entityClass: KClass<out E>): DBTable<E>
            = synchronized(tables) {
                tables.getOrPut(entityClass) {
                    DBTable(entityClass)
                } as DBTable<E>
            }

        // Convenience method
        fun <E : Any> of(entity: E) = of<E>(entity::class)
    }

    val name: String by lazy { tableClass.findAnnotation<Table>()?.name ?: tableClass.simpleName!! }
    val columns: List<DBColumn<E, *>>

    // Navigation properties mapped to their foreign key column
    val navigationProperties: Map<KProperty1<E, *>, DBColumn<E, *>> by lazy {
        columns.filter { it.foreignKeyPropertyName != null }.associateBy { col ->
            tableClass.memberProperties.first { it.name == col.foreignKeyPropertyName }
        }
    }

    val primaryKey: DBColumn<E, *> by lazy {
        val pk = columns.firstOrNull { it.isPrimaryKey }
        assert(pk != null) { "${tableClass.simpleName} must be have one @Key property." }
        return@lazy pk!!
    }

    init {
        assert(tableClass.isData) { "${tableClass.simpleName} is not a data class." }

        // Only data class properties defined in the constructor are valid as properties in the table
        val constructorProperties = tableClass.primaryConstructor!!.parameters

        columns = tableClass.memberProperties
            .filter { prop -> constructorProperties.any { it.name == prop.name } }
            .map { DBColumn.create(it, this) }
    }

}

