package io.github.nayasis.kotlin.swing.window

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT
import java.awt.Insets
import java.awt.RenderingHints
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.RootPaneContainer
import javax.swing.UIManager
import javax.swing.border.AbstractBorder

internal fun windowBorderColor(active: Boolean = false): Color =
    (if (active) UIManager.getColor("Component.focusedBorderColor") else null)
        ?: UIManager.getColor("Component.borderColor")
        ?: UIManager.getColor("Separator.foreground")
        ?: Color.GRAY

private fun windowCornerRadius(): Int =
    // Ordinary desktop windows use larger corners than small popup menus.
    UIManager.getInt("Popup.borderCornerRadius").coerceAtLeast(8)

/** Native decorations handle ordinary windows; custom undecorated windows need their own outline. */
internal fun Window.installRoundedCorners() {
    if (!graphicsConfiguration.device.isWindowTranslucencySupported(PERPIXEL_TRANSPARENT)) return
    fun updateShape() {
        val arc = windowCornerRadius() * 2.0
        shape = RoundRectangle2D.Double(0.0, 0.0, width.toDouble(), height.toDouble(), arc, arc)
    }
    addComponentListener(object: ComponentAdapter() {
        override fun componentResized(e: ComponentEvent) = updateShape()
    })
    (this as? RootPaneContainer)?.rootPane?.addPropertyChangeListener("UI") { updateShape() }
    updateShape()
}

/** Read colors while painting so changing themes also updates existing custom windows. */
internal class WindowBorder(private val active: () -> Boolean = { false }): AbstractBorder() {

    override fun getBorderInsets(c: Component): Insets = Insets(1, 1, 1, 1)

    override fun getBorderInsets(c: Component, insets: Insets): Insets = insets.apply { set(1, 1, 1, 1) }

    override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
        val graphics = g.create() as Graphics2D
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.color = windowBorderColor(active())
            graphics.stroke = BasicStroke(1f)
            val arc = windowCornerRadius() * 2.0
            graphics.draw(RoundRectangle2D.Double(x + 0.5, y + 0.5, width - 1.0, height - 1.0, arc, arc))
        } finally {
            graphics.dispose()
        }
    }
}
