package io.github.nayasis.kotlin.swing.control.table

import io.github.nayasis.kotlin.swing.collection.FilterSortList
import io.github.nayasis.kotlin.swing.common.installTheme
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

private data class Member(val name: String, var age: Int, val team: String?)

/** manual demo: header click sorts (ctrl-click adds keys), the text field filters, Age is editable. */
fun main() {
    installTheme()
    SwingUtilities.invokeLater {

        val list = FilterSortList(listOf(
            Member("Alice", 30, "dev"),
            Member("Bob", 25, null),
            Member("Carol", 41, "ops"),
            Member("Dave", 35, "dev"),
        ))
        val model = ListTableModel(list)
            .column("Name", Member::name)
            .column("Age", Member::age, editable = true)
            .column("Team", Member::team)
        val table = JTable().apply { name = "members" }
        table.bind(model)

        val filter = JTextField(20)
        filter.document.addDocumentListener(object: DocumentListener {
            private fun update() {
                val keyword = filter.text.trim()
                list.predicate = { keyword.isEmpty() || it.name.contains(keyword, ignoreCase = true) }
            }
            override fun insertUpdate(e: DocumentEvent) = update()
            override fun removeUpdate(e: DocumentEvent) = update()
            override fun changedUpdate(e: DocumentEvent) = update()
        })

        var added = 0
        val addButton = JButton("add").apply {
            addActionListener { list.add(Member("New${++added}", 20 + added, null)) }
        }

        JFrame("TableDemo").apply {
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JLabel("filter"))
                add(filter)
                add(addButton)
            }, BorderLayout.NORTH)
            add(JScrollPane(table), BorderLayout.CENTER)
            setSize(520, 360)
            setLocationRelativeTo(null)
            isVisible = true
        }
    }
}
