package io.github.nayasis.kotlin.swing.control

import io.github.nayasis.kotlin.basica.core.string.mask
import io.github.oshai.kotlinlogging.KotlinLogging
import java.awt.Component
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.JTextComponent

private val logger = KotlinLogging.logger {}

private val MASK_IGNORED_KEYS = setOf(
    KeyEvent.VK_HOME, KeyEvent.VK_END, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
    KeyEvent.VK_SHIFT, KeyEvent.VK_CONTROL, KeyEvent.VK_CAPS_LOCK, KeyEvent.VK_ESCAPE,
)

fun Component.addKeyPressed(handler: (KeyEvent) -> Unit) {
    addKeyListener(object: KeyAdapter() {
        override fun keyPressed(e: KeyEvent) = handler(e)
    })
}

fun Component.addKeyReleased(handler: (KeyEvent) -> Unit) {
    addKeyListener(object: KeyAdapter() {
        override fun keyReleased(e: KeyEvent) = handler(e)
    })
}

/**
 * format the text on every key release by [pattern], e.g. `####-##-##`.
 *
 * @param pass pattern char that lets an input char through
 * @param hide char shown in place of a masked input char
 * @param modifier pre-processes the raw text before masking
 */
fun JTextComponent.addMask(pattern: String, pass: Char = '#', hide: Char = '*', modifier: ((inputText: String) -> String)? = null): JTextComponent {
    addKeyReleased { e ->
        if (e.keyCode in MASK_IGNORED_KEYS) return@addKeyReleased
        val current   = text ?: ""
        val prevCaret = caretPosition
        val maskedVal = (modifier?.invoke(current) ?: current).mask(pattern, pass, hide)
        val currCaret = when {
            current.length < maskedVal.length -> prevCaret + (maskedVal.length - current.length) + 1
            e.keyCode == KeyEvent.VK_BACK_SPACE -> prevCaret
            e.keyCode == KeyEvent.VK_DELETE -> prevCaret
            current.length > maskedVal.length -> {
                // paste
                if (current.length > pattern.length) {
                    prevCaret - (current.length - maskedVal.length)
                // cut
                } else {
                    prevCaret - (current.length - maskedVal.length) + 1
                }
            }
            else -> prevCaret
        }
        logger.trace { "text: $current -> $maskedVal, caret: $prevCaret -> $currCaret, event: ${e.keyCode}" }
        if (maskedVal != current) text = maskedVal
        caretPosition = currCaret.coerceIn(0, maskedVal.length)
    }
    return this
}

/** call [handler] after every change of the text, whether typed or set. */
fun JTextComponent.addTextChanged(handler: () -> Unit): JTextComponent {
    document.addDocumentListener(object: DocumentListener {
        override fun insertUpdate(e: DocumentEvent) = handler()
        override fun removeUpdate(e: DocumentEvent) = handler()
        override fun changedUpdate(e: DocumentEvent) {}
    })
    return this
}
