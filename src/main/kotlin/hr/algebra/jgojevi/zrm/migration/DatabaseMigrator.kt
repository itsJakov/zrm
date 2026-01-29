package hr.algebra.jgojevi.zrm.migration

import hr.algebra.jgojevi.zrm.Database
import hr.algebra.jgojevi.zrm.exec.DDLExec
import kotlinx.serialization.json.Json
import java.io.File

class DatabaseMigrator(private val database: Database) {

    private val jsonConfig = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    private fun performMigration(oldSnapshot: Snapshot?, newSnapshot: Snapshot) {
        val oldTables = oldSnapshot?.tables?.associateBy { it.name } ?: emptyMap()
        val newTables = newSnapshot.tables.associateBy { it.name }

        val upStatements = mutableListOf<String>()
        val foreignKeyUpStatements = mutableListOf<String>()

        for (table in newTables.values) {
            val existingTable = oldTables[table.name]
            if (existingTable == null) {
                // CREATE TABLE
                val columns = table.columns
                    .joinToString { "${it.name} ${it.type}${it.modifiers()}"}

                upStatements.add("create table ${table.name} (${columns});")

                for (column in table.columns) {
                    if (column.foreignKey != null) {
                        foreignKeyUpStatements.add(column.addForeignKey(table))
                    }
                }
            } else {
                // ALTER TABLE
                val oldColumns = existingTable.columns.associateBy { it.name }
                val newColumns = table.columns.associateBy { it.name }

                val alterStatements = mutableListOf<String>()
                for (column in newColumns.values) {
                    val existingColumn = oldColumns[column.name]
                    if (existingColumn == null) {
                        // CREATE COLUMN
                        alterStatements.add("add column ${column.name} ${column.type}${column.modifiers()}")
                    } else {
                        // ALTER COLUMN
                        if (column.isNotNull != existingColumn.isNotNull) {
                            if (column.isNotNull) {
                                alterStatements.add("alter column ${column.name} set not null")
                            } else {
                                alterStatements.add("alter column ${column.name} drop not null")
                            }
                        }

                        if (column.foreignKey != existingColumn.foreignKey) {
                            if (existingColumn.foreignKey != null) {
                                foreignKeyUpStatements.add(column.dropForeignKey(table))
                            }

                            if (column.foreignKey != null) {
                                foreignKeyUpStatements.add(column.addForeignKey(table))
                            }
                        }
                    }
                }

                for (column in oldColumns.values) {
                    if (newColumns.containsKey(column.name)) continue
                    // DROP COLUMN
                    alterStatements.add("drop column ${column.name}")
                }

                if (alterStatements.isEmpty()) continue
                upStatements.add("alter table ${table.name} ${alterStatements.joinToString()};")
            }
        }

        for (table in oldTables.values) {
            if (newTables.containsKey(table.name)) continue
            // DROP TABLE
            upStatements.add("drop table ${table.name};")
        }

        val upSQL = (upStatements + foreignKeyUpStatements).joinToString("\n")
        println("UP:\n$upSQL")
        DDLExec.execute(upSQL, database.connection)
    }

    fun migrate() {
        var oldSnapshot: Snapshot? = null
        val file = File("target/snapshot.json")
        try {
            oldSnapshot = jsonConfig.decodeFromString<Snapshot>(file.readText())
        } catch (e: Exception) {
        }

        val snapshot = Snapshot.take(database)

        performMigration(oldSnapshot, snapshot)

        // - temp
        val json = jsonConfig.encodeToString(snapshot)
        file.parentFile.mkdirs()
        file.writeText(json)
    }

}