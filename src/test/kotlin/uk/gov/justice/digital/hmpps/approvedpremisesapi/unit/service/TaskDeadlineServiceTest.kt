package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.TaskDeadlineService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.TimeService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayCountService
import java.time.OffsetDateTime

@SuppressWarnings("MagicNumber")
class TaskDeadlineServiceTest {
  private val taskDeadlineService = TaskDeadlineService(
    // this treats all weekends as non-working days with no bank holidays
    WorkingDayCountService(
      bankHolidaysProvider = { emptyList() },
      timeService = TimeService(),
    ),
  )

  @Nested
  inner class GetAssessmentDeadline {

    @Test
    fun `getDeadline for a non-CAS1 assessment returns null `() {
      val user = UserEntityFactory()
        .withDefaultProbationRegion()
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val result = taskDeadlineService.getDeadline(assessment)

      assertThat(result).isNull()
    }

    @ParameterizedTest
    @CsvSource(
      // Monday 3pm. 14 days later, given two weekends
      "2023-01-02T15:00:00Z,2023-01-16T00:00:00Z",
      // Friday 3pm. 14 days later, given two weekends
      "2023-01-06T15:00:00Z,2023-01-20T00:00:00Z",
    )
    fun `getDeadline for a standard assessment returns created date plus 10 working days`(
      createdAt: OffsetDateTime,
      expectedDeadline: OffsetDateTime,
    ) {
      val assessment = createAssessment(
        noticeType = Cas1ApplicationTimelinessCategory.standard,
        isEsap = false,
        createdAt = createdAt,
      )

      val result = taskDeadlineService.getDeadline(assessment)

      assertThat(result!!).isEqualTo(expectedDeadline)
    }

    @ParameterizedTest
    @CsvSource(
      // Monday 3pm. 2 days later
      "2023-01-02T15:00:00Z,2023-01-04T00:00:00Z",
      // Friday 3pm. 4 days later, given the weekend
      "2023-01-06T15:00:00Z,2023-01-10T00:00:00Z",
    )
    fun `getDeadline for a short notice assessment returns created date plus 2 working days`(
      createdAt: OffsetDateTime,
      expectedDeadline: OffsetDateTime,
    ) {
      val assessment = createAssessment(
        noticeType = Cas1ApplicationTimelinessCategory.shortNotice,
        isEsap = false,
        createdAt = createdAt,
      )
      val result = taskDeadlineService.getDeadline(assessment)

      assertThat(result!!).isEqualTo(expectedDeadline)
    }

    @ParameterizedTest
    @CsvSource(
      // Sunday 11am. Created date plus 2 hours
      "2023-01-01T11:00:00Z,2023-01-01T13:00:00Z",
      // Sunday 12:59am. Created date plus 2 hours
      "2023-01-01T12:59:59Z,2023-01-01T14:59:59Z",
      // Sunday 13:00pm. 11am Next working day
      "2023-01-01T13:00:00Z,2023-01-02T11:00:00Z",
      // Sunday 3pm. 11am Next working day
      "2023-01-01T14:00:00Z,2023-01-02T11:00:00Z",
      // Friday 3pm. 11am Next working day (after the weekend)
      "2023-01-06T14:00:00Z,2023-01-09T11:00:00Z",
    )
    fun `getDeadline for an emergency assessment, 2 hours or 11am next working day if after 1pm`(
      createdAt: OffsetDateTime,
      expectedDeadline: OffsetDateTime,
    ) {
      val assessment = createAssessment(
        noticeType = Cas1ApplicationTimelinessCategory.emergency,
        isEsap = false,
        createdAt = createdAt,
      )
      val result = taskDeadlineService.getDeadline(assessment)

      assertThat(result).isEqualTo(expectedDeadline)
    }
  }

  @Nested
  inner class GetPlacementRequestDeadline {

    @ParameterizedTest
    @CsvSource(
      // Sunday 3pm, 5 days later
      "2023-01-01T15:00:00Z,2023-01-06T00:00:00Z",
      // Monday 3pm. 7 days later, given the weekend
      "2023-01-02T15:00:00Z,2023-01-09T00:00:00Z",
      // Tuesday 3pm. 7 days later, given the weekend
      "2023-01-03T15:00:00Z,2023-01-10T00:00:00Z",
    )
    fun `getDeadline for a standard placement request returns created date plus 5 working days`(
      createdAt: OffsetDateTime,
      expectedDeadline: OffsetDateTime,
    ) {
      val placementRequest = createPlacementRequest(
        noticeType = Cas1ApplicationTimelinessCategory.standard,
        isEsap = false,
        createdAt = createdAt,
      )

      val result = taskDeadlineService.getDeadline(placementRequest)

      assertThat(result).isEqualTo(expectedDeadline)
    }

    @ParameterizedTest
    @CsvSource(
      // Monday 3pm. 2 days later
      "2023-01-02T15:00:00Z,2023-01-04T00:00:00Z",
      // Tuesday 3pm. 2 days later
      "2023-01-03T15:00:00Z,2023-01-05T00:00:00Z",
      // Friday 3pm. 4 days later given the weekend
      "2023-01-06T15:00:00Z,2023-01-10T00:00:00Z",
    )
    fun `getDeadline for a short notice placement request returns created date plus 2 working days `(
      createdAt: OffsetDateTime,
      expectedDeadline: OffsetDateTime,
    ) {
      val placementRequest = createPlacementRequest(
        noticeType = Cas1ApplicationTimelinessCategory.shortNotice,
        isEsap = false,
        createdAt = createdAt,
      )

      val result = taskDeadlineService.getDeadline(placementRequest)

      assertThat(result).isEqualTo(expectedDeadline)
    }

    @ParameterizedTest
    @CsvSource(
      // Monday 3pm. Same date/time
      "2023-01-02T15:00:00Z,2023-01-02T15:00:00Z",
      // Tuesday 3pm. Same date/time
      "2023-01-03T15:00:00Z,2023-01-03T15:00:00Z",
      // Friday 3pm. Same date/time
      "2023-01-06T15:35:00Z,2023-01-06T15:35:00Z",
      // Saturday 1pm. Same date/time
      "2023-01-07T13:00:00Z,2023-01-07T13:00:00Z",
    )
    fun `getDeadline for an emergency placement request returns created date`(
      createdAt: OffsetDateTime,
      expectedDeadline: OffsetDateTime,
    ) {
      val placementRequest = createPlacementRequest(
        noticeType = Cas1ApplicationTimelinessCategory.emergency,
        isEsap = false,
        createdAt = createdAt,
      )

      val result = taskDeadlineService.getDeadline(placementRequest)

      assertThat(result).isEqualTo(expectedDeadline)
    }

    @ParameterizedTest
    @CsvSource(
      // Monday 3pm. Same date/time
      "2023-01-02T15:00:00Z,2023-01-02T15:00:00Z",
      // Tuesday 3pm. Same date/time
      "2023-01-03T15:00:00Z,2023-01-03T15:00:00Z",
      // Friday 3pm. Same date/time
      "2023-01-06T15:35:00Z,2023-01-06T15:35:00Z",
      // Saturday 1pm. Same date/time
      "2023-01-07T13:00:00Z,2023-01-07T13:00:00Z",
    )
    fun `getDeadline for an ESAP placement request returns created date`(
      createdAt: OffsetDateTime,
      expectedDeadline: OffsetDateTime,
    ) {
      val placementRequest = createPlacementRequest(
        noticeType = Cas1ApplicationTimelinessCategory.standard,
        isEsap = true,
        createdAt = createdAt,
      )

      val result = taskDeadlineService.getDeadline(placementRequest)

      assertThat(result).isEqualTo(expectedDeadline)
    }
  }

  @Nested
  inner class GetPlacementApplicationDeadline {

    @ParameterizedTest
    @CsvSource(
      // Monday 3pm. 14 days later, given two weekends
      "2023-01-02T15:00:00Z,2023-01-16T00:00:00Z",
      // Friday 3pm. 14 days later, given two weekends
      "2023-01-06T15:00:00Z,2023-01-20T00:00:00Z",
    )
    fun `getDeadline for a standard placement application returns submitted date plus 10 working days`(
      submittedAt: OffsetDateTime,
      expectedDeadline: OffsetDateTime,
    ) {
      val placementApplication = createPlacementApplication(
        noticeType = Cas1ApplicationTimelinessCategory.standard,
        isEsap = false,
        submittedAt = submittedAt,
      )

      val result = taskDeadlineService.getDeadline(placementApplication)

      assertThat(result).isEqualTo(expectedDeadline)
    }

    @ParameterizedTest
    @CsvSource(
      // Monday 3pm. 14 days later, given two weekends
      "2023-01-02T15:00:00Z,2023-01-16T00:00:00Z",
      // Friday 3pm. 14 days later, given two weekends
      "2023-01-06T15:00:00Z,2023-01-20T00:00:00Z",
    )
    fun `getDeadline for a short notice placement application returns submitted date plus 10 working days`(
      submittedAt: OffsetDateTime,
      expectedDeadline: OffsetDateTime,
    ) {
      val placementApplication = createPlacementApplication(
        noticeType = Cas1ApplicationTimelinessCategory.shortNotice,
        isEsap = false,
        submittedAt = submittedAt,
      )

      val result = taskDeadlineService.getDeadline(placementApplication)

      assertThat(result).isEqualTo(expectedDeadline)
    }

    @ParameterizedTest
    @CsvSource(
      // Monday 3pm. 14 days later, given two weekends
      "2023-01-02T15:00:00Z,2023-01-16T00:00:00Z",
      // Friday 3pm. 14 days later, given two weekends
      "2023-01-06T15:00:00Z,2023-01-20T00:00:00Z",
    )
    fun `getDeadline for an emergency placement application returns submitted date plus 10 working days`(
      submittedAt: OffsetDateTime,
      expectedDeadline: OffsetDateTime,
    ) {
      val placementApplication = createPlacementApplication(
        noticeType = Cas1ApplicationTimelinessCategory.standard,
        isEsap = true,
        submittedAt = submittedAt,
      )

      val result = taskDeadlineService.getDeadline(placementApplication)

      assertThat(result).isEqualTo(expectedDeadline)
    }
  }

  private fun createApplication(noticeType: Cas1ApplicationTimelinessCategory, isEsap: Boolean): ApprovedPremisesApplicationEntity {
    val user = UserEntityFactory()
      .withDefaultProbationRegion()
      .produce()

    val application = spyk(
      ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withNoticeType(noticeType)
        .apply {
          if (isEsap) {
            withApType(ApprovedPremisesType.ESAP)
          } else {
            withApType(ApprovedPremisesType.NORMAL)
          }
        }
        .produce(),
    )

    return application
  }

  private fun createPlacementRequest(noticeType: Cas1ApplicationTimelinessCategory, isEsap: Boolean, createdAt: OffsetDateTime): PlacementRequestEntity {
    val assessment = createAssessment(noticeType, isEsap, createdAt)

    return PlacementRequestEntityFactory()
      .withApplication(assessment.application as ApprovedPremisesApplicationEntity)
      .withAssessment(assessment)
      .withPlacementRequirements(
        PlacementRequirementsEntityFactory()
          .withApplication(assessment.application as ApprovedPremisesApplicationEntity)
          .withAssessment(assessment)
          .produce(),
      )
      .withCreatedAt(createdAt)
      .produce()
  }

  private fun createAssessment(noticeType: Cas1ApplicationTimelinessCategory, isEsap: Boolean, createdAt: OffsetDateTime): ApprovedPremisesAssessmentEntity {
    val application = createApplication(noticeType, isEsap)

    return ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .withCreatedAt(createdAt)
      .produce()
  }

  private fun createPlacementApplication(
    noticeType: Cas1ApplicationTimelinessCategory,
    isEsap: Boolean,
    submittedAt: OffsetDateTime,
  ): PlacementApplicationEntity {
    val application = createApplication(noticeType, isEsap)

    return PlacementApplicationEntityFactory()
      .withApplication(application)
      .withCreatedByUser(application.createdByUser)
      .withCreatedAt(submittedAt)
      .withSubmittedAt(submittedAt)
      .produce()
  }
}
