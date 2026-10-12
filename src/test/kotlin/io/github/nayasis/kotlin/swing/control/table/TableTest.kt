package io.github.nayasis.kotlin.swing.control.table

import io.github.nayasis.kotlin.basica.exception.implements.NotFound
import io.github.nayasis.kotlin.swing.collection.FilterSortList
import com.formdev.flatlaf.FlatDarkLaf
import io.github.nayasis.kotlin.swing.common.assumeDisplay
import io.github.nayasis.kotlin.swing.common.installTheme
import io.github.nayasis.kotlin.swing.misc.runUiAndWait
import io.github.nayasis.kotlin.swing.property.TableProperty
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.SwingUtilities

data class Person(val name: String, var age: Int, val nick: String? = null)

internal class TableTest {

    private val alice = Person("Alice", 30, "al")
    private val bob   = Person("Bob", 25)
    private val carol = Person("Carol", 41, "cc")

    private class Fixture(
        val list: FilterSortList<Person>,
        val model: ListTableModel<Person>,
        val table: JTable,
    )

    private fun fixture(): Fixture {
        val list  = FilterSortList(listOf(alice, bob, carol))
        val model = ListTableModel(list)
            .column("Name", Person::name)
            .column("Age", Person::age, editable = true)
            .column("Nick", Person::nick)
        val table = JTable().apply { name = "people" }
        table.bind(model)
        return Fixture(list, model, table)
    }

    private fun JTable.names() = (0 until rowCount).map { model.getValueAt(it, 0) }

    @Test
    fun `model exposes the visible rows and the declared columns`() {
        assumeDisplay()
        runUiAndWait {
            val (list, model, table) = fixture().let { Triple(it.list, it.model, it.table) }

            table.rowCount shouldBe 3
            table.columnCount shouldBe 3
            table.getColumnName(1) shouldBe "Age"
            table.getValueAt(1, 1) shouldBe 25
            model.getColumnClass(1) shouldBe Int::class.javaObjectType
            table.getColumnModel().getColumn(2).identifier shouldBe "Nick"

            list.predicate = { it.age > 28 }

            table.names() shouldBe listOf("Alice", "Carol")
        }
    }

    @Test
    fun `sortKeys sort the rows and keep nulls last in both directions`() {
        assumeDisplay()
        runUiAndWait {
            val f = fixture()

            f.table.sortKeys = listOf(ColumnSortKey("Age", true))
            f.table.names() shouldBe listOf("Bob", "Alice", "Carol")

            f.table.sortKeys = listOf(ColumnSortKey("Age", false))
            f.table.names() shouldBe listOf("Carol", "Alice", "Bob")

            f.table.sortKeys = listOf(ColumnSortKey("Nick", true))
            f.table.names() shouldBe listOf("Alice", "Carol", "Bob")

            f.table.sortKeys = listOf(ColumnSortKey("Nick", false))
            f.table.names() shouldBe listOf("Carol", "Alice", "Bob")

            f.table.sortKeys = emptyList()
            f.table.names() shouldBe listOf("Alice", "Bob", "Carol")
        }
    }

    @Test
    fun `header shows the sort priority once several keys apply`() {
        assumeDisplay()
        runUiAndWait {
            val f = fixture()
            fun headerIcon(column: Int) =
                (f.table.tableHeader.defaultRenderer.getTableCellRendererComponent(f.table, f.table.getColumnName(column), false, false, -1, column) as JLabel).icon

            f.table.sortKeys = listOf(ColumnSortKey("Age", true))
            val arrowOnly = headerIcon(1)!!.iconWidth
            headerIcon(0) shouldBe null

            f.table.sortKeys = listOf(ColumnSortKey("Age", true), ColumnSortKey("Name", false))
            (headerIcon(1)!!.iconWidth > arrowOnly) shouldBe true
            (headerIcon(0)!!.iconWidth > arrowOnly) shouldBe true
            headerIcon(2) shouldBe null
        }
    }

    @Test
    fun `narrow columns cut the text instead of adding an ellipsis`() {
        assumeDisplay()
        installTheme()
        runUiAndWait {
            val list  = FilterSortList(listOf(Person("Alexander the Great of Macedon", 1)))
            val model = ListTableModel(list).column("Name", Person::name).column("Age", Person::age)
            val table = JTable().apply { bind(model) }.clipTextInsteadOfEllipsis()
            val column = table.columnModel.getColumn(0).apply { width = 60 }

            val cell = table.prepareRenderer(table.getCellRenderer(0, 0), 0, 0) as JLabel
            val available = column.width - table.intercellSpacing.width - cell.insets.left - cell.insets.right
            ("Alexander the Great of Macedon".startsWith(cell.text) && cell.text.isNotEmpty()) shouldBe true
            (cell.getFontMetrics(cell.font).stringWidth(cell.text) <= available) shouldBe true
            // one more character would not have fitted
            val next = "Alexander the Great of Macedon".substring(0, cell.text.length + 1)
            (cell.getFontMetrics(cell.font).stringWidth(next) > available) shouldBe true

            column.width = 1000
            (table.prepareRenderer(table.getCellRenderer(0, 0), 0, 0) as JLabel).text shouldBe "Alexander the Great of Macedon"

            // header keeps its sort arrow and loses only text
            table.sortKeys = listOf(ColumnSortKey("Name", true))
            column.width = 30
            val header = table.tableHeader.defaultRenderer.getTableCellRendererComponent(table, "Name", false, false, -1, 0) as JLabel
            (header.icon != null) shouldBe true
            "Name".startsWith(header.text) shouldBe true
            (header.text.length < 4) shouldBe true
        }
    }

    @Test
    fun `wrapped renderers follow a theme switch`() {
        assumeDisplay()
        installTheme()
        try {
            runUiAndWait {
                val f = fixture()
                f.table.clipTextInsteadOfEllipsis()
                val innermost = f.table.tableHeader.defaultRenderer.innermost() as JLabel
                val before = innermost.ui

                FlatDarkLaf.setup()
                SwingUtilities.updateComponentTreeUI(f.table)

                (innermost.ui !== before) shouldBe true
            }
        } finally {
            runUiAndWait { installTheme() }
        }
    }

    @Test
    fun `selection follows the object across a refresh`() {
        assumeDisplay()
        runUiAndWait {
            val f = fixture()
            f.table.sortKeys = listOf(ColumnSortKey("Age", true))   // Bob, Alice, Carol

            f.table.selectBy(alice)

            f.table.selectedRow shouldBe 1

            f.list.predicate = { it.name != "Bob" }                  // Alice, Carol

            f.table.selectedRow shouldBe 0
            f.model.getRowObject(f.table.selectedRow) shouldBe alice
            f.table.focusedItem<Person>() shouldBe alice
        }
    }

    @Test
    fun `editing a cell writes through the setter`() {
        assumeDisplay()
        runUiAndWait {
            val f = fixture()

            f.table.isCellEditable(0, 1) shouldBe true
            f.table.isCellEditable(0, 0) shouldBe false

            f.table.setValueAt(31, 0, 1)

            alice.age shouldBe 31
        }
    }

    @Test
    fun `select focus and position helpers`() {
        assumeDisplay()
        runUiAndWait {
            val f = fixture()

            f.table.select(2, 1)

            f.table.selectedPosition shouldBe Position(2, 1)

            f.table.focus(1)

            f.table.focusedPosition.row shouldBe 1

            f.table.select(-1)

            f.table.selectedRow shouldBe -1

            f.table.select(99)

            f.table.selectedRow shouldBe -1
        }
    }

    @Test
    fun `columns can be hidden and restored in place`() {
        assumeDisplay()
        runUiAndWait {
            val f = fixture()

            f.table.setColumnVisible("Age", false)

            f.table.columnCount shouldBe 2
            f.table.isColumnVisible("Age") shouldBe false
            f.table.findColumnBy("Age").identifier shouldBe "Age"
            f.table.hiddenColumns.size shouldBe 1

            f.table.setColumnVisible("Age", true)

            f.table.allColumns.map { it.identifier } shouldBe listOf("Name", "Age", "Nick")
            f.table.hiddenColumns.size shouldBe 0

            shouldThrow<NotFound> { f.table.findColumnBy("nope") }
        }
    }

    @Test
    fun `table property round trips order width visibility and sort`() {
        assumeDisplay()
        runUiAndWait {
            val f = fixture()
            f.table.columnModel.getColumn(0).width = 120
            f.table.setColumnVisible("Nick", false)
            f.table.moveColumn(0, 1)                                   // Age, Name
            f.table.sortKeys = listOf(ColumnSortKey("Age", false))

            val saved = TableProperty(f.table)

            f.table.setColumnVisible("Nick", true)
            f.table.moveColumn(1, 0)
            f.table.columnModel.getColumn(1).width = 40
            f.table.sortKeys = emptyList()

            saved.bind(f.table)

            f.table.allColumns.map { it.identifier } shouldBe listOf("Age", "Name")
            f.table.findColumnBy("Name").width shouldBe 120
            f.table.sortKeys shouldBe listOf(ColumnSortKey("Age", false))
            f.table.names().first() shouldBe "Carol"
        }
    }

    @Test
    fun `visible range of a table without a viewport`() {
        assumeDisplay()
        runUiAndWait {
            val f = fixture()

            f.table.visibleRange.`in`(5) shouldBe false
            Range(2, 4).`in`(3) shouldBe true
            Range(2, 4).`in`(null) shouldBe false
        }
    }

}
