package io.github.nayasis.kotlin.swing.property

import io.github.nayasis.kotlin.swing.control.table.ColumnSortKey
import io.github.nayasis.kotlin.swing.control.table.sortKeys
import java.io.Serializable
import javax.swing.JTable

/** persisted sort criteria of a table bound to a [io.github.nayasis.kotlin.swing.control.table.ListTableModel]. */
data class TableColumnSortOrderProperty(
    var sortOrder: ArrayList<ColumnSortKey>? = null
): Serializable {

    constructor(table: JTable): this() {
        read(table)
    }

    fun read(table: JTable) {
        sortOrder = ArrayList(table.sortKeys)
    }

    fun bind(table: JTable) {
        sortOrder?.takeIf { it.isNotEmpty() }?.let { table.sortKeys = it }
    }

}
