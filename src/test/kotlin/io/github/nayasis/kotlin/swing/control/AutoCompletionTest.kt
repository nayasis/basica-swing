package io.github.nayasis.kotlin.swing.control

import io.github.nayasis.kotlin.swing.misc.runUiAndWait
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.UIManager

internal class AutoCompletionTest {

    @Test
    fun `an unattached field does not register a global listener`() = runUiAndWait<Unit> {
        val field = JTextField()
        val listeners = UIManager.getPropertyChangeListeners().size

        AutoCompletion<String>(field) {}

        UIManager.getPropertyChangeListeners().size shouldBe listeners
    }

    @Test
    fun `theme listener follows field attachment and removal`() = runUiAndWait<Unit> {
        val field = JTextField()
        val parent = JPanel().apply { add(field) }
        val listeners = UIManager.getPropertyChangeListeners().size
        AutoCompletion<String>(field) {}
        try {
            repeat(2) {
                parent.addNotify()
                UIManager.getPropertyChangeListeners().size shouldBe listeners + 1

                parent.removeNotify()
                UIManager.getPropertyChangeListeners().size shouldBe listeners
            }
        } finally {
            parent.removeNotify()
        }
    }

    @Test
    fun `an already attached field releases its theme listener on removal`() = runUiAndWait<Unit> {
        val field = JTextField()
        val parent = JPanel().apply { add(field) }
        val listeners = UIManager.getPropertyChangeListeners().size
        parent.addNotify()
        try {
            AutoCompletion<String>(field) {}
            UIManager.getPropertyChangeListeners().size shouldBe listeners + 1
        } finally {
            parent.removeNotify()
        }

        UIManager.getPropertyChangeListeners().size shouldBe listeners
    }

}
