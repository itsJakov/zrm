package hr.algebra.jgojevi.zrm.changes

import hr.algebra.jgojevi.zrm.schema.DBColumn
import hr.algebra.jgojevi.zrm.schema.DBTable

// A tracked entity
class Entry internal constructor(val entity: Any) {

    enum class State {
        UNCHANGED, INSERTED, UPDATED, DELETED
    }
    var state = State.UNCHANGED

    private var values = copyValues()
    var changedColumns = emptyList<DBColumn<Any, *>>()
        private set

    private fun copyValues(): Map<DBColumn<Any, *>, Any?>
        = DBTable.of(entity).columns.associateWith { it.get(entity) }

    fun detectChanges() {
        if (state != State.UNCHANGED) return

        val changedColumns = mutableListOf<DBColumn<Any, *>>()
        for ((column, value) in values) {
            val current = column.get(entity)

            if (current != value) {
                println("${column.name}: $value -> $current")
                changedColumns.add(column)
                state = State.UPDATED
            }
        }
        this.changedColumns = changedColumns
    }

    internal fun reset() {
        state = State.UNCHANGED
        values = copyValues()
    }

}