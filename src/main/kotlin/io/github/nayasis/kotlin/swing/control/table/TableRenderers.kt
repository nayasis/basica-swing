package io.github.nayasis.kotlin.swing.control.table

import java.awt.Component
import java.beans.PropertyChangeListener
import java.util.Date
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.SwingUtilities
import javax.swing.plaf.basic.BasicHTML
import javax.swing.table.TableCellRenderer

private const val KEY_UI_SYNC = "basica.table.rendererUiSync"

/** a renderer that decorates another one. */
internal interface WrappingRenderer: TableCellRenderer {
    val base: TableCellRenderer
}

internal fun TableCellRenderer.innermost(): TableCellRenderer {
    var renderer = this
    while (renderer is WrappingRenderer) renderer = renderer.base
    return renderer
}

/** cuts the text of [base]'s label to what fits the column, instead of letting the label end it with "...". */
class NoEllipsisRenderer(override val base: TableCellRenderer): WrappingRenderer {

    override fun getTableCellRendererComponent(table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        val component = base.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        if (component is JLabel && column in 0 until table.columnCount) {
            val width = table.columnModel.getColumn(column).width - if (row >= 0) table.intercellSpacing.width else 0
            component.cutTextToFit(width)
        }
        return component
    }

}

/**
 * show cell and header text cut at the column edge instead of shortened with "...".
 *
 * wraps the current default, column and header renderers, so call it after setting custom renderers.
 * calling it again only wraps what is new.
 */
fun JTable.clipTextInsteadOfEllipsis(): JTable {
    listOf(Any::class.java, Number::class.java, java.lang.Float::class.java, java.lang.Double::class.java, Date::class.java).forEach { type ->
        getDefaultRenderer(type)?.let { if (it !is NoEllipsisRenderer) setDefaultRenderer(type, NoEllipsisRenderer(it)) }
    }
    columnModel.columns.toList().forEach { column ->
        column.cellRenderer?.let { if (it !is NoEllipsisRenderer) column.cellRenderer = NoEllipsisRenderer(it) }
        column.headerRenderer?.let { if (it !is NoEllipsisRenderer) column.headerRenderer = NoEllipsisRenderer(it) }
    }
    tableHeader?.let { header ->
        header.defaultRenderer?.let { if (it !is NoEllipsisRenderer) header.defaultRenderer = NoEllipsisRenderer(it) }
    }
    syncWrappedRenderersWithLookAndFeel()
    return this
}

/**
 * JTable and JTableHeader only refresh renderers that are components on a theme switch;
 * a wrapper is not, so refresh what it wraps.
 */
internal fun JTable.syncWrappedRenderersWithLookAndFeel() {
    if (getClientProperty(KEY_UI_SYNC) != null) return
    val listener = PropertyChangeListener { updateWrappedRenderers() }
    addPropertyChangeListener("UI", listener)
    tableHeader?.addPropertyChangeListener("UI", listener)
    putClientProperty(KEY_UI_SYNC, listener)
}

private fun JTable.updateWrappedRenderers() {
    val renderers = buildList {
        listOf(Any::class.java, Number::class.java, java.lang.Float::class.java, java.lang.Double::class.java, Date::class.java)
            .forEach { add(getDefaultRenderer(it)) }
        columnModel.columns.toList().forEach { add(it.cellRenderer); add(it.headerRenderer) }
        add(tableHeader?.defaultRenderer)
    }
    renderers.filterIsInstance<WrappingRenderer>().map { it.innermost() }.distinct()
        .forEach { (it as? Component)?.let(SwingUtilities::updateComponentTreeUI) }
}

/** shorten the text to its longest start that fits [width], so the label has no reason to add "...". */
internal fun JLabel.cutTextToFit(width: Int) {
    val text = text
    if (text.isNullOrEmpty() || BasicHTML.isHTMLString(text)) return
    val iconSpace = icon?.let { it.iconWidth + iconTextGap } ?: 0
    val available = width - insets.left - insets.right - iconSpace
    // JComponent.getFontMetrics carries the component's text rendering hints, like the label UI uses
    val metrics = getFontMetrics(font)
    if (metrics.stringWidth(text) <= available) return
    var fits = 0
    var tooLong = text.codePointCount(0, text.length)
    while (tooLong - fits > 1) {
        val middle = (fits + tooLong) / 2
        if (metrics.stringWidth(text.substring(0, text.offsetByCodePoints(0, middle))) <= available) fits = middle else tooLong = middle
    }
    this.text = text.substring(0, text.offsetByCodePoints(0, fits))
}
