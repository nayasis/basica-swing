package io.github.nayasis.kotlin.swing.control.table

import java.awt.Color
import java.awt.Component
import java.beans.PropertyChangeListener
import java.util.Collections
import java.util.WeakHashMap
import javax.swing.JTable
import javax.swing.UIManager
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer

private const val KEY_DROP_HIGHLIGHT = "basica.table.dropHighlight"
private const val KEY_DROP_REPAINT   = "basica.table.dropHighlightRepaint"

/** what a table highlights while something is dragged over one of its rows. */
enum class DropHighlight {
    /** the cell under the cursor, as JTable does by itself. */
    CELL,
    /** every cell of the row under the cursor. */
    ROW,
}

/**
 * what is highlighted while dragging over a row: the cell under the cursor, or its whole row.
 *
 * only drops onto a row ([javax.swing.DropMode.ON] and alike) are highlighted this way, not insert locations.
 * [DropHighlight.ROW] wraps the current default and column renderers, so set it after setting custom renderers.
 */
var JTable.dropHighlight: DropHighlight
    get() = getClientProperty(KEY_DROP_HIGHLIGHT) as? DropHighlight ?: DropHighlight.CELL
    set(mode) {
        putClientProperty(KEY_DROP_HIGHLIGHT, mode)
        if (mode == DropHighlight.ROW) installDropRowHighlight()
        dropLocation?.let { repaintRow(it.row) }
    }

private fun JTable.installDropRowHighlight() {
    DEFAULT_RENDERER_TYPES.forEach { type ->
        getDefaultRenderer(type)?.let { if (!it.isWrappedBy<DropRowRenderer>()) setDefaultRenderer(type, DropRowRenderer(it)) }
    }
    (allColumns + hiddenColumns).forEach { column ->
        column.cellRenderer?.let { if (!it.isWrappedBy<DropRowRenderer>()) column.cellRenderer = DropRowRenderer(it) }
    }
    // JTable repaints only the cell of the old and new drop location
    if (getClientProperty(KEY_DROP_REPAINT) == null) {
        val listener = PropertyChangeListener { e ->
            if (dropHighlight != DropHighlight.ROW) return@PropertyChangeListener
            listOf(e.oldValue, e.newValue).filterIsInstance<JTable.DropLocation>().forEach { repaintRow(it.row) }
        }
        addPropertyChangeListener("dropLocation", listener)
        putClientProperty(KEY_DROP_REPAINT, listener)
    }
    syncWrappedRenderersWithLookAndFeel()
}

private fun JTable.repaintRow(row: Int) {
    if (row !in 0 until rowCount) return
    val rect = getCellRect(row, 0, true)
    repaint(0, rect.y, width, rect.height)
}

private fun JTable.isDropRow(row: Int): Boolean {
    val location = dropLocation ?: return false
    return !location.isInsertRow && !location.isInsertColumn && location.row == row
}

/** paints every cell of the drop row like the drop cell, while the table's [dropHighlight] is [DropHighlight.ROW]. */
internal class DropRowRenderer(override val base: TableCellRenderer): WrappingRenderer {

    // DefaultTableCellRenderer keeps what setBackground / setForeground get as its unselected colors.
    // so remember the ones it has, and put them back before it renders the next cell.
    private val unselectedColors = WeakHashMap<DefaultTableCellRenderer, Pair<Color?, Color?>>()
    private val recolored = Collections.newSetFromMap(WeakHashMap<DefaultTableCellRenderer, Boolean>())

    override fun getTableCellRendererComponent(table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        restoreUnselectedColors()
        val onDropRow = table.dropHighlight == DropHighlight.ROW && table.isDropRow(row)
        val component = base.getTableCellRendererComponent(table, value, isSelected || onDropRow, hasFocus, row, column)
        if (!onDropRow) {
            if (!isSelected) rememberUnselectedColors(component, table, hasFocus, row)
            return component
        }
        if (component is DefaultTableCellRenderer) recolored.add(component)
        component.background = UIManager.getColor("Table.dropCellBackground") ?: table.selectionBackground
        component.foreground = UIManager.getColor("Table.dropCellForeground") ?: table.selectionForeground
        return component
    }

    private fun restoreUnselectedColors() {
        if (recolored.isEmpty()) return
        recolored.forEach { renderer ->
            val (background, foreground) = unselectedColors[renderer] ?: (null to null)
            renderer.background = background
            renderer.foreground = foreground
        }
        recolored.clear()
    }

    /**
     * an unselected cell shows the renderer's own colors, or the table's when it has none.
     * odd rows may show the alternate row color and the focused cell its own, so only read the others.
     */
    private fun rememberUnselectedColors(component: Component, table: JTable, hasFocus: Boolean, row: Int) {
        if (component !is DefaultTableCellRenderer || hasFocus || row % 2 != 0) return
        unselectedColors[component] =
            component.background.takeUnless { it === table.background } to component.foreground.takeUnless { it === table.foreground }
    }

}
