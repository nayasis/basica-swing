package io.github.nayasis.kotlin.swing.control.token

import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.LayoutManager
import java.awt.RenderingHints
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.function.Predicate
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * text-field look-alike holding a row of [TokenChip]s and an [input] field for the next token.
 *
 * chips wrap onto as many rows as they need, keeping 1px to the border and 1px between rows; with a single
 * row the pane is exactly as tall as a text field. the input sits where subclasses put it with [render],
 * and stretches to the right edge when nothing follows it.
 *
 * the pane shows the focus border of a text field while [input] has the focus.
 */
abstract class TokenInputPane(inputColumns: Int): JPanel() {

    val input: JTextField = JTextField(inputColumns)

    private var laidOutWidth = -1

    init {
        layout = TokenFlowLayout(GAP, ROW_GAP, MARGIN, ::textFieldHeight)
        // the row count depends on the width, which the parent only settles while laying out
        addComponentListener(object: ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                if (width == laidOutWidth) return
                laidOutWidth = width
                revalidate()
            }
        })
        isOpaque = false
        isFocusable = false
        // FlatLaf paints the focus border of the pane when this predicate holds
        putClientProperty("JComponent.focusOwner", Predicate<JComponent> { input.isFocusOwner })
        // typed text can push a trailing input onto the next row
        input.document.addDocumentListener(object: DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = revalidate()
            override fun removeUpdate(e: DocumentEvent) = revalidate()
            override fun changedUpdate(e: DocumentEvent) {}
        })
        input.addFocusListener(object: FocusAdapter() {
            override fun focusGained(e: FocusEvent) = repaint()
            override fun focusLost(e: FocusEvent) = repaint()
        })
        // text set while the input had no width keeps a scroll offset that hides it once the input grows
        input.addComponentListener(object: ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                val textWidth = input.getFontMetrics(input.font).stringWidth(input.text)
                if (textWidth <= input.width - input.insets.left - input.insets.right) input.scrollOffset = 0
            }
        })
        addMouseListener(object: MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (SwingUtilities.isLeftMouseButton(e)) focusInput()
            }
        })
        // the input fills the rest of its row, so to the user it is blank space as well
        val rightClick = object: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e) && e.clickCount == 1) onBlankRightClick()
            }
        }
        addMouseListener(rightClick)
        input.addMouseListener(rightClick)
        updateUI()
    }

    /** right click on the pane outside any chip, including the input. does nothing by default. */
    protected open fun onBlankRightClick() {}

    override fun updateUI() {
        super.updateUI()
        // the input is created after the super constructor calls updateUI
        @Suppress("SENSELESS_COMPARISON")
        if (input == null) return
        border = UIManager.getBorder("TextField.border") ?: BorderFactory.createEtchedBorder()
        input.border   = BorderFactory.createEmptyBorder(0, 2, 0, 2)
        input.isOpaque = false
        background     = UIManager.getColor("TextField.background")
    }

    fun focusInput() {
        input.requestFocusInWindow()
    }

    /**
     * replace the content by [before], the input, then [after].
     *
     * the input itself stays attached: removing it would take the focus away while typing.
     */
    protected fun render(before: List<Component>, after: List<Component> = emptyList()) {
        components.filter { it !== input }.forEach { remove(it) }
        if (input.parent !== this) add(input)
        before.forEachIndexed { index, component -> add(component, index) }
        after.forEach { add(it) }
        revalidate()
        repaint()
    }

    protected fun chip(
        label: String,
        style: ChipStyle = ChipStyle.DEFAULT,
        onDoubleClick: (() -> Unit)? = null,
        onRightClick: (() -> Unit)? = null,
        onClick: () -> Unit,
    ): TokenChip = TokenChip(label, style, onClick, onDoubleClick, onRightClick)

    /** chips currently shown, in display order. */
    fun chips(): List<TokenChip> = components.filterIsInstance<TokenChip>()

    private fun textFieldHeight(): Int = JTextField().preferredSize.height

    override fun getMaximumSize(): Dimension = Dimension(Int.MAX_VALUE, preferredSize.height)

    // a rounded background inside the border, like the text fields of the look and feel
    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val focusWidth = UIManager.getInt("Component.focusWidth")
            val arc = UIManager.getInt("Component.arc")
            g2.color = if (isEnabled) background else UIManager.getColor("TextField.disabledBackground") ?: background
            g2.fillRoundRect(focusWidth, focusWidth, width - focusWidth * 2, height - focusWidth * 2, arc, arc)
        } finally {
            g2.dispose()
        }
    }

    private companion object {
        const val GAP     = 3
        const val ROW_GAP = 1
        const val MARGIN  = 1
    }

}

/**
 * flows children left to right at their preferred width and wraps them onto new rows.
 *
 * every child is stretched to one row height, which leaves exactly [margin] between a child and the
 * border, and [rowGap] between two rows. a text field that is the last child takes the rest of its row.
 *
 * @param singleRowHeight height of the whole pane when everything fits on one row
 */
private class TokenFlowLayout(
    private val hgap: Int,
    private val rowGap: Int,
    private val margin: Int,
    private val singleRowHeight: () -> Int,
): LayoutManager {

    private class Cell(val child: Component, val row: Int, val x: Int, val width: Int)

    override fun addLayoutComponent(name: String?, comp: Component?) {}

    override fun removeLayoutComponent(comp: Component?) {}

    // as narrow as the widest child: the rest wraps, so a long token list never forces the parent wider
    override fun preferredLayoutSize(parent: Container): Dimension {
        val insets = parent.insets
        val widest = visibleChildren(parent).maxOfOrNull { it.preferredSize.width } ?: 0
        return Dimension(widest + horizontalSpace(insets), heightFor(parent))
    }

    override fun minimumLayoutSize(parent: Container): Dimension =
        Dimension(horizontalSpace(parent.insets) + 20, heightFor(parent))

    override fun layoutContainer(parent: Container) {
        val insets    = parent.insets
        val rowHeight = rowHeight(parent)
        val top       = insets.top + margin
        cells(parent).forEach { cell ->
            cell.child.setBounds(cell.x, top + cell.row * (rowHeight + rowGap), cell.width, rowHeight)
        }
    }

    private fun heightFor(parent: Container): Int {
        val rows = (cells(parent).maxOfOrNull { it.row } ?: 0) + 1
        return verticalSpace(parent.insets) + rows * rowHeight(parent) + (rows - 1) * rowGap
    }

    /** before the first layout the width is unknown, so everything goes on one row. */
    private fun cells(parent: Container): List<Cell> {
        val insets   = parent.insets
        val left     = insets.left + margin
        val right    = if (parent.width > 0) parent.width - insets.right - margin else Int.MAX_VALUE
        val children = visibleChildren(parent)
        val cells    = ArrayList<Cell>(children.size)
        var row = 0
        var x   = left
        children.forEachIndexed { index, child ->
            val preferred = child.preferredSize.width
            val stretch   = index == children.lastIndex && child is JTextField
            // a trailing input only needs room for what is typed, otherwise an empty input would open a row of its own
            val needed    = if (stretch) minInputWidth(child, preferred) else preferred
            if (x > left && x + needed > right) {
                row++
                x = left
            }
            val width   = when {
                stretch && right != Int.MAX_VALUE -> (right - x).coerceAtLeast(preferred)
                else -> preferred
            }.coerceAtMost((right - x).coerceAtLeast(0))
            cells += Cell(child, row, x, width)
            x += width + hgap
        }
        return cells
    }

    private fun minInputWidth(input: JTextField, preferred: Int): Int {
        val metrics = input.getFontMetrics(input.font)
        val insets  = input.insets
        val text    = metrics.stringWidth(input.text) + CARET_WIDTH
        val least   = metrics.charWidth('m') * MIN_INPUT_CHARS
        return (maxOf(text, least) + insets.left + insets.right).coerceAtMost(preferred)
    }

    private fun rowHeight(parent: Container): Int {
        val fromTextField = singleRowHeight() - verticalSpace(parent.insets)
        val tallest = visibleChildren(parent).filter { it !is JTextField }.maxOfOrNull { it.preferredSize.height } ?: 0
        return maxOf(fromTextField, tallest)
    }

    private fun visibleChildren(parent: Container) = parent.components.filter { it.isVisible }

    private fun horizontalSpace(insets: java.awt.Insets) = insets.left + insets.right + margin * 2

    private fun verticalSpace(insets: java.awt.Insets) = insets.top + insets.bottom + margin * 2

    private companion object {
        const val MIN_INPUT_CHARS = 3
        const val CARET_WIDTH     = 2
    }

}
