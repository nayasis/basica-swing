package io.github.nayasis.kotlin.swing.window

import java.awt.Component
import java.awt.Container
import java.awt.Cursor
import java.awt.Cursor.*
import java.awt.Rectangle
import java.awt.Window
import java.awt.event.ContainerAdapter
import java.awt.event.ContainerEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import kotlin.math.max

/**
 * resize an undecorated window by dragging its edges.
 *
 * ```
 * WindowResizer(frame).install(frame)
 * ```
 */
class WindowResizer(
    private val window: Window,
    private val margin: Int = 5,
) {

    private var cursorType = DEFAULT_CURSOR
    // distance from the pointer to the bottom right corner when pressed
    private var gapX = 0
    private var gapY = 0
    private var cursorOwner: Component? = null
    private var ownerCursor: Cursor? = null

    private val listener = object: MouseAdapter() {
        override fun mouseMoved(e: MouseEvent)    = onMoved(e)
        override fun mouseExited(e: MouseEvent)   = restoreCursor()
        override fun mousePressed(e: MouseEvent)  = onPressed(e)
        override fun mouseDragged(e: MouseEvent)  = onDragged(e)
        override fun mouseReleased(e: MouseEvent) { if( cursorType == DEFAULT_CURSOR ) restoreCursor() }
    }

    private val containerListener = object: ContainerAdapter() {
        override fun componentAdded(e: ContainerEvent) { attach(e.child) }
    }

    fun isResizing(): Boolean = cursorType != DEFAULT_CURSOR

    /**
     * attach resize handling to [root] and all its descendants, including components added later.
     */
    fun install(root: Container) {
        attach(root)
    }

    private fun attach(component: Component) {
        component.removeMouseListener(listener)
        component.removeMouseMotionListener(listener)
        component.addMouseListener(listener)
        component.addMouseMotionListener(listener)
        if( component is Container ) {
            component.removeContainerListener(containerListener)
            component.addContainerListener(containerListener)
            component.components.forEach { attach(it) }
        }
    }

    private fun onMoved(e: MouseEvent) {
        val x = e.xOnScreen - window.x
        val y = e.yOnScreen - window.y
        val w = window.width
        val h = window.height
        cursorType = when {
            x <= margin     && y <= margin     -> NW_RESIZE_CURSOR
            x <= margin     && y >= h - margin -> SW_RESIZE_CURSOR
            x >= w - margin && y <= margin     -> NE_RESIZE_CURSOR
            x >= w - margin && y >= h - margin -> SE_RESIZE_CURSOR
            x <= margin                        -> W_RESIZE_CURSOR
            x >= w - margin                    -> E_RESIZE_CURSOR
            y <= margin                        -> N_RESIZE_CURSOR
            y >= h - margin                    -> S_RESIZE_CURSOR
            else                               -> DEFAULT_CURSOR
        }
        if( cursorType == DEFAULT_CURSOR ) restoreCursor() else applyCursor(e.component)
    }

    private fun applyCursor(source: Component) {
        if( cursorOwner !== source ) {
            restoreCursor()
            cursorOwner = source
            ownerCursor = if( source.isCursorSet ) source.cursor else null
        }
        source.cursor = getPredefinedCursor(cursorType)
    }

    private fun restoreCursor() {
        cursorOwner?.cursor = ownerCursor
        cursorOwner = null
        ownerCursor = null
    }

    private fun onPressed(e: MouseEvent) {
        gapX = window.width  - (e.xOnScreen - window.x)
        gapY = window.height - (e.yOnScreen - window.y)
    }

    private fun onDragged(e: MouseEvent) {
        if( cursorType == DEFAULT_CURSOR ) return

        val minSize = window.minimumSize
        val minW = max(minSize.width,  margin * 2)
        val minH = max(minSize.height, margin * 2)

        val bounds = Rectangle(window.bounds)
        val pointerX = e.xOnScreen
        val pointerY = e.yOnScreen

        if( cursorType != W_RESIZE_CURSOR && cursorType != E_RESIZE_CURSOR ) {
            if( cursorType in NORTH ) {
                val bottom = bounds.y + bounds.height
                bounds.height = max(bottom - pointerY, minH)
                bounds.y = bottom - bounds.height
            } else {
                bounds.height = max(pointerY - bounds.y + gapY, minH)
            }
        }
        if( cursorType != N_RESIZE_CURSOR && cursorType != S_RESIZE_CURSOR ) {
            if( cursorType in WEST ) {
                val right = bounds.x + bounds.width
                bounds.width = max(right - pointerX, minW)
                bounds.x = right - bounds.width
            } else {
                bounds.width = max(pointerX - bounds.x + gapX, minW)
            }
        }
        window.bounds = bounds
    }

    private companion object {
        val NORTH = setOf(NW_RESIZE_CURSOR, N_RESIZE_CURSOR, NE_RESIZE_CURSOR)
        val WEST  = setOf(NW_RESIZE_CURSOR, W_RESIZE_CURSOR, SW_RESIZE_CURSOR)
    }

}
