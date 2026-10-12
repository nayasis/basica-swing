package io.github.nayasis.kotlin.swing.control.combobox

import io.github.nayasis.kotlin.swing.control.addKeyPressed
import java.awt.Component
import java.awt.event.KeyEvent
import javax.swing.ComboBoxEditor
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JList
import javax.swing.ListCellRenderer
import javax.swing.plaf.UIResource

/**
 * combo box of [ItemCombo]: shows the label, selects by value.
 *
 * `JComboBox.addItem` / `removeItem` keep their Swing meaning; the value-aware variants are
 * [addCombo] / [removeCombo].
 */
@Suppress("unused")
class ItemComboBox(initialItems: Collection<ItemCombo>? = null): JComboBox<ItemCombo>(DefaultComboBoxModel<ItemCombo>()) {

    private val comboModel: DefaultComboBoxModel<ItemCombo>
        get() = model as DefaultComboBoxModel<ItemCombo>

    val items: List<ItemCombo>
        get() = (0 until comboModel.size).map { comboModel.getElementAt(it) }

    var value: String?
        get() = selectedCombo?.value
        set(value) { selectedItem = getItem(value) }

    var select: Int
        get() = selectedIndex
        set(value) { selectedIndex = if (value in 0 until itemCount) value else -1 }

    val selectedCombo: ItemCombo?
        get() = when (val selected = selectedItem) {
            is ItemCombo -> selected
            // an editable combo holds the typed text
            is String    -> items.firstOrNull { it.label == selected }
            else         -> null
        }

    init {
        initialItems?.forEach { comboModel.addElement(it) }
        addKeyPressed { e ->
            if (isEditable) return@addKeyPressed
            when (e.keyCode) {
                KeyEvent.VK_HOME -> { e.consume(); selectFirst() }
                KeyEvent.VK_END  -> { e.consume(); selectLast() }
            }
        }
    }

    // the look and feel installs its own (UIResource) renderer / editor, also on every theme switch,
    // so the label substitution is layered on top of it here.
    @Suppress("UNCHECKED_CAST")
    override fun setRenderer(aRenderer: ListCellRenderer<in ItemCombo>?) {
        val renderer: ListCellRenderer<in ItemCombo>? =
            if (aRenderer is UIResource && aRenderer !is LabelRenderer) LabelRenderer(aRenderer as ListCellRenderer<Any?>) else aRenderer
        super.setRenderer(renderer)
    }

    override fun setEditor(anEditor: ComboBoxEditor?) {
        super.setEditor(
            if (anEditor is UIResource && anEditor !is LabelEditor) LabelEditor(anEditor, this) else anEditor
        )
    }

    fun selectFirst(): ItemCombo? {
        select = 0
        return selectedCombo
    }

    fun selectLast(): ItemCombo? {
        select = itemCount - 1
        return selectedCombo
    }

    fun clear(): ItemComboBox {
        comboModel.removeAllElements()
        return this
    }

    fun getItem(value: String?): ItemCombo? {
        return items.firstOrNull { it.value == value }
    }

    fun addCombo(value: String, label: String = value, ref: Any? = null, index: Int? = null, overwriteOnExist: Boolean = true): ItemComboBox {
        return addCombo(ItemCombo(value, label, ref), index, overwriteOnExist)
    }

    fun addCombo(item: ItemCombo, index: Int? = null, overwriteOnExist: Boolean = true): ItemComboBox {
        val existing = items.firstOrNull { it.value == item.value }
        if (existing != null && overwriteOnExist) {
            existing.label = item.label
            existing.ref   = item.ref
            repaint()
        } else if (index == null) {
            comboModel.addElement(item)
        } else {
            comboModel.insertElementAt(item, index.coerceIn(0, comboModel.size))
        }
        return this
    }

    fun addCombos(items: Collection<ItemCombo>, overwriteOnExist: Boolean = true): ItemComboBox {
        if (overwriteOnExist) {
            val checker = this.items.associateBy { it.value }
            for (item in items) {
                val existing = checker[item.value]
                if (existing != null) {
                    existing.label = item.label
                    existing.ref   = item.ref
                } else {
                    comboModel.addElement(item)
                }
            }
            repaint()
        } else {
            items.forEach { comboModel.addElement(it) }
        }
        return this
    }

    fun removeCombo(value: String): Boolean {
        return removeCombo(ItemCombo(value))
    }

    fun removeCombo(item: ItemCombo): Boolean {
        val index = comboModel.getIndexOf(item)
        if (index < 0) return false
        comboModel.removeElementAt(index)
        return true
    }

    fun removeCombos(items: Collection<ItemCombo>): Boolean {
        return items.map { removeCombo(it) }.any { it }
    }

}

private class LabelRenderer(private val delegate: ListCellRenderer<Any?>): ListCellRenderer<Any?>, UIResource {
    override fun getListCellRendererComponent(list: JList<out Any?>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
        return delegate.getListCellRendererComponent(list, (value as? ItemCombo)?.label ?: value, index, isSelected, cellHasFocus)
    }
}

private class LabelEditor(private val delegate: ComboBoxEditor, private val combo: ItemComboBox): ComboBoxEditor by delegate, UIResource {

    override fun setItem(anObject: Any?) {
        delegate.setItem((anObject as? ItemCombo)?.label ?: anObject)
    }

    override fun getItem(): Any? {
        val item = delegate.getItem()
        return if (item is String) combo.items.firstOrNull { it.label == item } ?: item else item
    }

}
