@file:Suppress("unused")

package io.github.nayasis.kotlin.swing.window

import io.github.nayasis.kotlin.basica.etc.Platforms
import io.github.nayasis.kotlin.basica.etc.error
import io.github.nayasis.kotlin.swing.misc.runUiAndWait
import io.github.nayasis.kotlin.swing.property.PositionProperty
import io.github.nayasis.kotlin.swing.window.FilePickerMode.Multi
import io.github.nayasis.kotlin.swing.window.FilePickerMode.Save
import io.github.nayasis.kotlin.swing.window.FilePickerMode.Single
import io.github.nayasis.kotlin.swing.window.progress.MultiProgressDialog
import io.github.nayasis.kotlin.swing.window.progress.ProgressDialog
import io.github.oshai.kotlinlogging.KotlinLogging
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Window
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter

private val logger = KotlinLogging.logger {}

/**
 * message, confirm, input, progress and file dialogs.
 *
 * every function can be called from any thread. dialogs are modal to the focused window.
 */
class Dialog { companion object {

    @Suppress("MemberVisibilityCanBePrivate")
    var defaultTitle: String? = null

    fun alert(message: String?, content: String? = null, expanded: Boolean = true, point: PositionProperty? = null) {
        showOption(JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, "OptionPane.messageDialogTitle", message, content, expanded, point)
    }

    fun confirm(message: String?, content: String? = null, expanded: Boolean = true, point: PositionProperty? = null): Boolean {
        val result = showOption(JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, "OptionPane.titleText", message, content, expanded, point)
        return result == JOptionPane.OK_OPTION
    }

    fun error(message: String?, exception: Throwable? = null, expanded: Boolean = false, point: PositionProperty? = null) {
        if( exception != null ) logger.error(exception)
        showOption(
            JOptionPane.ERROR_MESSAGE, JOptionPane.DEFAULT_OPTION, "OptionPane.messageDialogTitle",
            message ?: exception?.message, exception?.stackTraceToString(), expanded, point,
        )
    }

    fun error(exception: Throwable?, expanded: Boolean = false, point: PositionProperty? = null) {
        error(exception?.message, exception, expanded, point)
    }

    /**
     * ask for a text.
     *
     * @return the input, or null when canceled
     */
    fun prompt(message: String?, defaultValue: String? = null, point: PositionProperty? = null): String? = runUiAndWait {
        val pane = JOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION).apply {
            wantsInput = true
            initialSelectionValue = defaultValue ?: ""
        }
        val result = showPane(pane, "OptionPane.inputDialogTitle", point)
        if( result == JOptionPane.OK_OPTION ) pane.inputValue as? String else null
    }

    fun progress(title: String? = null, async: Boolean = true, point: PositionProperty? = null, task: ((dialog: ProgressDialog) -> Unit)? = null): ProgressDialog {
        return ProgressDialog(title).apply{
            initOwner(Windows.focusedWindow)
            if(async) runAsync(point, task) else runSync(point, task)
        }
    }

    fun progress(title: String? = null, async: Boolean = true, headerButton: Component, point: PositionProperty? = null, task: ((dialog: ProgressDialog) -> Unit)? = null): ProgressDialog {
        return ProgressDialog(title).apply{
            initOwner(Windows.focusedWindow)
            addHeaderRight(headerButton)
            if(async) runAsync(point, task) else runSync(point, task)
        }
    }

    fun progressMulti(progressCount: Int, title: String? = null, async: Boolean = true, point: PositionProperty? = null, task: ((dialog: MultiProgressDialog) -> Unit)? = null): MultiProgressDialog {
        return MultiProgressDialog(progressCount,title).apply{
            initOwner(Windows.focusedWindow)
            if(async) runAsync(point, task) else runSync(point, task)
        }
    }

    fun progressMulti(progressCount: Int, title: String? = null, async: Boolean = true, headerButton: Component, point: PositionProperty? = null, task: ((dialog: MultiProgressDialog) -> Unit)? = null): MultiProgressDialog {
        return MultiProgressDialog(progressCount,title).apply{
            initOwner(Windows.focusedWindow)
            addHeaderRight(headerButton)
            if(async) runAsync(point, task) else runSync(point, task)
        }
    }

    /**
     * @param extension comma or semicolon separated extensions to filter (ex. `png,jpg`)
     * @param description description of the extension filter
     */
    fun filePicker(
        title: String? = null,
        extension: String? = null,
        description: String? = null,
        initialDirectory: Path? = null,
        initialFileName: String? = null,
        mode: FilePickerMode = Single,
        owner: Window? = null,
        option: JFileChooser.() -> Unit = {}
    ): List<Path> = runUiAndWait {
        val chooser = JFileChooser().apply {
            title?.let { dialogTitle = it }
            extension?.let {
                val extensions = it.split(',', ';').map { e -> e.trim().removePrefix("*").removePrefix(".") }.filter { e -> e.isNotEmpty() }
                if( extensions.isNotEmpty() ) {
                    fileFilter = FileNameExtensionFilter(description ?: extensions.joinToString(", "), *extensions.toTypedArray())
                }
            }
            initialDirectory?.let { dir ->
                currentDirectory = if( dir.isReadableDirectory() ) dir.toFile() else dirDesktop()
            }
            initialFileName?.let { name -> selectedFile = File(currentDirectory, name) }
            isMultiSelectionEnabled = mode == Multi
            fileSelectionMode = JFileChooser.FILES_ONLY
        }
        option(chooser)
        val parent = owner ?: Windows.focusedWindow
        val answer = when(mode) {
            Single, Multi -> chooser.showOpenDialog(parent)
            Save          -> chooser.showSaveDialog(parent)
        }
        val approved = answer == JFileChooser.APPROVE_OPTION
        when {
            ! approved -> emptyList()
            mode == Multi -> chooser.selectedFiles.map { it.toPath() }
            else -> listOfNotNull(chooser.selectedFile?.toPath())
        }
    }

    fun dirPicker(
        title: String? = null,
        initialDirectory: Path? = null,
        owner: Window? = null,
        option: JFileChooser.() -> Unit = {}
    ): Path? = runUiAndWait {
        val chooser = JFileChooser().apply {
            title?.let { dialogTitle = it }
            initialDirectory?.takeIf { it.isReadableDirectory() }?.let { currentDirectory = it.toFile() }
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        }
        option(chooser)
        val approved = chooser.showOpenDialog(owner ?: Windows.focusedWindow) == JFileChooser.APPROVE_OPTION
        if( approved ) chooser.selectedFile?.toPath() else null
    }

    private fun showOption(
        messageType: Int,
        optionType: Int,
        titleKey: String,
        message: String?,
        content: String?,
        expanded: Boolean,
        point: PositionProperty?,
    ): Int = runUiAndWait {
        val pane = JOptionPane(null, messageType, optionType)
        pane.message = if( content.isNullOrEmpty() ) message ?: "" else arrayOf(message ?: "", ExpandablePanel(content, expanded))
        showPane(pane, titleKey, point)
    }

    /** show the pane as a modal dialog and return the selected option. */
    private fun showPane(pane: JOptionPane, titleKey: String, point: PositionProperty?): Int {
        val title  = defaultTitle ?: UIManager.getString(titleKey)
        val dialog = pane.createDialog(Windows.focusedWindow, title)
        dialog.isAlwaysOnTop = true
        dialog.loadDefaultIcon()
        point?.bind(dialog)
        dialog.isVisible = true
        dialog.dispose()
        return when(val value = pane.value) {
            is Int -> value
            else   -> JOptionPane.CLOSED_OPTION
        }
    }

    private fun Path.isReadableDirectory(): Boolean = Files.isDirectory(this) && Files.isReadable(this)

    private fun dirDesktop(): File {
        val home = File(System.getProperty("user.home"))
        return when {
            Platforms.isWindows -> File(home, "Desktop").takeIf { it.isDirectory } ?: home
            else -> home
        }
    }

}}

/**
 * detail text that can be folded, such as a stack trace.
 */
private class ExpandablePanel(content: String, expanded: Boolean): JPanel(BorderLayout()) {

    private val area = JTextArea(content).apply {
        isEditable    = false
        caretPosition = 0
    }

    private val scroll = JScrollPane(area).apply {
        preferredSize = Dimension(480, 200)
        isVisible     = expanded
    }

    private val toggle = JButton().apply {
        isBorderPainted     = false
        isContentAreaFilled = false
        isFocusPainted      = false
        horizontalAlignment = SwingConstants.LEFT
        border = BorderFactory.createEmptyBorder(4, 0, 4, 0)
    }

    init {
        add(toggle, BorderLayout.NORTH)
        add(scroll, BorderLayout.CENTER)
        toggle.addActionListener {
            scroll.isVisible = !scroll.isVisible
            updateToggle()
            SwingUtilities.getWindowAncestor(this)?.pack()
        }
        updateToggle()
    }

    private fun updateToggle() {
        toggle.icon = UIManager.getIcon(if( scroll.isVisible ) "Tree.expandedIcon" else "Tree.collapsedIcon")
        toggle.text = if( UIManager.getIcon("Tree.expandedIcon") == null ) (if( scroll.isVisible ) "v Detail" else "> Detail") else "Detail"
    }

}
