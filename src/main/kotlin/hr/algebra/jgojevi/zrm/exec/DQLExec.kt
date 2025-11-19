package hr.algebra.jgojevi.zrm.exec

import hr.algebra.jgojevi.zrm.schema.DBTable
import java.sql.Connection
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties

private typealias Ctx = MutableMap<Pair<DBTable<*>, Any>, Any>  // (Table, Primary Key) -> Entity

internal object DQLExec {

    private fun <E : Any> instantiate(table: DBTable<E>, rs: BetterResultSet, ctx: Ctx): E {
        val params: Map<KParameter, Any?> = table.constructorParameters
            .mapValues { (_, column) -> rs.getObject(column) }

        val entity = table.constructor.callBy(params)

        table.tableClass.memberProperties
            .asSequence()
            .filter { (it.returnType.classifier as KClass<*>).isSubclassOf(MutableList::class) }
            .mapNotNull { it as? KMutableProperty1<E, MutableList<Any>?> }
            .forEach { property ->
                val otherTable = DBTable.of(property.returnType.arguments.first().type?.classifier as KClass<*>)
                if (!rs.containsColumn(otherTable.primaryKey)) return@forEach // Table not joined

                var list: MutableList<Any>? = property.get(entity)
                if (list == null) {
                    list = mutableListOf()
                    property.set(entity, list)
                }
            }

        return entity
    }

    private fun <E : Any> setNavigation(table: DBTable<E>, entity: E, rs: BetterResultSet, ctx: Ctx) {
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
                    val e = instantiate(otherTable, rs, ctx)
                    list.add(e)
                    e
                }

                setNavigation(otherTable, other, rs, ctx)
            }
    }

    fun <E : Any> all(table: DBTable<E>, sql: String, sqlParams: Sequence<Any>, conn: Connection): List<E> {
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
                        val e = instantiate(table, rs, ctx)
                        entities.add(e)
                        e
                    } as E

                    setNavigation(table, entity, rs, ctx)
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