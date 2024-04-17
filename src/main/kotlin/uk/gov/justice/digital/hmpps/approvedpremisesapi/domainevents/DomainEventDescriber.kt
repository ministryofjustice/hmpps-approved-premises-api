package uk.gov.justice.digital.hmpps.approvedpremisesapi.domainevents

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
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
import javax.xml.datatype.DatatypeConstants.DAYS

@Component
class DomainEventDescriber(
  private val domainEventService: DomainEventService,
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
      DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED -> "The placement had its arrival or departure date changed"
      DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN -> buildApplicationWithdrawnDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED -> buildAssessmentAppealedDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED -> buildAssessmentAllocatedDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN -> buildPlacementApplicationWithdrawnDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN -> buildMatchRequestWithdrawnDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED -> buildRequestForPlacementCreatedDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED -> buildPlacementApplicationAllocatedDescription(domainEventSummary)
      else -> throw IllegalArgumentException("Cannot map ${domainEventSummary.type}, only CAS1 is currently supported")
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
        "${it.eventDetails.arrivalOn.toUiFormat()} to ${it.eventDetails.departureOn.toUiFormat()}"
    }
  }

  private fun buildPersonArrivedDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getPersonArrivedEvent(domainEventSummary.id())
    return event.describe { "The person moved into the premises on ${LocalDate.ofInstant(it.eventDetails.arrivedAt, ZoneOffset.UTC)}" }
  }

  private fun buildPersonNotArrivedDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getPersonNotArrivedEvent(domainEventSummary.id())
    return event.describe { "The person was due to move into the premises on ${it.eventDetails.expectedArrivalOn} but did not arrive" }
  }

  private fun buildPersonDepartedDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getPersonDepartedEvent(domainEventSummary.id())
    return event.describe { "The person moved out of the premises on ${LocalDate.ofInstant(it.eventDetails.departedAt, ZoneOffset.UTC)}" }
  }

  private fun buildBookingNotMadeDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getBookingNotMadeEvent(domainEventSummary.id())
    return event.describe { "A placement was not made for the placement request. The reason was: ${it.eventDetails.failureDescription}" }
  }

  private fun buildBookingCancelledDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getBookingCancelledEvent(domainEventSummary.id())
    return event.describe { "The placement was cancelled. The reason was: '${it.eventDetails.cancellationReason}'" }
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
        } else { "" } +
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

  private fun buildRequestForPlacementDescription(expectedArrival: LocalDate, duration: Int): String {
    val endDate = expectedArrival.plusDays(duration.toLong())
    return "The placement request is for ${expectedArrival.toUiFormat()} to ${endDate.toUiFormat()} (${toWeekAndDayDurationString(duration)})"
  }

  private fun DomainEventSummary.id(): UUID = UUID.fromString(this.id)
  private fun <T> DomainEvent<T>?.describe(describe: (T) -> String?): String? = this?.let { describe(it.data) }
  private val StaffMember.name: String
    get() = "${this.forenames} ${this.surname}".trim()
}
