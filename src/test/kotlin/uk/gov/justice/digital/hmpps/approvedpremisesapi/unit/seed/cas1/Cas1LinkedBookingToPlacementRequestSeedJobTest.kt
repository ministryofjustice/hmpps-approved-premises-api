package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.seed.cas1

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApplicationTimelineNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1LinkBookingToPlacementRequestSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1LinkBookingToPlacementRequestSeedJobCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationTimelineNoteService
import java.time.LocalDate
import java.util.UUID

class Cas1LinkedBookingToPlacementRequestSeedJobTest {

  val placementRequestRepository = mockk<PlacementRequestRepository>()
  val bookingRepository = mockk<BookingRepository>()
  val applicationTimelineNoteService = mockk<ApplicationTimelineNoteService>()

  val validPlacementRequest = PlacementRequestEntityFactory()
    .withDefaults()
    .withApplication(ApprovedPremisesApplicationEntityFactory.DEFAULT)

  val validBooking = BookingEntityFactory()
    .withDefaults()
    .withApplication(ApprovedPremisesApplicationEntityFactory.DEFAULT)
    .withArrivalDate(LocalDate.of(2059, 12, 11))
    .withAdhoc(true)

  companion object {
    val placementRequestId: UUID = UUID.randomUUID()
    val bookingId: UUID = UUID.randomUUID()
  }

  val service = Cas1LinkBookingToPlacementRequestSeedJob(
    placementRequestRepository = placementRequestRepository,
    bookingRepository = bookingRepository,
    applicationTimelineNoteService = applicationTimelineNoteService,
  )

  @Test
  fun `error if placement request doesn't exist`() {
    every { placementRequestRepository.findByIdOrNull(placementRequestId) } returns null

    assertThatThrownBy {
      service.processRow(Cas1LinkBookingToPlacementRequestSeedJobCsvRow(bookingId, placementRequestId))
    }.hasMessage("Could not find placement request with id $placementRequestId")
  }

  @Test
  fun `error if booking doesn't exist`() {
    every { placementRequestRepository.findByIdOrNull(placementRequestId) } returns validPlacementRequest.produce()
    every { bookingRepository.findByIdOrNull(bookingId) } returns null

    assertThatThrownBy {
      service.processRow(Cas1LinkBookingToPlacementRequestSeedJobCsvRow(bookingId, placementRequestId))
    }.hasMessage("Could not find booking with id $bookingId")
  }

  @ParameterizedTest
  @CsvSource(
    nullValues = [ "null" ],
    value = [ "null", "false" ],
  )
  fun `error if not adhoc booking`(adhoc: Boolean?) {
    val nonAdhocBooking = validBooking
      .withAdhoc(adhoc)

    every { placementRequestRepository.findByIdOrNull(placementRequestId) } returns validPlacementRequest.produce()
    every { bookingRepository.findByIdOrNull(bookingId) } returns nonAdhocBooking.produce()

    assertThatThrownBy {
      service.processRow(Cas1LinkBookingToPlacementRequestSeedJobCsvRow(bookingId, placementRequestId))
    }.hasMessage("Can't link to non-adhoc booking $bookingId")
  }

  @Test
  fun `error if not same application`() {
    val bookingWithDifferentApplication = validBooking
      .withApplication(ApprovedPremisesApplicationEntityFactory().withDefaults().withId(UUID.randomUUID()).produce())

    every { placementRequestRepository.findByIdOrNull(placementRequestId) } returns validPlacementRequest.produce()
    every { bookingRepository.findByIdOrNull(bookingId) } returns bookingWithDifferentApplication.produce()

    assertThatThrownBy {
      service.processRow(Cas1LinkBookingToPlacementRequestSeedJobCsvRow(bookingId, placementRequestId))
    }.hasMessage("Can only link booking to placement request from same application")
  }

  @Test
  fun `error if already linked to placement request`() {
    val bookingLinkedToOtherPlacementRequest = validBooking
      .withPlacementRequest(PlacementRequestEntityFactory().withDefaults().produce())

    every { placementRequestRepository.findByIdOrNull(placementRequestId) } returns validPlacementRequest.produce()
    every { bookingRepository.findByIdOrNull(bookingId) } returns bookingLinkedToOtherPlacementRequest.produce()

    assertThatThrownBy {
      service.processRow(Cas1LinkBookingToPlacementRequestSeedJobCsvRow(bookingId, placementRequestId))
    }.hasMessage("Booking $bookingId is already linked to a placement request $placementRequestId")
  }

  @Test
  fun `error if already linked to booking`() {
    val placementRequestLinkedToOtherBooking = validPlacementRequest
      .withBooking(BookingEntityFactory().withDefaults().produce())

    every { placementRequestRepository.findByIdOrNull(placementRequestId) } returns placementRequestLinkedToOtherBooking.produce()
    every { bookingRepository.findByIdOrNull(bookingId) } returns validBooking.produce()

    assertThatThrownBy {
      service.processRow(Cas1LinkBookingToPlacementRequestSeedJobCsvRow(bookingId, placementRequestId))
    }.hasMessage("Placement Request $placementRequestId is already linked to booking $bookingId")
  }

  @Test
  fun `links placement request to booking`() {
    val placementRequest = validPlacementRequest.produce()
    val booking = validBooking.produce()

    every { placementRequestRepository.findByIdOrNull(placementRequestId) } returns placementRequest
    every { bookingRepository.findByIdOrNull(bookingId) } returns booking

    every { placementRequestRepository.save(any()) } returns placementRequest
    every { applicationTimelineNoteService.saveApplicationTimelineNote(any(), any(), any()) } returns ApplicationTimelineNoteEntityFactory.DEFAULT

    service.processRow(Cas1LinkBookingToPlacementRequestSeedJobCsvRow(bookingId, placementRequestId))

    verify {
      placementRequestRepository.save(
        withArg {
          assertThat(it).isEqualTo(placementRequest)
          assertThat(it.booking).isEqualTo(booking)
        },
      )
    }
  }

  @Test
  fun `creates note after linked`() {
    val placementRequest = validPlacementRequest.produce()
    val booking = validBooking.produce()

    every { placementRequestRepository.findByIdOrNull(placementRequestId) } returns placementRequest
    every { bookingRepository.findByIdOrNull(bookingId) } returns booking

    every { placementRequestRepository.save(any()) } returns placementRequest
    every { applicationTimelineNoteService.saveApplicationTimelineNote(any(), any(), any()) } returns ApplicationTimelineNoteEntityFactory.DEFAULT

    service.processRow(Cas1LinkBookingToPlacementRequestSeedJobCsvRow(bookingId, placementRequestId))

    verify {
      applicationTimelineNoteService.saveApplicationTimelineNote(
        applicationId = ApprovedPremisesApplicationEntityFactory.DEFAULT.id,
        "Adhoc booking with arrival date 'Thursday 11 December 2059' linked to corresponding request for placement by Application Support",
        user = null,
      )
    }
  }
}
