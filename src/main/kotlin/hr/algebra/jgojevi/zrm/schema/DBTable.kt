package hr.algebra.jgojevi.zrm.schema

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

class DBTable<E : Any> private constructor(internal val tableClass: KClass<E>) {

    companion object {
        private val tables = mutableMapOf<KClass<*>, DBTable<*>>()

        fun <E : Any> of(entityClass: KClass<out E>): DBTable<E>
            = synchronized(tables) {
                @Suppress("UNCHECKED_CAST") // The tables hashmap will ensure type consistency
                tables.getOrPut(entityClass) {
                    DBTable(entityClass)
                } as DBTable<E>
            }

        // Convenience method
        fun <E : Any> of(entity: E) = of<E>(entity::class)
    }

    val name: String by lazy { tableClass.findAnnotation<Table>()?.name ?: tableClass.simpleName!! }
    val columns: List<DBColumn<E, *>>

    internal val constructor = tableClass.primaryConstructor!!
    internal val constructorParameters: Map<KParameter, DBColumn<E, *>> by lazy {
        // This is still a bit slow
        tableClass.primaryConstructor!!.parameters.associateWith {
            p -> columns.first { it.property.name == p.name }
        }
    }

    // Navigation properties mapped to their foreign key column
    // Example: Album::artist -> DBColumn(table=Album, name="artist_id")
    // Used by the query builder to find out what foreign key column is mapped to the navigation property,
    //  when doing a to-one navigation property
    internal val navigationProperties: Map<KProperty1<E, *>, DBColumn<E, *>> by lazy {
        columns.filter { it.foreignKeyPropertyName != null }.associateBy { col ->
            tableClass.memberProperties.first { it.name == col.foreignKeyPropertyName }
        }
    }

    // Tables mapped to their foreign key column
    // Example: DBTable(Artist) -> DBColumn(table=Album, name="artist_id")
    // Used by the query builder to find out what foreign key column is referencing the given table,
    //  when doing a to-many navigation property.
    // This whole system could fail if a table has multiple foreign keys to the same table
    internal val foreignKeyByTable: Map<DBTable<*>, DBColumn<E, *>> by lazy { // messy
        navigationProperties.mapKeys { (property, column) ->
            DBTable.of(property.returnType.classifier as KClass<*>)
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

