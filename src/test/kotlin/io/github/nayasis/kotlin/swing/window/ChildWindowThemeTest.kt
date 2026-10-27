package io.github.nayasis.kotlin.swing.window

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.FlatLightLaf
import io.github.nayasis.kotlin.swing.common.assumeDisplay
import io.github.nayasis.kotlin.swing.misc.runUiAndWait
import io.github.nayasis.kotlin.swing.window.progress.ProgressDialogWindow
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.Insets
import java.awt.GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT
import java.awt.event.ComponentEvent
import java.awt.image.BufferedImage
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.SwingUtilities
import javax.swing.UIManager
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class ChildWindowThemeTest {

    @Test
    fun `open progress dialogs and popovers follow the current look and feel`() {
        assumeDisplay()
        runUiAndWait<Unit> {
            val previous = UIManager.getLookAndFeel()
            FlatLightLaf.setup()
            val anchor = JButton("queue")
            val owner = JFrame().apply { add(anchor); setSize(400, 200); isVisible = true }
            val progress = ProgressDialogWindow(1, owner)
            val content = JLabel("pending")
            val popOver = PopOver(content)
            try {
                progress.isVisible = true
                popOver.show(anchor)
                val popup = SwingUtilities.getWindowAncestor(content)
                val holder = content.parent as JComponent
                val supportsShaping = progress.graphicsConfiguration.device.isWindowTranslucencySupported(PERPIXEL_TRANSPARENT)
                if (supportsShaping) {
                    assertNotNull(progress.shape)
                    assertFalse(progress.shape.contains(0.0, 0.0))
                    assertNotNull(popup.shape)
                }
                assertEquals(Insets(1, 1, 1, 1), (progress.contentPane as JComponent).border.getBorderInsets(progress.contentPane))
                FlatDarkLaf.setup()
                FlatLaf.updateUI()
                val child = JDialog(owner)
                try {
                    val label = JLabel("inherits the application's theme")
                    child.add(label)
                    child.pack()
                    assertEquals(UIManager.getColor("Label.foreground"), label.foreground)
                    assertEquals(owner.contentPane.background, child.contentPane.background)
                } finally {
                    child.dispose()
                }
                val color = UIManager.getColor("Component.borderColor")
                    ?: UIManager.getColor("Separator.foreground") ?: Color.GRAY
                assertEquals(color.rgb, borderColor(progress.contentPane as JComponent))
                assertEquals(color.rgb, borderColor(holder))
                assertEquals(UIManager.getColor("Panel.background"), progress.contentPane.background)
                assertEquals(UIManager.getFont("Label.font").size, progress.titleHeader.font.size)
                progress.setSize(420, 180)
                progress.dispatchEvent(ComponentEvent(progress, ComponentEvent.COMPONENT_RESIZED))
                if (supportsShaping) {
                    assertEquals(420.0, progress.shape.bounds2D.width)
                    assertEquals(180.0, progress.shape.bounds2D.height)
                }
                FlatLightLaf.setup()
                FlatLaf.updateUI()
                assertEquals(UIManager.getColor("Panel.background"), progress.contentPane.background)
            } finally {
                popOver.hide()
                progress.dispose()
                owner.dispose()
                UIManager.setLookAndFeel(previous)
                FlatLaf.updateUI()
            }
        }
    }

    private fun borderColor(component: JComponent): Int {
        val image = BufferedImage(component.width, component.height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            component.border.paintBorder(component, graphics, 0, 0, image.width, image.height)
        } finally {
            graphics.dispose()
        }
        return image.getRGB(image.width / 2, 0)
    }
}
