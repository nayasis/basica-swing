package io.github.nayasis.kotlin.swing.misc

import java.beans.PropertyChangeListener
import javax.swing.UIManager

/** whether the current look and feel draws on a dark background. */
fun isDarkLookAndFeel(): Boolean {
    val background = UIManager.getColor("Panel.background") ?: return false
    return 0.299 * background.red + 0.587 * background.green + 0.114 * background.blue < 128
}

/**
 * call [onChange] after every look and feel switch, once the new defaults are installed.
 *
 * components outside a window (popups not showing, panels swapped out) miss the switch; refresh them here
 * with `SwingUtilities.updateComponentTreeUI`. close the returned handle to stop listening.
 */
fun onLookAndFeelChange(onChange: () -> Unit): AutoCloseable {
    val listener = PropertyChangeListener { if (it.propertyName == "lookAndFeel") onChange() }
    UIManager.addPropertyChangeListener(listener)
    return AutoCloseable { UIManager.removePropertyChangeListener(listener) }
}
