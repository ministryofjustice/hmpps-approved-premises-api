package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmitted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ApprovedPremisesApplicationMetricsSummaryDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.DailyMetricReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ReportService
import java.time.LocalDate

class DailyMetricsReportGenerator(
  private val domainEvents: List<DomainEventEntity>,
  private val applications: List<ApprovedPremisesApplicationMetricsSummaryDto>,
  private val domainEventService: Cas1DomainEventService,
) : ReportGenerator<LocalDate, DailyMetricReportRow, Cas1ReportService.MonthSpecificReportParams>(DailyMetricReportRow::class) {
  override fun filter(properties: Cas1ReportService.MonthSpecificReportParams): (LocalDate) -> Boolean = {
    true
  }

  override val convert: LocalDate.(properties: Cas1ReportService.MonthSpecificReportParams) -> List<DailyMetricReportRow> = {
    val applicationsCreatedToday = applications.filter { application ->
      application.createdAt == this
    }

    val domainEventsToday = domainEvents.filter { domainEventEntity ->
      domainEventEntity.occurredAt.toLocalDate() == this
    }

    val applicationsSubmittedToday = domainEventsToday.filter { domainEventEntity ->
      domainEventEntity.type == DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED
    }.map { domainEventService.toDomainEvent(it, ApplicationSubmitted::class) }

    val assessmentsCompletedToday = domainEventsToday.filter { domainEventEntity ->
      domainEventEntity.type == DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED
    }.map { domainEventService.toDomainEvent(it, ApplicationAssessed::class) }

    val bookingsMadeToday = domainEventsToday.filter { domainEventEntity ->
      domainEventEntity.type == DomainEventType.APPROVED_PREMISES_BOOKING_MADE
    }.map { domainEventService.toDomainEvent(it, BookingMade::class) }

    listOf(
      DailyMetricReportRow(
        report_date = this,
        applications_started = applicationsCreatedToday.size,
        unique_users_starting_applications = applicationsCreatedToday.groupBy { application -> application.createdByUserId }.size,
        applications_submitted = applicationsSubmittedToday.size,
        unique_users_submitting_applications = applicationsSubmittedToday.groupBy { domainEvent -> domainEvent.data.eventDetails.submittedBy.staffMember.staffCode }.size,
        assessments_completed = assessmentsCompletedToday.size,
        unique_users_completing_assessments = assessmentsCompletedToday.groupBy { domainEvent -> domainEvent.data.eventDetails.assessedBy.staffMember!!.staffCode }.size,
        bookings_made = bookingsMadeToday.size,
        unique_users_making_bookings = bookingsMadeToday.groupBy { domainEvent -> domainEvent.data.eventDetails.bookedBy }.size,
      ),
    )
  }
}
