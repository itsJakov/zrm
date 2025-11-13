package hr.algebra.jgojevi.zrm.exec

import hr.algebra.jgojevi.zrm.schema.DBColumn
import java.sql.PreparedStatement
import java.sql.ResultSet

internal fun PreparedStatement.executeBetterQuery() = BetterResultSet(executeQuery())

internal class BetterResultSet(private val resultSet: ResultSet) : AutoCloseable by resultSet {

    private val metadata = resultSet.metaData

    private val columnsByQualifiedName: Map<String, Int>
        = (1 .. metadata.columnCount)
            .associateBy { "\"${resultSet.metaData.getTableName(it)}\".\"${resultSet.metaData.getColumnName(it)}\"" }

    fun <T> collect(block: (BetterResultSet) -> T): List<T> {
        val list = mutableListOf<T>()
        while (resultSet.next()) {
            list.add(block(this))
        }
        return list
    }

    fun <T> mapOne(block: (BetterResultSet) -> T): T? {
        if (resultSet.next()) {
            return block(this)
        }
        return null
    }

    fun getObject(column: DBColumn<*, *>): Any?
        = resultSet.getObject(columnsByQualifiedName[column.qualifiedName]!!)

}