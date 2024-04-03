package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.reporting.generator

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApplicationTimelinessEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.PlacementMetricsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.PlacementMetricsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.TierCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.PlacementMetricsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import java.sql.Timestamp
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime

class PlacementMetricsReportGeneratorTest {
  private val mockWorkingDayService = mockk<WorkingDayService>()

  @Test
  fun `it returns a count of successful placements`() {
    val successfulRequests = (1..5).toList().map { createTimelinessEntity("A1", LocalDateTime.now(), LocalDateTime.now(), 1, 1) }
    val unSuccessfulRequests = (1..7).toList().map { createTimelinessEntity("A1", LocalDateTime.now(), null, null, null) }

    val entries = listOf(
      successfulRequests,
      unSuccessfulRequests,
    ).flatten().toMutableList()

    entries += createTimelinessEntity("B1", LocalDateTime.now(), LocalDateTime.now(), null, null)

    every { mockWorkingDayService.getWorkingDaysCount(any(), any()) } returns 1

    val results = PlacementMetricsReportGenerator(entries.toList(), mockWorkingDayService).createReport(
      listOf(TierCategory.A1),
      PlacementMetricsReportProperties(1, 2023),
    )

    assertThat(results[0][PlacementMetricsReportRow::tier]).isEqualTo(TierCategory.A1)
    assertThat(results[0][PlacementMetricsReportRow::uniqueRequests]).isEqualTo(entries.size - 1)
    assertThat(results[0][PlacementMetricsReportRow::successfulRequests]).isEqualTo(successfulRequests.size)
    assertThat(results[0][PlacementMetricsReportRow::percentageOfSuccessfulPlacements]).isEqualTo("41.7%")
  }

  @Test
  fun `it returns average timeliness for successful placements`() {
    val entries = listOf(
      createTimelinessEntity("A1", LocalDateTime.now(), null, null, null),
      createTimelinessEntity("A1", LocalDateTime.now(), LocalDateTime.now(), 5, 1),
      createTimelinessEntity("A1", LocalDateTime.now(), LocalDateTime.now(), 6, 2),
      createTimelinessEntity("A1", LocalDateTime.now(), LocalDateTime.now(), 7, 3),
      createTimelinessEntity("A1", LocalDateTime.now(), LocalDateTime.now(), 3, 4),
      createTimelinessEntity("A1", LocalDateTime.now(), LocalDateTime.now(), 1, 1),
      createTimelinessEntity("A1", LocalDateTime.now(), LocalDateTime.now(), null, null),
    )

    every { mockWorkingDayService.getWorkingDaysCount(any(), any()) } returns 1

    val results = PlacementMetricsReportGenerator(entries, mockWorkingDayService).createReport(
      listOf(TierCategory.A1),
      PlacementMetricsReportProperties(1, 2023),
    )

    assertThat(results[0][PlacementMetricsReportRow::averageTimeliness]).isEqualTo("4.4 days")
    assertThat(results[0][PlacementMetricsReportRow::averagePlacementMatchingTimeliness]).isEqualTo("2.2 days")
  }

  @Test
  fun `it returns timeliness counts by day`() {
    val timelinessSameDayAdmissionEntities = (1..randomInt(1, 10)).map {
      createTimelinessEntity("A1", LocalDateTime.now(), LocalDateTime.now(), 0, 0)
    }
    val timeliness1To2WorkingDaysEntities = createMultipleTimelinessEntities("A1", randomInt(1, 10), 1, 2)
    val timeliness3To5WorkingDaysEntities = createMultipleTimelinessEntities("A1", randomInt(1, 10), 3, 5)
    val timeliness8To15CalendarDaysEntities = createMultipleTimelinessEntities("A1", randomInt(1, 10), 8, 15)
    val timeliness16To30CalendarDaysEntities = createMultipleTimelinessEntities("A1", randomInt(1, 10), 16, 30)
    val timeliness31To60CalendarDaysEntities = createMultipleTimelinessEntities("A1", randomInt(1, 10), 31, 60)
    val timeliness61To90CalendarDaysEntities = createMultipleTimelinessEntities("A1", randomInt(1, 10), 61, 90)
    val timeliness91To120CalendarDaysEntities = createMultipleTimelinessEntities("A1", randomInt(1, 10), 91, 120)
    val timeliness121To150CalendarDaysEntities = createMultipleTimelinessEntities("A1", randomInt(1, 10), 121, 150)
    val timeliness151To180CalendarDaysEntities = createMultipleTimelinessEntities("A1", randomInt(1, 10), 151, 180)
    val timeliness181To275CalendarDaysEntities = createMultipleTimelinessEntities("A1", randomInt(1, 10), 181, 275)
    val timeliness276To365CalendarDaysEntities = createMultipleTimelinessEntities("A1", randomInt(1, 10), 276, 365)
    val timeliness366PlusCalendarDaysEntities = createMultipleTimelinessEntities("A1", randomInt(1, 10), 366, 9999)

    val entries = listOf(
      timelinessSameDayAdmissionEntities,
      timeliness1To2WorkingDaysEntities,
      timeliness3To5WorkingDaysEntities,
      timeliness8To15CalendarDaysEntities,
      timeliness16To30CalendarDaysEntities,
      timeliness31To60CalendarDaysEntities,
      timeliness61To90CalendarDaysEntities,
      timeliness91To120CalendarDaysEntities,
      timeliness121To150CalendarDaysEntities,
      timeliness151To180CalendarDaysEntities,
      timeliness181To275CalendarDaysEntities,
      timeliness276To365CalendarDaysEntities,
      timeliness366PlusCalendarDaysEntities,
    ).flatten()

    every { mockWorkingDayService.getWorkingDaysCount(any<LocalDate>(), any<LocalDate>()) } answers {
      Duration.between(LocalDateTime.of(firstArg(), LocalTime.MIDNIGHT), LocalDateTime.of(secondArg(), LocalTime.MIDNIGHT)).toDays().toInt()
    }

    val results = PlacementMetricsReportGenerator(entries, mockWorkingDayService).createReport(
      listOf(TierCategory.A1),
      PlacementMetricsReportProperties(1, 2023),
    )

    assertThat(results[0][PlacementMetricsReportRow::timelinessSameDayAdmission]).isEqualTo(timelinessSameDayAdmissionEntities.count())
    assertThat(results[0][PlacementMetricsReportRow::timeliness1To2WorkingDays]).isEqualTo(timeliness1To2WorkingDaysEntities.count())
    assertThat(results[0][PlacementMetricsReportRow::timeliness3To5WorkingDays]).isEqualTo(timeliness3To5WorkingDaysEntities.count())
    assertThat(results[0][PlacementMetricsReportRow::timeliness8To15CalendarDays]).isEqualTo(timeliness8To15CalendarDaysEntities.count())
    assertThat(results[0][PlacementMetricsReportRow::timeliness16To30CalendarDays]).isEqualTo(timeliness16To30CalendarDaysEntities.count())
    assertThat(results[0][PlacementMetricsReportRow::timeliness31To60CalendarDays]).isEqualTo(timeliness31To60CalendarDaysEntities.count())
    assertThat(results[0][PlacementMetricsReportRow::timeliness61To90CalendarDays]).isEqualTo(timeliness61To90CalendarDaysEntities.count())
    assertThat(results[0][PlacementMetricsReportRow::timeliness91To120CalendarDays]).isEqualTo(timeliness91To120CalendarDaysEntities.count())
    assertThat(results[0][PlacementMetricsReportRow::timeliness121To150CalendarDays]).isEqualTo(timeliness121To150CalendarDaysEntities.count())
    assertThat(results[0][PlacementMetricsReportRow::timeliness151To180CalendarDays]).isEqualTo(timeliness151To180CalendarDaysEntities.count())
    assertThat(results[0][PlacementMetricsReportRow::timeliness181To275CalendarDays]).isEqualTo(timeliness181To275CalendarDaysEntities.count())
    assertThat(results[0][PlacementMetricsReportRow::timeliness276To365CalendarDays]).isEqualTo(timeliness276To365CalendarDaysEntities.count())
    assertThat(results[0][PlacementMetricsReportRow::timeliness366PlusCalendarDays]).isEqualTo(timeliness366PlusCalendarDaysEntities.count())
  }

  private fun createMultipleTimelinessEntities(tier: String, num: Int, minTimeliness: Int, maxTimeliness: Int) = (1..num).map {
    val timeliness = randomInt(minTimeliness, maxTimeliness)
    val applicationSubmittedAt = OffsetDateTime.now().randomDateTimeBefore(timeliness).toLocalDateTime()
    createTimelinessEntity(tier, applicationSubmittedAt, applicationSubmittedAt.plusDays(timeliness.toLong()), timeliness, timeliness + 5)
  }

  private fun createTimelinessEntity(tier: String, applicationSubmittedAt: LocalDateTime?, bookingMadeAt: LocalDateTime?, overallTimeliness: Int?, placementMatchingTimeliness: Int?) =
    ApplicationTimelinessEntityFactory()
      .withTier(tier)
      .withApplicationSubmittedAt(if (applicationSubmittedAt != null) { Timestamp.valueOf(applicationSubmittedAt) } else { null })
      .withBookingMadeAt(if (bookingMadeAt != null) { Timestamp.valueOf(bookingMadeAt) } else { null })
      .withOverallTimeliness(overallTimeliness)
      .withPlacementMatchingTimeliness(placementMatchingTimeliness)
      .produce()
}
