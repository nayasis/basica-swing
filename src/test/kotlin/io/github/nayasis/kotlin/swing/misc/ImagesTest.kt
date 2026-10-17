package io.github.nayasis.kotlin.swing.misc

import io.github.nayasis.kotlin.basica.core.string.toResource
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.AlphaComposite
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.Base64
import javax.imageio.ImageIO
import javax.imageio.IIOImage
import javax.imageio.ImageWriteParam
import javax.swing.ImageIcon

class ImagesTest {

    private fun tempFile(prefix: String, suffix: String): File =
        Files.createTempFile(prefix, suffix).toFile().apply { deleteOnExit() }

    private fun testImage(): BufferedImage =
        "test.png".toResource()?.toBufferedImage() ?: throw IOException("Image not found or could not be loaded.")

    @Test
    fun writePng() {

        val fullWidth  = 1920
        val fullHeight = 1080

        val image = testImage().resize(fullWidth, fullHeight)

        val newWidth = getWidth("4:3", fullHeight)
        val x = (fullWidth - newWidth) / 2

        image.createGraphics().run {
            composite = AlphaComposite.Clear
            fillRect(x, 0, newWidth, fullHeight)
            dispose()
        }

        val file = tempFile("basica-swing-image-test-", ".png")

        ImageIO.write(image, "png", file)

        assertTrue(file.length() > 0)

    }

    private fun getWidth(ratio: String, baseHeight: Int): Int {
        val (width, height) = ratio.split(":").map { it.toInt() }.let { it[0] to it[1] }
        return (1.0 * width * baseHeight / height).toInt()
    }

    @Test
    fun rotate() {
        val src  = testImage()
        val file = tempFile("basica-swing-image-rotated-", ".png")
        val rotated = src.rotate(90.0)
        rotated.write(file)

        rotated.width shouldBe src.height
        rotated.height shouldBe src.width
        assertTrue(file.length() > 0)
    }

    @Test
    fun resizeKeepsRatioByMaxPixel() {
        val image = BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB)
        val resized = image.resize(100)
        resized.width shouldBe 100
        resized.height shouldBe 50
    }

    @Test
    fun resizeKeepsAtLeastOnePixelOnEachAxis() {
        val wide = BufferedImage(1000, 1, BufferedImage.TYPE_INT_ARGB).resize(32)
        wide.width shouldBe 32
        wide.height shouldBe 1

        val tall = BufferedImage(1, 1000, BufferedImage.TYPE_INT_ARGB).resize(32)
        tall.width shouldBe 1
        tall.height shouldBe 32
    }

    @Test
    fun cropRemovesPixels() {
        val image = BufferedImage(40, 30, BufferedImage.TYPE_INT_ARGB)
        image.cropTop(5).height shouldBe 25
        image.cropBottom(5).height shouldBe 25
        image.cropLeft(10).width shouldBe 30
        image.cropRight(10).width shouldBe 30
    }

    @Test
    fun base64DataUriShouldBeDecoded() {
        val image = BufferedImage(8, 6, BufferedImage.TYPE_INT_ARGB)
        val encoded = Base64.getEncoder().encodeToString(image.toBinary("png"))

        val decoded = "data:image/png;base64,$encoded".toBufferedImage()

        decoded.width shouldBe 8
        decoded.height shouldBe 6
    }

    @Test
    fun webpRoundTripsColorAndTransparency() {
        val image = BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB).apply {
            setRGB(0, 0, 0x804080C0.toInt())
            setRGB(1, 1, 0xFF123456.toInt())
        }
        val writer = ImageIO.getImageWritersByFormatName("webp").next()
        val bytes = ByteArrayOutputStream()
        try {
            ImageIO.createImageOutputStream(bytes).use { output ->
                writer.output = output
                val parameters = writer.defaultWriteParam.apply {
                    compressionMode = ImageWriteParam.MODE_EXPLICIT
                    compressionType = compressionTypes.first { it.equals("Lossless", ignoreCase = true) }
                }
                writer.write(null, IIOImage(image, null, null), parameters)
            }
        } finally {
            writer.dispose()
        }

        val decoded = bytes.toByteArray().toBufferedImage()
        decoded.width shouldBe 2
        decoded.height shouldBe 2
        decoded.getRGB(0, 0) shouldBe image.getRGB(0, 0)
        decoded.getRGB(1, 1) shouldBe image.getRGB(1, 1)
    }

    @Test
    fun malformedWebpIsRejected() {
        val truncated = "RIFF".toByteArray() + byteArrayOf(4, 0, 0, 0) + "WEBPVP8L".toByteArray()
        org.junit.jupiter.api.assertThrows<IOException> { truncated.toBufferedImage() }
    }

    @Test
    fun emptyByteArrayShouldBeEmpty() {
        byteArrayOf().toBufferedImage().isEmpty() shouldBe true
    }

    @Test
    fun zeroSizedImageIconShouldFail() {
        ImageIcon().toBufferedImage().isEmpty() shouldBe true
    }

    @Test
    fun invalidSourceShouldThrow() {
        org.junit.jupiter.api.assertThrows<IOException> {
            "not-exist-file-path.png".toBufferedImage()
        }
    }

}
