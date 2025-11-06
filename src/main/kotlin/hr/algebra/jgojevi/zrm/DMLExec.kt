package hr.algebra.jgojevi.zrm

import java.sql.Connection
import java.sql.Statement

object DMLExec {

    fun insert(entity: Any, conn: Connection) {
        val table = EntityTable(entity::class)
        val columns = table.columns
            .filter { !it.isPrimaryKey }
        val placeholders = Array(columns.size) { "?" }.joinToString()

        val sql = "insert into \"${table.name}\" (${columns.joinToString { "\"${it.name}\"" }}) values ($placeholders)"
        println("[DMLExec] $sql")

        conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
            for ((i, column) in columns.withIndex()) {
                val value = column.property.getter.call(entity)
                stmt.setObject(i+1, value)
            }

            stmt.executeUpdate()
            stmt.generatedKeys.use { rs ->
                if (rs.next()) {
                    table.primaryKey.property.setter.call(entity, rs.getObject(1))
                }
            }
        }
    }

    fun <E : Any> update(entity: EntityStore.TrackedEntity<E>, conn: Connection) {
        val table = EntityTable(entity.entity::class)
        val changes = entity.changedColumns.joinToString { "\"${it.name}\" = ?" }
        val sql = "update \"${table.name}\" set $changes where \"${table.primaryKey.name}\" = ?"
        println("[DMLExec] $sql")

        conn.prepareStatement(sql).use { stmt ->
            for ((i, column) in entity.changedColumns.withIndex()) {
                val value = column.property.getter.call(entity.entity)
                stmt.setObject(i+1, value)
            }

            val primaryKey = table.primaryKey.property.getter.call(entity.entity)
            stmt.setObject(entity.changedColumns.size+1, primaryKey)

            stmt.executeUpdate()
        }
    }

    fun delete(entity: Any, conn: Connection) {
        val table = EntityTable(entity::class)
        val sql = "delete from \"${table.name}\" where \"${table.primaryKey.name}\" = ?"
        println("[DMLExec] $sql")

        conn.prepareStatement(sql).use { stmt ->
            val primaryKey = table.primaryKey.property.getter.call(entity)
            stmt.setObject(1, primaryKey)
            stmt.executeUpdate()
        }
    }

}