package hr.algebra.jgojevi.zrm.migration

import Snapshot
import hr.algebra.jgojevi.zrm.Database
import kotlinx.serialization.json.Json
import java.io.File

class DatabaseMigrator {

    private val jsonConfig = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    private fun takeSnapshot(database: Database): Snapshot {
        val tables = database.getAllTables()
            .map { Snapshot.Table(it.name, emptyList()) }
            .toList()

        return Snapshot(
            version = 1,
            tables = tables
        )
    }

    fun migrate(database: Database) {
        val snapshot = takeSnapshot(database)

        // - temp
        val json = jsonConfig.encodeToString(snapshot)
        val file = File("target/migrations.json")
        file.parentFile.mkdirs()
        file.writeText(json)
    }

}