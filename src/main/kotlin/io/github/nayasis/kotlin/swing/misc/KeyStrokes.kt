@file:Suppress("unused")

package io.github.nayasis.kotlin.swing.misc

import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.lang.reflect.Modifier
import javax.swing.KeyStroke

/** modifiers a shortcut can carry. lock keys and mouse buttons are not part of it. */
private const val SHORTCUT_MODIFIERS =
    InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK or InputEvent.ALT_DOWN_MASK or InputEvent.META_DOWN_MASK

private val MODIFIER_NAMES = linkedMapOf(
    InputEvent.CTRL_DOWN_MASK  to "Ctrl",
    InputEvent.SHIFT_DOWN_MASK to "Shift",
    InputEvent.ALT_DOWN_MASK   to "Alt",
    InputEvent.META_DOWN_MASK  to "Meta",
)

private val MODIFIER_ALIASES = mapOf(
    "ctrl"     to InputEvent.CTRL_DOWN_MASK,
    "control"  to InputEvent.CTRL_DOWN_MASK,
    "shift"    to InputEvent.SHIFT_DOWN_MASK,
    "alt"      to InputEvent.ALT_DOWN_MASK,
    "option"   to InputEvent.ALT_DOWN_MASK,
    "meta"     to InputEvent.META_DOWN_MASK,
    "cmd"      to InputEvent.META_DOWN_MASK,
    "command"  to InputEvent.META_DOWN_MASK,
)

private val MODIFIER_KEYS = setOf(
    KeyEvent.VK_SHIFT, KeyEvent.VK_CONTROL, KeyEvent.VK_ALT, KeyEvent.VK_META,
    KeyEvent.VK_ALT_GRAPH, KeyEvent.VK_WINDOWS, KeyEvent.VK_CONTEXT_MENU,
)

// key names other toolkits (JavaFX, browsers) write differently
private val KEY_ALIASES = mapOf(
    "esc"    to "escape",
    "del"    to "delete",
    "ins"    to "insert",
    "return" to "enter",
    "pgup"   to "pageup",
    "pgdn"   to "pagedown",
)

/** `VK_*` constants by their name without prefix and separators, e.g. `backspace` -> VK_BACK_SPACE. */
private val KEY_CODES: Map<String, Int> by lazy {
    KeyEvent::class.java.fields
        .filter { Modifier.isStatic(it.modifiers) && it.type == Int::class.javaPrimitiveType && it.name.startsWith("VK_") }
        .associate { normalizeKeyName(it.name.removePrefix("VK_")) to it.getInt(null) }
}

private val KEY_NAMES: Map<Int, String> by lazy {
    KeyEvent::class.java.fields
        .filter { Modifier.isStatic(it.modifiers) && it.type == Int::class.javaPrimitiveType && it.name.startsWith("VK_") }
        .associate { it.getInt(null) to it.name.removePrefix("VK_") }
}

private fun normalizeKeyName(name: String): String =
    name.lowercase().filter { it.isLetterOrDigit() }

/**
 * create a key stroke pressed with [modifiers] (`InputEvent.*_DOWN_MASK`).
 *
 * key strokes made here compare equal whenever they mean the same keys.
 */
fun keyStroke(keyCode: Int, modifiers: Int = 0): KeyStroke =
    KeyStroke.getKeyStroke(keyCode, modifiers and SHORTCUT_MODIFIERS)

/** the `InputEvent.*_DOWN_MASK` modifiers of this key stroke. */
val KeyStroke.modifiersEx: Int
    get() = modifiers and SHORTCUT_MODIFIERS

/**
 * readable text like `Ctrl+Shift+N`.
 *
 * it is also the persistent form: [toKeyStroke] parses it back.
 */
val KeyStroke.displayText: String
    get() {
        val parts = MODIFIER_NAMES.filterKeys { (modifiersEx and it) != 0 }.values.toMutableList()
        parts += keyName(keyCode)
        return parts.joinToString("+")
    }

private fun keyName(keyCode: Int): String {
    val name = KEY_NAMES[keyCode] ?: return KeyEvent.getKeyText(keyCode)
    return name.split('_').joinToString(" ") { word ->
        if (word.length <= 1 || word.matches("F\\d+".toRegex())) word
        else word.lowercase().replaceFirstChar { it.uppercase() }
    }
}

/**
 * parse text like `Ctrl+Shift+N`, `Alt+DELETE` or `Esc` into a key stroke.
 *
 * names are matched case insensitively, ignoring spaces and underscores, so `Back Space`, `BACK_SPACE` and
 * `Backspace` are the same key. `Shortcut` means Ctrl, or Meta on macOS.
 *
 * @return the key stroke, or null when the text is blank or names an unknown key
 */
fun String?.toKeyStroke(): KeyStroke? {
    if (this.isNullOrBlank()) return null
    val parts = split('+').map { it.trim() }
    // "Ctrl++" names the plus key
    val keyPart = if (endsWith("++")) "plus" else parts.last()
    if (keyPart.isEmpty()) return null
    var modifiers = 0
    for (part in parts.dropLast(if (endsWith("++")) 2 else 1)) {
        val name = normalizeKeyName(part)
        modifiers = modifiers or when (name) {
            "shortcut" -> if (System.getProperty("os.name").orEmpty().lowercase().contains("mac")) InputEvent.META_DOWN_MASK else InputEvent.CTRL_DOWN_MASK
            else -> MODIFIER_ALIASES[name] ?: return null
        }
    }
    val keyName = normalizeKeyName(keyPart).let { KEY_ALIASES[it] ?: it }
    val keyCode = KEY_CODES[keyName] ?: return null
    return keyStroke(keyCode, modifiers)
}

/**
 * the key stroke this key press makes, or null when only a modifier key is pressed.
 */
fun KeyEvent.toKeyStroke(): KeyStroke? {
    if (keyCode in MODIFIER_KEYS || keyCode == KeyEvent.VK_UNDEFINED) return null
    return keyStroke(keyCode, modifiersEx)
}

/** whether this key stroke is pressed by [event]. only key presses can match. */
fun KeyStroke.matches(event: KeyEvent): Boolean {
    if (event.id != KeyEvent.KEY_PRESSED) return false
    return event.keyCode == keyCode && (event.modifiersEx and SHORTCUT_MODIFIERS) == modifiersEx
}
