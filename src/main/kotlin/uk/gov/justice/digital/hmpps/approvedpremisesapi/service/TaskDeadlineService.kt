package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

@Service
class TaskDeadlineService(
  private val workingDayService: WorkingDayService,
) {
  fun getDeadline(assessment: AssessmentEntity): OffsetDateTime? {
    val application = assessment.application
    return when {
      application !is ApprovedPremisesApplicationEntity -> null
      application.noticeType == Cas1ApplicationTimelinessCategory.emergency -> emergencyAssessmentDueDateTime(assessment)
      application.noticeType == Cas1ApplicationTimelinessCategory.shortNotice -> addWorkingDays(assessment.createdAt, ASSESSMENT_SHORT_NOTICE_TIMEFRAME)
      else -> addWorkingDays(assessment.createdAt, ASSESSMENT_STANDARD_TIMEFRAME)
    }
  }

  fun getDeadline(placementRequest: PlacementRequestEntity): OffsetDateTime {
    val application = placementRequest.application
    return when {
      application.noticeType == Cas1ApplicationTimelinessCategory.emergency -> placementRequest.createdAt
      application.noticeType == Cas1ApplicationTimelinessCategory.shortNotice -> addWorkingDays(placementRequest.createdAt, PLACEMENT_REQUEST_SHORT_NOTICE_TIMEFRAME)
      application.isEsapApplication -> placementRequest.createdAt
      else -> addWorkingDays(placementRequest.createdAt, PLACEMENT_REQUEST_STANDARD_TIMEFRAME)
    }
  }

  fun getDeadline(placementApplication: PlacementApplicationEntity): OffsetDateTime {
    return addWorkingDays(placementApplication.submittedAt!!, PLACEMENT_APPLICATION_TIMEFRAME)
  }

  fun addWorkingDays(date: OffsetDateTime, workingDays: Int): OffsetDateTime = workingDayService.addWorkingDays(date.toLocalDate(), workingDays).toLocalDateTime()

  private fun emergencyAssessmentDueDateTime(assessment: AssessmentEntity): OffsetDateTime {
    return if (assessment.createdAt.hour < 13) {
      assessment.createdAt.plus(2.hours.toJavaDuration()).toInstant().atOffset(ZoneOffset.UTC)
    } else {
      val nextWorkingDay = workingDayService.nextWorkingDay(assessment.createdAt.toLocalDate())
      nextWorkingDay.atTime(11, 0).atOffset(ZoneOffset.UTC)
    }
  }

  companion object {
    const val ASSESSMENT_STANDARD_TIMEFRAME = 10
    const val ASSESSMENT_SHORT_NOTICE_TIMEFRAME = 2
    const val PLACEMENT_REQUEST_STANDARD_TIMEFRAME = 5
    const val PLACEMENT_REQUEST_SHORT_NOTICE_TIMEFRAME = 2
    const val PLACEMENT_APPLICATION_TIMEFRAME = 10
  }
}
