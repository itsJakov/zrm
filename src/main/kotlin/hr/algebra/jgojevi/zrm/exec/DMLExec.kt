package hr.algebra.jgojevi.zrm.exec

import hr.algebra.jgojevi.zrm.schema.DBColumn
import hr.algebra.jgojevi.zrm.schema.DBTable
import java.sql.Connection

internal object DMLExec {

//    fun insert() {}

    fun <E : Any> update(entity: E, changedColumns: List<DBColumn<E, *>>, conn: Connection) {
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

    fun <E : Any> delete(entity: E, conn: Connection) {
        val table = DBTable.of(entity)
        val sql = "delete from \"${table.name}\" where \"${table.primaryKey.name}\" = ?"
        println("[DMLExec] $sql")

        conn.prepareStatement(sql).use { stmt ->
            stmt.setObject(1, table.primaryKey.get(entity))
            stmt.executeUpdate()
        }
    }

}