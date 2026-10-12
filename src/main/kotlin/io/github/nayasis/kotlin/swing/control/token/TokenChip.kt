package io.github.nayasis.kotlin.swing.control.token

import io.github.nayasis.kotlin.swing.misc.isDarkLookAndFeel
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.SwingUtilities
import javax.swing.UIManager

/**
 * colors and size of a [TokenChip].
 *
 * @param fontScale font size relative to the look and feel's label font
 * @param dark colors used instead while the look and feel is dark; its own font scale is ignored
 */
data class ChipStyle(
    val background: Color = Color(0xe8eef8),
    val border: Color = Color(0xb8c7e6),
    val foreground: Color? = null,
    val fontScale: Float = 1.0f,
    val dark: ChipStyle? = null,
) {

    /** the colors for the current look and feel. */
    fun current(): ChipStyle = dark?.takeIf { isDarkLookAndFeel() } ?: this

    companion object {
        val DEFAULT = ChipStyle(dark = ChipStyle(Color(0x34405a), Color(0x4e6087)))
    }
}

/**
 * small rounded label standing for one token.
 *
 * a left click calls [onClick], a left double click [onDoubleClick] (after the [onClick] of its first click),
 * a right click [onRightClick].
 *
 * the text is clipped, never shortened with an ellipsis, so a chip always shows how a token starts.
 */
class TokenChip(
    text: String,
    style: ChipStyle = ChipStyle.DEFAULT,
    private val onClick: (() -> Unit)? = null,
    private val onDoubleClick: (() -> Unit)? = null,
    private val onRightClick: (() -> Unit)? = null,
): JComponent() {

    /** a selected chip draws a stronger border. */
    var selected: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            repaint()
        }

    var text: String = text
        set(value) {
            field = value
            revalidate()
            repaint()
        }

    var style: ChipStyle = style
        set(value) {
            field = value
            updateUI()
        }

    init {
        isFocusable = false
        isOpaque    = false
        cursor      = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addMouseListener(object: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                when {
                    SwingUtilities.isLeftMouseButton(e) && e.clickCount == 1 -> onClick?.invoke()
                    SwingUtilities.isLeftMouseButton(e) && e.clickCount == 2 -> onDoubleClick?.invoke()
                    SwingUtilities.isRightMouseButton(e) && e.clickCount == 1 -> onRightClick?.invoke()
                }
            }
        })
        updateUI()
    }

    override fun updateUI() {
        super.updateUI()
        val base = UIManager.getFont("Label.font") ?: font
        if (base != null) font = base.deriveFont(base.size2D * style.fontScale)
        revalidate()
        repaint()
    }

    override fun getPreferredSize(): Dimension {
        if (isPreferredSizeSet) return super.getPreferredSize()
        val metrics = getFontMetrics(font)
        return Dimension(metrics.stringWidth(text) + PADDING_X * 2, metrics.height + PADDING_Y * 2)
    }

    override fun getMinimumSize(): Dimension = preferredSize

    override fun getMaximumSize(): Dimension = preferredSize

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            val colors = style.current()
            g2.color = colors.background
            g2.fillRoundRect(0, 0, width - 1, height - 1, ARC, ARC)
            g2.color = if (selected) emphasized(colors.border) else colors.border
            g2.drawRoundRect(0, 0, width - 1, height - 1, ARC, ARC)
            g2.clipRect(PADDING_X, 0, (width - PADDING_X * 2).coerceAtLeast(0), height)
            g2.font  = font
            g2.color = colors.foreground ?: UIManager.getColor("Label.foreground") ?: foreground
            val metrics = g2.fontMetrics
            g2.drawString(text, PADDING_X, (height - metrics.height) / 2 + metrics.ascent)
        } finally {
            g2.dispose()
        }
    }

    // stronger means further from the background: darker on light themes, lighter on dark ones
    private fun emphasized(border: Color): Color =
        if (isDarkLookAndFeel()) border.brighter().brighter() else border.darker().darker()

    private companion object {
        const val PADDING_X = 5
        const val PADDING_Y = 1
        const val ARC = 6
    }

}
