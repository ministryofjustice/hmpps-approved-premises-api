package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingCancelled
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementAssessed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventContentPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEventSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent.LegacyTimelineFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent.TimelineFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.javaConstantNameToSentence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiDateTimeFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toWeekAndDayDurationString
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

@SuppressWarnings("TooManyFunctions", "TooGenericExceptionThrown")
@Component
class Cas1DomainEventDescriber(
  private val domainEventService: Cas1DomainEventService,
  private val assessmentClarificationNoteRepository: AssessmentClarificationNoteRepository,
  private val bookingRepository: BookingRepository,
  private val cas1SpaceBookingRepository: Cas1SpaceBookingRepository,
  private val payloadFactories: List<TimelineFactory<*>>,
  private val legacyPayloadFactories: List<LegacyTimelineFactory<*>>,
) {

  data class BookingCancellationDetail(val premisesName: String, val cancellationReason: String, val arrivalDate: String, val departureDate: String)

  /**
   * For any new domain event only payload should be defined, as the
   * UI should be responsible for rendering a suitable message using
   * the payload
   */
  data class EventDescriptionAndPayload<T : Cas1TimelineEventContentPayload>(
    val description: String?,
    val payload: T?,
  )

  @SuppressWarnings("CyclomaticComplexMethod")
  fun getDescriptionAndPayload(domainEventSummary: DomainEventSummary): EventDescriptionAndPayload<*> {
    payloadFactories.firstOrNull { it.forType() == domainEventSummary.type }?.let {
      return EventDescriptionAndPayload(
        description = null,
        payload = it.produce(domainEventSummary.id()),
      )
    }

    legacyPayloadFactories.firstOrNull { it.forType() == domainEventSummary.type }?.let {
      return it.produce(domainEventSummary.id())
    }

    // Do _not_ add to this list! Instead, create an implementation of [TimelineFactory]
    return when (domainEventSummary.type) {
      DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED -> EventDescriptionAndPayload(
        "The application was submitted",
        null,
      )

      DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED -> buildApplicationAssessedDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_BOOKING_MADE -> buildBookingMadeDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED -> buildPersonArrivedDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED -> buildPersonNotArrivedDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED -> buildPersonDepartedDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE -> buildBookingNotMadeDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED -> buildBookingCancelledDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_BOOKING_KEYWORKER_ASSIGNED -> buildBookingKeyWorkerAssignedDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN -> buildApplicationWithdrawnDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_APPLICATION_EXPIRED -> buildApplicationExpiredDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED -> buildAssessmentAppealedDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED -> buildAssessmentAllocatedDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN -> buildPlacementApplicationWithdrawnDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN -> buildMatchRequestWithdrawnDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED -> buildRequestForPlacementCreatedDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_ASSESSED -> buildRequestForPlacementAssessedDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED -> buildPlacementApplicationAllocatedDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED -> buildInfoRequestDescription(domainEventSummary)

      else -> throw IllegalArgumentException("Cannot map ${domainEventSummary.type}, only CAS1 is currently supported")
    }
  }

  private fun buildInfoRequestDescription(domainEventSummary: DomainEventSummary): EventDescriptionAndPayload<*> {
    val event = domainEventService.getFurtherInformationRequestMadeEvent(domainEventSummary.id())

    val description = event.describe { data ->
      val assessmentClarificationNote = assessmentClarificationNoteRepository.findByIdOrNull(data.eventDetails.requestId)
        ?: throw RuntimeException("Clarification note with ID ${data.eventDetails.requestId} not found")

      """
        A further information request was made to the applicant:
        "${assessmentClarificationNote.query}"
      """.trimIndent()
    }
    return EventDescriptionAndPayload(description, null)
  }

  private fun buildApplicationWithdrawnDescription(domainEventSummary: DomainEventSummary): EventDescriptionAndPayload<*> {
    val event = domainEventService.getApplicationWithdrawnEvent(domainEventSummary.id())

    val description = event.describe { data ->
      val formattedWithdrawalReason = data.eventDetails.withdrawalReason.replace("_", " ")

      "The application was withdrawn. The reason was: '$formattedWithdrawalReason'" +
        (data.eventDetails.otherWithdrawalReason?.let { " ($it)" } ?: "")
    }
    return EventDescriptionAndPayload(description, null)
  }

  private fun buildApplicationExpiredDescription(domainEventSummary: DomainEventSummary): EventDescriptionAndPayload<*> {
    val event = domainEventService.getApplicationExpiredEvent(domainEventSummary.id())
    return EventDescriptionAndPayload(event.describe { "The application has expired." }, null)
  }

  private fun buildApplicationAssessedDescription(domainEventSummary: DomainEventSummary): EventDescriptionAndPayload<*> {
    val event = domainEventService.getApplicationAssessedDomainEvent(domainEventSummary.id())
    return EventDescriptionAndPayload(event.describe { "The application was assessed and ${it.eventDetails.decision.lowercase()}" }, null)
  }

  private fun buildBookingMadeDescription(domainEventSummary: DomainEventSummary): EventDescriptionAndPayload<*> {
    val event = domainEventService.getBookingMadeEvent(domainEventSummary.id())
    val description = event.describe {
      "A placement at ${it.eventDetails.premises.name} was booked for " +
        "${it.eventDetails.arrivalOn.toUiFormat()} to ${it.eventDetails.departureOn.toUiFormat()} " +
        "against Delius Event Number ${it.eventDetails.deliusEventNumber}"
    }

    return EventDescriptionAndPayload(description, null)
  }

  private fun buildBookingKeyWorkerAssignedDescription(domainEventSummary: DomainEventSummary): EventDescriptionAndPayload<*> {
    val event = domainEventService.getBookingKeyWorkerAssignedEvent(domainEventSummary.id())
    val keyWorkersDetail = event?.data?.eventDetails?.previousKeyWorkerName?.let {
      "changes from $it to ${event.data.eventDetails.assignedKeyWorkerName}"
    } ?: "set to ${event?.data?.eventDetails?.assignedKeyWorkerName}"
    val description = event.describe {
      "Keyworker for placement at ${it.eventDetails.premises.name} for ${it.eventDetails.arrivalDate.toUiFormat()} to ${it.eventDetails.departureDate.toUiFormat()} $keyWorkersDetail".trimMargin()
    }
    return EventDescriptionAndPayload(description, null)
  }

  private fun buildPersonArrivedDescription(domainEventSummary: DomainEventSummary): EventDescriptionAndPayload<*> {
    val event = domainEventService.getPersonArrivedEvent(domainEventSummary.id())
    return EventDescriptionAndPayload(
      event.describe { "The person moved into the premises on ${LocalDateTime.ofInstant(it.eventDetails.arrivedAt, ZoneId.systemDefault()).toUiDateTimeFormat()}" },
      null,
    )
  }

  private fun buildPersonNotArrivedDescription(domainEventSummary: DomainEventSummary): EventDescriptionAndPayload<*> {
    val event = domainEventService.getPersonNotArrivedEvent(domainEventSummary.id())
    return EventDescriptionAndPayload(
      event.describe { "The person was due to move into the premises on ${it.eventDetails.expectedArrivalOn.toUiFormat()} but did not arrive" },
      null,
    )
  }

  private fun buildPersonDepartedDescription(domainEventSummary: DomainEventSummary): EventDescriptionAndPayload<*> {
    val event = domainEventService.getPersonDepartedEvent(domainEventSummary.id())
    return EventDescriptionAndPayload(event.describe { "The person moved out of the premises on ${LocalDateTime.ofInstant(it.eventDetails.departedAt, ZoneId.systemDefault()).toUiDateTimeFormat()}" }, null)
  }

  private fun buildBookingNotMadeDescription(domainEventSummary: DomainEventSummary): EventDescriptionAndPayload<*> {
    val event = domainEventService.getBookingNotMadeEvent(domainEventSummary.id())
    val failureReason = event?.data?.eventDetails?.failureDescription?.let {
      " The reason was: $it"
    } ?: ""
    return EventDescriptionAndPayload(event.describe { "A placement was not made for the placement request.$failureReason" }, null)
  }

  private fun buildBookingCancelledDescription(domainEventSummary: DomainEventSummary): EventDescriptionAndPayload<*> {
    val event = domainEventService.getBookingCancelledEvent(domainEventSummary.id())
    val bookingId = event!!.data.eventDetails.bookingId

    val bookingDetail: BookingCancellationDetail = if (domainEventSummary.cas1SpaceBookingId != null) {
      getSpaceBookingCancellationDetailForEvent(bookingId, event)
    } else {
      getBookingCancellationDetailForEvent(bookingId, event)
    }

    val description = "A placement at ${bookingDetail.premisesName} booked for " +
      "${bookingDetail.arrivalDate} to ${bookingDetail.departureDate} " +
      "was cancelled. The reason was: ${bookingDetail.cancellationReason}"
    return EventDescriptionAndPayload(description, null)
  }

  private fun buildAssessmentAppealedDescription(domainEventSummary: DomainEventSummary): EventDescriptionAndPayload<*> {
    val event = domainEventService.getAssessmentAppealedEvent(domainEventSummary.id())
    return EventDescriptionAndPayload(event.describe { "The assessment was appealed and ${it.eventDetails.decision.value}. The reason was: ${it.eventDetails.decisionDetail}" }, null)
  }

  private fun buildAssessmentAllocatedDescription(domainEventSummary: DomainEventSummary): EventDescriptionAndPayload<*> {
    val event = domainEventService.getAssessmentAllocatedEvent(domainEventSummary.id())
    val description = event.describe { ev ->
      val eventDetails = ev.eventDetails
      buildAllocationMessage(
        entityDescription = "The assessment",
        allocatedBy = eventDetails.allocatedBy,
        allocatedTo = eventDetails.allocatedTo,
      )
    }
    return EventDescriptionAndPayload(description, null)
  }

  private fun buildPlacementApplicationWithdrawnDescription(domainEventSummary: DomainEventSummary): EventDescriptionAndPayload<*> {
    val event = domainEventService.getPlacementApplicationWithdrawnEvent(domainEventSummary.id())

    val description = event.describe { data ->
      val dates = data.eventDetails.placementDates ?: emptyList()
      val reasonDescription = data.eventDetails.withdrawalReason.javaConstantNameToSentence()

      "A request for placement was withdrawn" +
        if (dates.isNotEmpty()) {
          " for dates " + dates.joinToString(", ") { "${it.startDate.toUiFormat()} to ${it.endDate.toUiFormat()}" }
        } else {
          ""
        } +
        ". The reason was: '$reasonDescription'"
    }
    return EventDescriptionAndPayload(description, null)
  }

  private fun buildPlacementApplicationAllocatedDescription(domainEventSummary: DomainEventSummary): EventDescriptionAndPayload<*> {
    val event = domainEventService.getPlacementApplicationAllocatedEvent(domainEventSummary.id())

    val description = event.describe { data ->
      val details = data.eventDetails
      val dates = details.placementDates[0]
      val duration = ChronoUnit.DAYS.between(dates.startDate, dates.endDate)
      buildAllocationMessage("A request for placement", details.allocatedBy, details.allocatedTo) + " for assessment. " +
        buildRequestForPlacementDescription(dates.startDate, duration.toInt())
    }
    return EventDescriptionAndPayload(description, null)
  }

  private fun buildMatchRequestWithdrawnDescription(domainEventSummary: DomainEventSummary): EventDescriptionAndPayload<*> {
    val event = domainEventService.getMatchRequestWithdrawnEvent(domainEventSummary.id())

    /**
     * See documentation in [Cas1PlacementRequestDomainEventService] for why this is reported as a request for placement
     **/
    val description = event.describe { data ->
      val dates = data.eventDetails.datePeriod
      val reasonDescription = data.eventDetails.withdrawalReason.javaConstantNameToSentence()

      "A request for placement was withdrawn for dates ${dates.startDate.toUiFormat()} to ${dates.endDate.toUiFormat()}. " +
        "The reason was: '$reasonDescription'"
    }

    return EventDescriptionAndPayload(description, null)
  }

  private fun buildRequestForPlacementCreatedDescription(domainEventSummary: DomainEventSummary): EventDescriptionAndPayload<*> {
    val event = domainEventService.getRequestForPlacementCreatedEvent(domainEventSummary.id())

    val description = event.describe { data ->
      val details = data.eventDetails

      val description = when (details.requestForPlacementType) {
        RequestForPlacementType.initial -> "A placement was automatically requested after the application was assessed"
        RequestForPlacementType.rotl -> "A placement was requested with the reason 'Release on Temporary Licence (ROTL)'"
        RequestForPlacementType.releaseFollowingDecisions -> "A placement was requested with the reason 'Release directed following parole board or other hearing/decision'"
        RequestForPlacementType.additionalPlacement -> "A placement was requested with the reason 'An additional placement on an existing application'"
      }

      "$description. ${buildRequestForPlacementDescription(details.expectedArrival, details.duration)}"
    }

    return EventDescriptionAndPayload(description, null)
  }

  private fun buildRequestForPlacementAssessedDescription(domainEventSummary: DomainEventSummary): EventDescriptionAndPayload<*> {
    val event = domainEventService.getRequestForPlacementAssessedEvent(domainEventSummary.id())

    val description = event.describe { data ->
      val details = data.eventDetails
      val summary = details.decisionSummary?.let { " The reason was: $it." } ?: ""

      when (details.decision) {
        RequestForPlacementAssessed.Decision.accepted ->
          "A request for placement assessment was accepted. ${buildRequestForPlacementDescription(details.expectedArrival, details.duration)}.$summary"
        RequestForPlacementAssessed.Decision.rejected ->
          "A request for placement assessment was rejected. ${buildRequestForPlacementDescription(details.expectedArrival, details.duration, true)}.$summary"
      }
    }

    return EventDescriptionAndPayload(description, null)
  }

  private fun buildAllocationMessage(entityDescription: String, allocatedBy: StaffMember?, allocatedTo: StaffMember?): String {
    val automatic = allocatedBy == null
    val prelude = if (automatic) {
      "$entityDescription was automatically allocated to"
    } else {
      "$entityDescription was allocated to"
    }
    val allocatedToDescription = allocatedTo?.name ?: "an unknown user"
    val allocatedByDescription = allocatedBy?.let { "by ${it.name}" } ?: ""
    return "$prelude $allocatedToDescription $allocatedByDescription".trim()
  }

  private fun buildRequestForPlacementDescription(expectedArrival: LocalDate, duration: Int, rejected: Boolean = false): String {
    val endDate = expectedArrival.plusDays(duration.toLong())
    return "The placement request ${if (rejected) "was" else "is"} for ${expectedArrival.toUiFormat()} to ${endDate.toUiFormat()} (${toWeekAndDayDurationString(duration)})"
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  private fun getSpaceBookingCancellationDetailForEvent(bookingId: UUID, event: GetCas1DomainEvent<Cas1DomainEventEnvelope<BookingCancelled>>): BookingCancellationDetail {
    val spaceBooking = cas1SpaceBookingRepository.findByIdOrNull(bookingId)
      ?: throw RuntimeException("Space Booking ID $bookingId with cancellation not found")
    if (spaceBooking.cancellationReason == null) {
      throw RuntimeException("Space Booking ID $bookingId does not have a cancellation")
    }
    return BookingCancellationDetail(
      premisesName = spaceBooking.premises.name,
      cancellationReason = "'${event.data.eventDetails.cancellationReason}'",
      arrivalDate = spaceBooking.canonicalArrivalDate.toUiFormat(),
      departureDate = spaceBooking.canonicalDepartureDate.toUiFormat(),
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
      cancellationReason = "'${event.data.eventDetails.cancellationReason}'$otherReasonText",
      arrivalDate = booking.arrivalDate.toUiFormat(),
      departureDate = booking.departureDate.toUiFormat(),
    )
  }

  private fun DomainEventSummary.id(): UUID = UUID.fromString(this.id)
  private fun <T> GetCas1DomainEvent<T>?.describe(describe: (T) -> String?): String? = this?.let { describe(it.data) }
  private val StaffMember.name: String
    get() = "${this.forenames} ${this.surname}".trim()
}
