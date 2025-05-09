package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingCancelled
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1BookingCancelledContentPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventPayloadBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventDescriber
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.GetCas1DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiFormat
import java.time.LocalDate
import java.util.UUID

@Service
class BookingCancelledTimelineFactory(
  val domainEventService: Cas1DomainEventService,
  private val bookingRepository: BookingRepository,
  private val cas1SpaceBookingRepository: Cas1SpaceBookingRepository,
) : LegacyTimelineFactory<Cas1BookingCancelledContentPayload> {
  override fun produce(domainEventId: UUID): Cas1DomainEventDescriber.EventDescriptionAndPayload<Cas1BookingCancelledContentPayload> {
    val event = domainEventService.get(domainEventId, BookingCancelled::class)!!

    val bookingId = event.data.eventDetails.bookingId

    val bookingDetail: BookingCancellationDetail = if (event.spaceBookingId != null) {
      getSpaceBookingCancellationDetailForEvent(bookingId, event)
    } else {
      getBookingCancellationDetailForEvent(bookingId, event)
    }

    val eventDetails = event.data.eventDetails

    return Cas1DomainEventDescriber.EventDescriptionAndPayload(
      buildDescription(bookingDetail),
      Cas1BookingCancelledContentPayload(
        type = Cas1TimelineEventType.bookingCancelled,
        booking = Cas1TimelineEventPayloadBookingSummary(
          bookingId = bookingId,
          premises = NamedId(
            bookingDetail.premisesId,
            bookingDetail.premisesName,
          ),
          arrivalDate = bookingDetail.arrivalDate,
          departureDate = bookingDetail.departureDate,
        ),
        cancellationReason = bookingDetail.cancellationReason,
      ),
    )
  }

  private fun buildDescription(bookingDetail: BookingCancellationDetail) = "A placement at ${bookingDetail.premisesName} booked for " +
    "${bookingDetail.arrivalDate.toUiFormat()} to ${bookingDetail.departureDate.toUiFormat()} " +
    "was cancelled. The reason was: ${bookingDetail.cancellationReason}"

  @SuppressWarnings("TooGenericExceptionThrown")
  private fun getSpaceBookingCancellationDetailForEvent(bookingId: UUID, event: GetCas1DomainEvent<Cas1DomainEventEnvelope<BookingCancelled>>): BookingCancellationDetail {
    val spaceBooking = cas1SpaceBookingRepository.findByIdOrNull(bookingId)
      ?: throw RuntimeException("Space Booking ID $bookingId with cancellation not found")
    if (spaceBooking.cancellationReason == null) {
      throw RuntimeException("Space Booking ID $bookingId does not have a cancellation")
    }
    return BookingCancellationDetail(
      premisesName = spaceBooking.premises.name,
      premisesId = spaceBooking.premises.id,
      cancellationReason = "'${event.data.eventDetails.cancellationReason}'",
      arrivalDate = spaceBooking.canonicalArrivalDate,
      departureDate = spaceBooking.canonicalDepartureDate,
    )
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  private fun getBookingCancellationDetailForEvent(bookingId: UUID, event: GetCas1DomainEvent<Cas1DomainEventEnvelope<BookingCancelled>>): BookingCancellationDetail {
    val booking = bookingRepository.findByIdOrNull(bookingId)
      ?: throw RuntimeException("Booking ID $bookingId with cancellation not found")
    if (booking.cancellations.count() != 1) {
      throw RuntimeException("Booking ID $bookingId does not have one cancellation")
    }
    val cancellation = booking.cancellations.first()
    val otherReasonText =
      if (cancellation.reason.id == CancellationReasonRepository.CAS1_RELATED_OTHER_ID &&
        !cancellation.otherReason.isNullOrEmpty()
      ) {
        ": ${cancellation.otherReason}."
      } else {
        ""
      }
    return BookingCancellationDetail(
      premisesName = booking.premises.name,
      premisesId = booking.premises.id,
      cancellationReason = "'${event.data.eventDetails.cancellationReason}'$otherReasonText",
      arrivalDate = booking.arrivalDate,
      departureDate = booking.departureDate,
    )
  }

  private data class BookingCancellationDetail(
    val premisesName: String,
    val premisesId: UUID,
    val cancellationReason: String,
    val arrivalDate: LocalDate,
    val departureDate: LocalDate,
  )

  override fun forType() = DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED
}
