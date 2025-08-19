package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementAssessed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventContentPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteRepository
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
  private val payloadFactories: List<TimelineFactory<*>>,
  private val legacyPayloadFactories: List<LegacyTimelineFactory<*>>,
) {

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
    // If migrating code from here into a [TimelineFactory], use a [LegacyTimelineFactory]
    return when (domainEventSummary.type) {
      DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED -> EventDescriptionAndPayload(
        "The application was submitted",
        null,
      )

      DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED -> buildApplicationAssessedDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED -> buildPersonArrivedDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED -> buildPersonNotArrivedDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED -> buildPersonDepartedDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE -> buildBookingNotMadeDescription(domainEventSummary)
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
     * In prior versions of the application if a request for placement was made
     * on the original application no placement_application was created or
     * linked to the relates placement_request.
     *
     * To allow us to report on that request for placement being withdrawn,
     * we tied the withdrawal events to the placement_request itself, instead
     * of a placement_application (Which didn't exist)
     *
     * This behaviour no longer exists because a placement application will always
     * exist to tie the withdrawal to.
     *
     * Regardless, we still need to show the description for these legacy
     * withdrawal events
     * */
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

  private fun DomainEventSummary.id(): UUID = UUID.fromString(this.id)
  private fun <T> GetCas1DomainEvent<T>?.describe(describe: (T) -> String?): String? = this?.let { describe(it.data) }
  private val StaffMember.name: String
    get() = "${this.forenames} ${this.surname}".trim()
}
