@file:Suppress("unused")

package io.github.nayasis.kotlin.swing.misc

import io.github.oshai.kotlinlogging.KotlinLogging
import java.awt.Image
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.event.InputEvent
import java.awt.image.BufferedImage
import java.io.File
import java.net.URI
import javax.swing.JComponent
import javax.swing.TransferHandler

private val logger = KotlinLogging.logger {}

private val URI_LIST_FLAVOR = DataFlavor("text/uri-list;class=java.lang.String")
private val HTML_FLAVORS    = listOf(DataFlavor.fragmentHtmlFlavor, DataFlavor.allHtmlFlavor, DataFlavor.selectionHtmlFlavor)
private val HTML_IMG_TAG    = "(?is)^<img\\W".toRegex()

/** [Transferable] holding a single image, for putting images on the clipboard or into a drag. */
class ImageSelection(private val image: Image): Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)
    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == DataFlavor.imageFlavor
    override fun getTransferData(flavor: DataFlavor): Any {
        if(!isDataFlavorSupported(flavor)) throw UnsupportedFlavorException(flavor)
        return image
    }
}

fun Transferable?.isAcceptableImage(): Boolean {
    return when {
        this == null -> false
        hasHtmlImgTag() -> true
        isDataFlavorSupported(DataFlavor.imageFlavor) -> true
        isDataFlavorSupported(URI_LIST_FLAVOR) -> true
        hasRegularFile() -> true
        isDataFlavorSupported(DataFlavor.stringFlavor) -> true
        else -> false
    }
}

fun Transferable?.toBufferedImage(): BufferedImage? {
    if(this == null) return null
    return runCatching { when {
        hasHtmlImgTag() -> getSrcFromImageTag(html()!!)?.toBufferedImage()
        hasRegularFile() -> files().firstOrNull()?.toBufferedImage()
        isDataFlavorSupported(URI_LIST_FLAVOR) -> uri()?.let { if(it.startsWith("file:")) File(URI(it)).toBufferedImage() else it.toBufferedImage() }
        isDataFlavorSupported(DataFlavor.stringFlavor) -> (getTransferData(DataFlavor.stringFlavor) as? String)?.toBufferedImage()
        isDataFlavorSupported(DataFlavor.imageFlavor) -> readImage()
        else -> null
    }}.onFailure { logger.debug { "fail to read image from transferable: ${it.message}" } }.getOrNull()
}

fun Clipboard?.toBufferedImage(): BufferedImage? {
    val contents = this?.getContents(null) ?: return null
    return if(contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
        runCatching { contents.readImage() }.getOrNull()
    } else {
        contents.toBufferedImage()
    }
}

private fun Transferable.readImage(): BufferedImage? =
    (getTransferData(DataFlavor.imageFlavor) as? Image)?.toBufferedImage()

private fun Transferable.html(): String? =
    HTML_FLAVORS.firstOrNull { isDataFlavorSupported(it) }?.let { getTransferData(it) as? String }

@Suppress("UNCHECKED_CAST")
private fun Transferable.files(): List<File> =
    if(isDataFlavorSupported(DataFlavor.javaFileListFlavor)) getTransferData(DataFlavor.javaFileListFlavor) as List<File> else emptyList()

private fun Transferable.uri(): String? =
    (getTransferData(URI_LIST_FLAVOR) as? String)
        ?.lineSequence()
        ?.map { it.trim() }
        ?.firstOrNull { it.isNotEmpty() && !it.startsWith("#") }

private fun Transferable.hasRegularFile(): Boolean =
    runCatching { files().firstOrNull() != null }.getOrDefault(false)

private fun Transferable.hasHtmlImgTag(): Boolean =
    runCatching { html()?.let { HTML_IMG_TAG.containsMatchIn(it) } ?: false }.getOrDefault(false)

private fun getSrcFromImageTag(imgTag: String): String? {

    val regex = "(?is)^<img\\W.*?(src|srcset)=[\"|'](.*?)[\"|'].*?>".toRegex()
    val info = imgTag.replaceFirst(regex, "$1 :: $2").split(" :: ")
    val type = info[0].lowercase()
    val url  = info[1].replace("(?is)&amp;".toRegex(), "&")

    logger.trace { "html : ${imgTag}\ntype : ${type}\nurl  : ${url}" }

    return when(type) {
        "src" -> url
        "srcset" -> {
            val urls = HashMap<String,String>()
            var tmpKey = 0
            for (line in url.split(",")) {
                val values = line.split(" ")
                if (values.size == 2) {
                    urls[values[1]] = values[0]
                } else if (values.size == 1) {
                    urls[tmpKey++.toString()] = values[0]
                }
            }
            if(urls.isNotEmpty()) urls[ ArrayList(urls.keys).apply{reverse()}[0] ] else null
        }
        else -> null
    }

}

/**
 * accept files dropped on this component.
 *
 * the component's own transfer handler keeps serving everything else, so a text field still copies,
 * pastes and drags text.
 *
 * @param onHover called with true while files are dragged over the component, false when they leave or drop
 * @param onDrop called with the dropped files and the drop details, e.g. the row of a [javax.swing.JTable] drop
 * @return the installed handler
 */
fun JComponent.onFileDrop(
    onHover: ((Boolean) -> Unit)? = null,
    onDrop: (files: List<File>, support: TransferHandler.TransferSupport) -> Unit,
): TransferHandler {
    val handler = FileDropHandler(transferHandler, onHover, onDrop)
    transferHandler = handler
    if (onHover != null) {
        dropTarget?.addDropTargetListener(object: DropTargetAdapter() {
            override fun dragExit(dte: DropTargetEvent) = onHover(false)
            override fun drop(dtde: DropTargetDropEvent) = onHover(false)
        })
    }
    return handler
}

private class FileDropHandler(
    private val delegate: TransferHandler?,
    private val onHover: ((Boolean) -> Unit)?,
    private val onDrop: (List<File>, TransferSupport) -> Unit,
): TransferHandler() {

    override fun canImport(support: TransferSupport): Boolean {
        if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            if (support.isDrop) {
                support.dropAction = COPY
                onHover?.invoke(true)
            }
            return true
        }
        return delegate?.canImport(support) ?: false
    }

    @Suppress("UNCHECKED_CAST")
    override fun importData(support: TransferSupport): Boolean {
        if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            return delegate?.importData(support) ?: false
        }
        val files = runCatching { support.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File> }
            .onFailure { logger.debug { "fail to read dropped files: ${it.message}" } }
            .getOrNull() ?: return false
        onHover?.invoke(false)
        if (files.isEmpty()) return false
        onDrop(files, support)
        return true
    }

    override fun getSourceActions(c: JComponent): Int = delegate?.getSourceActions(c) ?: NONE

    override fun exportAsDrag(comp: JComponent, e: InputEvent, action: Int) {
        delegate?.exportAsDrag(comp, e, action)
    }

    override fun exportToClipboard(comp: JComponent, clip: Clipboard, action: Int) {
        delegate?.exportToClipboard(comp, clip, action)
    }

}
