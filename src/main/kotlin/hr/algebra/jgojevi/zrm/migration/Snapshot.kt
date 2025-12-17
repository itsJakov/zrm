package hr.algebra.jgojevi.zrm.migration

import hr.algebra.jgojevi.zrm.Database
import hr.algebra.jgojevi.zrm.schema.DBColumn
import hr.algebra.jgojevi.zrm.schema.DBTable
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.reflect.KClass

@Serializable
data class Snapshot(val version: Int, val tables: List<Table>) {

    @Serializable
    data class Table(
        val name: String,
        val columns: List<Column>,

        @Transient val alter: Boolean = false
    )

    @Serializable
    data class Column(
        val name: String,
        val type: String,
        val isPrimaryKey: Boolean,
        val foreignKey: ForeignKey?
    )

    @Serializable
    data class ForeignKey(val table: String, val column: String)

}

// TODO: Should this even be here?
val typeMapping: Map<KClass<*>, String> = mapOf(
    String::class to "text",
    Int::class to "int",
)

fun Snapshot.Column.Companion.of(dbColumn: DBColumn<*, *>): Snapshot.Column {
    val columnType = dbColumn.property.returnType.classifier as KClass<*>
    val type = if (dbColumn.isPrimaryKey && columnType == Int::class) {
        "serial"
    } else {
        typeMapping[columnType]!!
    }

    return Snapshot.Column(
        name = dbColumn.name,
        type = type,
        isPrimaryKey = dbColumn.isPrimaryKey,
        foreignKey = null // TODO
    )
}

fun Snapshot.Column.modifiers(): String {
    val modifiers = mutableListOf<String>()
    if (this.isPrimaryKey) {
        modifiers.add("primary key")
    }

    if (modifiers.isEmpty()) return ""
    return " " + modifiers.joinToString(" ")
}

fun Snapshot.Table.Companion.of(dbTable: DBTable<*>): Snapshot.Table =
    Snapshot.Table(
        name = dbTable.name,
        columns = dbTable.columns.map { Snapshot.Column.of(it) },
    )

fun Snapshot.Companion.take(database: Database): Snapshot {
    val tables = database.getAllTables()
        .map { Snapshot.Table.of(it) }
        .toList()

    return Snapshot(
        version = 1,
        tables = tables
    )
}