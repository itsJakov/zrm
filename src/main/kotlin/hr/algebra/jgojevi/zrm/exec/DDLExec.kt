package hr.algebra.jgojevi.zrm.exec

import java.sql.Connection

// DDLExec should take more responsibilities from the DatabaseMigrator
object DDLExec {

    fun execute(sql: String, conn: Connection) {
        conn.createStatement().use { stmt ->
            stmt.execute(sql)
        }
    }

}