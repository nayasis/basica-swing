package io.github.nayasis.kotlin.swing.control.table

import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import javax.swing.table.TableCellRenderer

private const val KEY_HEADER_LISTENER = "basica.table.headerListener"
private const val KEY_MODEL_LISTENER  = "basica.table.modelListener"
private const val KEY_REFRESH_WRAPPER = "basica.table.refreshWrapper"

/** sort criteria of the bound [ListTableModel], empty when the table is not bound to one. */
var JTable.sortKeys: List<ColumnSortKey>
    get() = (model as? ListTableModel<*>)?.sortKeys ?: emptyList()
    set(value) { (model as? ListTableModel<*>)?.sortKeys = value }

/**
 * show [model] in this table.
 *
 * - header click sorts the column: ascending, descending, none. ctrl-click adds a secondary key.
 *   with several keys, each sorted header shows its priority next to the arrow.
 * - the selection follows its objects across filtering and sorting.
 */
fun <S> JTable.bind(model: ListTableModel<S>): JTable {

    (getClientProperty(KEY_MODEL_LISTENER) as? TableModelListener)?.let { this.model.removeTableModelListener(it) }
    (this.model as? ListTableModel<*>)?.let { previous ->
        if (previous.refreshWrapper === getClientProperty(KEY_REFRESH_WRAPPER)) previous.refreshWrapper = null
    }
    rowSorter = null
    this.model = model

    applyColumnIds(model)
    val structureListener = TableModelListener { e ->
        if (e.firstRow == TableModelEvent.HEADER_ROW) SwingUtilities.invokeLater { applyColumnIds(model) }
    }
    model.addTableModelListener(structureListener)
    putClientProperty(KEY_MODEL_LISTENER, structureListener)

    val refreshWrapper: (() -> Unit) -> Unit = { refresh ->
        val previous: List<S> = selectedRows.toList().mapNotNull { row -> model.previousRowObject(row) }
        refresh()
        if (previous.isNotEmpty()) {
            selectionModel.valueIsAdjusting = true
            try {
                previous.forEach { item ->
                    val row = model.indexOf(item)
                    if (row >= 0) addRowSelectionInterval(row, row)
                }
            } finally {
                selectionModel.valueIsAdjusting = false
            }
        }
        tableHeader?.repaint()
    }
    model.refreshWrapper = refreshWrapper
    putClientProperty(KEY_REFRESH_WRAPPER, refreshWrapper)

    installSortHeader()
    return this
}

private fun JTable.applyColumnIds(model: ListTableModel<*>) {
    columnModel.columns.toList().forEach { column ->
        model.columns.getOrNull(column.modelIndex)?.let { column.identifier = it.id }
    }
}

private fun JTable.installSortHeader() {

    val header = tableHeader ?: return

    (header.getClientProperty(KEY_HEADER_LISTENER) as? MouseAdapter)?.let { header.removeMouseListener(it) }
    val listener = object: MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            if (!SwingUtilities.isLeftMouseButton(e) || header.draggedColumn != null || header.resizingColumn != null) return
            if (header.cursor.type == Cursor.E_RESIZE_CURSOR || header.cursor.type == Cursor.W_RESIZE_CURSOR) return
            val viewColumn = header.columnAtPoint(e.point).takeIf { it >= 0 } ?: return
            val listModel  = model as? ListTableModel<*> ?: return
            val id         = columnModel.getColumn(viewColumn).identifier as? String ?: return
            if (listModel.columns.firstOrNull { it.id == id }?.sortable != true) return
            listModel.sortKeys = nextSortKeys(listModel.sortKeys, id, additive = e.isControlDown)
        }
    }
    header.addMouseListener(listener)
    header.putClientProperty(KEY_HEADER_LISTENER, listener)

    val current = header.defaultRenderer
    var renderer: TableCellRenderer? = current
    while (renderer is WrappingRenderer && renderer !is SortArrowRenderer) renderer = renderer.base
    if (renderer !is SortArrowRenderer) {
        // the arrow must go in before the text is cut to the column, or it would not fit
        header.defaultRenderer = if (current is NoEllipsisRenderer) NoEllipsisRenderer(SortArrowRenderer(current.base)) else SortArrowRenderer(current)
    }
    syncWrappedRenderersWithLookAndFeel()

}

private fun nextSortKeys(current: List<ColumnSortKey>, id: String, additive: Boolean): List<ColumnSortKey> {
    val index = current.indexOfFirst { it.columnId == id }
    val next  = when {
        index < 0                    -> ColumnSortKey(id, true)
        current[index].ascending     -> ColumnSortKey(id, false)
        else                         -> null
    }
    if (!additive) return listOfNotNull(next)
    return current.toMutableList().apply {
        when {
            index < 0    -> add(next!!)
            next == null -> removeAt(index)
            else         -> this[index] = next
        }
    }
}

private class SortArrowRenderer(override val base: TableCellRenderer): WrappingRenderer {

    override fun getTableCellRendererComponent(table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        val component = base.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        if (component is JLabel) {
            val id    = table.columnModel.takeIf { column in 0 until it.columnCount }?.getColumn(column)?.identifier
            val keys  = (table.model as? ListTableModel<*>)?.sortKeys ?: emptyList()
            val index = keys.indexOfFirst { it.columnId == id }
            component.icon = keys.getOrNull(index)?.let { key ->
                val arrow = if (key.ascending) icon("Table.ascendingSortIcon", true) else icon("Table.descendingSortIcon", false)
                // the priority only means something once a second key exists
                if (keys.size > 1) SortPriorityIcon(arrow, index + 1, component) else arrow
            }
            component.horizontalTextPosition = SwingConstants.LEADING
        }
        return component
    }

    private fun icon(key: String, ascending: Boolean): Icon = UIManager.getIcon(key) ?: ArrowIcon(ascending)

}

/** sort arrow followed by the 1-based priority of its key. */
private class SortPriorityIcon(private val arrow: Icon, priority: Int, label: JLabel): Icon {

    private val text    = priority.toString()
    private val font    = label.font.deriveFont(label.font.size2D * 0.8f)
    private val metrics = label.getFontMetrics(font)
    // halfway to the header background: dimmer than the title in any theme
    private val color   = blend(label.foreground, label.background ?: UIManager.getColor("TableHeader.background"), 0.5f)

    override fun getIconWidth() = arrow.iconWidth + GAP + metrics.stringWidth(text)
    override fun getIconHeight() = maxOf(arrow.iconHeight, metrics.ascent + metrics.descent)

    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        arrow.paintIcon(c, g, x, y + (iconHeight - arrow.iconHeight) / 2)
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g2.font  = font
            g2.color = color
            g2.drawString(text, x + arrow.iconWidth + GAP, y + (iconHeight - metrics.ascent - metrics.descent) / 2 + metrics.ascent)
        } finally {
            g2.dispose()
        }
    }

    private companion object {
        const val GAP = 2

        fun blend(from: Color, to: Color?, ratio: Float): Color {
            if (to == null) return from
            fun mix(a: Int, b: Int) = (a + (b - a) * ratio).toInt()
            return Color(mix(from.red, to.red), mix(from.green, to.green), mix(from.blue, to.blue))
        }
    }

}

private class ArrowIcon(private val ascending: Boolean): Icon {

    override fun getIconWidth() = 10
    override fun getIconHeight() = 6

    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        g.color = UIManager.getColor("TableHeader.foreground") ?: Color.GRAY
        val xs = intArrayOf(x, x + 10, x + 5)
        val ys = if (ascending) intArrayOf(y + 6, y + 6, y) else intArrayOf(y, y, y + 6)
        g.fillPolygon(xs, ys, 3)
    }

}
