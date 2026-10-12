@file:Suppress("unused")

package io.github.nayasis.kotlin.swing.window

import io.github.nayasis.kotlin.basica.core.string.message
import java.awt.Dialog
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Frame
import java.awt.Image
import java.awt.Window
import java.awt.event.WindowEvent
import javax.swing.AbstractButton
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JPanel

private const val ICON_SIZE = 16

enum class WindowButtonType(val key: String) {
    Close("close"),
    Hide("hide"),
    Zoom("zoom"),
}

/**
 * button for an undecorated window. the icon is swapped while the pointer is over it.
 */
class WindowButton(val type: WindowButtonType): JButton() {

    init {
        icon         = loadIcon(type.key)
        rolloverIcon = loadIcon("${type.key}-hover")
        isRolloverEnabled  = true
        isBorderPainted    = false
        isContentAreaFilled = false
        isFocusPainted     = false
        isFocusable        = false
        isOpaque           = false
        margin             = java.awt.Insets(0, 0, 0, 0)
        toolTipText        = runCatching { type.key.message() }.getOrDefault(type.key)
        preferredSize      = Dimension(ICON_SIZE + 4, ICON_SIZE + 4)
    }

    private fun loadIcon(name: String): ImageIcon? {
        val url = WindowButton::class.java.getResource("/basica-swing/images/$name.png") ?: return null
        val scaled = ImageIcon(url).image.getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH)
        return ImageIcon(scaled)
    }

}

/**
 * make the window undecorated.
 *
 * [Window] decoration can be changed only before the window is displayable, so call this
 * before `pack()` or `isVisible = true`.
 *
 * @param resizable resize by dragging the window edges
 * @param margin width of the edge area used for resizing
 */
fun Window.setBorderless(resizable: Boolean = true, margin: Int = 5) {
    check( ! isDisplayable ) { "setBorderless should be called before the window is displayable" }
    when(this) {
        is Frame  -> isUndecorated = true
        is Dialog -> isUndecorated = true
    }
    if( resizable ) WindowResizer(this, margin).install(this)
}

val Window.isBorderless: Boolean
    get() = when(this) {
        is Frame  -> isUndecorated
        is Dialog -> isUndecorated
        else      -> true
    }

/** request the window to close, as a close button of a decorated window does. */
fun Window.addClose(button: AbstractButton) {
    button.addActionListener { dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING)) }
}

fun Window.addIconified(button: AbstractButton) {
    button.addActionListener {
        (this as? Frame)?.let { it.extendedState = it.extendedState or Frame.ICONIFIED }
    }
}

fun Window.addZoomed(button: AbstractButton) {
    button.addActionListener {
        if( isMaximized ) isMaximized = false else setZoom(!isZoomed)
    }
}

/**
 * create a panel of window buttons wired to this window.
 */
fun Window.createWindowButtons(
    close: Boolean = true,
    hide: Boolean = true,
    zoom: Boolean = true,
): JPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 3, 0)).apply {
    isOpaque = false
    if( close ) add(WindowButton(WindowButtonType.Close).also { addClose(it) })
    if( hide  ) add(WindowButton(WindowButtonType.Hide ).also { addIconified(it) })
    if( zoom  ) add(WindowButton(WindowButtonType.Zoom ).also { addZoomed(it) })
}
