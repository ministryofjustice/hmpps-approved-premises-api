package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.domainevents

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventTransferType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventTransferType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TransferReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.EventBookingSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.EventPremisesFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.EventTransferInfoFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent.BookingMadeTimelineFactory
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class BookingMadeTimelineFactoryTest {

  @MockK
  lateinit var domainEventService: Cas1DomainEventService

  @InjectMockKs
  lateinit var service: BookingMadeTimelineFactory

  val id: UUID = UUID.randomUUID()

  @Test
  fun success() {
    val bookingId = UUID.randomUUID()
    val transferredFromBookingId = UUID.randomUUID()
    val transferredFromChangeRequestId = UUID.randomUUID()

    every { domainEventService.get(id, BookingMade::class) } returns buildDomainEvent(
      data = BookingMadeFactory()
        .withBookingId(bookingId)
        .withArrivalOn(LocalDate.of(2024, 1, 1))
        .withDepartureOn(LocalDate.of(2024, 4, 1))
        .withPremises(
          EventPremisesFactory()
            .withName("The Premises Name")
            .produce(),
        )
        .withDeliusEventNumber("989")
        .withTransferredFrom(
          EventTransferInfoFactory()
            .withType(EventTransferType.EMERGENCY)
            .withBooking(
              EventBookingSummaryFactory()
                .withBookingId(transferredFromBookingId)
                .withPremises(EventPremisesFactory().withName("From Premises Name").produce())
                .withArrivalOn(LocalDate.of(2024, 4, 1))
                .withDepartureOn(LocalDate.of(2024, 5, 1))
                .produce(),
            )
            .withChangeRequestId(transferredFromChangeRequestId)
            .produce(),
        )
        .withTransferReason(TransferReason.riskToResident)
        .withAdditionalInformation("Additional information for the transfer booking")
        .produce(),

    )

    val result = service.produce(id)

    assertThat(result.description).isEqualTo(
      "A placement at The Premises Name was booked for " +
        "Monday 1 January 2024 to Monday 1 April 2024 against Delius Event Number 989",
    )

    val payload = result.payload!!

    assertThat(payload.booking.bookingId).isEqualTo(bookingId)
    assertThat(payload.booking.premises.name).isEqualTo("The Premises Name")
    assertThat(payload.booking.arrivalDate).isEqualTo(LocalDate.of(2024, 1, 1))
    assertThat(payload.booking.departureDate).isEqualTo(LocalDate.of(2024, 4, 1))
    assertThat(payload.eventNumber).isEqualTo("989")

    val transferredFrom = payload.transferredFrom!!
    assertThat(transferredFrom.type).isEqualTo(Cas1TimelineEventTransferType.EMERGENCY)
    assertThat(transferredFrom.changeRequestId).isEqualTo(transferredFromChangeRequestId)
    assertThat(transferredFrom.booking.bookingId).isEqualTo(transferredFromBookingId)
    assertThat(transferredFrom.booking.arrivalDate).isEqualTo(LocalDate.of(2024, 4, 1))
    assertThat(transferredFrom.booking.departureDate).isEqualTo(LocalDate.of(2024, 5, 1))
    assertThat(transferredFrom.booking.premises.name).isEqualTo("From Premises Name")
    assertThat(payload.booking.transferReason).isEqualTo(TransferReason.riskToResident)
    assertThat(payload.booking.additionalInformation).isEqualTo("Additional information for the transfer booking")
  }
}
