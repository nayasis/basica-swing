package io.github.nayasis.kotlin.swing.app

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.sift.MDCBasedDiscriminator
import ch.qos.logback.classic.spi.LoggingEvent
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class LogbackSecurityTest {

    @Test
    fun `MDC values cannot escape log destinations`() {
        val context = LoggerContext()
        try {
            val discriminator = MDCBasedDiscriminator().apply {
                this.context = context
                key = "tenant"
                defaultValue = "default"
                start()
            }
            listOf("../escape", "..\\escape", "tenant/child", "tenant\\child", "\${tenant}", "tenant@example.com", "", "a".repeat(65)).forEach { value ->
                val event = LoggingEvent().apply { setMDCPropertyMap(mapOf("tenant" to value)) }
                discriminator.getDiscriminatingValue(event) shouldBe "default"
            }
        } finally {
            context.stop()
        }
    }

    @Test
    fun `ordinary MDC values and missing values still work`() {
        val context = LoggerContext()
        try {
            val discriminator = MDCBasedDiscriminator().apply {
                this.context = context
                key = "tenant"
                defaultValue = "default"
                start()
            }
            val event = LoggingEvent().apply { setMDCPropertyMap(mapOf("tenant" to "tenant-01")) }
            discriminator.getDiscriminatingValue(event) shouldBe "tenant-01"

            val missing = LoggingEvent().apply { setMDCPropertyMap(emptyMap()) }
            discriminator.getDiscriminatingValue(missing) shouldBe "default"
        } finally {
            context.stop()
        }
    }

}
