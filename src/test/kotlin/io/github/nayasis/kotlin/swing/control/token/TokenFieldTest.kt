package io.github.nayasis.kotlin.swing.control.token

import io.github.nayasis.kotlin.swing.common.assumeDisplay
import io.github.nayasis.kotlin.swing.common.installTheme
import io.github.nayasis.kotlin.swing.control.AutoCompletion
import io.github.nayasis.kotlin.swing.misc.runUiAndWait
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.awt.event.KeyEvent
import javax.swing.JTextField
import javax.swing.UIManager

internal class TokenFieldTest {

    // call the listeners directly: dispatchEvent goes through the focus manager, which a test has no use for
    private fun JTextField.press(code: Int, modifiers: Int = 0): KeyEvent {
        val event = KeyEvent(this, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), modifiers, code, KeyEvent.CHAR_UNDEFINED)
        keyListeners.forEach { it.keyPressed(event) }
        return event
    }

    @Test
    fun `enter commits the input as tokens`() {
        assumeDisplay()
        runUiAndWait {
            val field = TokenField(normalize = { values -> values.flatMap { it.orEmpty().split(' ') }.filter { it.isNotBlank() } })
            var changes = 0
            field.onTokenChanged = { changes++ }

            field.input.text = "alpha beta"
            changes = 0
            field.input.press(KeyEvent.VK_ENTER).isConsumed.shouldBeTrue()

            field.currentTokens() shouldBe setOf("alpha", "beta")
            field.input.text shouldBe ""
            field.chips().map { it.text } shouldBe listOf("alpha", "beta")
            changes shouldBe 1
        }
    }

    @Test
    fun `backspace in an empty input removes the last token`() {
        assumeDisplay()
        runUiAndWait {
            val field = TokenField()
            field.setTokens(listOf("a", "b"))

            field.input.press(KeyEvent.VK_BACK_SPACE)

            field.currentTokens() shouldBe setOf("a")
        }
    }

    @Test
    fun `getTokens commits pending input without firing a change`() {
        assumeDisplay()
        runUiAndWait {
            val field = TokenField()
            field.setTokens(listOf("a"))
            field.input.text = "b"
            var changes = 0
            field.onTokenChanged = { changes++ }

            field.hasPendingInput().shouldBeTrue()
            field.getTokens() shouldBe setOf("a", "b")
            field.hasPendingInput().shouldBeFalse()
            changes shouldBe 0
        }
    }

    @Test
    fun `minimum height matches a text field with or without chips`() {
        assumeDisplay()
        runUiAndWait {
            val reference = JTextField().preferredSize.height
            val field = TokenField()
            field.minimumSize.height shouldBe reference
            field.preferredSize.height shouldBe reference

            field.setTokens(listOf("util"))
            field.minimumSize.height shouldBe field.preferredSize.height
        }
    }

    @Test
    fun `chips keep 1px to the border line and 1px between rows`() {
        assumeDisplay()
        installTheme()
        runUiAndWait {
            val field = TokenField()
            field.setTokens(listOf("alpha", "beta", "gamma", "delta", "epsilon", "zeta"))
            val insets = field.insets
            // FlatLaf border insets are the focus ring plus the border line, so chips start right after the line
            insets.top shouldBe UIManager.getInt("Component.focusWidth") + UIManager.getInt("Component.borderWidth")

            // one row: as tall as a text field, chips 1px off every edge
            field.setSize(2000, field.preferredSize.height)
            field.doLayout()
            field.height shouldBe JTextField().preferredSize.height
            val first = field.chips().first()
            first.x shouldBe insets.left + 1
            first.y shouldBe insets.top + 1
            first.y + first.height shouldBe field.height - insets.bottom - 1

            // narrow enough to wrap: rows 1px apart, the last row 1px above the bottom line
            val narrow = insets.left + insets.right + 2 + field.chips().take(2).sumOf { it.preferredSize.width } + 3
            field.setSize(narrow, 1000)
            field.setSize(narrow, field.preferredSize.height)
            field.doLayout()
            val rows = field.chips().map { it.y }.distinct().sorted()
            (rows.size > 1).shouldBeTrue()
            rows.zipWithNext().forEach { (upper, lower) -> lower - upper shouldBe first.height + 1 }
            val lowest = (field.chips() + field.input).maxOf { it.y + it.height }
            lowest shouldBe field.height - insets.bottom - 1
            field.chips().groupBy { it.y }.values.forEach { row -> row.minOf { it.x } shouldBe insets.left + 1 }
        }
    }

    @Test
    fun `an empty input stays on the chip row of a narrow field`() {
        assumeDisplay()
        installTheme()
        runUiAndWait {
            val field = TokenField()
            field.setTokens(listOf("util"))
            val chip = field.chips().single()
            // the chip plus a few characters: less than the input's preferred width
            val width = field.insets.left + field.insets.right + 2 + chip.preferredSize.width + 3 + 40
            (field.input.preferredSize.width > 40) shouldBe true
            field.setSize(width, 1000)
            field.setSize(width, field.preferredSize.height)
            field.doLayout()

            field.height shouldBe JTextField().preferredSize.height
            field.input.y shouldBe chip.y
        }
    }

    @Test
    fun `chip colors follow a dark theme`() {
        assumeDisplay()
        try {
            runUiAndWait {
                installTheme()
                ChipStyle.DEFAULT.current() shouldBe ChipStyle.DEFAULT
                com.formdev.flatlaf.FlatDarkLaf.setup()
                ChipStyle.DEFAULT.current() shouldBe ChipStyle.DEFAULT.dark
                ChipStyle(dark = null).current() shouldBe ChipStyle(dark = null)
            }
        } finally {
            runUiAndWait { installTheme() }
        }
    }

    @Test
    fun `an open completion popup takes enter before the field`() {
        assumeDisplay()
        runUiAndWait {
            val field = TokenField()
            var selected: String? = null
            val completion = AutoCompletion<String>(field.input) { selected = it }

            // not shown (the field is not on screen), so enter reaches the field
            completion.show(listOf("x"))
            completion.isShowing.shouldBeFalse()
            field.input.text = "typed"
            field.input.press(KeyEvent.VK_ENTER)

            selected shouldBe null
            field.currentTokens() shouldBe setOf("typed")
        }
    }

}
