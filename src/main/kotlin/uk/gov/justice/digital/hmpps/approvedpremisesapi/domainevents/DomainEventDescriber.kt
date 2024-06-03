package uk.gov.justice.digital.hmpps.approvedpremisesapi.domainevents

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.RequestForPlacementAssessed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEventSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.javaConstantNameToSentence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toWeekAndDayDurationString
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

@SuppressWarnings("TooManyFunctions")
@Component
class DomainEventDescriber(
  private val domainEventService: DomainEventService,
  private val assessmentClarificationNoteRepository: AssessmentClarificationNoteRepository,
  private val bookingRepository: BookingRepository,
) {

  @SuppressWarnings("CyclomaticComplexMethod")
  fun getDescription(domainEventSummary: DomainEventSummary): String? {
    return when (domainEventSummary.type) {
      DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED -> "The application was submitted"
      DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED -> buildApplicationAssessedDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_BOOKING_MADE -> buildBookingMadeDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED -> buildPersonArrivedDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED -> buildPersonNotArrivedDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED -> buildPersonDepartedDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE -> buildBookingNotMadeDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED -> buildBookingCancelledDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED -> buildBookingChangedDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN -> buildApplicationWithdrawnDescription(domainEventSummary)
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

  private fun buildInfoRequestDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getFurtherInformationRequestMadeEvent(domainEventSummary.id())

    return event.describe { data ->
      val assessmentClarificationNote = assessmentClarificationNoteRepository.findByIdOrNull(data.eventDetails.requestId)
        ?: throw RuntimeException("Clarification note with ID ${data.eventDetails.requestId} not found")

      """
        A further information request was made to the applicant:
        "${assessmentClarificationNote.query}"
      """.trimIndent()
    }
  }

  private fun buildApplicationWithdrawnDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getApplicationWithdrawnEvent(domainEventSummary.id())

    return event.describe { data ->
      val formattedWithdrawalReason = data.eventDetails.withdrawalReason.replace("_", " ")

      "The application was withdrawn. The reason was: '$formattedWithdrawalReason'" +
        (data.eventDetails.otherWithdrawalReason?.let { " ($it)" } ?: "")
    }
  }

  private fun buildApplicationAssessedDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getApplicationAssessedDomainEvent(domainEventSummary.id())
    return event.describe { "The application was assessed and ${it.eventDetails.decision.lowercase()}" }
  }

  private fun buildBookingMadeDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getBookingMadeEvent(domainEventSummary.id())
    return event.describe {
      "A placement at ${it.eventDetails.premises.name} was booked for " +
        "${it.eventDetails.arrivalOn.toUiFormat()} to ${it.eventDetails.departureOn.toUiFormat()} " +
        "against Delius Event Number ${it.eventDetails.deliusEventNumber}"
    }
  }

  private fun buildBookingChangedDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getBookingChangedEvent(domainEventSummary.id())
    return event.describe {
      "A placement at ${it.eventDetails.premises.name} had its arrival and/or departure date changed to " +
        "${it.eventDetails.arrivalOn.toUiFormat()} to ${it.eventDetails.departureOn.toUiFormat()}"
    }
  }

  private fun buildPersonArrivedDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getPersonArrivedEvent(domainEventSummary.id())
    return event.describe { "The person moved into the premises on ${LocalDate.ofInstant(it.eventDetails.arrivedAt, ZoneOffset.UTC).toUiFormat()}" }
  }

  private fun buildPersonNotArrivedDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getPersonNotArrivedEvent(domainEventSummary.id())
    return event.describe { "The person was due to move into the premises on ${it.eventDetails.expectedArrivalOn.toUiFormat()} but did not arrive" }
  }

  private fun buildPersonDepartedDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getPersonDepartedEvent(domainEventSummary.id())
    return event.describe { "The person moved out of the premises on ${LocalDate.ofInstant(it.eventDetails.departedAt, ZoneOffset.UTC).toUiFormat()}" }
  }

  private fun buildBookingNotMadeDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getBookingNotMadeEvent(domainEventSummary.id())
    val failureReason = event?.data?.eventDetails?.failureDescription?.let {
      " The reason was: $it"
    } ?: ""
    return event.describe { "A placement was not made for the placement request.$failureReason" }
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  private fun buildBookingCancelledDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getBookingCancelledEvent(domainEventSummary.id())
    return event.describe { data ->
      val booking = bookingRepository.findByIdOrNull(data.eventDetails.bookingId)
        ?: throw RuntimeException("Booking ID ${data.eventDetails.bookingId} with cancellation not found")
      if (booking.cancellations.count() != 1) {
        throw RuntimeException("Booking ID ${data.eventDetails.bookingId} does not have one cancellation")
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

      "A placement at ${booking.premises.name} booked for " +
        "${booking.arrivalDate.toUiFormat()} to ${booking.departureDate.toUiFormat()} " +
        "was cancelled. The reason was: '${data.eventDetails.cancellationReason}'$otherReasonText"
    }
  }

  private fun buildAssessmentAppealedDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getAssessmentAppealedEvent(domainEventSummary.id())
    return event.describe { "The assessment was appealed and ${it.eventDetails.decision.value}. The reason was: ${it.eventDetails.decisionDetail}" }
  }

  private fun buildAssessmentAllocatedDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getAssessmentAllocatedEvent(domainEventSummary.id())
    return event.describe { ev ->
      val eventDetails = ev.eventDetails
      buildAllocationMessage(
        entityDescription = "The assessment",
        allocatedBy = eventDetails.allocatedBy,
        allocatedTo = eventDetails.allocatedTo,
      )
    }
  }

  private fun buildPlacementApplicationWithdrawnDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getPlacementApplicationWithdrawnEvent(domainEventSummary.id())

    return event.describe { data ->
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
  }

  private fun buildPlacementApplicationAllocatedDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getPlacementApplicationAllocatedEvent(domainEventSummary.id())

    return event.describe { data ->
      val details = data.eventDetails
      val dates = details.placementDates[0]
      val duration = ChronoUnit.DAYS.between(dates.startDate, dates.endDate)
      buildAllocationMessage("A request for placement", details.allocatedBy, details.allocatedTo) + " for assessment. " +
        buildRequestForPlacementDescription(dates.startDate, duration.toInt())
    }
  }

  private fun buildMatchRequestWithdrawnDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getMatchRequestWithdrawnEvent(domainEventSummary.id())
    /**
     * See documentation in [Cas1PlacementRequestDomainEventService] for why this is reported as a request for placement
     **/
    return event.describe { data ->
      val dates = data.eventDetails.datePeriod
      val reasonDescription = data.eventDetails.withdrawalReason.javaConstantNameToSentence()

      "A request for placement was withdrawn for dates ${dates.startDate.toUiFormat()} to ${dates.endDate.toUiFormat()}. " +
        "The reason was: '$reasonDescription'"
    }
  }

  private fun buildRequestForPlacementCreatedDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getRequestForPlacementCreatedEvent(domainEventSummary.id())

    return event.describe { data ->
      val details = data.eventDetails

      val description = when (details.requestForPlacementType) {
        RequestForPlacementType.initial -> "A placement was automatically requested after the application was assessed"
        RequestForPlacementType.rotl -> "A placement was requested with the reason 'Release on Temporary Licence (ROTL)'"
        RequestForPlacementType.releaseFollowingDecisions -> "A placement was requested with the reason 'Release directed following parole board or other hearing/decision'"
        RequestForPlacementType.additionalPlacement -> "A placement was requested with the reason 'An additional placement on an existing application'"
      }

      "$description. ${buildRequestForPlacementDescription(details.expectedArrival, details.duration)}"
    }
  }

  private fun buildRequestForPlacementAssessedDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getRequestForPlacementAssessedEvent(domainEventSummary.id())

    return event.describe { data ->
      val details = data.eventDetails
      val summary = details.decisionSummary?.let { " The reason was: $it." } ?: ""

      when (details.decision) {
        RequestForPlacementAssessed.Decision.accepted ->
          "A request for placement assessment was accepted. ${buildRequestForPlacementDescription(details.expectedArrival, details.duration)}.$summary"
        RequestForPlacementAssessed.Decision.rejected ->
          "A request for placement assessment was rejected. ${buildRequestForPlacementDescription(details.expectedArrival, details.duration, true)}.$summary"
      }
    }
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

  private fun DomainEventSummary.id(): UUID = UUID.fromString(this.id)
  private fun <T> DomainEvent<T>?.describe(describe: (T) -> String?): String? = this?.let { describe(it.data) }
  private val StaffMember.name: String
    get() = "${this.forenames} ${this.surname}".trim()
}
