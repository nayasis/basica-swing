package io.github.nayasis.kotlin.swing.property

import java.io.Serializable
import javax.swing.table.TableColumn

/**
 * persisted state of one table column. sort state lives in [TableColumnSortOrderProperty];
 * whether the column is shown or hidden is applied by [TableProperty], which owns the column model.
 */
data class TableColumnProperty(
    var id: String? = null,
    var width: Int? = null,
    var show: Boolean? = null,
): Serializable {

    constructor(column: TableColumn, show: Boolean = true): this() {
        read(column, show)
    }

    fun read(column: TableColumn, show: Boolean = true) {
        id         = column.identifier?.toString()
        width      = column.width
        this.show  = show
    }

    fun bind(column: TableColumn) {
        width?.let {
            column.preferredWidth = it
            column.width          = it
        }
    }

}
