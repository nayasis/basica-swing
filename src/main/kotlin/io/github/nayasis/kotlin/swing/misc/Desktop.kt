package io.github.nayasis.kotlin.swing.misc

import io.github.nayasis.kotlin.basica.etc.Platforms
import io.github.nayasis.kotlin.basica.exec.Command
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI
import java.awt.Desktop as AwtDesktop

class Desktop { companion object {

    val toolkit: Toolkit
        get() = Toolkit.getDefaultToolkit()

    val clipboard: Clipboard
        get() = toolkit.systemClipboard

    val graphics: GraphicsEnvironment
        get() = GraphicsEnvironment.getLocalGraphicsEnvironment()

    val allFonts: List<Font>
        get() = graphics.allFonts.toList()

    private fun support(action: AwtDesktop.Action): Boolean {
        return when {
            !AwtDesktop.isDesktopSupported() -> false
            else -> AwtDesktop.getDesktop().isSupported(action)
        }
    }

    private fun execute(command: String, parameter: String? = null): Boolean {
        return try {
            Command(command).append(parameter).run()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun openOsSpecific(what: String?) {
        if (Platforms.isLinux || Platforms.isUnix || Platforms.isSolaris) {
            if (execute("kde-open",   what)) return
            if (execute("gnome-open", what)) return
            if (execute("xdg-open",   what)) return
            throw IllegalArgumentException("fail to open(kde, gnome, xdg).")
        } else if (Platforms.isMac) {
            execute("open", what)
        } else if (Platforms.isWindows) {
            execute("explorer", what)
        }
    }

    fun browse(uri: String) {
        if (support(AwtDesktop.Action.BROWSE)) {
            AwtDesktop.getDesktop().browse(URI(uri))
        } else {
            openOsSpecific(uri)
        }
    }

    fun open(file: File) {
        if (support(AwtDesktop.Action.OPEN)) {
            AwtDesktop.getDesktop().open(file)
        } else {
            openOsSpecific(file.path)
        }
    }

    fun edit(file: File) {
        if (support(AwtDesktop.Action.EDIT)) {
            AwtDesktop.getDesktop().edit(file)
        } else {
            openOsSpecific(file.path)
        }
    }

}}

fun Clipboard.set(text: String?) {
    text?.let { setContents(StringSelection(it), null) }
}

fun Clipboard.set(image: Image?) {
    image?.let { setContents(ImageSelection(it), null) }
}

fun Clipboard.getText(): String {
    return if(isDataFlavorAvailable(DataFlavor.stringFlavor)) getData(DataFlavor.stringFlavor) as String else ""
}
