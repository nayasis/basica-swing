package io.github.nayasis.kotlin.swing.control

import io.github.nayasis.kotlin.swing.misc.onLookAndFeelChange
import java.awt.Component
import java.awt.Dimension
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.HierarchyEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.JPopupMenu
import javax.swing.JScrollPane
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.JTextComponent

/**
 * suggestion popup under a text component. the focus stays in the text component.
 *
 * while the popup is shown, Up / Down move the selection, Enter or a click accepts it and Escape closes it.
 * those keys are consumed, so the text component's own key listeners must skip consumed events.
 *
 * ```
 * // on demand
 * val completion = AutoCompletion<String>(field) { field.text = it }
 * completion.show(listOf("apple", "banana"))
 *
 * // while typing
 * AutoCompletion<String>(field) { field.text = it }.live { text -> words.filter { it.contains(text) } }
 * ```
 *
 * @param toText label of a suggestion in the popup
 * @param onSelected called with the accepted suggestion
 */
class AutoCompletion<T>(
    private val field: JTextComponent,
    private val toText: (T) -> String = { it.toString() },
    private val onSelected: (T) -> Unit,
) {

    private val model = DefaultListModel<T>()

    private val list = JList(model).apply {
        isFocusable  = false
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        cellRenderer = object: DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
                @Suppress("UNCHECKED_CAST")
                val text = (value as? T)?.let(toText) ?: ""
                return super.getListCellRendererComponent(list, text, index, isSelected, false)
            }
        }
    }

    private val scroll = JScrollPane(list).apply {
        border = BorderFactory.createEmptyBorder()
        isFocusable = false
        verticalScrollBar.isFocusable = false
    }

    private val popup = JPopupMenu().apply {
        isFocusable = false
        add(scroll)
    }

    /** rows shown before the list scrolls. */
    var visibleRows = 10

    val isShowing: Boolean
        get() = popup.isVisible

    private var lookAndFeelSubscription: AutoCloseable? = null

    init {
        // the popup is outside any window while hidden, so a theme switch would not reach it
        field.addHierarchyListener { e ->
            if (e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L) syncLookAndFeelSubscription()
        }
        syncLookAndFeelSubscription()
        list.addMouseListener(object: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (!SwingUtilities.isLeftMouseButton(e)) return
                val index = list.locationToIndex(e.point).takeIf { it >= 0 } ?: return
                list.selectedIndex = index
                accept()
            }
        })
        field.addFocusListener(object: FocusAdapter() {
            override fun focusLost(e: FocusEvent) = hide()
        })
        installAheadOfOtherKeyListeners(object: KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (!isShowing) return
                when (e.keyCode) {
                    KeyEvent.VK_DOWN   -> move(1)
                    KeyEvent.VK_UP     -> move(-1)
                    KeyEvent.VK_PAGE_DOWN -> move(visibleRows)
                    KeyEvent.VK_PAGE_UP   -> move(-visibleRows)
                    KeyEvent.VK_ENTER  -> accept()
                    KeyEvent.VK_ESCAPE -> hide()
                    else -> return
                }
                e.consume()
            }
        })
    }

    /** show [items] under the text component. an empty list hides the popup. */
    fun show(items: List<T>) {
        if (items.isEmpty() || !field.isShowing) {
            hide()
            return
        }
        model.clear()
        model.addAll(items)
        list.visibleRowCount = items.size.coerceAtMost(visibleRows)
        list.selectedIndex = 0
        list.ensureIndexIsVisible(0)
        scroll.preferredSize = null
        val preferred = scroll.preferredSize
        scroll.preferredSize = Dimension(maxOf(preferred.width, field.width), preferred.height)
        popup.pack()
        if (popup.isVisible) popup.isVisible = false
        popup.show(field, 0, field.height)
        field.requestFocusInWindow()
    }

    fun hide() {
        if (popup.isVisible) popup.isVisible = false
    }

    private fun syncLookAndFeelSubscription() {
        if (field.isDisplayable) {
            if (lookAndFeelSubscription != null) return
            SwingUtilities.updateComponentTreeUI(popup)
            lookAndFeelSubscription = onLookAndFeelChange { SwingUtilities.updateComponentTreeUI(popup) }
        } else {
            lookAndFeelSubscription?.close()
            lookAndFeelSubscription = null
            hide()
        }
    }

    /**
     * refresh the popup on every edit with the suggestions for the trimmed text. blank text hides it.
     */
    fun live(suggestions: (text: String) -> List<T>): AutoCompletion<T> {
        field.document.addDocumentListener(object: DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = refresh()
            override fun removeUpdate(e: DocumentEvent) = refresh()
            override fun changedUpdate(e: DocumentEvent) {}
            // the document is locked while notifying, so the popup is updated afterwards
            private fun refresh() = SwingUtilities.invokeLater {
                val text = field.text.trim()
                if (text.isEmpty() || !field.isFocusOwner) hide() else show(suggestions(text))
            }
        })
        return this
    }

    private fun move(delta: Int) {
        if (model.isEmpty) return
        val next = (list.selectedIndex + delta).coerceIn(0, model.size() - 1)
        list.selectedIndex = next
        list.ensureIndexIsVisible(next)
    }

    private fun accept() {
        val selected = list.selectedValue
        hide()
        if (selected != null) onSelected(selected)
    }

    // listeners run in registration order, so the popup keys are consumed before the field's own handlers see them
    private fun installAheadOfOtherKeyListeners(listener: KeyAdapter) {
        val existing = field.keyListeners
        existing.forEach { field.removeKeyListener(it) }
        field.addKeyListener(listener)
        existing.forEach { field.addKeyListener(it) }
    }

}
