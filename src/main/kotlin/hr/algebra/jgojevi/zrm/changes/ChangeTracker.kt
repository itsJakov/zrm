package hr.algebra.jgojevi.zrm.changes

import hr.algebra.jgojevi.zrm.schema.DBTable

private typealias PrimaryKey = Any

class ChangeTracker {

    private val entities = mutableMapOf<Pair<DBTable<*>, PrimaryKey>, Entry>()

    private fun keyForEntity(entity: Any): Pair<DBTable<*>, PrimaryKey> {
        val table = DBTable.of(entity)
        val pk = table.primaryKey.get(entity)!!

        return table to pk
    }

    fun attach(entity: Any): Entry {
        val key = keyForEntity(entity)
        if (entities.containsKey(key)) throw Exception("Entity with same primary key already exists")

        val entry = Entry(entity)
        entities[key] = entry
        return entry
    }

    fun detach(entity: Any): Boolean = entities.remove(keyForEntity(entity)) != null

    fun entry(entity: Any): Entry? = entities[keyForEntity(entity)]

    internal fun <T : Any> findEntry(table: DBTable<T>, primaryKey: Any) =
        entities[table to primaryKey]?.entity as T?

    val entries: Collection<Entry> get() = entities.values

    fun detectChanges() {
        entities.values.forEach { it.detectChanges() }
    }

}