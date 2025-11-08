package hr.algebra.jgojevi.zrm.schema

import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

class DBColumn<E : Any, T> private constructor(internal val property: KProperty1<E, T>, private val table: DBTable<E>) {

    companion object {
        private val columns = mutableMapOf<KProperty1<*, *>, DBColumn<*, *>>()

        internal fun <E : Any, T> create(property: KProperty1<E, T>, table: DBTable<E>): DBColumn<E, T>
            = synchronized(columns) {
                val column = DBColumn(property, table)
                columns[property] = column
                return column
            }

        fun <E : Any, T> of(property: KProperty1<E, T>): DBColumn<E, T>
            = synchronized(columns) {
                val column = columns[property]
                assert(column != null) { "Column accessed before the DbTable was made. Internal bug! "}
                return@synchronized column!!
            } as DBColumn<E, T>
    }

    val name: String by lazy { property.findAnnotation<Column>()?.name ?: property.name }
    val qualifiedName: String by lazy { "\"${table.name}\".\"$name\"" }

    val isPrimaryKey: Boolean by lazy { property.hasAnnotation<Key>() }

    fun get(entity: E): T = property.get(entity)
    fun set(entity: E, value: Any) = (property as KMutableProperty1<E, T>).set(entity, value as T) // bad probably

}