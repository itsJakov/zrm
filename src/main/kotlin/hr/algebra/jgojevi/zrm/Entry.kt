package hr.algebra.jgojevi.zrm

import hr.algebra.jgojevi.zrm.schema.DBColumn
import hr.algebra.jgojevi.zrm.schema.DBTable

// A tracked entity
class Entry internal constructor(val entity: Any) {

    enum class State {
        UNCHANGED, INSERT, UPDATED, DELETED
    }
    var state = State.UNCHANGED
        private set

    private var values = copyValues()
    internal var changedColumns = emptyList<DBColumn<Any, *>>()
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

    fun reset() {
        state = State.UNCHANGED
        values = copyValues()
    }

}