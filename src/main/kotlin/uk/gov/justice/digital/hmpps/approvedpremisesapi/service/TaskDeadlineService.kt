package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Service
class TaskDeadlineService(
  private val workingDayService: WorkingDayService,
) {
  fun getDeadline(assessment: AssessmentEntity): OffsetDateTime? {
    val application = assessment.application
    val effectiveApplicationCreatedAt = assessment.createdAt.slewedToWorkingPattern()

    return when {
      application !is ApprovedPremisesApplicationEntity ->
        null

      application.noticeType == Cas1ApplicationTimelinessCategory.emergency ->
        effectiveApplicationCreatedAt.plusHours(ASSESSMENT_EMERGENCY_TIMEFRAME_HOURS)

      application.noticeType == Cas1ApplicationTimelinessCategory.shortNotice ->
        addWorkingDays(effectiveApplicationCreatedAt, ASSESSMENT_SHORT_NOTICE_TIMEFRAME_WORKDAYS)

      else ->
        addWorkingDays(effectiveApplicationCreatedAt, ASSESSMENT_STANDARD_TIMEFRAME_WORKDAYS)
    }
  }

  fun getDeadline(placementRequest: PlacementRequestEntity): OffsetDateTime {
    val application = placementRequest.application
    val effectivePlacementRequestCreatedAt = placementRequest.createdAt.slewedToWorkingPattern()

    return when {
      application.noticeType == Cas1ApplicationTimelinessCategory.emergency ->
        effectivePlacementRequestCreatedAt

      application.noticeType == Cas1ApplicationTimelinessCategory.shortNotice ->
        addWorkingDays(effectivePlacementRequestCreatedAt, PLACEMENT_REQUEST_SHORT_NOTICE_TIMEFRAME_WORKDAYS)

      application.isEsapApplication ->
        effectivePlacementRequestCreatedAt

      else ->
        addWorkingDays(effectivePlacementRequestCreatedAt, PLACEMENT_REQUEST_STANDARD_TIMEFRAME_WORKDAYS)
    }
  }

  fun getDeadline(placementApplication: PlacementApplicationEntity): OffsetDateTime {
    val effectivePlacementApplicationSubmittedAt = placementApplication.submittedAt!!.slewedToWorkingPattern()

    return addWorkingDays(effectivePlacementApplicationSubmittedAt, PLACEMENT_APPLICATION_TIMEFRAME_WORKDAYS)
  }

  private fun addWorkingDays(date: OffsetDateTime, workingDays: Int): OffsetDateTime =
    workingDayService
      .addWorkingDays(date.toLocalDate(), workingDays)
      .atTime(date.toOffsetTime())

  private fun ZonedDateTime.isWorkingDay() = this.toLocalDate().isWorkingDay(workingDayService.bankHolidays)
  private fun ZonedDateTime.isBeforeSameWorkingDayDeadline() = this.toLocalTime().isBefore(SAME_WORKING_DAY_DEADLINE_TIME)

  private fun OffsetDateTime.slewedToWorkingPattern(): OffsetDateTime {
    val zonedDateTime = this.toZonedDateTime().withZoneSameInstant(GB_LOCAL_TIMEZONE)
    return if (zonedDateTime.isWorkingDay() && zonedDateTime.isBeforeSameWorkingDayDeadline()) {
      zonedDateTime.withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime()
    } else {
      workingDayService
        .nextWorkingDay(zonedDateTime.toLocalDate())
        .atTime(WORKING_DAY_START_TIME)
        .atZone(GB_LOCAL_TIMEZONE)
        .withZoneSameInstant(ZoneOffset.UTC)
        .toOffsetDateTime()
    }
  }

  companion object {
    private const val ASSESSMENT_STANDARD_TIMEFRAME_WORKDAYS = 10
    private const val ASSESSMENT_SHORT_NOTICE_TIMEFRAME_WORKDAYS = 2
    private const val ASSESSMENT_EMERGENCY_TIMEFRAME_HOURS = 2L
    private const val PLACEMENT_REQUEST_STANDARD_TIMEFRAME_WORKDAYS = 5
    private const val PLACEMENT_REQUEST_SHORT_NOTICE_TIMEFRAME_WORKDAYS = 2
    private const val PLACEMENT_APPLICATION_TIMEFRAME_WORKDAYS = 2

    private val SAME_WORKING_DAY_DEADLINE_TIME: LocalTime = LocalTime.of(13, 0)
    private val WORKING_DAY_START_TIME: LocalTime = LocalTime.of(9, 0)

    private val GB_LOCAL_TIMEZONE = ZoneId.of("Europe/London")
  }
}
