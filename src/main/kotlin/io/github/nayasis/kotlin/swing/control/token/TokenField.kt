package io.github.nayasis.kotlin.swing.control.token

import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * input of a set of string tokens, shown as chips.
 *
 * - Enter turns the typed text into tokens
 * - Backspace in an empty input removes the last token
 * - clicking a chip removes its token
 *
 * @param normalize cleans the typed values up into tokens: trims, splits, drops blanks...
 */
open class TokenField(
    inputColumns: Int = 8,
    private val normalize: (Iterable<String?>) -> Collection<String> = { values ->
        values.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
    },
    private val chipStyle: ChipStyle = ChipStyle.DEFAULT,
): TokenInputPane(inputColumns) {

    private val tokens = LinkedHashSet<String>()
    private var suppressChange = false

    /** called whenever the tokens or the pending input change. */
    var onTokenChanged: (() -> Unit)? = null

    init {
        input.document.addDocumentListener(object: DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = onInputChanged()
            override fun removeUpdate(e: DocumentEvent) = onInputChanged()
            override fun changedUpdate(e: DocumentEvent) = onInputChanged()
        })
        input.addKeyListener(object: KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.isConsumed) return
                when {
                    e.keyCode == KeyEvent.VK_ENTER && e.modifiersEx == 0 -> {
                        addFromInput()
                        e.consume()
                    }
                    e.keyCode == KeyEvent.VK_BACK_SPACE && input.text.isEmpty() && tokens.isNotEmpty() -> {
                        tokens.remove(tokens.last())
                        renderTokens()
                        fireTokenChanged()
                        e.consume()
                    }
                }
            }
        })
        renderTokens()
    }

    fun setTokens(values: Iterable<String?>?) {
        silently {
            tokens.clear()
            values?.let { tokens.addAll(normalize(it)) }
            input.text = ""
            renderTokens()
        }
    }

    /** the tokens, including the pending input, which is committed first. */
    fun getTokens(): HashSet<String> {
        addFromInput(fireChanged = false)
        return HashSet(tokens)
    }

    /** the tokens without the pending input. */
    fun currentTokens(): HashSet<String> = HashSet(tokens)

    fun hasPendingInput(): Boolean = input.text.isNotBlank()

    fun clearTokens() {
        silently {
            tokens.clear()
            input.text = ""
            renderTokens()
        }
    }

    /** turn the pending input into tokens. */
    fun commitInput() {
        addFromInput()
    }

    private fun addFromInput(fireChanged: Boolean = true) {
        val text = input.text
        if (text.isEmpty()) return
        val added = normalize(listOf(text)).filter { tokens.add(it) }.isNotEmpty()
        silently { input.text = "" }
        if (added) {
            renderTokens()
            if (fireChanged) fireTokenChanged()
        }
    }

    private fun renderTokens() {
        render(tokens.map { token ->
            chip(token, chipStyle) {
                tokens.remove(token)
                renderTokens()
                fireTokenChanged()
            }
        })
    }

    private fun onInputChanged() {
        if (!suppressChange) fireTokenChanged()
    }

    private fun fireTokenChanged() {
        onTokenChanged?.invoke()
    }

    private fun silently(block: () -> Unit) {
        val previous = suppressChange
        suppressChange = true
        try {
            block()
        } finally {
            suppressChange = previous
        }
    }

}
