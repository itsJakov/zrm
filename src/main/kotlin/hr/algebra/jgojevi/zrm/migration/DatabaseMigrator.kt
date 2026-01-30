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
        val downStatements = mutableListOf<String>()
        val foreignKeyUpStatements = mutableListOf<String>()
        val foreignKeyDownStatements = mutableListOf<String>()

        for (table in newTables.values) {
            val existingTable = oldTables[table.name]
            if (existingTable == null) {
                // CREATE TABLE
                upStatements.add(DDLGen.createTable(table))
                downStatements.add(DDLGen.dropTable(table))

                for (column in table.columns) {
                    if (column.foreignKey != null) {
                        foreignKeyUpStatements.add(DDLGen.addForeignKeyConstraint(table, column))
                    }
                }
            } else {
                // ALTER TABLE
                val oldColumns = existingTable.columns.associateBy { it.name }
                val newColumns = table.columns.associateBy { it.name }

                val upAlter = mutableListOf<String>()
                val downAlter = mutableListOf<String>()

                for (column in newColumns.values) {
                    val existingColumn = oldColumns[column.name]
                    if (existingColumn == null) {
                        // CREATE COLUMN
                        upAlter.add(DDLGen.addColumn(column))
                        downAlter.add(DDLGen.dropColumn(column))

                        if (column.foreignKey != null) {
                            foreignKeyUpStatements.add(DDLGen.addForeignKeyConstraint(table, column))
                        }
                    } else {
                        // ALTER COLUMN
                        if (column.isNotNull != existingColumn.isNotNull) {
                            if (column.isNotNull) {
                                upAlter.add(DDLGen.setNotNull(column))
                                downAlter.add(DDLGen.dropNotNull(column))
                            } else {
                                upAlter.add(DDLGen.dropNotNull(column))
                                downAlter.add(DDLGen.setNotNull(column))
                            }
                        }

                        if (column.foreignKey != existingColumn.foreignKey) {
                            if (existingColumn.foreignKey != null) {
                                foreignKeyUpStatements.add(DDLGen.dropForeignKeyConstraint(table, existingColumn))
                                foreignKeyDownStatements.add(DDLGen.addForeignKeyConstraint(table, existingColumn))
                            }

                            if (column.foreignKey != null) {
                                foreignKeyUpStatements.add(DDLGen.addForeignKeyConstraint(table, column))
                                foreignKeyDownStatements.add(DDLGen.dropForeignKeyConstraint(table, column))
                            }
                        }
                    }
                }

                for (column in oldColumns.values) {
                    if (newColumns.containsKey(column.name)) continue
                    // DROP COLUMN
                    upAlter.add(DDLGen.dropColumn(column))
                    downAlter.add(DDLGen.addColumn(column))

                    if (column.foreignKey != null) {
                        foreignKeyDownStatements.add(DDLGen.addForeignKeyConstraint(table, column))
                    }
                }

                if (!upAlter.isEmpty())
                    upStatements.add(DDLGen.alterTable(table, upAlter))

                if (!downAlter.isEmpty())
                    downStatements.add(DDLGen.alterTable(table, downAlter))
            }
        }

        for (table in oldTables.values) {
            if (newTables.containsKey(table.name)) continue
            // DROP TABLE
            upStatements.add(DDLGen.dropTable(table))
            downStatements.add(DDLGen.createTable(table))

            for (column in table.columns) {
                if (column.foreignKey != null) {
                    foreignKeyDownStatements.add(DDLGen.addForeignKeyConstraint(table, column))
                }
            }
        }

        val upSQL = (upStatements + foreignKeyUpStatements).joinToString("\n")
        println("UP:\n$upSQL")
        DDLExec.execute(upSQL, database.connection)

        val downSQL = (downStatements + foreignKeyDownStatements).joinToString("\n")
        println("DOWN:\n$downSQL")

        val file = File("target/_down.sql")
        file.parentFile.mkdirs()
        file.appendText("\n--\n$downSQL")
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