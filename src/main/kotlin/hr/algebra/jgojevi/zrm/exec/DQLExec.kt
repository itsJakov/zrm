package hr.algebra.jgojevi.zrm.exec

import hr.algebra.jgojevi.zrm.schema.DBTable
import java.sql.Connection
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberProperties

internal object DQLExec {

    // TODO: This can fail in many many many different fun ways
    private fun <E : Any> decode(table: DBTable<E>, rs: BetterResultSet): E {
        val params: Map<KParameter, Any?> = table.constructorParameters
            .mapValues { (_, column) -> rs.getObject(column) }

        val entity = table.constructor.callBy(params)

        // Navigation properties
        for ((navigationProperty, foreignKey) in table.navigationProperties) {
            if (navigationProperty !is KMutableProperty<*>) { continue } // Navigation properties have to be a var (not val)
            val otherClass = navigationProperty.returnType.classifier as KClass<*>
            val table = DBTable.of(otherClass)

            // TODO: Not handling to-many relationships
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

            return stmt.executeBetterQuery()
                .collect { decode(table, it) }
        }
    }

    fun <E : Any> one(table: DBTable<E>, sql: String, sqlParams: Sequence<Any>, conn: Connection): E? {
        println("[DQLExec] $sql")

        conn.prepareStatement(sql).use { stmt ->
            for ((i, o) in sqlParams.withIndex()) {
                stmt.setObject(i+1, o)
            }

            return stmt.executeBetterQuery()
                .mapOne { decode(table, it) }
        }
    }

}