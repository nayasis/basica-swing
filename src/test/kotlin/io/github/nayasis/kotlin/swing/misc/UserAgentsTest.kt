package io.github.nayasis.kotlin.swing.misc

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.lang.Thread.sleep
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class UserAgentsTest {

    @Test
    fun pickShouldReuseUserAgentWithinCacheWindow() {
        val userAgents = UserAgents(5.minutes)

        val first = userAgents.pick()
        sleep(200)
        val second = userAgents.pick()

        first shouldBe second
    }

    @Test
    fun pickShouldRotateUserAgentAfterCacheWindowExpires() {
        val userAgents = UserAgents(100.milliseconds)

        val first = userAgents.pick()
        sleep(300)
        val second = userAgents.pick()

        first shouldNotBe second
    }

}
