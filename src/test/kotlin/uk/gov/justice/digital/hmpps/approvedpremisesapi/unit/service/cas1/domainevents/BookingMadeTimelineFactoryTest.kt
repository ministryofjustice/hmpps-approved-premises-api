package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.domainevents

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.EventPremisesFactory
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
    val arrivalDate = LocalDate.of(2024, 1, 1)
    val departureDate = LocalDate.of(2024, 4, 1)

    every { domainEventService.get(id, BookingMade::class) } returns buildDomainEvent(
      data = BookingMadeFactory()
        .withBookingId(bookingId)
        .withArrivalOn(arrivalDate)
        .withDepartureOn(departureDate)
        .withPremises(
          EventPremisesFactory()
            .withName("The Premises Name")
            .produce(),
        )
        .withDeliusEventNumber("989")
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
  }
}
