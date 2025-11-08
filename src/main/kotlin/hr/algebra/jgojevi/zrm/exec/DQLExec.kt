package hr.algebra.jgojevi.zrm.exec

import hr.algebra.jgojevi.zrm.schema.DBTable
import java.sql.Connection
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

internal object DQLExec {

    // TODO: This can fail in many many many different fun ways
    private fun <E : Any> decode(table: DBTable<E>, rs: ResultSet): E {
        val constructor = table.tableClass.primaryConstructor!!
        val params: Map<KParameter, Any> = constructor.parameters
            .associateWith { param ->
                // TODO: This code is slooooowww
                val column = table.columns.first { it.property.name == param.name }
                rs.getObject(column.name)
            }

        val entity = constructor.callBy(params)

        // Navigation properties
        for (navigationProperty in table.tableClass.memberProperties) {
            if (navigationProperty !is KMutableProperty<*>) { continue } // Navigation properties have to be a var (not val)
            val otherClass = navigationProperty.returnType.classifier as KClass<*>
            if (!otherClass.isData) { continue } // Entities are always data classes
            val table = DBTable.of(otherClass)

            try {
                val navEntity = decode(table, rs)
                navigationProperty.setter.call(entity, navEntity)
            } catch (_: Exception) {
                // Ignore for now, since we don't know if the join was even supposed to happen
            }
        }

        return entity
    }

    fun <E : Any> all(table: DBTable<E>, sql: String, sqlParams: Sequence<Any>, conn: Connection): List<E> {
        println("[DQLExec] $sql")

        conn.prepareStatement(sql).use { stmt ->
            for ((i, o) in sqlParams.withIndex()) {
                stmt.setObject(i+1, o)
            }

            val entities = mutableListOf<E>()
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    entities.add(decode(table, rs))
                }
            }
            return entities
        }
    }

    fun <E : Any> one(table: DBTable<E>, sql: String, sqlParams: Sequence<Any>, conn: Connection): E? {
        println("[DQLExec] $sql")

        conn.prepareStatement(sql).use { stmt ->
            for ((i, o) in sqlParams.withIndex()) {
                stmt.setObject(i+1, o)
            }

            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    return decode(table, rs)
                }
            }
            return null
        }
    }

}