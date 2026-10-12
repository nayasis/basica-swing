package io.github.nayasis.kotlin.swing.misc

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.awt.Canvas
import java.awt.event.InputEvent.ALT_DOWN_MASK
import java.awt.event.InputEvent.CTRL_DOWN_MASK
import java.awt.event.InputEvent.SHIFT_DOWN_MASK
import java.awt.event.KeyEvent

internal class KeyStrokesTest {

    private val source = Canvas()

    private fun press(code: Int, modifiers: Int = 0) =
        KeyEvent(source, KeyEvent.KEY_PRESSED, 0, modifiers, code, KeyEvent.CHAR_UNDEFINED)

    @Test
    fun `display text lists modifiers before the key`() {
        keyStroke(KeyEvent.VK_N, SHIFT_DOWN_MASK or CTRL_DOWN_MASK).displayText shouldBe "Ctrl+Shift+N"
        keyStroke(KeyEvent.VK_F1).displayText shouldBe "F1"
        keyStroke(KeyEvent.VK_BACK_SPACE).displayText shouldBe "Back Space"
        keyStroke(KeyEvent.VK_COMMA, CTRL_DOWN_MASK).displayText shouldBe "Ctrl+Comma"
    }

    @Test
    fun `display text parses back to the same key stroke`() {
        val strokes = listOf(
            keyStroke(KeyEvent.VK_S, CTRL_DOWN_MASK),
            keyStroke(KeyEvent.VK_DELETE, SHIFT_DOWN_MASK),
            keyStroke(KeyEvent.VK_PAGE_UP, ALT_DOWN_MASK or CTRL_DOWN_MASK),
            keyStroke(KeyEvent.VK_ENTER),
        )
        strokes.forEach { it.displayText.toKeyStroke() shouldBe it }
    }

    @Test
    fun `parsing accepts other spellings`() {
        "Alt+DELETE".toKeyStroke() shouldBe keyStroke(KeyEvent.VK_DELETE, ALT_DOWN_MASK)
        "ctrl + shift + n".toKeyStroke() shouldBe keyStroke(KeyEvent.VK_N, CTRL_DOWN_MASK or SHIFT_DOWN_MASK)
        "Esc".toKeyStroke() shouldBe keyStroke(KeyEvent.VK_ESCAPE)
        "Backspace".toKeyStroke() shouldBe keyStroke(KeyEvent.VK_BACK_SPACE)
    }

    @Test
    fun `unknown or blank text does not parse`() {
        "".toKeyStroke().shouldBeNull()
        "   ".toKeyStroke().shouldBeNull()
        "Ctrl+NoSuchKey".toKeyStroke().shouldBeNull()
        "Hyper+S".toKeyStroke().shouldBeNull()
    }

    @Test
    fun `key events match their key stroke only`() {
        val save = keyStroke(KeyEvent.VK_S, CTRL_DOWN_MASK)

        save.matches(press(KeyEvent.VK_S, CTRL_DOWN_MASK)).shouldBeTrue()
        save.matches(press(KeyEvent.VK_S)).shouldBeFalse()
        save.matches(press(KeyEvent.VK_S, CTRL_DOWN_MASK or SHIFT_DOWN_MASK)).shouldBeFalse()
    }

    @Test
    fun `a modifier key alone makes no key stroke`() {
        press(KeyEvent.VK_CONTROL, CTRL_DOWN_MASK).toKeyStroke().shouldBeNull()
        press(KeyEvent.VK_A, ALT_DOWN_MASK).toKeyStroke() shouldBe keyStroke(KeyEvent.VK_A, ALT_DOWN_MASK)
    }

}
