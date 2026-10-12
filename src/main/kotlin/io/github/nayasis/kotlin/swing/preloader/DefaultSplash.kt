package io.github.nayasis.kotlin.swing.preloader

import io.github.nayasis.kotlin.swing.window.installWindowMover
import io.github.nayasis.kotlin.swing.window.loadDefaultIcon
import io.github.oshai.kotlinlogging.KotlinLogging
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JWindow
import javax.swing.SwingConstants

private val logger = KotlinLogging.logger {}

private const val PROGRESS_SCALE = 1000

/**
 * Default splash view for preloader
 *
 * ```
 * class Splash: DefaultSplash(500,300,"/view/splash/splash.jpg")
 *
 * fun main(args: Array<String>) {
 *   DefaultPreloader.set(Splash::class)
 * }
 * ```
 *
 * @param imagePath classpath resource or file path of the background image
 */
abstract class DefaultSplash(
    private val width: Int,
    private val height: Int,
    imagePath: String? = null,
    val showProgressBar: Boolean = true,
): DefaultPreloader() {

    private val backgroundImage = imagePath?.let { readImage(it) }

    private val progressBar = JProgressBar(0, PROGRESS_SCALE).apply {
        preferredSize = Dimension(0, 6)
        isVisible     = false
    }

    private val label = JLabel(" ").apply {
        foreground          = Color.WHITE
        font                = font.deriveFont(Font.BOLD, 11f)
        horizontalAlignment = SwingConstants.RIGHT
        border              = BorderFactory.createEmptyBorder(7, 10, 10, 5)
    }

    private val strip = object: JPanel(BorderLayout()) {
        init { isOpaque = false }
        override fun paintComponent(g: Graphics) {
            g.color = Color(0, 0, 0, 110)
            g.fillRect(0, 0, this.width, this.height)
        }
    }.apply {
        add(progressBar, BorderLayout.NORTH)
        add(label, BorderLayout.CENTER)
    }

    private val root = object: JPanel(BorderLayout()) {
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            backgroundImage?.let {
                val g2 = g.create() as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                g2.drawImage(it, 0, 0, this.width, this.height, null)
                g2.dispose()
            }
        }
    }.apply {
        add(strip, BorderLayout.SOUTH)
    }

    private var window: JWindow? = null

    override fun onShow() {
        window = JWindow().apply {
            contentPane = root
            root.preferredSize = Dimension(this@DefaultSplash.width, this@DefaultSplash.height)
            pack()
            setLocationRelativeTo(null)
            isAlwaysOnTop = true
            loadDefaultIcon()
            root.installWindowMover()
            strip.installWindowMover()
            isVisible = true
        }
    }

    override fun onProgress(notificator: ProgressNotificator) {
        with(notificator) {
            message?.let { label.text = it.ifEmpty { " " } }
            if(showProgressBar && progress != null && progress.toDouble() > 0) {
                progressBar.isVisible = true
                progressBar.value = (progress.toDouble().coerceIn(0.0, 1.0) * PROGRESS_SCALE).toInt()
            }
        }
    }

    override fun onHide() {
        window?.isVisible = false
    }

    override fun onClose() {
        window?.dispose()
        window = null
    }

    override fun isShowing(): Boolean = window?.isShowing ?: false

}

private fun readImage(path: String): BufferedImage? = runCatching {
    val resource = DefaultSplash::class.java.getResource("/" + path.removePrefix("/"))
    when {
        resource != null -> ImageIO.read(resource)
        File(path).isFile -> ImageIO.read(File(path))
        else -> null
    }
}.onFailure { logger.warn(it) { "fail to read splash image : $path" } }.getOrNull()
