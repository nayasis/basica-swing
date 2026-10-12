package io.github.nayasis.kotlin.swing.control.combobox

import io.github.nayasis.kotlin.swing.common.assumeDisplay
import io.github.nayasis.kotlin.swing.misc.runUiAndWait
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ItemComboBoxTest {

    private fun combo() = ItemComboBox(('A'..'E').map { ItemCombo("$it") })

    @Test
    fun `value selects by value and null clears`() {
        assumeDisplay()
        runUiAndWait {
            val box = combo()

            box.value = "C"

            box.value shouldBe "C"
            box.select shouldBe 2
            box.selectedCombo shouldBe ItemCombo("C")

            box.value = null

            box.select shouldBe -1
        }
    }

    @Test
    fun `selectFirst and selectLast`() {
        assumeDisplay()
        runUiAndWait {
            val box = combo()

            box.selectLast()?.value shouldBe "E"
            box.selectFirst()?.value shouldBe "A"
            box.clear().selectFirst() shouldBe null
        }
    }

    @Test
    fun `addCombo overwrites an existing value or inserts at the index`() {
        assumeDisplay()
        runUiAndWait {
            val box = combo()

            box.addCombo("B", label = "bee")

            box.items.size shouldBe 5
            box.getItem("B")?.label shouldBe "bee"

            box.addCombo("Z", index = 1)

            box.items.map { it.value } shouldBe listOf("A", "Z", "B", "C", "D", "E")

            box.addCombo("A", label = "again", overwriteOnExist = false)

            box.items.count { it.value == "A" } shouldBe 2
        }
    }

    @Test
    fun `removeCombo by value or item`() {
        assumeDisplay()
        runUiAndWait {
            val box = combo()

            box.removeCombo("C") shouldBe true
            box.removeCombo("C") shouldBe false
            box.removeCombo(ItemCombo("A")) shouldBe true
            box.removeCombos(listOf(ItemCombo("B"), ItemCombo("Q"))) shouldBe true

            box.items.map { it.value } shouldBe listOf("D", "E")
        }
    }

    @Test
    fun `addCombos merges by value`() {
        assumeDisplay()
        runUiAndWait {
            val box = combo()

            box.addCombos(listOf(ItemCombo("A", "alpha"), ItemCombo("F")))

            box.items.size shouldBe 6
            box.getItem("A")?.label shouldBe "alpha"
        }
    }

}
