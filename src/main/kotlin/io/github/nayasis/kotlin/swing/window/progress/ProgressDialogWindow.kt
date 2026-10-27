@file:Suppress("MemberVisibilityCanBePrivate")

package io.github.nayasis.kotlin.swing.window.progress

import io.github.nayasis.kotlin.swing.misc.runLater
import io.github.nayasis.kotlin.swing.window.installWindowMover
import io.github.nayasis.kotlin.swing.window.installRoundedCorners
import io.github.nayasis.kotlin.swing.window.loadDefaultIcon
import io.github.nayasis.kotlin.swing.window.WindowBorder
import io.github.nayasis.kotlin.swing.window.windowBorderColor
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.Window
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.SwingConstants
import javax.swing.UIManager
import javax.swing.plaf.FontUIResource

private const val PROGRESS_SCALE = 1000

/**
 * undecorated dialog showing a title and [progressCount] pairs of message and progress bar.
 *
 * update methods can be called from any thread.
 */
class ProgressDialogWindow(progressCount: Int, owner: Window? = null): JDialog(owner) {

    val titleHeader = object: JLabel(" ") {
        override fun updateUI() {
            super.updateUI()
            font = FontUIResource(UIManager.getFont("Label.font").deriveFont(Font.BOLD))
        }
    }
    val progressBars = ArrayList<JProgressBar>()
    val messages     = ArrayList<JLabel>()
    val subMessages  = ArrayList<JLabel>()
    val headerRight  = JPanel().apply {
        isOpaque = false
        layout   = java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 2, 0)
    }

    // progress as the caller sees it. bars are updated later on the event dispatch thread.
    private val progressValues = DoubleArray(progressCount)

    init {

        repeat(progressCount) {
            messages.add(JLabel(" "))
            subMessages.add(JLabel(" ").apply { horizontalAlignment = SwingConstants.RIGHT })
            progressBars.add(JProgressBar(0, PROGRESS_SCALE))
        }

        var dragging = false
        val root = JPanel(GridBagLayout())
        root.border = WindowBorder { dragging }

        var row = 0
        fun add(component: JComponent, bottom: Int = 0) {
            root.add(component, GridBagConstraints().apply {
                gridx   = 0
                gridy   = row++
                weightx = 1.0
                fill    = GridBagConstraints.HORIZONTAL
                insets  = Insets(0, 0, bottom, 0)
            })
        }

        add(object: JPanel(BorderLayout()) {
            override fun updateUI() {
                super.updateUI()
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, windowBorderColor()),
                    BorderFactory.createEmptyBorder(7, 5, 7, 5),
                )
            }
        }.apply {
            isOpaque = false
            add(titleHeader, BorderLayout.CENTER)
            add(headerRight, BorderLayout.EAST)
        }, bottom = 7)

        for( i in 0 until progressCount ) {
            add(JPanel(BorderLayout()).apply {
                isOpaque = false
                border   = BorderFactory.createEmptyBorder(0, 5, 3, 5)
                add(messages[i], BorderLayout.CENTER)
                add(subMessages[i], BorderLayout.EAST)
            })
            add(progressBars[i].also {
                it.border = BorderFactory.createEmptyBorder(0, 5, 0, 5)
            }, bottom = 7)
        }

        root.preferredSize = Dimension(300, root.preferredSize.height)
        contentPane = root

        isUndecorated = true
        isAlwaysOnTop = true
        loadDefaultIcon()
        pack()
        installRoundedCorners()
        setLocationRelativeTo(owner)

        root.installWindowMover(styleOnDrag = { active ->
            dragging = active
            root.repaint()
        })
    }

    fun updateProgress(index: Int, percent: Number) {
        val value = percent.toDouble()
        progressValues[index] = value
        runLater { progressBars[index].setProgress(value) }
    }

    fun updateProgress(index: Int, done: Number, max: Number) {
        val d = done.toDouble()
        val m = max.toDouble()
        updateProgress(index, when {
            d.isInfinite() || d.isNaN() -> 0.0
            m.isInfinite() || m.isNaN() -> 0.0
            else -> d / m
        })
    }

    fun getProgress(index: Int): Double = progressValues[index]

    fun updateMessage(index: Int, message: String?) {
        runLater { messages[index].text = message.orBlank() }
    }

    fun updateSubMessage(index: Int, message: String?) {
        runLater { subMessages[index].text = message.orBlank() }
    }

    fun updateSubMessageAsProgress(index: Int, format: String = "%.1f%%") {
        updateSubMessage(index, format.format(progressValues[index] * 100))
    }

    fun updateTitle(title: String?) {
        runLater { titleHeader.text = title.orBlank() }
    }

    fun addHeaderRight(component: Component) {
        runLater {
            headerRight.add(component)
            headerRight.revalidate()
        }
    }

    // empty text collapses the label height, which would resize the layout
    private fun String?.orBlank(): String = if(isNullOrEmpty()) " " else this

    // negative progress means indeterminate
    private fun JProgressBar.setProgress(value: Double) {
        isIndeterminate = value < 0
        if( !isIndeterminate ) this.value = (value.coerceIn(0.0, 1.0) * PROGRESS_SCALE).toInt()
    }

}
