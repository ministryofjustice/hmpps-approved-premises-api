package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelinessEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.PlacementMetricsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.TierCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.PlacementMetricsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayCountService

class PlacementMetricsReportGenerator(
  private val applicationRows: List<ApplicationTimelinessEntity>,
  private val workingDayCountService: WorkingDayCountService,
) : ReportGenerator<TierCategory, PlacementMetricsReportRow, PlacementMetricsReportProperties>(PlacementMetricsReportRow::class) {
  override fun filter(properties: PlacementMetricsReportProperties): (TierCategory) -> Boolean = {
    true
  }

  override val convert: TierCategory.(properties: PlacementMetricsReportProperties) -> List<PlacementMetricsReportRow> = {
    val applications = when (this) {
      TierCategory.ALL -> applicationRows
      TierCategory.NONE -> applicationRows.filter { row -> row.getTier() == null }
      else -> applicationRows.filter { row -> row.getTier() == this.toString() }
    }

    val successfulRequests = applications.filter { row -> row.getBookingMadeAt() !== null }
    val averageTimeliness = successfulRequests.map { row -> row.getOverallTimeliness()!! }.average()
    val averagePlacementMatchingTimeliness = successfulRequests.map { row -> row.getPlacementMatchingTimeliness()!! }.average()

    successfulRequests.map { row ->
      val start = row.getApplicationSubmittedAt()?.toLocalDateTime()?.toLocalDate()
      val end = row.getBookingMadeAt()?.toLocalDateTime()?.toLocalDate()

      if (start != null && end != null) {
        row.overallTimelinessInWorkingDays = workingDayCountService.getWorkingDaysCount(
          start,
          end,
        )
      }
    }

    listOf(
      PlacementMetricsReportRow(
        tier = this,
        uniqueRequests = applications.size,
        successfulRequests = successfulRequests.size,
        percentageOfSuccessfulPlacements = if (successfulRequests.isEmpty()) {
          "N/A"
        } else {
          val percentage = (successfulRequests.size.toDouble() / applications.size) * 100
          String.format("%.1f%%", percentage)
        },
        averageTimeliness = String.format("%.1f days", averageTimeliness),
        averagePlacementMatchingTimeliness = String.format("%.1f days", averagePlacementMatchingTimeliness),
        timelinessSameDayAdmission = successfulRequests.count { it.getOverallTimeliness() == 0 },
        timeliness1To2WorkingDays = countDifferenceInWorkingDays(successfulRequests, 1, 2),
        timeliness3To5WorkingDays = countDifferenceInWorkingDays(successfulRequests, 3, 4),
        timeliness8To15CalendarDays = countDifferenceInCalendarDays(successfulRequests, 8, 15),
        timeliness16To30CalendarDays = countDifferenceInCalendarDays(successfulRequests, 16, 30),
        timeliness31To60CalendarDays = countDifferenceInCalendarDays(successfulRequests, 31, 60),
        timeliness61To90CalendarDays = countDifferenceInCalendarDays(successfulRequests, 61, 90),
        timeliness91To120CalendarDays = countDifferenceInCalendarDays(successfulRequests, 91, 120),
        timeliness121To150CalendarDays = countDifferenceInCalendarDays(successfulRequests, 121, 150),
        timeliness151To180CalendarDays = countDifferenceInCalendarDays(successfulRequests, 151, 180),
        timeliness181To275CalendarDays = countDifferenceInCalendarDays(successfulRequests, 181, 275),
        timeliness276To365CalendarDays = countDifferenceInCalendarDays(successfulRequests, 276, 365),
        timeliness366PlusCalendarDays = successfulRequests.count { it.getOverallTimeliness() != null && it.getOverallTimeliness()!! >= 366 },
      ),
    )
  }

  private fun countDifferenceInWorkingDays(requests: List<ApplicationTimelinessEntity>, lowerBounds: Int, upperBounds: Int) = requests.count {
    it.overallTimelinessInWorkingDays in lowerBounds..upperBounds
  }

  private fun countDifferenceInCalendarDays(requests: List<ApplicationTimelinessEntity>, lowerBounds: Int, upperBounds: Int) = requests.count {
    it.getOverallTimeliness() in lowerBounds..upperBounds
  }
}
