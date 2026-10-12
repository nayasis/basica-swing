package io.github.nayasis.kotlin.swing.window

import io.github.nayasis.kotlin.basica.core.io.extension
import io.github.nayasis.kotlin.basica.core.string.toPath
import io.github.nayasis.kotlin.basica.core.string.toResource
import io.github.nayasis.kotlin.basica.core.url.toInputStream
import io.github.nayasis.kotlin.swing.misc.toIconImages
import java.awt.Image
import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO

class IconContainer {

    private val list = ArrayList<Image>()

    val icons: List<Image>
        get() = list

    fun add(icon: Image) = list.add(icon)

    fun add(resourcePath: String) {
        resourcePath.toResource()?.toInputStream()?.use { bis ->
            if( resourcePath.toPath().extension == "ico" ) {
                list.addAll(bis.toIconImages())
            } else {
                ImageIO.read(bis)?.let { list.add(it) }
            }
        }
    }

    fun add(file: File) = list.addAll(file.toIconImages())

    fun add(path: Path) = add(path.toFile())

    fun clear() = list.clear()

    fun isEmpty(): Boolean = list.isEmpty()

}
