package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.domainevents

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementChangeRequestCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestType
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

  @ParameterizedTest
  @EnumSource(EventChangeRequestType::class)
  fun produce(changeRequestType: EventChangeRequestType) {
    val id = UUID.randomUUID()

    every {
      domainEventService.get(id, PlacementChangeRequestCreated::class)
    } returns buildDomainEvent(
      data = PlacementChangeRequestCreatedFactory()
        .withChangeRequestType(changeRequestType)
        .withBooking(
          EventBookingSummaryFactory()
            .withPremises(EventPremisesFactory().withName("The Premises Name").produce())
            .withArrivalOn(LocalDate.of(2015, 12, 1))
            .withDepartureOn(LocalDate.of(2015, 12, 2))
            .produce(),
        )
        .withReason(Cas1DomainEventCodedIdFactory().withCode("The reason code").produce())
        .produce(),
    )

    val result = service.produce(id)

    assertThat(result.type).isEqualTo(Cas1TimelineEventType.placementChangeRequestCreated)
    assertThat(result.changeRequestType).isEqualTo(
      when (changeRequestType) {
        EventChangeRequestType.PLACEMENT_APPEAL -> Cas1ChangeRequestType.PLACEMENT_APPEAL
        EventChangeRequestType.PLACEMENT_EXTENSION -> Cas1ChangeRequestType.PLACEMENT_EXTENSION
        EventChangeRequestType.PLANNED_TRANSFER -> Cas1ChangeRequestType.PLANNED_TRANSFER
      },
    )
    assertThat(result.booking.premises.name).isEqualTo("The Premises Name")
    assertThat(result.booking.arrivalDate).isEqualTo(LocalDate.of(2015, 12, 1))
    assertThat(result.booking.departureDate).isEqualTo(LocalDate.of(2015, 12, 2))
    assertThat(result.reason.name).isEqualTo("The reason code")
  }
}
