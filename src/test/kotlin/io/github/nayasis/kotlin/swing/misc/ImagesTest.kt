package io.github.nayasis.kotlin.swing.misc

import io.github.nayasis.kotlin.basica.core.string.toResource
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.AlphaComposite
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.Base64
import javax.imageio.ImageIO
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
