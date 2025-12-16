package hr.algebra.jgojevi.zrm.migration

import hr.algebra.jgojevi.zrm.Database
import kotlinx.serialization.json.Json
import java.io.File

class DatabaseMigrator {

    private val jsonConfig = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    private fun performDiff(oldSnapshot: Snapshot?, newSnapshot: Snapshot) {
        val oldTables = oldSnapshot?.tables?.associateBy { it.name } ?: emptyMap()
        val newTables = newSnapshot.tables.associateBy { it.name }

        val up = StringBuilder()

        for (table in newTables.values) {
            val existingTable = oldTables[table.name]
            if (existingTable == null) {
                // CREATE TABLE
                val columns = table.columns
                    .joinToString { "${it.name} ${it.type}" }

                up.append("create table ${table.name} (${columns});\n")
            } else {
                // ALTER TABLE?
            }
        }

        for (table in oldTables.values) {
            if (newTables.containsKey(table.name)) continue
            // DELETE TABLE
            up.append("drop table ${table.name};\n") // TODO: What about foreign keys?
        }

        println("UP:\n$up")
    }

    fun migrate(database: Database) {
        var oldSnapshot: Snapshot? = null
        val file = File("target/snapshot.json")
        try {
            oldSnapshot = jsonConfig.decodeFromString<Snapshot>(file.readText())
        } catch (e: Exception) {
        }

        val snapshot = Snapshot.take(database)

        performDiff(oldSnapshot, snapshot)

        // - temp
        val json = jsonConfig.encodeToString(snapshot)
        file.parentFile.mkdirs()
        file.writeText(json)
    }

}