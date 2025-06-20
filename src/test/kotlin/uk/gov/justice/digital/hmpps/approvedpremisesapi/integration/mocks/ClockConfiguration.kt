package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.mocks

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.test.context.event.annotation.BeforeTestMethod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@Configuration
class ClockConfiguration {

  val clock: MutableClock
    @Bean
    @Primary
    get() = MutableClock()

  @BeforeTestMethod
  fun reset() {
    clock.reset()
  }

  class FixedClock : MutableClock(Instant.now())

  open class MutableClock(time: Instant? = null) : Clock() {
    var fixedTime: Instant? = time

    fun reset() {
      fixedTime = null
    }

    fun setNow(now: LocalDateTime) {
      fixedTime = now.toInstant(ZoneOffset.UTC)
    }

    fun setNow(now: Instant) {
      fixedTime = now
    }

    fun setToNowWithoutMillis() {
      setNow(LocalDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
    }

    fun advanceOneMinute() {
      fixedTime = (fixedTime ?: Instant.now()).plusSeconds(60)
    }

    override fun instant(): Instant = fixedTime ?: Instant.now()

    override fun withZone(zone: ZoneId?): Clock {
      error("Not supported")
    }

    override fun getZone(): ZoneId = ZoneId.systemDefault()
  }
}
