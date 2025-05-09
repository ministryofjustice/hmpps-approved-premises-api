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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingCancelled
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingCancelledFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent.BookingCancelledTimelineFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.seed.cas1.Cas1LinkedBookingToPlacementRequestSeedJobTest.Companion.bookingId
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
  inner class Booking {

    @Test
    fun `Throws exception for booking cancelled event when booking is not found`() {
      val bookingId = UUID.fromString("75ed7091-1767-4901-8c2b-371dd0f5864c")

      every { domainEventService.get(id, BookingCancelled::class) } returns buildDomainEvent(
        data = BookingCancelledFactory()
          .withBookingId(bookingId)
          .produce(),
      )

      every { bookingRepository.findByIdOrNull(any()) } returns null

      val exception = assertThrows<RuntimeException> {
        service.produce(id)
      }
      assertThat(exception.message).isEqualTo("Booking ID $bookingId with cancellation not found")
    }

    @Test
    fun `Throws exception for booking cancelled event when booking does not have one cancellation`() {
      val bookingEntity = getBookingEntity()

      every { domainEventService.get(id, BookingCancelled::class) } returns buildDomainEvent(
        data = BookingCancelledFactory()
          .withBookingId(bookingEntity.id)
          .produce(),
      )

      every { bookingRepository.findByIdOrNull(any()) } returns bookingEntity

      val exception = assertThrows<RuntimeException> {
        service.produce(id)
      }
      assertThat(exception.message).isEqualTo("Booking ID ${bookingEntity.id} does not have one cancellation")
    }

    @ParameterizedTest
    @CsvSource(value = ["Reason A", "Reason B"])
    fun `Returns expected description and payload`(reason: String) {
      val bookingEntity = getBookingEntity()
      val cancellation = CancellationEntityFactory()
        .withDefaultReason()
        .withBooking(bookingEntity)
        .produce()

      bookingEntity.cancellations += cancellation

      every { domainEventService.get(id, BookingCancelled::class) } returns buildDomainEvent(
        data = BookingCancelledFactory()
          .withBookingId(bookingEntity.id)
          .withCancellationReason(reason)
          .produce(),
      )

      every { bookingRepository.findByIdOrNull(bookingEntity.id) } returns bookingEntity

      val result = service.produce(id)

      assertThat(result.description).isEqualTo("A placement at premisesName booked for Thursday 15 August 2024 to Sunday 18 August 2024 was cancelled. The reason was: '$reason'")

      val payload = result.payload!!
      assertThat(payload.booking.bookingId).isEqualTo(bookingId)
      assertThat(payload.booking.premises.name).isEqualTo("premisesName")
      assertThat(payload.booking.arrivalDate).isEqualTo(LocalDate.of(2024, 8, 15))
      assertThat(payload.booking.departureDate).isEqualTo(LocalDate.of(2024, 8, 18))
    }

    @ParameterizedTest
    @CsvSource(value = ["narrative A", "narrative B"])
    fun `Returns expected description and payload when reason is other with text narrative`(
      otherReasonText: String,
    ) {
      val bookingEntity = getBookingEntity()
      val cancellationOtherReason = CancellationReasonEntityFactory()
        .withServiceScope(ServiceName.approvedPremises.value)
        .withName("Other")
        .withId(CancellationReasonRepository.CAS1_RELATED_OTHER_ID)
        .produce()

      val cancellation = CancellationEntityFactory()
        .withReason(cancellationOtherReason)
        .withOtherReason(otherReasonText)
        .withBooking(bookingEntity)
        .produce()

      bookingEntity.cancellations += cancellation

      every { domainEventService.get(id, BookingCancelled::class) } returns buildDomainEvent(
        data = BookingCancelledFactory()
          .withBookingId(bookingEntity.id)
          .withCancellationReason(cancellationOtherReason.name)
          .produce(),
      )

      every { bookingRepository.findByIdOrNull(bookingEntity.id) } returns bookingEntity

      val result = service.produce(id)

      assertThat(result.description).isEqualTo("A placement at premisesName booked for Thursday 15 August 2024 to Sunday 18 August 2024 was cancelled. The reason was: 'Other': $otherReasonText.")

      val payload = result.payload!!
      assertThat(payload.booking.bookingId).isEqualTo(bookingId)
      assertThat(payload.booking.premises.name).isEqualTo("premisesName")
      assertThat(payload.booking.arrivalDate).isEqualTo(LocalDate.of(2024, 8, 15))
      assertThat(payload.booking.departureDate).isEqualTo(LocalDate.of(2024, 8, 18))
    }

    @Test
    fun `Returns expected description and payload when reason is other without text narrative`() {
      val bookingEntity = getBookingEntity()
      val cancellationOtherReason = CancellationReasonEntityFactory()
        .withServiceScope(ServiceName.approvedPremises.value)
        .withName("Other")
        .withId(CancellationReasonRepository.CAS1_RELATED_OTHER_ID)
        .produce()
      val cancellation = CancellationEntityFactory()
        .withReason(cancellationOtherReason)
        .withBooking(bookingEntity)
        .produce()

      bookingEntity.cancellations += cancellation

      every { domainEventService.get(id, BookingCancelled::class) } returns buildDomainEvent(
        data = BookingCancelledFactory()
          .withBookingId(bookingEntity.id)
          .withCancellationReason(cancellationOtherReason.name)
          .produce(),
      )

      every { bookingRepository.findByIdOrNull(bookingEntity.id) } returns bookingEntity

      val result = service.produce(id)

      assertThat(result.description).isEqualTo("A placement at premisesName booked for Thursday 15 August 2024 to Sunday 18 August 2024 was cancelled. The reason was: 'Other'")

      val payload = result.payload!!
      assertThat(payload.booking.bookingId).isEqualTo(bookingId)
      assertThat(payload.booking.premises.name).isEqualTo("premisesName")
      assertThat(payload.booking.arrivalDate).isEqualTo(LocalDate.of(2024, 8, 15))
      assertThat(payload.booking.departureDate).isEqualTo(LocalDate.of(2024, 8, 18))
    }

    private fun getBookingEntity(): BookingEntity {
      val premisesId = UUID.randomUUID()
      val premisesName = "premisesName"

      val keyWorker = ContextStaffMemberFactory().produce()

      val premisesEntity = ApprovedPremisesEntityFactory()
        .withId(premisesId)
        .withName(premisesName)
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
        .produce()

      return BookingEntityFactory()
        .withId(bookingId)
        .withPremises(premisesEntity)
        .withArrivalDate(LocalDate.parse("2024-08-15"))
        .withDepartureDate(LocalDate.parse("2024-08-18"))
        .withStaffKeyWorkerCode(keyWorker.code)
        .produce()
    }
  }

  @Nested
  inner class SpaceBooking {

    @Test
    fun `Returns expected description and payload`() {
      val spaceBookingId = UUID.randomUUID()
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
