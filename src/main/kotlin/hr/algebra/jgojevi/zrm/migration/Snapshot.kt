package hr.algebra.jgojevi.zrm.migration

import hr.algebra.jgojevi.zrm.Database
import hr.algebra.jgojevi.zrm.schema.DBColumn
import hr.algebra.jgojevi.zrm.schema.DBTable
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class Snapshot(val version: Int, val tables: List<Table>) {

    @Serializable
    data class Table(
        val name: String,
        val columns: List<Column>
    )

    @Serializable
    data class Column(
        val name: String,
        val type: String,
        val isPrimaryKey: Boolean,
        val isNotNull: Boolean,
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

fun Snapshot.Column.Companion.of(dbTable: DBTable<*>, dbColumn: DBColumn<*, *>): Snapshot.Column {
    val columnType = dbColumn.property.returnType.classifier as KClass<*>
    val type = if (dbColumn.isPrimaryKey && columnType == Int::class) {
        "serial"
    } else {
        typeMapping[columnType]!!
    }

    val fk = if (dbColumn.isForeignKey) {
        val targetClass = dbTable.navigationProperties.entries
            .first { it.value.name == dbColumn.name }
            .key
            .returnType.classifier as KClass<*>

        val targetTable = DBTable.of(targetClass)

        Snapshot.ForeignKey(
            table = targetTable.name,
            column = targetTable.primaryKey.name
        )
    } else null

    return Snapshot.Column(
        name = dbColumn.name,
        type = type,
        isPrimaryKey = dbColumn.isPrimaryKey,
        isNotNull = !dbColumn.property.returnType.isMarkedNullable,
        foreignKey = fk
    )
}

fun Snapshot.Column.modifiers(): String {
    val modifiers = mutableListOf<String>()

    if (isPrimaryKey) modifiers.add("primary key")
    modifiers.add(if (isNotNull) "not null" else "null")

    if (modifiers.isEmpty()) return ""
    return " " + modifiers.joinToString(" ")
}

fun Snapshot.Column.addForeignKey(table: Snapshot.Table) =
    "alter table ${table.name}" +
        " add constraint fk_$name" +
        " foreign key ($name)" +
        " references ${foreignKey!!.table}(${foreignKey.column})" +
        " on delete cascade" +
        " on update cascade;"

fun Snapshot.Column.dropForeignKey(table: Snapshot.Table) =
    "alter table ${table.name} drop constraint fk_$name;"

fun Snapshot.Table.Companion.of(dbTable: DBTable<*>): Snapshot.Table =
    Snapshot.Table(
        name = dbTable.name,
        columns = dbTable.columns.map { Snapshot.Column.of(dbTable, it) },
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