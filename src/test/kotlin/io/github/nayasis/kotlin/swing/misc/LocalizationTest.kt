package io.github.nayasis.kotlin.swing.misc

import io.github.nayasis.kotlin.swing.control.combobox.ItemCombo
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import javax.swing.JLabel

internal class LocalizationTest {

    @Test
    fun boundTextsAndListenersFollowARefresh() = runUiAndWait<Unit> {
        var language = "en"
        val label = JLabel().localize { text = "label-$language" }
        label.text shouldBe "label-en"

        var refreshed = 0
        val handle = Localization.onRefresh { refreshed++ }

        language = "ja"
        Localization.refresh()
        label.text shouldBe "label-ja"
        refreshed shouldBe 1

        handle.close()
        Localization.refresh()
        refreshed shouldBe 1
    }

    @Test
    fun aTargetCanHoldSeveralTexts() = runUiAndWait<Unit> {
        var suffix = "1"
        val label = JLabel()
            .localize { text = "text-$suffix" }
            .localize { toolTipText = "tip-$suffix" }

        suffix = "2"
        Localization.refresh()
        label.text shouldBe "text-2"
        label.toolTipText shouldBe "tip-2"
    }

    @Test
    fun equalTargetsAreRefreshedSeparately() = runUiAndWait<Unit> {
        var language = "en"
        val first = ItemCombo("same").localize { label = "first-$language" }
        val second = ItemCombo("same").localize { label = "second-$language" }

        language = "ja"
        Localization.refresh()

        first.label shouldBe "first-ja"
        second.label shouldBe "second-ja"
    }

}
