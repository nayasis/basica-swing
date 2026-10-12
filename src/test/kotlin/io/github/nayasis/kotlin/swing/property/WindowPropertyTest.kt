package io.github.nayasis.kotlin.swing.property

import io.github.nayasis.kotlin.swing.common.assumeDisplay
import io.github.nayasis.kotlin.swing.misc.runUiAndWait
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.awt.FlowLayout
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import javax.swing.DefaultListModel
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JFrame
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JTextField

internal class WindowPropertyTest {

    private class Fixture {
        val field    = JTextField("hello").apply { name = "txt" }
        val check    = JCheckBox("check").apply { name = "chk"; isSelected = true }
        val combo    = JComboBox(arrayOf("a", "b", "c")).apply { name = "combo"; selectedIndex = 2 }
        val list     = JList(DefaultListModel<String>().apply { addElement("x"); addElement("y"); addElement("z") }).apply { name = "list"; selectedIndices = intArrayOf(0, 2) }
        val password = JPasswordField("secret").apply { name = "pwd" }
        val frame    = JFrame("test").apply {
            contentPane = JPanel(FlowLayout()).apply { listOf(field, check, combo, list, password).forEach { add(it) } }
            setBounds(120, 140, 640, 480)
        }
    }

    @Test
    fun `component state round trips through bind`() {
        assumeDisplay()
        runUiAndWait {
            val f = Fixture()
            val property = WindowProperty(f.frame)

            f.field.text          = "changed"
            f.check.isSelected    = false
            f.combo.selectedIndex = 0
            f.list.clearSelection()
            f.field.isEditable    = false
            f.field.isEnabled     = false
            f.frame.setLocation(300, 300)

            property.bind(f.frame)

            f.field.text          shouldBe "hello"
            f.check.isSelected    shouldBe true
            f.combo.selectedIndex shouldBe 2
            f.list.selectedIndices.toList() shouldBe listOf(0, 2)
            f.field.isEditable    shouldBe true
            f.field.isEnabled     shouldBe true
            f.frame.x             shouldBe 120
            f.frame.y             shouldBe 140

            f.frame.dispose()
        }
    }

    @Test
    fun `passwords are not stored`() {
        assumeDisplay()
        runUiAndWait {
            val f = Fixture()

            val property = WindowProperty(f.frame)

            property.values?.keys shouldBe setOf("txt")
            f.frame.dispose()
        }
    }

    @Test
    fun `id and class filters narrow what is stored`() {
        assumeDisplay()
        runUiAndWait {
            val f = Fixture()

            val byId = WindowProperty().apply { excludeId.add("txt") }.also { it.read(f.frame) }
            byId.values?.containsKey("txt") shouldBe false
            byId.checks?.containsKey("chk") shouldBe true

            val byClass = WindowProperty().apply { includeKlass.add(JCheckBox::class) }.also { it.read(f.frame) }
            byClass.checks?.keys shouldBe setOf("chk")
            byClass.values?.isEmpty() shouldBe true

            f.frame.dispose()
        }
    }

    @Test
    fun `property survives java serialization`() {
        assumeDisplay()
        runUiAndWait {
            val f = Fixture()
            val property = WindowProperty(f.frame)

            val bytes = ByteArrayOutputStream().also { ObjectOutputStream(it).use { out -> out.writeObject(property) } }.toByteArray()
            val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as WindowProperty }

            restored.values shouldBe property.values
            restored.indices shouldBe property.indices
            restored.inset shouldBe property.inset

            f.frame.dispose()
        }
    }

}
