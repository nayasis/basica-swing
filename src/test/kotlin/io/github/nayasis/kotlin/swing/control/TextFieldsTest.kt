package io.github.nayasis.kotlin.swing.control

import io.github.nayasis.kotlin.swing.common.assumeDisplay
import io.github.nayasis.kotlin.swing.misc.runUiAndWait
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.awt.event.KeyEvent
import javax.swing.JTextField

internal class TextFieldsTest {

    // call the listeners directly: dispatchEvent goes through the focus manager, which a test has no use for
    private fun JTextField.release(code: Int, char: Char = KeyEvent.CHAR_UNDEFINED) {
        val event = KeyEvent(this, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, code, char)
        keyListeners.forEach { it.keyReleased(event) }
    }

    @Test
    fun `mask formats the text on key release`() {
        assumeDisplay()
        runUiAndWait {
            val field = JTextField().apply { addMask("####-##-##") }

            field.text = "20240102"
            field.release(KeyEvent.VK_2, '2')

            field.text shouldBe "2024-01-02"
        }
    }

    @Test
    fun `navigation keys leave the text alone`() {
        assumeDisplay()
        runUiAndWait {
            val field = JTextField().apply { addMask("####-##-##") }

            field.text = "20240102"
            field.release(KeyEvent.VK_LEFT)

            field.text shouldBe "20240102"
        }
    }

    @Test
    fun `modifier pre-processes the text`() {
        assumeDisplay()
        runUiAndWait {
            val field = JTextField().apply { addMask("####", modifier = { it.filter(Char::isDigit) }) }

            field.text = "1a2b3c4"
            field.release(KeyEvent.VK_4, '4')

            field.text shouldBe "1234"
        }
    }

    @Test
    fun `addKeyPressed receives key events`() {
        assumeDisplay()
        runUiAndWait {
            val field = JTextField()
            var received = -1
            field.addKeyPressed { received = it.keyCode }

            val event = KeyEvent(field, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_A, 'a')
            field.keyListeners.forEach { it.keyPressed(event) }

            received shouldBe KeyEvent.VK_A
        }
    }

}
