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
                upStatements.add(DDLGen.createTable(table))
                for (column in table.columns) {
                    if (column.foreignKey != null) {
                        foreignKeyUpStatements.add(DDLGen.addForeignKeyConstraint(table, column))
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
                        alterStatements.add(DDLGen.addColumn(column))
                    } else {
                        // ALTER COLUMN
                        if (column.isNotNull != existingColumn.isNotNull) {
                            if (column.isNotNull) {
                                alterStatements.add(DDLGen.setNotNull(column))
                            } else {
                                alterStatements.add(DDLGen.dropNotNull(column))
                            }
                        }

                        if (column.foreignKey != existingColumn.foreignKey) {
                            if (existingColumn.foreignKey != null) {
                                foreignKeyUpStatements.add(DDLGen.dropForeignKeyConstraint(table, column))
                            }

                            if (column.foreignKey != null) {
                                foreignKeyUpStatements.add(DDLGen.addForeignKeyConstraint(table, column))
                            }
                        }
                    }
                }

                for (column in oldColumns.values) {
                    if (newColumns.containsKey(column.name)) continue
                    // DROP COLUMN
                    alterStatements.add(DDLGen.dropColumn(column))
                }

                if (alterStatements.isEmpty()) continue
                upStatements.add(DDLGen.alterTable(table, alterStatements))
            }
        }

        for (table in oldTables.values) {
            if (newTables.containsKey(table.name)) continue
            // DROP TABLE
            upStatements.add(DDLGen.dropTable(table))
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