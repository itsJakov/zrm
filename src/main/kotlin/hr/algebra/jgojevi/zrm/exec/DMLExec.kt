package hr.algebra.jgojevi.zrm.exec

import hr.algebra.jgojevi.zrm.schema.DBColumn
import hr.algebra.jgojevi.zrm.schema.DBTable
import java.sql.Connection
import java.sql.PreparedStatement

internal class DMLExec private constructor(val conn: Connection) {

    companion object {
        fun inTransaction(conn: Connection, block: (DMLExec) -> Unit) {
            try {
                conn.autoCommit = false // Ugly
                block(DMLExec(conn))
            } catch (e: Exception) {
                conn.rollback()
                throw e
            } finally {
                conn.autoCommit = true // Acts like a ugly commit()
            }
        }
    }

    fun <E : Any> insert(entity: E) {
        val table = DBTable.of(entity)
        val columns = table.columns.filter { !it.isPrimaryKey }

        val placeholders = Array(columns.size) { "?" }.joinToString()
        val sql = "insert into \"${table.name}\" (${columns.joinToString { "\"${it.name}\"" }}) values ($placeholders)"
        println("[DMLExec] $sql")

        conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS).use { stmt ->
            for ((i, column) in columns.withIndex()) {
                stmt.setObject(i + 1, column.get(entity))
            }

            stmt.executeUpdate()
            stmt.generatedKeys.use { rs ->
                if (rs.next()) {
                    // Unchecked cast because DBColumn<E, *> prohibits using the setter
                    (table.primaryKey as DBColumn<E, Any>).set(entity, rs.getObject(1))
                }
            }
        }
    }

    fun <E : Any> update(entity: E, changedColumns: List<DBColumn<E, *>>) {
        val table = DBTable.of(entity)
        val changes = changedColumns.joinToString { "\"${it.name}\" = ?" }
        val sql = "update \"${table.name}\" set $changes where \"${table.primaryKey.name}\" = ?"
        println("[DMLExec] $sql")

        conn.prepareStatement(sql).use { stmt ->
            for ((i, column) in changedColumns.withIndex()) {
                stmt.setObject(i + 1, column.get(entity))
            }
            stmt.setObject(changedColumns.size + 1, table.primaryKey.get(entity))
            stmt.executeUpdate()
        }
    }

    fun <E : Any> delete(entity: E) {
        val table = DBTable.of(entity)
        val sql = "delete from \"${table.name}\" where \"${table.primaryKey.name}\" = ?"
        println("[DMLExec] $sql")

        conn.prepareStatement(sql).use { stmt ->
            stmt.setObject(1, table.primaryKey.get(entity))
            stmt.executeUpdate()
        }
    }

}