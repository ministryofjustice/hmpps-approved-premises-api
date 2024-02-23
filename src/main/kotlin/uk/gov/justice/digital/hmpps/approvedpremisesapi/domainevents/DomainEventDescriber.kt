package uk.gov.justice.digital.hmpps.approvedpremisesapi.domainevents

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEventSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

@Component
class DomainEventDescriber(
  private val domainEventService: DomainEventService,
) {

  val cas1UiExtendedDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy")

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
      DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED -> "The booking had its arrival or departure date changed"
      DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN -> "The application was withdrawn"
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED -> buildAssessmentAppealedDescription(domainEventSummary)
      DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN -> buildPlacementApplicationWithdrawnDescription(domainEventSummary)
      else -> throw IllegalArgumentException("Cannot map ${domainEventSummary.type}, only CAS1 is currently supported")
    }
  }

  private fun buildApplicationAssessedDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getApplicationAssessedDomainEvent(domainEventSummary.id())
    return event.describe { "The application was assessed and ${it.eventDetails.decision.lowercase()}" }
  }

  private fun buildBookingMadeDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getBookingMadeEvent(domainEventSummary.id())
    return event.describe { "A booking was made for between ${it.eventDetails.arrivalOn} and ${it.eventDetails.departureOn}" }
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
    return event.describe { "A booking was not made for the placement request. The reason was: ${it.eventDetails.failureDescription}" }
  }

  private fun buildBookingCancelledDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getBookingCancelledEvent(domainEventSummary.id())
    return event.describe { "The booking was cancelled. The reason was: ${it.eventDetails.cancellationReason}" }
  }

  private fun buildAssessmentAppealedDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getAssessmentAppealedEvent(domainEventSummary.id())
    return event.describe { "The assessment was appealed and ${it.eventDetails.decision.value}. The reason was: ${it.eventDetails.decisionDetail}" }
  }

  private fun buildPlacementApplicationWithdrawnDescription(domainEventSummary: DomainEventSummary): String? {
    val event = domainEventService.getPlacementApplicationWithdrawnEvent(domainEventSummary.id())
    val dates = event?.data?.eventDetails?.placementDates ?: emptyList()
    return "A request for placement was withdrawn" +
      if (dates.isNotEmpty()) {
        " for dates " + dates.joinToString(", ") { "${it.startDate.format(cas1UiExtendedDateFormat)} to ${it.endDate.format(cas1UiExtendedDateFormat)}" }
      } else { "" }
  }

  private fun DomainEventSummary.id(): UUID = UUID.fromString(this.id)
  private fun <T> DomainEvent<T>?.describe(describe: (T) -> String?): String? = this?.let { describe(it.data) }
}
