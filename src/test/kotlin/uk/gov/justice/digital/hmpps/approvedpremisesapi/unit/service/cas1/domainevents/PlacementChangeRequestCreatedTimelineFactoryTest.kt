package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.domainevents

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementChangeRequestCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.Cas1DomainEventCodedIdFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.EventBookingSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.EventPremisesFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PlacementChangeRequestCreatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent.PlacementChangeRequestCreatedTimelineFactory
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class PlacementChangeRequestCreatedTimelineFactoryTest {
  @MockK
  lateinit var domainEventService: Cas1DomainEventService

  @InjectMockKs
  lateinit var service: PlacementChangeRequestCreatedTimelineFactory

  @Test
  fun produce() {
    val id = UUID.randomUUID()

    every {
      domainEventService.get(id, PlacementChangeRequestCreated::class)
    } returns buildDomainEvent(
      data = PlacementChangeRequestCreatedFactory()
        .withBooking(
          EventBookingSummaryFactory()
            .withPremises(EventPremisesFactory().withName("The Premises Name").produce())
            .withArrivalOn(LocalDate.of(2015, 12, 1))
            .withDepartureOn(LocalDate.of(2015, 12, 2))
            .produce(),
        )
        .withReason(Cas1DomainEventCodedIdFactory().withCode("The appeal name").produce())
        .produce(),
    )

    val result = service.produce(id)

    assertThat(result.type).isEqualTo(Cas1TimelineEventType.placementChangeRequestCreated)
    assertThat(result.booking.premises.name).isEqualTo("The Premises Name")
    assertThat(result.booking.arrivalDate).isEqualTo(LocalDate.of(2015, 12, 1))
    assertThat(result.booking.departureDate).isEqualTo(LocalDate.of(2015, 12, 2))
  }
}
