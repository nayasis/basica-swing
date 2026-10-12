package io.github.nayasis.kotlin.swing.window

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.border.TitledBorder

class LocalizatorTest {

    @Test
    fun `texts without percent prefix are kept over the whole tree`() {

        val label  = JLabel("label").apply { toolTipText = "tip" }
        val button = JButton("button")
        val field  = JTextField("field")
        val item   = JMenuItem("item")
        val menu   = JMenu("menu").apply { add(item) }
        val bar    = JMenuBar().apply { add(menu) }
        val tabs   = JTabbedPane().apply { addTab("tab", JPanel().apply { add(button) }) }
        val table  = JTable(1, 1).apply { columnModel.getColumn(0).headerValue = "column" }
        val panel  = JPanel().apply {
            border = BorderFactory.createTitledBorder("title")
            add(label)
            add(field)
            add(bar)
            add(tabs)
            add(table)
        }

        Localizator(panel)

        label.text shouldBe "label"
        label.toolTipText shouldBe "tip"
        button.text shouldBe "button"
        field.text shouldBe "field"
        menu.text shouldBe "menu"
        item.text shouldBe "item"
        tabs.getTitleAt(0) shouldBe "tab"
        table.columnModel.getColumn(0).headerValue shouldBe "column"
        (panel.border as TitledBorder).title shouldBe "title"

    }

    @Test
    fun `null component and empty texts are accepted`() {
        Localizator(null)
        val label = JLabel("")
        Localizator(label)
        label.text shouldBe ""
    }

}
