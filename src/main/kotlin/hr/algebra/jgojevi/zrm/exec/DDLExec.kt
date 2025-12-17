package hr.algebra.jgojevi.zrm.exec

import java.sql.Connection

object DDLExec {

    fun inTransaction(sql: String, conn: Connection) {
        // TODO: Is this actually in a transaction?
        conn.createStatement().use { stmt ->
            stmt.execute(sql)
        }
    }

}