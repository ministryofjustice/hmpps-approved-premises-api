package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.domainevents

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingCancelled
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingCancelledFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent.BookingCancelledTimelineFactory
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class BookingCancelledTimelineFactoryTest {

  @MockK
  lateinit var domainEventService: Cas1DomainEventService

  @MockK
  lateinit var bookingRepository: BookingRepository

  @MockK
  lateinit var spaceBookingRepository: Cas1SpaceBookingRepository

  @InjectMockKs
  lateinit var service: BookingCancelledTimelineFactory

  val id: UUID = UUID.randomUUID()

  @Nested
  inner class SpaceBooking {

    @Test
    fun `Returns expected description and payload`() {
      val spaceBookingId = UUID.randomUUID()
      val appealChangeRequestId = UUID.randomUUID()
      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withName("The Premises Name")
        .produce()

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withId(spaceBookingId)
        .withPremises(premises)
        .withActualArrivalDate(null)
        .withCanonicalArrivalDate(LocalDate.of(2024, 1, 1))
        .withCanonicalDepartureDate(LocalDate.of(2024, 4, 1))
        .withCancellationOccurredAt(null)
        .withCancellationReason(CancellationReasonEntityFactory().produce())
        .produce()

      every { spaceBookingRepository.findByIdOrNull(any()) } returns spaceBooking

      every { domainEventService.get(id, BookingCancelled::class) } returns buildDomainEvent(
        data = BookingCancelledFactory()
          .withBookingId(spaceBookingId)
          .withCancellationReason("reason for cancellation")
          .withAppealChangeRequestId(appealChangeRequestId)
          .produce(),
        spaceBookingId = spaceBookingId,
      )

      val result = service.produce(id)
      assertThat(result.description).isEqualTo(
        "A placement at The Premises Name booked for Monday 1 January 2024 to Monday 1 April 2024 was cancelled. " +
          "The reason was: 'reason for cancellation'",
      )

      val payload = result.payload!!

      assertThat(payload.booking.bookingId).isEqualTo(spaceBookingId)
      assertThat(payload.booking.premises.name).isEqualTo("The Premises Name")
      assertThat(payload.booking.arrivalDate).isEqualTo(LocalDate.of(2024, 1, 1))
      assertThat(payload.booking.departureDate).isEqualTo(LocalDate.of(2024, 4, 1))
      assertThat(payload.appealChangeRequestId).isEqualTo(appealChangeRequestId)
    }

    @Test
    fun `Throws exception for space booking cancelled event when space booking is not found`() {
      val spaceBookingId = UUID.randomUUID()

      every { spaceBookingRepository.findByIdOrNull(any()) } returns null

      every { domainEventService.get(id, BookingCancelled::class) } returns buildDomainEvent(
        data = BookingCancelledFactory()
          .withBookingId(spaceBookingId)
          .produce(),
        spaceBookingId = spaceBookingId,
      )

      val exception = assertThrows<RuntimeException> {
        service.produce(id)
      }
      assertThat(exception.message).isEqualTo("Space Booking ID $spaceBookingId with cancellation not found")
    }

    @Test
    fun `Throws exception for space booking cancelled event when booking does not have a cancelled reason`() {
      val spaceBookingId = UUID.randomUUID()
      val premises = ApprovedPremisesEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
        .produce()

      every { spaceBookingRepository.findByIdOrNull(any()) } returns
        Cas1SpaceBookingEntityFactory()
          .withId(spaceBookingId)
          .withPremises(premises)
          .withActualArrivalDate(null)
          .withCancellationOccurredAt(null)
          .produce()

      every { domainEventService.get(id, BookingCancelled::class) } returns buildDomainEvent(
        data = BookingCancelledFactory()
          .withBookingId(spaceBookingId)
          .produce(),
        spaceBookingId = spaceBookingId,
      )

      val exception = assertThrows<RuntimeException> {
        service.produce(id)
      }
      assertThat(exception.message).isEqualTo("Space Booking ID $spaceBookingId does not have a cancellation")
    }
  }
}
