package io.github.nayasis.kotlin.swing.control.table

import io.github.nayasis.kotlin.basica.exception.implements.NotFound
import io.github.nayasis.kotlin.swing.control.table.ScrollMode.ALWAYS_TO_MIDDLE
import io.github.nayasis.kotlin.swing.control.table.ScrollMode.OUTBOUND_TO_MIDDLE
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.Serializable
import javax.swing.JCheckBoxMenuItem
import javax.swing.JPopupMenu
import javax.swing.JTable
import javax.swing.JViewport
import javax.swing.table.TableColumn

private const val KEY_HIDDEN = "basica.table.hiddenColumns"

data class Position(val row: Int = -1, val col: Int = -1): Serializable

data class Range(
    val start: Int = -1,
    val end: Int = -1,
) {
    fun `in`(index: Int?): Boolean {
        return index?.let {
            start <= index && index <= end
        } ?: false
    }
}

enum class ScrollMode {
    DEFAULT,
    ALWAYS_TO_MIDDLE,
    OUTBOUND_TO_MIDDLE,
}

// ---------------------------------------------------------------- columns

private class HiddenColumn(val column: TableColumn, val index: Int)

@Suppress("UNCHECKED_CAST")
private val JTable.hidden: LinkedHashMap<String, HiddenColumn>
    get() = getClientProperty(KEY_HIDDEN) as? LinkedHashMap<String, HiddenColumn>
        ?: LinkedHashMap<String, HiddenColumn>().also { putClientProperty(KEY_HIDDEN, it) }

/** shown columns in view order. hidden ones are in [hiddenColumns]. */
val JTable.allColumns: List<TableColumn>
    get() = columnModel.columns.toList()

/** columns taken out of the column model by [setColumnVisible]. */
val JTable.hiddenColumns: List<TableColumn>
    get() = hidden.values.map { it.column }

/** column by its identifier, hidden ones included. */
fun JTable.findColumnBy(id: String): TableColumn {
    return (allColumns + hiddenColumns).firstOrNull { it.identifier == id }
        ?: throw NotFound("id:$id")
}

fun JTable.isColumnVisible(id: String): Boolean = allColumns.any { it.identifier == id }

/** hide a column by removing it from the column model, or put it back where it was. */
fun JTable.setColumnVisible(id: String, visible: Boolean): JTable {
    if (visible) {
        val restored = hidden.remove(id) ?: return this
        columnModel.addColumn(restored.column)
        val last = columnModel.columnCount - 1
        val target = restored.index.coerceIn(0, last)
        if (target != last) moveColumn(last, target)
    } else {
        val column = allColumns.firstOrNull { it.identifier == id } ?: return this
        hidden[id] = HiddenColumn(column, columnModel.getColumnIndex(id))
        columnModel.removeColumn(column)
    }
    return this
}

/** give every column a unique, non-empty identifier, which table state persistence relies on. */
fun JTable.fillColumnIds(): JTable {
    val seen = HashSet<Any>()
    (allColumns + hiddenColumns).forEachIndexed { index, column ->
        val id = column.identifier
        if (id == null || id.toString().isEmpty() || !seen.add(id)) {
            column.identifier = "${name ?: "table"}-$index"
            seen.add(column.identifier)
        }
    }
    return this
}

// ---------------------------------------------------------------- selection

var JTable.selectedPosition: Position
    get() = Position(selectedRow, selectedColumn)
    set(pos) { select(pos.row, pos.col) }

/** the lead cell: the one that has focus within the table. */
var JTable.focusedPosition: Position
    get() = Position(selectionModel.leadSelectionIndex, columnModel.selectionModel.leadSelectionIndex)
    set(pos) { focus(pos.row, pos.col) }

fun JTable.select(row: Int, col: Int = -1): JTable {
    clearSelection()
    if (row < 0 || row >= rowCount) return this
    if (col >= 0 && columnCount > 0) {
        changeSelection(row, col.coerceIn(0, columnCount - 1), false, false)
    } else {
        setRowSelectionInterval(row, row)
    }
    return this
}

fun JTable.focus(row: Int, col: Int = -1): JTable = select(row, col)

@Suppress("UNCHECKED_CAST")
private fun <S> JTable.indexOfItem(item: S?): Int =
    (model as? ListTableModel<S>)?.indexOf(item) ?: -1

fun <S> JTable.selectBy(item: S?, col: Int = -1): JTable = select(indexOfItem(item), col)

fun <S> JTable.focusBy(item: S?, col: Int = -1): JTable = focus(indexOfItem(item), col)

/** the item in the lead row of a [ListTableModel] table. */
@Suppress("UNCHECKED_CAST")
fun <S> JTable.focusedItem(): S? {
    val row = focusedPosition.row.takeIf { it in 0 until rowCount } ?: return null
    return (model as? ListTableModel<S>)?.getRowObject(row)
}

// ---------------------------------------------------------------- scroll

val JTable.visibleRange: Range
    get() {
        if (rowCount == 0) return Range()
        val rect = visibleRect
        if (rect.isEmpty) return Range()
        val first = rowAtPoint(Point(rect.x, rect.y)).takeIf { it >= 0 } ?: return Range()
        val last  = rowAtPoint(Point(rect.x, rect.y + rect.height - 1)).takeIf { it >= 0 } ?: (rowCount - 1)
        return Range(first, last)
    }

val JTable.visibleRows: Int
    get() = visibleRange.let { if (it.start < 0) 0 else it.end - it.start + 1 }

fun JTable.scroll(row: Int, mode: ScrollMode = ALWAYS_TO_MIDDLE): JTable {
    val index = when (mode) {
        ALWAYS_TO_MIDDLE   -> middleIndex(row, visibleRows)
        OUTBOUND_TO_MIDDLE -> if (visibleRange.`in`(row)) null else middleIndex(row, visibleRows)
        else               -> row
    }
    index?.let { scrollToTop(it) }
    return this
}

fun <S> JTable.scrollBy(item: S?, mode: ScrollMode = ALWAYS_TO_MIDDLE): JTable = scroll(indexOfItem(item), mode)

private fun middleIndex(row: Int, visibleRows: Int): Int {
    return if (visibleRows % 2 == 0) {
        maxOf(row - (visibleRows / 2) + 1, 0)
    } else {
        maxOf(row - (visibleRows / 2), 0)
    }
}

// put [row] on the top of the viewport, as far as the table length allows
private fun JTable.scrollToTop(row: Int) {
    if (row < 0 || row >= rowCount) return
    val rect = getCellRect(row, 0, true)
    val viewport = parent as? JViewport
    if (viewport == null) {
        scrollRectToVisible(rect)
        return
    }
    val maxY = (height - viewport.height).coerceAtLeast(0)
    viewport.viewPosition = Point(viewport.viewPosition.x, rect.y.coerceIn(0, maxY))
}

// ---------------------------------------------------------------- column visibility menu

private const val KEY_VISIBILITY_MENU = "basica.table.columnVisibilityMenu"

/**
 * right click on the header lists the columns with a check box each, to hide or show them.
 * the last shown column cannot be hidden.
 */
fun JTable.installColumnVisibilityMenu(): JTable {
    val header = tableHeader ?: return this
    (header.getClientProperty(KEY_VISIBILITY_MENU) as? MouseAdapter)?.let { header.removeMouseListener(it) }
    val listener = object: MouseAdapter() {
        override fun mousePressed(e: MouseEvent) = showIfTrigger(e)
        override fun mouseReleased(e: MouseEvent) = showIfTrigger(e)
        private fun showIfTrigger(e: MouseEvent) {
            if (!e.isPopupTrigger) return
            fillColumnIds()
            val columns = (allColumns + hiddenColumns).sortedBy { it.modelIndex }
            val menu = JPopupMenu()
            columns.forEach { column ->
                val id = column.identifier.toString()
                val shown = isColumnVisible(id)
                menu.add(JCheckBoxMenuItem(column.headerValue?.toString() ?: id, shown).apply {
                    isEnabled = !shown || allColumns.size > 1
                    addActionListener { setColumnVisible(id, isSelected) }
                })
            }
            menu.show(header, e.x, e.y)
        }
    }
    header.addMouseListener(listener)
    header.putClientProperty(KEY_VISIBILITY_MENU, listener)
    return this
}
