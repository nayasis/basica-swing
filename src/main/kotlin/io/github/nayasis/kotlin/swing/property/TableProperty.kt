package io.github.nayasis.kotlin.swing.property

import io.github.nayasis.kotlin.swing.control.table.Position
import io.github.nayasis.kotlin.swing.control.table.allColumns
import io.github.nayasis.kotlin.swing.control.table.fillColumnIds
import io.github.nayasis.kotlin.swing.control.table.findColumnBy
import io.github.nayasis.kotlin.swing.control.table.focusedPosition
import io.github.nayasis.kotlin.swing.control.table.hiddenColumns
import io.github.nayasis.kotlin.swing.control.table.isColumnVisible
import io.github.nayasis.kotlin.swing.control.table.scroll
import io.github.nayasis.kotlin.swing.control.table.setColumnVisible
import io.github.nayasis.kotlin.swing.misc.runLater
import java.io.Serializable
import javax.swing.JTable

private var tableIndex = 0

data class TableProperty(
    var columns: LinkedHashMap<String,TableColumnProperty>? = null,
    var columnSortOrder: TableColumnSortOrderProperty? = null,
    var visible: Boolean? = null,
    var focused: Position? = null,
): Serializable {

    constructor(table: JTable): this() {
        read(table)
    }

    fun read(table: JTable) {
        setId(table)
        visible         = table.isVisible
        focused         = table.focusedPosition
        columnSortOrder = TableColumnSortOrderProperty(table)
        columns = LinkedHashMap<String,TableColumnProperty>().also { map ->
            table.allColumns.forEach    { map[it.identifier.toString()] = TableColumnProperty(it, true) }
            table.hiddenColumns.forEach { map[it.identifier.toString()] = TableColumnProperty(it, false) }
        }
    }

    fun bind(table: JTable) {
        setId(table)
        visible?.let { table.isVisible = it }
        reorderColumns(table)
        columnSortOrder?.bind(table)
        focused?.let {
            runLater {
                table.focusedPosition = it
                table.scroll(it.row)
            }
        }
    }

    private fun setId(table: JTable) {
        if (table.name.isNullOrEmpty())
            table.name = "table${tableIndex++}"
        table.fillColumnIds()
    }

    private fun reorderColumns(table: JTable) {

        val saved = columns ?: return

        // show / hide first, so the column model holds exactly the columns to order
        saved.forEach { (id, property) ->
            if (runCatching { table.findColumnBy(id) }.isFailure) return@forEach
            property.show?.let { show ->
                if (show != table.isColumnVisible(id)) table.setColumnVisible(id, show)
            }
            property.bind(table.findColumnBy(id))
        }

        // saved order first, columns unknown to the saved state keep their order after them
        val order = saved.keys.filter { table.isColumnVisible(it) } +
            table.allColumns.map { it.identifier.toString() }.filter { it !in saved.keys }

        order.forEachIndexed { target, id ->
            val current = table.columnModel.getColumnIndex(id)
            if (current != target) table.moveColumn(current, target)
        }

    }

}
