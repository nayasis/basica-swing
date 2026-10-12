package io.github.nayasis.kotlin.swing.window

import io.github.nayasis.kotlin.swing.common.installTheme
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.UIManager

/** manual demo. asserts nothing. */
object BorderlessWindowDemo {
    @JvmStatic fun main(args: Array<String>) {
        installTheme()
        SwingUtilities.invokeLater {
            val menuBar = JMenuBar().apply {
                add(JMenu("File").apply { add(JMenuItem("Exit").apply { addActionListener { System.exit(0) } }) })
                add(JMenu("Help"))
            }
            JFrame("Borderless window").apply {
                setBorderless()
                defaultCloseOperation = JFrame.EXIT_ON_CLOSE
                rootPane.border = BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor") ?: Color.GRAY)
                contentPane.add(WindowHeader(menuBar), BorderLayout.NORTH)
                contentPane.add(JLabel("drag the header, double click it, or drag the edges", SwingConstants.CENTER), BorderLayout.CENTER)
                setSize(560, 320)
                setLocationRelativeTo(null)
                watchMaximized()
                isVisible = true
            }
        }
    }
}
