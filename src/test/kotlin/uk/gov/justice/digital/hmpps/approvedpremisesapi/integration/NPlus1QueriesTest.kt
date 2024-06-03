package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@ActiveProfiles("test", "log-sql")
class NPlus1QueriesTest : IntegrationTestBase() {
  val appender = mockk<Appender<ILoggingEvent>>()

  val capturedLogs = mutableListOf<ILoggingEvent>()

  val log = LoggerFactory.getLogger(this::class.java)

  @BeforeEach
  fun setup() {
    val logger = LoggerFactory.getLogger("org.hibernate.SQL") as ch.qos.logback.classic.Logger
    logger.addAppender(appender)

    every { appender.doAppend(capture(capturedLogs)) } returns Unit
  }

  @Test
  @Disabled("Load tests are not run by default")
  fun `Listing a large number of Temporary Accommodation premises does not result in the N+1 queries problem`() {
    givenAUser { user, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(200) {
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withProbationRegion(user.probationRegion)
        withService("CAS3")
        withYieldedProbationDeliveryUnit {
          probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(user.probationRegion)
          }
        }
      }

      premises.forEach { p ->
        val rooms = roomEntityFactory.produceAndPersistMultiple(20) {
          withPremises(p)
        }

        rooms.forEach { r ->
          bedEntityFactory.produceAndPersist {
            withRoom(r)
          }
        }
      }

      capturedLogs.clear()

      val requestTime = measureTimeMillis {
        webTestClient.get()
          .uri("/premises/summary")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", "temporary-accommodation")
          .header("X-User-Region", "${user.probationRegion.id}")
          .exchange()
          .expectStatus()
          .isOk
      }.milliseconds

      log.info("Request took $requestTime (executed ${capturedLogs.size} queries)")

      assertThat(capturedLogs).size().isEqualTo(1)
      assertThat(requestTime).isLessThan(10.seconds)
    }
  }
}
