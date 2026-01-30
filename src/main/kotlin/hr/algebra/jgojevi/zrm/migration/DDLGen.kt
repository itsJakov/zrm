package hr.algebra.jgojevi.zrm.migration

internal object DDLGen {

    fun createTable(table: Snapshot.Table): String {
        val columns = table.columns
            .joinToString { "${it.name} ${it.type}${it.modifiers()}"}
        return "create table ${table.name} (${columns});"
    }

    fun alterTable(table: Snapshot.Table, statements: Iterable<String>) =
        "alter table ${table.name} ${statements.joinToString()};"

    fun dropTable(table: Snapshot.Table) =
        "drop table ${table.name} cascade;"

    fun addColumn(column: Snapshot.Column) =
        "add column ${column.name} ${column.type}${column.modifiers()}"

    fun dropColumn(column: Snapshot.Column) =
        "drop column ${column.name}"

    fun setNotNull(column: Snapshot.Column) =
        "alter column ${column.name} set not null"

    fun dropNotNull(column: Snapshot.Column) =
        "alter column ${column.name} drop not null"

    fun addForeignKeyConstraint(table: Snapshot.Table, column: Snapshot.Column): String {
        assert(column.foreignKey != null)
        return "alter table ${table.name}" +
                " add constraint fk_${column.name}" +
                " foreign key (${column.name})" +
                " references ${column.foreignKey!!.table}(${column.foreignKey.column})" +
                " on delete ${column.foreignKey.onDelete}" +
                " on update ${column.foreignKey.onUpdate};"
    }

    fun dropForeignKeyConstraint(table: Snapshot.Table, column: Snapshot.Column) =
        "alter table ${table.name} drop constraint fk_${column.name};"

}