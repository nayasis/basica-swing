package io.github.nayasis.kotlin.swing.window

import java.awt.Component
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

/**
 * make the owning window draggable by dragging this component.
 *
 * dragging a maximized or zoomed window restores it first and keeps the pointer at
 * the same horizontal ratio of the restored window.
 *
 * @param styleOnDrag called with true when a drag starts and false when it ends
 * @param accept decides whether a mouse press may start a drag (event is relative to this component)
 */
fun Component.installWindowMover(
    styleOnDrag: ((dragging: Boolean) -> Unit)? = null,
    accept: (MouseEvent) -> Boolean = { true },
) {
    val component = this
    val mover = object: MouseAdapter() {

        var offsetX = 0
        var offsetY = 0
        var restoreRatioX = 0.5
        var armed    = false
        var dragging = false

        override fun mousePressed(e: MouseEvent) {
            armed = false
            if( !SwingUtilities.isLeftMouseButton(e) || !accept(e) ) return
            val window = SwingUtilities.getWindowAncestor(component) ?: return
            armed = true
            if( window.isMaximized || window.isZoomed ) {
                val screen: Rectangle = BoundaryChecker().getMajorScreen(window).visibleBounds
                restoreRatioX = ((e.xOnScreen - screen.x).toDouble() / screen.width).coerceIn(0.0, 1.0)
                offsetY = e.yOnScreen - window.y
            } else {
                offsetX = e.xOnScreen - window.x
                offsetY = e.yOnScreen - window.y
            }
        }

        override fun mouseDragged(e: MouseEvent) {
            if( !armed ) return
            val window = SwingUtilities.getWindowAncestor(component) ?: return
            if( window.isMaximized || window.isZoomed ) {
                val wasMaximized = window.isMaximized
                if( wasMaximized ) window.isMaximized = false else window.setZoom(false)
                // the restored size is not applied synchronously after leaving maximized state
                val restoredWidth = when {
                    wasMaximized && window.maximizedWatcherInstalled -> window.previousBoundary.boundary.width
                    else -> window.width
                }
                offsetX = (restoredWidth * restoreRatioX).toInt()
            }
            window.setLocation(e.xOnScreen - offsetX, e.yOnScreen - offsetY)
            if( !dragging ) {
                dragging = true
                styleOnDrag?.invoke(true)
            }
        }

        override fun mouseReleased(e: MouseEvent) {
            armed = false
            if( dragging ) {
                dragging = false
                styleOnDrag?.invoke(false)
            }
        }
    }
    addMouseListener(mover)
    addMouseMotionListener(mover)
}
