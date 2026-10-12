@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.github.nayasis.kotlin.swing.misc

import io.github.nayasis.kotlin.basica.core.io.isFile
import io.github.nayasis.kotlin.basica.core.io.makeDir
import io.github.nayasis.kotlin.basica.core.string.*
import java.util.Base64
import io.github.nayasis.kotlin.basica.core.url.toFile
import io.github.oshai.kotlinlogging.KotlinLogging
import net.sf.image4j.codec.ico.ICODecoder
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Image
import java.awt.RenderingHints.*
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_CUSTOM
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.io.*
import java.lang.Math.toRadians
import java.net.URL
import java.nio.file.Path
import javax.imageio.ImageIO
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.filechooser.FileSystemView
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

private val logger = KotlinLogging.logger {}


private var webBrowser: WebBrowser? = null

fun BufferedImage?.isValid(): Boolean {
    return this != null && this.width > 0 && this.height > 0
}

fun BufferedImage?.isEmpty(): Boolean {
    return this == null || (this.width <= 1 && this.height <= 1)
}

fun ImageIcon?.isEmpty(): Boolean {
    return this == null || (this.iconWidth <= 1 && this.iconHeight <= 1)
}

fun BufferedImage.cropTop(pixel: Int): BufferedImage {
    return if( pixel >= height ) this else getSubimage(0, pixel, width, height - pixel).copy()
}

fun BufferedImage.cropBottom(pixel: Int): BufferedImage {
    return if( pixel >= height ) this else getSubimage(0, 0, width, height - pixel).copy()
}

fun BufferedImage.cropLeft(pixel: Int): BufferedImage {
    return if( pixel >= width ) this else getSubimage(pixel, 0, width - pixel, height).copy()
}

fun BufferedImage.cropRight(pixel: Int): BufferedImage {
    return if( pixel >= width ) this else getSubimage(0, 0, width - pixel, height).copy()
}

/**
 * convert to buffered image
 *
 * @param timeout timeout to wait response
 * @return BufferedImage
 */
fun URL.toBufferedImage(
    timeout: Double = 30_000.0,
): BufferedImage {
    return when {
        this.protocol == "file" -> this.toFile().toBufferedImage()
        else -> {
            if (webBrowser == null) {
                webBrowser = WebBrowser(timeout = timeout.toInt())
                Runtime.getRuntime().addShutdownHook(Thread {
                    webBrowser?.close()
                    webBrowser = null
                })
            }
            webBrowser!!.downloadContent(this).toBufferedImage()
        }
    }
}

/**
 * convert to buffered image
 *
 * accepts http(s) URL, `data:...;base64,` URI and file path.
 *
 * @param timeout timeout to wait response
 * @return BufferedImage
 */
fun String.toBufferedImage(
    timeout: Double = 30_000.0,
): BufferedImage {
    return runCatching { this.let { url -> when {
        url.find("^http(s?)://".toRegex()) -> url.toUrl().toBufferedImage(timeout)
        url.find("^data:.*?;base64,".toRegex()) -> {
            val encoded = url.replaceFirst("^data:.*?;base64,".toRegex(), "")
            Base64.getMimeDecoder().decode(encoded).toBufferedImage()
        }
        url.toFile().exists() -> url.toFile().toBufferedImage()
        else -> throw IOException("not a valid URL or file path.")
    }}}.getOrElse {
        throw IOException("can not be converted to image. ($this)", it)
    }
}

fun File.toBufferedImage(): BufferedImage {
    if( ! this.isFile ) throw IOException("only file could be converted to image. ($this)")
    return ImageIO.read(this) ?: throw IOException("can not convert file to BufferedImage. ($this)")
}

fun Path.toBufferedImage(): BufferedImage {
    if( ! this.isFile() ) throw IOException("only file could be converted to image. ($this)")
    return toFile().toBufferedImage()
}

fun ByteArray.toBufferedImage(): BufferedImage {
    return this.let {
        if(it.isEmpty()) {
            BufferedImage(1,1, TYPE_INT_ARGB)
        } else {
            ByteArrayInputStream(it).use { bis ->
                ImageIO.read(bis)
            } ?: throw IOException("can not convert byte array to BufferedImage. (size=${it.size})")
        }
    }
}

/**
 * convert any AWT image to buffered image. a [BufferedImage] is returned as it is.
 */
fun Image.toBufferedImage(): BufferedImage {
    if(this is BufferedImage) return this
    val icon = ImageIcon(this)
    val width  = icon.iconWidth.coerceAtLeast(1)
    val height = icon.iconHeight.coerceAtLeast(1)
    return BufferedImage(width, height, TYPE_INT_ARGB).also {
        it.createGraphics().run {
            drawImage(this@toBufferedImage, 0, 0, null)
            dispose()
        }
    }
}

fun ImageIcon.toBufferedImage(): BufferedImage {
    return if(this.isEmpty()) BufferedImage(1,1,TYPE_INT_ARGB) else paintToImage(this)
}

fun ImageIcon.toBufferedImageOrNull(): BufferedImage? {
    return runCatching { toBufferedImage() }.getOrNull()
}

fun BufferedImage.toIcon(): ImageIcon = ImageIcon(this)

private fun paintToImage(icon: Icon): BufferedImage {
    if(icon.iconWidth <= 0 || icon.iconHeight <= 0) return BufferedImage(1,1,TYPE_INT_ARGB)
    return BufferedImage(icon.iconWidth, icon.iconHeight, TYPE_INT_ARGB).also {
        it.createGraphics().apply {
            setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC)
            setRenderingHint(KEY_ALPHA_INTERPOLATION, VALUE_ALPHA_INTERPOLATION_QUALITY)
            setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)
            setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY)
            setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_GASP)
        }.let { graphics ->
            icon.paintIcon(null, graphics,0,0)
            graphics.dispose()
        }
    }
}

fun BufferedImage.toBinary(format: String = "jpg"): ByteArray {
    return ByteArrayOutputStream().use { bos ->
        ImageIO.write(this,format,bos)
        bos.toByteArray()
    }
}

/**
 * drop the alpha channel.
 *
 * @param background color the transparent parts are blended onto. without it, transparent pixels keep only
 *                   their color channels, which usually turns them black.
 */
fun BufferedImage.removeAlpha(background: Color? = null): BufferedImage {
    return this.let { src ->
        BufferedImage(src.width,src.height,BufferedImage.TYPE_INT_RGB).apply {
            createGraphics().run {
                if(background == null) {
                    composite = AlphaComposite.Src
                } else {
                    color = background
                    fillRect(0, 0, src.width, src.height)
                }
                drawImage(src,0,0,null)
                dispose()
            }
        }
    }
}

fun BufferedImage.toJpgBinary(background: Color? = null): ByteArray {
    return this.removeAlpha(background).toBinary("jpg")
}

fun File.toJpgBinary(): ByteArray {
    return this.toBufferedImage().toJpgBinary()
}

fun Path.toJpgBinary(): ByteArray {
    return this.toBufferedImage().toJpgBinary()
}

fun File.toIconImages(): List<BufferedImage> {
    if( ! isFile) return emptyList()
    return try {
        this.inputStream().toIconImages()
    } catch (e: Exception) {
        FileSystemView.getFileSystemView().getSystemIcon(this)?.let { listOf(paintToImage(it)) } ?: emptyList()
    }
}

fun InputStream.toIconImages(): List<BufferedImage> {
    return this.use {
        ICODecoder.read(it).toList()
    }
}

fun String.toBase64Image(): String {
    val bytes = this.toBufferedImage().toJpgBinary()
    return "data:image/jpeg;base64,${Base64.getEncoder().encodeToString(bytes)}"
}

fun BufferedImage.copy(): BufferedImage {
    return this.let { src ->
        BufferedImage(src.width,src.height,getType(src)).apply {
            graphics.run {
                drawImage(src,0,0,null)
                dispose()
            }
        }
    }
}

fun BufferedImage.resize(width: Int, height: Int): BufferedImage {
    return this.let { image ->
        BufferedImage(width, height, getType(image)).apply {
            createGraphics().run {
                drawImage(image,0,0,width,height,null)
                dispose()
            }
        }
    }
}

fun BufferedImage.resize(maxPixel: Int): BufferedImage {
    return this.let { image ->
        var w = image.width.coerceAtLeast(1).toDouble()
        var h = image.height.coerceAtLeast(1).toDouble()
        when {
            w < maxPixel && h < maxPixel -> image
            w > h -> {
                h = h * maxPixel / width
                w = maxPixel.toDouble()
                image.resize(w.toInt(),h.toInt())
            }
            else -> {
                w = w * maxPixel / height
                h = maxPixel.toDouble()
                image.resize(w.toInt(),h.toInt())
            }
        }
    }
}

fun BufferedImage.rotate(angle: Double): BufferedImage {
    return this.let { image ->

        val radian = toRadians(angle)
        val sinAbs = abs(sin(radian))
        val cosAbs = abs(cos(radian))

        val srcWidth  = image.width
        val srcHeight = image.height
        val trgWidth  = floor(srcWidth * cosAbs + srcHeight * sinAbs).toInt()
        val newHeight = floor(srcHeight * cosAbs + srcWidth * sinAbs).toInt()

        logger.trace { """
            >> rotate image
               - src : $srcWidth x $srcHeight
               - trg : $trgWidth x $newHeight
        """.trimIndent() }

        BufferedImage(trgWidth, newHeight, getType(image)).apply {
            createGraphics().run {
                translate((trgWidth - srcWidth) / 2.0, (newHeight - srcHeight) / 2.0)
                rotate(radian, srcWidth.toDouble() / 2, srcHeight.toDouble() / 2)
                drawRenderedImage(image, null)
                dispose()
            }
        }
    }
}

fun BufferedImage.write(path: String) {
    this.write(path.toPath())
}

fun BufferedImage.write(path: Path) {
    this.write(path.toFile())
}

fun BufferedImage.write(file: File) {
    this.let { image ->
        val extension   = file.extension
        val outputImage = if(extension in setOf("jpg", "jpeg")) image.removeAlpha() else image
        file.toPath().toAbsolutePath().parent?.makeDir()
        val written = ImageIO.write(outputImage, extension, file)
        if(!written) {
            throw IOException("can not write image. unsupported format or writer not found. (file=$file, format=$extension)")
        }
    }
}

private fun getType(image: BufferedImage?): Int {
    return when(image?.type) {
        null -> TYPE_CUSTOM
        0    -> TYPE_INT_ARGB
        else -> image.type
    }
}

/**
 * the icon the operating system shows for this file, e.g. the icon embedded in an executable.
 *
 * @param size requested edge length in pixels. the system may return a smaller icon.
 * @return the icon, or null when the system has none
 */
fun File.toSystemIconImage(size: Int = 32): BufferedImage? {
    if( ! exists() ) return null
    return runCatching {
        FileSystemView.getFileSystemView().getSystemIcon(this, size, size)?.let { paintToImage(it) }
    }.onFailure { logger.debug { "fail to read system icon of $this: ${it.message}" } }.getOrNull()
}
