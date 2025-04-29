package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.domainevents

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlannedTransferRequestAccepted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.EventBookingSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.EventPremisesFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PlannedTransferRequestAcceptedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent.PlannedTransferRequestAcceptedTimelineFactory
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class PlannedTransferRequestAcceptedTimelineFactoryTest {
  @MockK
  lateinit var domainEventService: Cas1DomainEventService

  @InjectMockKs
  lateinit var service: PlannedTransferRequestAcceptedTimelineFactory

  @Test
  fun produce() {
    val id = UUID.randomUUID()

    every {
      domainEventService.get(id, PlannedTransferRequestAccepted::class)
    } returns buildDomainEvent(
      data = PlannedTransferRequestAcceptedFactory()
        .withFrom(
          EventBookingSummaryFactory()
            .withPremises(EventPremisesFactory().withName("The Premises Name from").produce())
            .withArrivalOn(LocalDate.of(2015, 12, 1))
            .withDepartureOn(LocalDate.of(2015, 12, 2))
            .produce(),
        )
        .withTo(
          EventBookingSummaryFactory()
            .withPremises(EventPremisesFactory().withName("The Premises Name to").produce())
            .withArrivalOn(LocalDate.of(2016, 12, 1))
            .withDepartureOn(LocalDate.of(2016, 12, 2))
            .produce(),
        )
        .produce(),
    )

    val result = service.produce(id)

    assertThat(result.type).isEqualTo(Cas1TimelineEventType.plannedTransferRequestAccepted)
    assertThat(result.from.premises.name).isEqualTo("The Premises Name from")
    assertThat(result.from.arrivalDate).isEqualTo(LocalDate.of(2015, 12, 1))
    assertThat(result.from.departureDate).isEqualTo(LocalDate.of(2015, 12, 2))
    assertThat(result.to.premises.name).isEqualTo("The Premises Name to")
    assertThat(result.to.arrivalDate).isEqualTo(LocalDate.of(2016, 12, 1))
    assertThat(result.to.departureDate).isEqualTo(LocalDate.of(2016, 12, 2))
  }
}
