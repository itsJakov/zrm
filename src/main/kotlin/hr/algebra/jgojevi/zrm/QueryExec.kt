package hr.algebra.jgojevi.zrm

import java.sql.Connection
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

internal object QueryExec {

    // TODO: This can fail in many many many different fun ways
    fun <E : Any> decode(entityClass: KClass<E>, rs: ResultSet): E {
        val constructor = entityClass.primaryConstructor!!
        val args: Map<KParameter, Any> = constructor.parameters.associateWith { rs.getObject(it.name!!) }

        val entity = constructor.callBy(args)

        // Navigation properties
        for (navigationProperty in entity::class.memberProperties) {
            if (navigationProperty !is KMutableProperty<*>) { continue }
            val otherClass = navigationProperty.returnType.classifier as KClass<*>
            if (!otherClass.isData) { continue }

            try {
                val navEntity = decode(otherClass, rs)
                navigationProperty.setter.call(entity, navEntity) // sets entity.nav = navEntity
            } catch (_: Exception) {
                // Ignore for now, since we don't know if the join was even supposed to happen
            }
        }

        return entity
    }


    fun <E : Any> all(conn: Connection, sql: String, entityClass: KClass<E>): List<E> {
        println("[QueryExec] $sql")

        val entities = mutableListOf<E>()
        conn.prepareStatement(sql).use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    entities.add(decode(entityClass, rs))
                }
            }
        }

        return entities
    }

    fun <E : Any> one(conn: Connection, sql: String, entityClass: KClass<E>): E? {
        println("[QueryExec] $sql")

        conn.prepareStatement(sql).use { stmt ->
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    return decode(entityClass, rs)
                }
            }
        }

        return null
    }

}