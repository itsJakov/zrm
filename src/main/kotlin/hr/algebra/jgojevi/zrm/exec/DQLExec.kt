package hr.algebra.jgojevi.zrm.exec

import hr.algebra.jgojevi.zrm.schema.DBTable
import java.sql.Connection
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberProperties

internal object DQLExec {

    // TODO: This can fail in many many many different fun ways
    private fun <E : Any> decode(table: DBTable<E>, rs: BetterResultSet): E {
        val params: Map<KParameter, Any?> = table.constructorParameters
            .mapValues { (_, column) -> rs.getObject(column) }

        val entity = table.constructor.callBy(params)

        // To one navigation properties
        for ((navigationProperty, foreignKey) in table.navigationProperties) {
            if (navigationProperty !is KMutableProperty<*>) { continue } // Navigation properties have to be a var (not val)
            val otherClass = navigationProperty.returnType.classifier as KClass<*>
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

            var currentEntity: E? = null
            var currentPrimaryKey: Any? = null
            stmt.executeBetterQuery().forEach { rs ->
                val primaryKey = rs.getObject(table.primaryKey)
                if (currentPrimaryKey != primaryKey) {
                    currentPrimaryKey = primaryKey
                    currentEntity = decode(table, rs)
                    entities.add(currentEntity)

                    table.tableClass.memberProperties
                        .asSequence()
                        .mapNotNull { it as? KMutableProperty1<E, *> }
                        .filter { (it.returnType.classifier as KClass<*>) == MutableList::class }
                        .forEach {
                            val other = DBTable.of(it.returnType.arguments.first().type?.classifier as KClass<*>)
                            if (!rs.containsColumn(other.primaryKey)) { return@forEach }
                            it.setter.call(currentEntity, mutableListOf<Any>())
                        }
                }

                table.tableClass.memberProperties
                    .asSequence()
                    .mapNotNull { it as? KMutableProperty1<E, *> }
                    .filter { (it.returnType.classifier as KClass<*>) == MutableList::class }
                    .forEach {
                        val other = DBTable.of(it.returnType.arguments.first().type?.classifier as KClass<*>)
                        if (!rs.containsColumn(other.primaryKey)) { return@forEach }
                        if (rs.getObject(other.primaryKey) == null) { return@forEach }

                        val list = it.get(currentEntity!!) as MutableList<Any>
                        list.add(decode(other, rs))
                    }
            }
            return entities
        }
    }

    fun <E : Any> one(table: DBTable<E>, sql: String, sqlParams: Sequence<Any>, conn: Connection): E? {
        // TODO: Do this properly
        return all(table, sql, sqlParams, conn).firstOrNull()
    }

}