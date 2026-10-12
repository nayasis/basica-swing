package io.github.nayasis.kotlin.swing.window

import io.github.nayasis.kotlin.basica.model.Messages
import java.awt.Component
import java.awt.Container
import java.awt.Dialog
import java.awt.Frame
import java.util.Locale
import javax.swing.AbstractButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JPasswordField
import javax.swing.JTabbedPane
import javax.swing.JTable
import javax.swing.border.TitledBorder
import javax.swing.text.JTextComponent

/**
 * replace texts starting with `%` by messages, for the whole component tree.
 *
 * `%label.name` is replaced by `Messages["label.name", locale]`.
 */
class Localizator(
    component: Component?,
    val locale: Locale = Locale.getDefault(),
) {

    init {
        set(component)
    }

    fun set(component: Component?) {
        when(component) {
            null -> return
            is Frame  -> component.title = convert(component.title)
            is Dialog -> component.title = convert(component.title)
        }
        if( component is JComponent ) {
            component.toolTipText = convert(component.toolTipText)
            (component.border as? TitledBorder)?.let { it.title = convert(it.title) }
            component.componentPopupMenu?.let { set(it) }
        }
        when(component) {
            is JMenuBar    -> for( i in 0 until component.menuCount ) set(component.getMenu(i))
            is JMenu       -> {
                component.text = convert(component.text)
                component.menuComponents.forEach { set(it) }
            }
            is AbstractButton -> component.text = convert(component.text)
            is JLabel         -> component.text = convert(component.text)
            is JPasswordField -> {}
            is JTextComponent -> component.text = convert(component.text)
            is JTabbedPane    -> set(component)
            is JTable         -> set(component)
        }
        if( component is Container && component !is JMenu && component !is JMenuBar ) {
            component.components.forEach { set(it) }
        }
    }

    private fun set(tabs: JTabbedPane) {
        for( i in 0 until tabs.tabCount ) {
            tabs.setTitleAt(i, convert(tabs.getTitleAt(i)))
            tabs.setToolTipTextAt(i, convert(tabs.getToolTipTextAt(i)))
        }
    }

    private fun set(table: JTable) {
        table.columnModel.columns.asIterator().forEach { column ->
            (column.headerValue as? String)?.let { column.headerValue = convert(it) }
        }
    }

    private fun convert(text: String?): String? {
        if( text.isNullOrEmpty() || ! text.startsWith("%") ) return text
        return Messages[text.substring(1), locale]
    }

}
