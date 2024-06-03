package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelinessEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ApplicationTimelinessDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.PlacementMetricsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.TierCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.PlacementMetricsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import java.time.ZoneOffset

class PlacementMetricsReportGenerator(
  applicationRows: List<ApplicationTimelinessEntity>,
  private val workingDayService: WorkingDayService,
) : ReportGenerator<TierCategory, PlacementMetricsReportRow, PlacementMetricsReportProperties>(PlacementMetricsReportRow::class) {
  private val timelinessEntries: List<ApplicationTimelinessDto>

  init {
    timelinessEntries = applicationRows.map {
      ApplicationTimelinessDto(
        id = it.getId(),
        tier = it.getTier(),
        applicationSubmittedAt = it.getApplicationSubmittedAt()?.atZone(ZoneOffset.UTC)?.toLocalDate(),
        bookingMadeAt = it.getBookingMadeAt()?.atZone(ZoneOffset.UTC)?.toLocalDate(),
        overallTimeliness = it.getOverallTimeliness(),
        placementMatchingTimeliness = it.getPlacementMatchingTimeliness(),
        overallTimelinessInWorkingDays = null,
      )
    }
  }

  override fun filter(properties: PlacementMetricsReportProperties): (TierCategory) -> Boolean = {
    true
  }

  override val convert: TierCategory.(properties: PlacementMetricsReportProperties) -> List<PlacementMetricsReportRow> = {
    val applications = when (this) {
      TierCategory.ALL -> timelinessEntries
      TierCategory.NONE -> timelinessEntries.filter { row -> row.tier == null }
      else -> timelinessEntries.filter { row -> row.tier == this.toString() }
    }

    val successfulRequests = applications.filter { row -> row.bookingMadeAt !== null }
    val averageTimeliness = successfulRequests
      .mapNotNull { row -> row.overallTimeliness }
      .average()
    val averagePlacementMatchingTimeliness = successfulRequests
      .mapNotNull { row -> row.placementMatchingTimeliness }
      .average()

    successfulRequests.map { row ->
      val start = row.applicationSubmittedAt
      val end = row.bookingMadeAt

      if (start != null && end != null) {
        row.overallTimelinessInWorkingDays = workingDayService.getWorkingDaysCount(
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
        timelinessSameDayAdmission = successfulRequests.count { it.overallTimeliness == 0 },
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
        timeliness366PlusCalendarDays = successfulRequests.count { it.overallTimeliness != null && it.overallTimeliness >= 366 },
      ),
    )
  }

  private fun countDifferenceInWorkingDays(requests: List<ApplicationTimelinessDto>, lowerBounds: Int, upperBounds: Int) = requests.count {
    it.overallTimelinessInWorkingDays in lowerBounds..upperBounds
  }

  private fun countDifferenceInCalendarDays(requests: List<ApplicationTimelinessDto>, lowerBounds: Int, upperBounds: Int) = requests.count {
    it.overallTimeliness in lowerBounds..upperBounds
  }
}
