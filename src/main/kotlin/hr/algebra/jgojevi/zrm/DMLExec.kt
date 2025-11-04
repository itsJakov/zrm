package hr.algebra.jgojevi.zrm

import java.sql.Connection
import java.sql.Statement
import java.util.*

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

}