package io.github.nayasis.kotlin.swing.window

import java.awt.BorderLayout
import java.awt.Component
import java.awt.Point
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JWindow
import javax.swing.SwingUtilities

/**
 * small window that stays open under an anchor component until [hide] is called.
 *
 * unlike a popup menu it does not close on outside clicks, so it can hold buttons that are used repeatedly.
 * it follows its anchor when the owner window moves, and closes with it.
 *
 * ```
 * val popOver = PopOver(JLabel("hello"))
 * button.addActionListener { popOver.toggle(button) }
 * ```
 */
class PopOver(content: JComponent) {

    var content: JComponent = content
        set(value) {
            field = value
            window?.let {
                holder.removeAll()
                holder.add(value, BorderLayout.CENTER)
                repack()
            }
        }

    /** called after the window is closed, by [hide] or along with its owner. */
    var onHidden: (() -> Unit)? = null

    private val holder = JPanel(BorderLayout())
    private var window: JWindow? = null
    private var anchor: Component? = null

    private val follower = object: ComponentAdapter() {
        override fun componentMoved(e: ComponentEvent) = place()
        override fun componentResized(e: ComponentEvent) = place()
        override fun componentHidden(e: ComponentEvent) = hide()
    }

    private val closer = object: WindowAdapter() {
        override fun windowClosed(e: WindowEvent) = hide()
        override fun windowIconified(e: WindowEvent) = hide()
    }

    val isShowing: Boolean
        get() = window?.isVisible == true

    fun toggle(anchor: Component) {
        if (isShowing) hide() else show(anchor)
    }

    fun show(anchor: Component) {
        if (isShowing && this.anchor === anchor) return
        hide()
        val owner = SwingUtilities.getWindowAncestor(anchor)
        this.anchor = anchor
        holder.removeAll()
        holder.border = WindowBorder()
        holder.add(content, BorderLayout.CENTER)
        window = JWindow(owner).apply {
            contentPane = holder
            focusableWindowState = false
            pack()
            installRoundedCorners()
        }
        owner?.addComponentListener(follower)
        owner?.addWindowListener(closer)
        place()
        window?.isVisible = true
    }

    fun hide() {
        val current = window ?: return
        window = null
        anchor?.let { SwingUtilities.getWindowAncestor(it) }?.let { owner: Window ->
            owner.removeComponentListener(follower)
            owner.removeWindowListener(closer)
        }
        anchor = null
        current.isVisible = false
        current.dispose()
        onHidden?.invoke()
    }

    /** resize to the content, after it changed. */
    fun repack() {
        window?.let {
            it.pack()
            place()
        }
    }

    // below the anchor, right edges aligned, kept on the anchor's screen
    private fun place() {
        val window = window ?: return
        val anchor = anchor?.takeIf { it.isShowing } ?: return
        val origin = anchor.locationOnScreen
        val screen = anchor.graphicsConfiguration?.visibleBounds
        var x = origin.x + anchor.width - window.width
        var y = origin.y + anchor.height + 2
        if (screen != null) {
            if (y + window.height > screen.y + screen.height) y = origin.y - window.height - 2
            x = x.coerceIn(screen.x, (screen.x + screen.width - window.width).coerceAtLeast(screen.x))
            y = y.coerceAtLeast(screen.y)
        }
        window.location = Point(x, y)
    }

}
