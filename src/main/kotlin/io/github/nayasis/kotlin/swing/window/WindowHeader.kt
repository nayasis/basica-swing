package io.github.nayasis.kotlin.swing.window

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Dialog
import java.awt.Frame
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Image
import java.awt.Insets
import java.awt.Window
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.UIManager

private const val TOP_RESIZE_HEIGHT = 5
private const val ICON_SIZE = 16

/**
 * title bar of an undecorated window: icon, title, optional menu bar and window buttons.
 *
 * dragging the bar moves the window, double click toggles maximize (zoom), and the top edge
 * resizes the window. the bar follows the window title and icon, and is dimmed while the
 * window is not focused.
 *
 * ```
 * frame.setBorderless()
 * frame.contentPane.add(WindowHeader(menuBar), BorderLayout.NORTH)
 * ```
 *
 * @param menuBar menu bar to put in the bar
 * @param buttons window buttons to show, in order
 */
class WindowHeader(
    private val menuBar: JMenuBar? = null,
    private val buttons: List<WindowButtonType> = listOf(WindowButtonType.Close, WindowButtonType.Hide, WindowButtonType.Zoom),
): JPanel(BorderLayout()) {

    private val iconLabel  = JLabel()
    private val titleLabel = JLabel()
    private var boundWindow: Window? = null
    private val menuForegrounds = HashMap<JMenu, Color?>()

    private var topResizing = false
    private var resizeStartY = 0
    private var resizeStartBottom = 0

    private val focusListener = object: WindowFocusListener {
        override fun windowGainedFocus(e: WindowEvent) = updateActive(true)
        override fun windowLostFocus(e: WindowEvent)   = updateActive(false)
    }

    private val titleListener = java.beans.PropertyChangeListener { titleLabel.text = (boundWindow?.title() ?: "") }
    private val iconListener  = java.beans.PropertyChangeListener { syncIcon() }

    init {
        isOpaque = false
        border   = BorderFactory.createEmptyBorder()

        val left = JPanel(BorderLayout(6, 0)).apply { isOpaque = false }
        left.add(JPanel(GridBagLayout()).apply {
            isOpaque = false
            add(iconLabel, GridBagConstraints().apply { insets = Insets(0, 4, 0, 0) })
            add(titleLabel, GridBagConstraints().apply { insets = Insets(0, 6, 0, 8) })
        }, BorderLayout.WEST)
        menuBar?.let { left.add(it, BorderLayout.CENTER) }
        add(left, BorderLayout.WEST)

        installWindowMover(accept = { it.y > TOP_RESIZE_HEIGHT })
        addMouseListener(object: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) = onClicked(e)
        })

        // the top resize area must work over the interactive children too
        val topResizer = TopResizer()
        listOfNotNull<Component>(this, menuBar).forEach {
            it.addMouseListener(topResizer)
            it.addMouseMotionListener(topResizer)
        }
    }

    override fun addNotify() {
        super.addNotify()
        bind(SwingUtilities.getWindowAncestor(this))
    }

    override fun removeNotify() {
        unbind()
        super.removeNotify()
    }

    private fun bind(window: Window?) {
        if( window == null || window === boundWindow ) return
        unbind()
        boundWindow = window

        // buttons are created here because they are wired to the window
        if( componentCount == 1 && buttons.isNotEmpty() ) {
            val panel = window.createWindowButtons(
                close = WindowButtonType.Close in buttons,
                hide  = WindowButtonType.Hide  in buttons,
                zoom  = WindowButtonType.Zoom  in buttons,
            )
            add(panel, BorderLayout.EAST)
            revalidate()
        }

        titleLabel.text = window.title()
        window.addPropertyChangeListener("title", titleListener)
        window.addPropertyChangeListener("iconImage", iconListener)
        window.addWindowFocusListener(focusListener)
        syncIcon()
        updateActive(window.isFocused)
    }

    private fun unbind() {
        boundWindow?.let {
            it.removePropertyChangeListener("title", titleListener)
            it.removePropertyChangeListener("iconImage", iconListener)
            it.removeWindowFocusListener(focusListener)
        }
        boundWindow = null
    }

    private fun Window.title(): String = when(this) {
        is Frame  -> title ?: ""
        is Dialog -> title ?: ""
        else      -> ""
    }

    private fun syncIcon() {
        val image = boundWindow?.iconImages?.firstOrNull()
        iconLabel.icon = image?.let { ImageIcon(it.getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH)) }
    }

    private fun updateActive(active: Boolean) {
        val normal   = UIManager.getColor("Label.foreground")
        val inactive = UIManager.getColor("Label.disabledForeground")
        titleLabel.foreground = if( active ) normal else inactive
        menuBar?.let { bar ->
            for( i in 0 until bar.menuCount ) {
                val menu = bar.getMenu(i) ?: continue
                if( active ) {
                    if( menu in menuForegrounds ) menu.foreground = menuForegrounds.remove(menu)
                } else {
                    menuForegrounds.getOrPut(menu) { menu.foreground }
                    menu.foreground = inactive
                }
            }
        }
    }

    private fun onClicked(e: MouseEvent) {
        if( !SwingUtilities.isLeftMouseButton(e) || e.clickCount != 2 || e.y <= TOP_RESIZE_HEIGHT ) return
        val window = boundWindow ?: return
        if( window.isMaximized ) window.isMaximized = false else window.setZoom(!window.isZoomed)
    }

    private inner class TopResizer: MouseAdapter() {

        override fun mouseMoved(e: MouseEvent) {
            val source = e.component
            if( inTopArea(e) ) {
                source.cursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)
            } else if( source.cursor.type == Cursor.N_RESIZE_CURSOR ) {
                source.cursor = null
            }
        }

        override fun mouseExited(e: MouseEvent) {
            val source = e.component
            if( source.cursor.type == Cursor.N_RESIZE_CURSOR ) source.cursor = null
        }

        override fun mousePressed(e: MouseEvent) {
            val window = boundWindow ?: return
            topResizing = false
            if( !SwingUtilities.isLeftMouseButton(e) || !inTopArea(e) || window.isMaximized || window.isZoomed ) return
            topResizing = true
            resizeStartY      = e.yOnScreen
            resizeStartBottom = window.y + window.height
        }

        override fun mouseDragged(e: MouseEvent) {
            val window = boundWindow ?: return
            if( !topResizing ) return
            val minHeight = window.minimumSize.height
            val height = (resizeStartBottom - e.yOnScreen).coerceAtLeast(minHeight)
            window.setBounds(window.x, resizeStartBottom - height, window.width, height)
        }

        override fun mouseReleased(e: MouseEvent) {
            topResizing = false
        }

        private fun inTopArea(e: MouseEvent): Boolean =
            SwingUtilities.convertMouseEvent(e.component, e, this@WindowHeader).y in 0..TOP_RESIZE_HEIGHT

    }

}
