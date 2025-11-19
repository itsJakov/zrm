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

    fun next() = resultSet.next()

    fun containsColumn(column: DBColumn<*, *>): Boolean
        = columnsByQualifiedName.containsKey(column.qualifiedName)

    fun getObject(column: DBColumn<*, *>): Any?
        = resultSet.getObject(columnsByQualifiedName[column.qualifiedName]!!)

}