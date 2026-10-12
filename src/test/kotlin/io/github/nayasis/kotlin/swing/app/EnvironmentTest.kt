package io.github.nayasis.kotlin.swing.app

import io.github.nayasis.kotlin.basica.core.validator.cast
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

class EnvironmentTest {

    @Test
    fun `basic test`() {

        val env = Environment()

        env.contains("logging") shouldBe true
        env.get<String>("logging.file.path") shouldBe "log/log"
        env.get<String>("logging.pattern.console") shouldNotBe null
        env.get<String>("logging.level.root") shouldBe false
        env.get<String>("logging.level.Exposed") shouldBe "debug"
        env.get<String>("logging.level['org.hibernate.type.descriptor.sql.BasicBinder']") shouldBe "error"

        env.startsWith("logging.level").cast<Map<String, Any>>().size shouldBeGreaterThan 0

    }

}