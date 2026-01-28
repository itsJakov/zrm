package hr.algebra.jgojevi.zrm.exec

import hr.algebra.jgojevi.zrm.changes.ChangeTracker
import hr.algebra.jgojevi.zrm.schema.DBTable
import java.sql.Connection
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties

private typealias Ctx = MutableMap<Pair<DBTable<*>, Any>, Any>  // (Table, Primary Key) -> Entity

internal object DQLExec {

    private fun <E : Any> instantiate(table: DBTable<E>, rs: BetterResultSet, ctx: Ctx, changeTracker: ChangeTracker?): E {
        if (changeTracker != null) {
            val pk = rs.getObject(table.primaryKey)!!
            val entity = changeTracker.findEntry(table, pk)
            if (entity != null) return entity
        }

        val params: Map<KParameter, Any?> = table.constructorParameters
            .mapValues { (_, column) -> rs.getObject(column) }

        val entity = table.constructor.callBy(params)
        changeTracker?.attach(entity)

        for (property in table.tableClass.memberProperties) {
            if ((property.returnType.classifier as KClass<*>).isSubclassOf(MutableList::class)) { // To-many
                val property = property as? KMutableProperty1<E, MutableList<Any>?> ?: continue
                val otherTable = DBTable.of(property.returnType.arguments.first().type?.classifier as KClass<*>)
                if (!rs.containsColumn(otherTable.primaryKey)) continue // TODO: Bad way of checking for joins

                val list: MutableList<Any>? = property.get(entity)
                if (list == null) {
                    property.set(entity, mutableListOf())
                }
            } else { // To-one
                val property = property as? KMutableProperty1<E, Any?> ?: continue

                if (!table.navigationProperties.containsKey(property)) continue
                val otherTable = DBTable.of(property.returnType.classifier as KClass<*>)

                if (!rs.containsColumn(otherTable.primaryKey)) continue
                if (rs.getObject(otherTable.primaryKey) == null) continue

                val otherEntity = instantiate(otherTable, rs, ctx, changeTracker)
                property.set(entity, otherEntity)
            }
        }

        return entity
    }

    private fun <E : Any> setInverseNavigation(table: DBTable<E>, entity: E, rs: BetterResultSet, ctx: Ctx, changeTracker: ChangeTracker?) {
        table.tableClass.memberProperties
            .asSequence()
            .filter { (it.returnType.classifier as KClass<*>).isSubclassOf(MutableList::class) }
            .mapNotNull { it as? KMutableProperty1<E, MutableList<Any>?> }
            .forEach { property ->
                val otherTable = DBTable.of(property.returnType.arguments.first().type?.classifier as KClass<*>)
                if (!rs.containsColumn(otherTable.primaryKey)) return@forEach // Table not joined
                val list: MutableList<Any> = property.get(entity)!!

                val otherPk = rs.getObject(otherTable.primaryKey) ?: return@forEach

                val other = ctx.getOrPut(otherTable to otherPk) {
                    val e = instantiate(otherTable, rs, ctx, changeTracker)
                    list.add(e)
                    e
                }

                setInverseNavigation(otherTable, other, rs, ctx, changeTracker)
            }
    }

    fun <E : Any> all(table: DBTable<E>, sql: String, sqlParams: Sequence<Any>, conn: Connection, changeTracker: ChangeTracker?): List<E> {
        println("[DQLExec] $sql")

        conn.prepareStatement(sql).use { stmt ->
            for ((i, o) in sqlParams.withIndex()) {
                stmt.setObject(i+1, o)
            }

            val entities = mutableListOf<E>()
            val ctx: Ctx = mutableMapOf()

            stmt.executeBetterQuery().use { rs ->
                while (rs.next()) {
                    val pk = rs.getObject(table.primaryKey)!!
                    val entity = ctx.getOrPut(table to pk) {
                        val e = instantiate(table, rs, ctx, changeTracker)
                        entities.add(e)
                        e
                    } as E

                    setInverseNavigation(table, entity, rs, ctx, changeTracker)
                }
            }

            return entities
        }
    }

    fun <E : Any> one(table: DBTable<E>, sql: String, sqlParams: Sequence<Any>, conn: Connection, changeTracker: ChangeTracker?): E? {
        // TODO: Do this properly
        return all(table, sql, sqlParams, conn, changeTracker).firstOrNull()
    }

}