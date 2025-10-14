package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.domainevents

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationExpiredManually
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationExpiredManuallyFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent.ApplicationExpiredManuallyTimelineFactory
import java.util.UUID

@ExtendWith(MockKExtension::class)
class ApplicationExpiredManuallyTimelineFactoryTest {
  @MockK
  lateinit var domainEventService: Cas1DomainEventService

  @InjectMockKs
  lateinit var service: ApplicationExpiredManuallyTimelineFactory

  @Test
  fun produce() {
    val id = UUID.randomUUID()

    every {
      domainEventService.get(id, ApplicationExpiredManually::class)
    } returns buildDomainEvent(
      data = ApplicationExpiredManuallyFactory()
        .withExpiryReason("Superseded by another application.")
        .produce(),
    )

    val result = service.produce(id)

    assertThat(result.type).isEqualTo(Cas1TimelineEventType.applicationManuallyExpired)
    assertThat(result.expiredReason).isEqualTo("Superseded by another application.")
  }
}
