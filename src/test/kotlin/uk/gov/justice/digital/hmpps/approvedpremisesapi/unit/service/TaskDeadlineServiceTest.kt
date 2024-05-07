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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import java.time.OffsetDateTime

class TaskDeadlineServiceTest {
  private val taskDeadlineService = TaskDeadlineService(
    // this treats all weekends as non-working days with no bank holidays
    WorkingDayService(
      bankHolidaysProvider = { emptyList() },
      timeService = TimeService(),
    ),
  )

  @Nested
  inner class GetAssessmentDeadline {

    @Test
    fun `getDeadline for a non-CAS1 assessment returns null`() {
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
      // Winter (GMT/UTC), i.e. noon local time is at 12:00Z
      // Sunday 11am. 10 working days after 9am on the next working day (Monday), so two weeks on Monday
      "2023-01-01T11:00:00Z,2023-01-16T09:00:00Z",
      // Sunday 12:59pm. 10 working days after 9am on the next working day (Monday), so two weeks on Monday
      "2023-01-01T12:59:59Z,2023-01-16T09:00:00Z",
      // Sunday 1pm. 10 working days after 9am on the next working day (Monday), so two weeks on Monday
      "2023-01-01T13:00:00Z,2023-01-16T09:00:00Z",
      // Sunday 2pm. 10 working days after 9am on the next working day (Monday), so two weeks on Monday
      "2023-01-01T14:00:00Z,2023-01-16T09:00:00Z",
      // Monday 11am. 10 working days later, so two weeks on Monday
      "2023-01-02T11:00:00Z,2023-01-16T11:00:00Z",
      // Monday 12:59pm. 10 working days later, so two weeks on Monday
      "2023-01-02T12:59:59Z,2023-01-16T12:59:59Z",
      // Monday 1pm. 10 working days after 9am on the next working day (Tuesday), so two weeks on Tuesday
      "2023-01-02T13:00:00Z,2023-01-17T09:00:00Z",
      // Monday 2pm. 10 working days after 9am on the next working day (Tuesday), so two weeks on Tuesday
      "2023-01-02T14:00:00Z,2023-01-17T09:00:00Z",
      // Thursday 2pm. 10 working days after 9am on the next working day (Friday), so two weeks on Friday
      "2023-01-05T14:00:00Z,2023-01-20T09:00:00Z",
      // Friday 2pm. 10 working days after 9am on the next working day (Monday), so two weeks on Monday
      "2023-01-06T14:00:00Z,2023-01-23T09:00:00Z",

      // Summer (BST/UTC+1), i.e. noon local time is at 11:00Z
      // Sunday 11am. 10 working days after 9am on the next working day (Monday), so two weeks on Monday
      "2023-06-04T10:00:00Z,2023-06-19T08:00:00Z",
      // Sunday 12:59pm. 10 working days after 9am on the next working day (Monday), so two weeks on Monday
      "2023-06-04T11:59:59Z,2023-06-19T08:00:00Z",
      // Sunday 1pm. 10 working days after 9am on the next working day (Monday), so two weeks on Monday
      "2023-06-04T12:00:00Z,2023-06-19T08:00:00Z",
      // Sunday 2pm. 10 working days after 9am on the next working day (Monday), so two weeks on Monday
      "2023-06-04T13:00:00Z,2023-06-19T08:00:00Z",
      // Monday 11am. 10 working days later, so two weeks on Monday
      "2023-06-05T10:00:00Z,2023-06-19T10:00:00Z",
      // Monday 12:59pm. 10 working days later, so two weeks on Monday
      "2023-06-05T11:59:59Z,2023-06-19T11:59:59Z",
      // Monday 1pm. 10 working days after 9am on the next working day (Tuesday), so two weeks on Tuesday
      "2023-06-05T12:00:00Z,2023-06-20T08:00:00Z",
      // Monday 2pm. 10 working days after 9am on the next working day (Tuesday), so two weeks on Tuesday
      "2023-06-05T13:00:00Z,2023-06-20T08:00:00Z",
      // Thursday 2pm. 10 working days after 9am on the next working day (Friday), so two weeks on Friday
      "2023-06-08T13:00:00Z,2023-06-23T08:00:00Z",
      // Friday 2pm. 10 working days after 9am on the next working day (Monday), so two weeks on Monday
      "2023-06-09T13:00:00Z,2023-06-26T08:00:00Z",

      // Multi-timezone drifting!!
      // GMT -> BST: Friday 1pm. 10 working days after 9am on the next working day (Monday), so two weeks on Monday
      "2023-03-24T13:00:00Z,2023-04-10T08:00:00Z",
      // BST -> GMT: Friday 1pm. 10 working days after 9am on the next working day (Monday), so two weeks on Monday
      "2023-10-27T12:00:00Z,2023-11-13T09:00:00Z",
    )
    fun `getDeadline for a standard assessment returns created date plus 10 working days, or 10 working days after 9am on the next working day if after 1pm or a non-working day`(
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
      // Winter (GMT/UTC), i.e. noon local time is at 12:00Z
      // Sunday 11am. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-01T11:00:00Z,2023-01-04T09:00:00Z",
      // Sunday 12:59pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-01T12:59:59Z,2023-01-04T09:00:00Z",
      // Sunday 1pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-01T13:00:00Z,2023-01-04T09:00:00Z",
      // Sunday 2pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-01T14:00:00Z,2023-01-04T09:00:00Z",
      // Monday 11am. 2 working days later (Wednesday)
      "2023-01-02T11:00:00Z,2023-01-04T11:00:00Z",
      // Monday 12:59pm. 2 working days later (Wednesday)
      "2023-01-02T12:59:59Z,2023-01-04T12:59:59Z",
      // Monday 1pm. 2 working days after 9am on the next working day (Tuesday), so 9am Thursday
      "2023-01-02T13:00:00Z,2023-01-05T09:00:00Z",
      // Monday 2pm. 2 working days after 9am on the next working day (Tuesday), so 9am Thursday
      "2023-01-02T14:00:00Z,2023-01-05T09:00:00Z",
      // Thursday 2pm. 2 working days after 9am on the next working day (Friday), so 9am Tuesday
      "2023-01-05T14:00:00Z,2023-01-10T09:00:00Z",
      // Friday 2pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-06T14:00:00Z,2023-01-11T09:00:00Z",

      // Summer (BST/UTC+1), i.e. noon local time is at 11:00Z
      // Sunday 11am. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-04T10:00:00Z,2023-06-07T08:00:00Z",
      // Sunday 12:59pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-04T11:59:59Z,2023-06-07T08:00:00Z",
      // Sunday 1pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-04T12:00:00Z,2023-06-07T08:00:00Z",
      // Sunday 2pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-04T13:00:00Z,2023-06-07T08:00:00Z",
      // Monday 11am. 2 working days later (Wednesday)
      "2023-06-05T10:00:00Z,2023-06-07T10:00:00Z",
      // Monday 12:59pm. 2 working days later (Wednesday)
      "2023-06-05T11:59:59Z,2023-06-07T11:59:59Z",
      // Monday 1pm. 2 working days after 9am on the next working day (Tuesday), so 9am Thursday
      "2023-06-05T12:00:00Z,2023-06-08T08:00:00Z",
      // Monday 2pm. 2 working days after 9am on the next working day (Tuesday), so 9am Thursday
      "2023-06-05T13:00:00Z,2023-06-08T08:00:00Z",
      // Thursday 2pm. 2 working days after 9am on the next working day (Friday), so 9am Tuesday
      "2023-06-08T13:00:00Z,2023-06-13T08:00:00Z",
      // Friday 2pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-09T13:00:00Z,2023-06-14T08:00:00Z",

      // Multi-timezone drifting!!
      // GMT -> BST: Friday 1pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-03-24T13:00:00Z,2023-03-29T08:00:00Z",
      // BST -> GMT: Friday 1pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-10-27T12:00:00Z,2023-11-01T09:00:00Z",
    )
    fun `getDeadline for a short notice assessment returns created date plus 2 working days, or 2 working days after 9am on the next working day if after 1pm or a non-working day`(
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
      // Winter (GMT/UTC), i.e. noon local time is at 12:00Z
      // Sunday 11am. 11am next working day (Monday)
      "2023-01-01T11:00:00Z,2023-01-02T11:00:00Z",
      // Sunday 12:59pm. 11am next working day (Monday)
      "2023-01-01T12:59:59Z,2023-01-02T11:00:00Z",
      // Sunday 1pm. 11am next working day (Monday)
      "2023-01-01T13:00:00Z,2023-01-02T11:00:00Z",
      // Sunday 2pm. 11am next working day (Monday)
      "2023-01-01T14:00:00Z,2023-01-02T11:00:00Z",
      // Monday 11am. Created date plus 2 hours
      "2023-01-02T11:00:00Z,2023-01-02T13:00:00Z",
      // Monday 12:59pm. Created date plus 2 hours
      "2023-01-02T12:59:59Z,2023-01-02T14:59:59Z",
      // Monday 1pm. next working day (Tuesday)
      "2023-01-02T13:00:00Z,2023-01-03T11:00:00Z",
      // Monday 2pm. 11am next working day (Tuesday)
      "2023-01-02T14:00:00Z,2023-01-03T11:00:00Z",
      // Friday 2pm. 11am next working day (Monday)
      "2023-01-06T14:00:00Z,2023-01-09T11:00:00Z",

      // Summer (BST/UTC+1), i.e. noon local time is at 11:00Z
      // Sunday 11am. 11am next working day (Monday)
      "2023-06-04T10:00:00Z,2023-06-05T10:00:00Z",
      // Sunday 12:59pm. 11am next working day (Monday)
      "2023-06-04T11:59:59Z,2023-06-05T10:00:00Z",
      // Sunday 1pm. 11am next working day (Monday)
      "2023-06-04T12:00:00Z,2023-06-05T10:00:00Z",
      // Sunday 2pm. 11am next working day (Monday)
      "2023-06-04T13:00:00Z,2023-06-05T10:00:00Z",
      // Monday 11am. Created date plus 2 hours
      "2023-06-05T10:00:00Z,2023-06-05T12:00:00Z",
      // Monday 12:59pm. Created date plus 2 hours
      "2023-06-05T11:59:59Z,2023-06-05T13:59:59Z",
      // Monday 1pm. 11am next working day (Tuesday)
      "2023-06-05T12:00:00Z,2023-06-06T10:00:00Z",
      // Monday 2pm. 11am next working day (Tuesday)
      "2023-06-05T13:00:00Z,2023-06-06T10:00:00Z",
      // Friday 2pm. 11am next working day (Monday)
      "2023-06-09T13:00:00Z,2023-06-12T10:00:00Z",

      // Multi-timezone drifting!!
      // GMT -> BST: Friday 1pm. 11am next working day (Monday)
      "2023-03-24T13:00:00Z,2023-03-27T10:00:00Z",
      // BST -> GMT: Friday 1pm. 11am next working day (Monday)
      "2023-10-27T12:00:00Z,2023-10-30T11:00:00Z",
    )
    fun `getDeadline for an emergency assessment returns created date plus 2 hours, or 11am next working day if after 1pm or a non-working day`(
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
      // Winter (GMT/UTC), i.e. noon local time is at 12:00Z
      // Sunday 11am. 5 working days after 9am on the next working day (Monday), so a week on Monday
      "2023-01-01T11:00:00Z,2023-01-09T09:00:00Z",
      // Sunday 12:59pm. 5 working days after 9am on the next working day (Monday), so a week on Monday
      "2023-01-01T12:59:59Z,2023-01-09T09:00:00Z",
      // Sunday 1pm. 5 working days after 9am on the next working day (Monday), so a week on Monday
      "2023-01-01T13:00:00Z,2023-01-09T09:00:00Z",
      // Sunday 2pm. 5 working days after 9am on the next working day (Monday), so a week on Monday
      "2023-01-01T14:00:00Z,2023-01-09T09:00:00Z",
      // Monday 11am. 5 working days later, so a week on Monday
      "2023-01-02T11:00:00Z,2023-01-09T11:00:00Z",
      // Monday 12:59pm. 5 working days later, so a week on Monday
      "2023-01-02T12:59:59Z,2023-01-09T12:59:59Z",
      // Monday 1pm. 5 working days after 9am on the next working day (Tuesday), so a week on Tuesday
      "2023-01-02T13:00:00Z,2023-01-10T09:00:00Z",
      // Monday 2pm. 5 working days after 9am on the next working day (Tuesday), so a week on Tuesday
      "2023-01-02T14:00:00Z,2023-01-10T09:00:00Z",
      // Thursday 2pm. 5 working days after 9am on the next working day (Friday), so a week on Friday
      "2023-01-05T14:00:00Z,2023-01-13T09:00:00Z",
      // Friday 2pm. 5 working days after 9am on the next working day (Monday), so a week on Monday
      "2023-01-06T14:00:00Z,2023-01-16T09:00:00Z",

      // Summer (BST/UTC+1), i.e. noon local time is at 11:00Z
      // Sunday 11am. 5 working days after 9am on the next working day (Monday), so a week on Monday
      "2023-06-04T10:00:00Z,2023-06-12T08:00:00Z",
      // Sunday 12:59pm. 5 working days after 9am on the next working day (Monday), so a week on Monday
      "2023-06-04T11:59:59Z,2023-06-12T08:00:00Z",
      // Sunday 1pm. 5 working days after 9am on the next working day (Monday), so a week on Monday
      "2023-06-04T12:00:00Z,2023-06-12T08:00:00Z",
      // Sunday 2pm. 5 working days after 9am on the next working day (Monday), so a week on Monday
      "2023-06-04T13:00:00Z,2023-06-12T08:00:00Z",
      // Monday 11am. 5 working days later, so a week on Monday
      "2023-06-05T10:00:00Z,2023-06-12T10:00:00Z",
      // Monday 12:59pm. 5 working days later, so a week on Monday
      "2023-06-05T11:59:59Z,2023-06-12T11:59:59Z",
      // Monday 1pm. 5 working days after 9am on the next working day (Tuesday), so a week on Tuesday
      "2023-06-05T12:00:00Z,2023-06-13T08:00:00Z",
      // Monday 2pm. 5 working days after 9am on the next working day (Tuesday), so a week on Tuesday
      "2023-06-05T13:00:00Z,2023-06-13T08:00:00Z",
      // Thursday 2pm. 5 working days after 9am on the next working day (Friday), so a week on Friday
      "2023-06-08T13:00:00Z,2023-06-16T08:00:00Z",
      // Friday 2pm. 5 working days after 9am on the next working day (Monday), so a week on Monday
      "2023-06-09T13:00:00Z,2023-06-19T08:00:00Z",

      // Multi-timezone drifting!!
      // GMT -> BST: Friday 1pm. 5 working days after 9am on the next working day (Monday), so a week on Monday
      "2023-03-24T13:00:00Z,2023-04-03T08:00:00Z",
      // BST -> GMT: Friday 1pm. 5 working days after 9am on the next working day (Monday), so a week on Monday
      "2023-10-27T12:00:00Z,2023-11-06T09:00:00Z",
    )
    fun `getDeadline for a standard placement request returns created date plus 5 working days, or 5 working days after 9am on the next working day if after 1pm or a non-working day`(
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
      // Winter (GMT/UTC), i.e. noon local time is at 12:00Z
      // Sunday 11am. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-01T11:00:00Z,2023-01-04T09:00:00Z",
      // Sunday 12:59pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-01T12:59:59Z,2023-01-04T09:00:00Z",
      // Sunday 1pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-01T13:00:00Z,2023-01-04T09:00:00Z",
      // Sunday 2pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-01T14:00:00Z,2023-01-04T09:00:00Z",
      // Monday 11am. 2 working days later (Wednesday)
      "2023-01-02T11:00:00Z,2023-01-04T11:00:00Z",
      // Monday 12:59pm. 2 working days later (Wednesday)
      "2023-01-02T12:59:59Z,2023-01-04T12:59:59Z",
      // Monday 1pm. 2 working days after 9am on the next working day (Tuesday), so 9am Thursday
      "2023-01-02T13:00:00Z,2023-01-05T09:00:00Z",
      // Monday 2pm. 2 working days after 9am on the next working day (Tuesday), so 9am Thursday
      "2023-01-02T14:00:00Z,2023-01-05T09:00:00Z",
      // Thursday 2pm. 2 working days after 9am on the next working day (Friday), so 9am Tuesday
      "2023-01-05T14:00:00Z,2023-01-10T09:00:00Z",
      // Friday 2pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-06T14:00:00Z,2023-01-11T09:00:00Z",

      // Summer (BST/UTC+1), i.e. noon local time is at 11:00Z
      // Sunday 11am. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-04T10:00:00Z,2023-06-07T08:00:00Z",
      // Sunday 12:59pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-04T11:59:59Z,2023-06-07T08:00:00Z",
      // Sunday 1pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-04T12:00:00Z,2023-06-07T08:00:00Z",
      // Sunday 2pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-04T13:00:00Z,2023-06-07T08:00:00Z",
      // Monday 11am. 2 working days later (Wednesday)
      "2023-06-05T10:00:00Z,2023-06-07T10:00:00Z",
      // Monday 12:59pm. 2 working days later (Wednesday)
      "2023-06-05T11:59:59Z,2023-06-07T11:59:59Z",
      // Monday 1pm. 2 working days after 9am on the next working day (Tuesday), so 9am Thursday
      "2023-06-05T12:00:00Z,2023-06-08T08:00:00Z",
      // Monday 2pm. 2 working days after 9am on the next working day (Tuesday), so 9am Thursday
      "2023-06-05T13:00:00Z,2023-06-08T08:00:00Z",
      // Thursday 2pm. 2 working days after 9am on the next working day (Friday), so 9am Tuesday
      "2023-06-08T13:00:00Z,2023-06-13T08:00:00Z",
      // Friday 2pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-09T13:00:00Z,2023-06-14T08:00:00Z",

      // Multi-timezone drifting!!
      // GMT -> BST: Friday 1pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-03-24T13:00:00Z,2023-03-29T08:00:00Z",
      // BST -> GMT: Friday 1pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-10-27T12:00:00Z,2023-11-01T09:00:00Z",
    )
    fun `getDeadline for a short notice placement request returns created date plus 2 working days, or 2 working days after 9am on the next working day if after 1pm or a non-working day`(
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
      // Winter (GMT/UTC), i.e. noon local time is at 12:00Z
      // Sunday 11am. 9am next working day (Monday)
      "2023-01-01T11:00:00Z,2023-01-02T09:00:00Z",
      // Sunday 12:59pm. 9am next working day (Monday)
      "2023-01-01T12:59:59Z,2023-01-02T09:00:00Z",
      // Sunday 1pm. 9am next working day (Monday)
      "2023-01-01T13:00:00Z,2023-01-02T09:00:00Z",
      // Sunday 2pm. 9am next working day (Monday)
      "2023-01-01T14:00:00Z,2023-01-02T09:00:00Z",
      // Monday 11am. Created date (immediately)
      "2023-01-02T11:00:00Z,2023-01-02T11:00:00Z",
      // Monday 12:59pm. Created date (immediately)
      "2023-01-02T12:59:59Z,2023-01-02T12:59:59Z",
      // Monday 1pm. next working day (Tuesday)
      "2023-01-02T13:00:00Z,2023-01-03T09:00:00Z",
      // Monday 2pm. 9am next working day (Tuesday)
      "2023-01-02T14:00:00Z,2023-01-03T09:00:00Z",
      // Friday 2pm. 9am next working day (Monday)
      "2023-01-06T14:00:00Z,2023-01-09T09:00:00Z",

      // Summer (BST/UTC+1), i.e. noon local time is at 11:00Z
      // Sunday 11am. 9am next working day (Monday)
      "2023-06-04T10:00:00Z,2023-06-05T08:00:00Z",
      // Sunday 12:59pm. 9am next working day (Monday)
      "2023-06-04T11:59:59Z,2023-06-05T08:00:00Z",
      // Sunday 1pm. 9am next working day (Monday)
      "2023-06-04T12:00:00Z,2023-06-05T08:00:00Z",
      // Sunday 2pm. 9am next working day (Monday)
      "2023-06-04T13:00:00Z,2023-06-05T08:00:00Z",
      // Monday 11am. Created date (immediately)
      "2023-06-05T10:00:00Z,2023-06-05T10:00:00Z",
      // Monday 12:59pm. Created date (immediately)
      "2023-06-05T11:59:59Z,2023-06-05T11:59:59Z",
      // Monday 1pm. 9am next working day (Tuesday)
      "2023-06-05T12:00:00Z,2023-06-06T08:00:00Z",
      // Monday 2pm. 9am next working day (Tuesday)
      "2023-06-05T13:00:00Z,2023-06-06T08:00:00Z",
      // Friday 2pm. 9am next working day (Monday)
      "2023-06-09T13:00:00Z,2023-06-12T08:00:00Z",

      // Multi-timezone drifting!!
      // GMT -> BST: Friday 1pm. 9am next working day (Monday)
      "2023-03-24T13:00:00Z,2023-03-27T08:00:00Z",
      // BST -> GMT: Friday 1pm. 9am next working day (Monday)
      "2023-10-27T12:00:00Z,2023-10-30T09:00:00Z",
    )
    fun `getDeadline for an emergency placement request returns created date, or 9am on the next working day if after 1pm or a non-working day`(
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
      // Winter (GMT/UTC), i.e. noon local time is at 12:00Z
      // Sunday 11am. 9am next working day (Monday)
      "2023-01-01T11:00:00Z,2023-01-02T09:00:00Z",
      // Sunday 12:59pm. 9am next working day (Monday)
      "2023-01-01T12:59:59Z,2023-01-02T09:00:00Z",
      // Sunday 1pm. 9am next working day (Monday)
      "2023-01-01T13:00:00Z,2023-01-02T09:00:00Z",
      // Sunday 2pm. 9am next working day (Monday)
      "2023-01-01T14:00:00Z,2023-01-02T09:00:00Z",
      // Monday 11am. Created date (immediately)
      "2023-01-02T11:00:00Z,2023-01-02T11:00:00Z",
      // Monday 12:59pm. Created date (immediately)
      "2023-01-02T12:59:59Z,2023-01-02T12:59:59Z",
      // Monday 1pm. next working day (Tuesday)
      "2023-01-02T13:00:00Z,2023-01-03T09:00:00Z",
      // Monday 2pm. 9am next working day (Tuesday)
      "2023-01-02T14:00:00Z,2023-01-03T09:00:00Z",
      // Friday 2pm. 9am next working day (Monday)
      "2023-01-06T14:00:00Z,2023-01-09T09:00:00Z",

      // Summer (BST/UTC+1), i.e. noon local time is at 11:00Z
      // Sunday 11am. 9am next working day (Monday)
      "2023-06-04T10:00:00Z,2023-06-05T08:00:00Z",
      // Sunday 12:59pm. 9am next working day (Monday)
      "2023-06-04T11:59:59Z,2023-06-05T08:00:00Z",
      // Sunday 1pm. 9am next working day (Monday)
      "2023-06-04T12:00:00Z,2023-06-05T08:00:00Z",
      // Sunday 2pm. 9am next working day (Monday)
      "2023-06-04T13:00:00Z,2023-06-05T08:00:00Z",
      // Monday 11am. Created date (immediately)
      "2023-06-05T10:00:00Z,2023-06-05T10:00:00Z",
      // Monday 12:59pm. Created date (immediately)
      "2023-06-05T11:59:59Z,2023-06-05T11:59:59Z",
      // Monday 1pm. 9am next working day (Tuesday)
      "2023-06-05T12:00:00Z,2023-06-06T08:00:00Z",
      // Monday 2pm. 9am next working day (Tuesday)
      "2023-06-05T13:00:00Z,2023-06-06T08:00:00Z",
      // Friday 2pm. 9am next working day (Monday)
      "2023-06-09T13:00:00Z,2023-06-12T08:00:00Z",

      // Multi-timezone drifting!!
      // GMT -> BST: Friday 1pm. 9am next working day (Monday)
      "2023-03-24T13:00:00Z,2023-03-27T08:00:00Z",
      // BST -> GMT: Friday 1pm. 9am next working day (Monday)
      "2023-10-27T12:00:00Z,2023-10-30T09:00:00Z",
    )
    fun `getDeadline for an ESAP placement request returns created date, or 9am on the next working day if after 1pm or a non-working day`(
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
      // Winter (GMT/UTC), i.e. noon local time is at 12:00Z
      // Sunday 11am. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-01T11:00:00Z,2023-01-04T09:00:00Z",
      // Sunday 12:59pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-01T12:59:59Z,2023-01-04T09:00:00Z",
      // Sunday 1pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-01T13:00:00Z,2023-01-04T09:00:00Z",
      // Sunday 2pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-01T14:00:00Z,2023-01-04T09:00:00Z",
      // Monday 11am. 2 working days later (Wednesday)
      "2023-01-02T11:00:00Z,2023-01-04T11:00:00Z",
      // Monday 12:59pm. 2 working days later (Wednesday)
      "2023-01-02T12:59:59Z,2023-01-04T12:59:59Z",
      // Monday 1pm. 2 working days after 9am on the next working day (Tuesday), so 9am Thursday
      "2023-01-02T13:00:00Z,2023-01-05T09:00:00Z",
      // Monday 2pm. 2 working days after 9am on the next working day (Tuesday), so 9am Thursday
      "2023-01-02T14:00:00Z,2023-01-05T09:00:00Z",
      // Thursday 2pm. 2 working days after 9am on the next working day (Friday), so 9am Tuesday
      "2023-01-05T14:00:00Z,2023-01-10T09:00:00Z",
      // Friday 2pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-06T14:00:00Z,2023-01-11T09:00:00Z",

      // Summer (BST/UTC+1), i.e. noon local time is at 11:00Z
      // Sunday 11am. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-04T10:00:00Z,2023-06-07T08:00:00Z",
      // Sunday 12:59pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-04T11:59:59Z,2023-06-07T08:00:00Z",
      // Sunday 1pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-04T12:00:00Z,2023-06-07T08:00:00Z",
      // Sunday 2pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-04T13:00:00Z,2023-06-07T08:00:00Z",
      // Monday 11am. 2 working days later (Wednesday)
      "2023-06-05T10:00:00Z,2023-06-07T10:00:00Z",
      // Monday 12:59pm. 2 working days later (Wednesday)
      "2023-06-05T11:59:59Z,2023-06-07T11:59:59Z",
      // Monday 1pm. 2 working days after 9am on the next working day (Tuesday), so 9am Thursday
      "2023-06-05T12:00:00Z,2023-06-08T08:00:00Z",
      // Monday 2pm. 2 working days after 9am on the next working day (Tuesday), so 9am Thursday
      "2023-06-05T13:00:00Z,2023-06-08T08:00:00Z",
      // Thursday 2pm. 2 working days after 9am on the next working day (Friday), so 9am Tuesday
      "2023-06-08T13:00:00Z,2023-06-13T08:00:00Z",
      // Friday 2pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-09T13:00:00Z,2023-06-14T08:00:00Z",

      // Multi-timezone drifting!!
      // GMT -> BST: Friday 1pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-03-24T13:00:00Z,2023-03-29T08:00:00Z",
      // BST -> GMT: Friday 1pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-10-27T12:00:00Z,2023-11-01T09:00:00Z",
    )
    fun `getDeadline for a standard placement application returns submitted date plus 2 working days, or 2 working days after 9am on the next working day if after 1pm or a non-working day`(
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
      // Winter (GMT/UTC), i.e. noon local time is at 12:00Z
      // Sunday 11am. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-01T11:00:00Z,2023-01-04T09:00:00Z",
      // Sunday 12:59pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-01T12:59:59Z,2023-01-04T09:00:00Z",
      // Sunday 1pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-01T13:00:00Z,2023-01-04T09:00:00Z",
      // Sunday 2pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-01T14:00:00Z,2023-01-04T09:00:00Z",
      // Monday 11am. 2 working days later (Wednesday)
      "2023-01-02T11:00:00Z,2023-01-04T11:00:00Z",
      // Monday 12:59pm. 2 working days later (Wednesday)
      "2023-01-02T12:59:59Z,2023-01-04T12:59:59Z",
      // Monday 1pm. 2 working days after 9am on the next working day (Tuesday), so 9am Thursday
      "2023-01-02T13:00:00Z,2023-01-05T09:00:00Z",
      // Monday 2pm. 2 working days after 9am on the next working day (Tuesday), so 9am Thursday
      "2023-01-02T14:00:00Z,2023-01-05T09:00:00Z",
      // Thursday 2pm. 2 working days after 9am on the next working day (Friday), so 9am Tuesday
      "2023-01-05T14:00:00Z,2023-01-10T09:00:00Z",
      // Friday 2pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-06T14:00:00Z,2023-01-11T09:00:00Z",

      // Summer (BST/UTC+1), i.e. noon local time is at 11:00Z
      // Sunday 11am. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-04T10:00:00Z,2023-06-07T08:00:00Z",
      // Sunday 12:59pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-04T11:59:59Z,2023-06-07T08:00:00Z",
      // Sunday 1pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-04T12:00:00Z,2023-06-07T08:00:00Z",
      // Sunday 2pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-04T13:00:00Z,2023-06-07T08:00:00Z",
      // Monday 11am. 2 working days later (Wednesday)
      "2023-06-05T10:00:00Z,2023-06-07T10:00:00Z",
      // Monday 12:59pm. 2 working days later (Wednesday)
      "2023-06-05T11:59:59Z,2023-06-07T11:59:59Z",
      // Monday 1pm. 2 working days after 9am on the next working day (Tuesday), so 9am Thursday
      "2023-06-05T12:00:00Z,2023-06-08T08:00:00Z",
      // Monday 2pm. 2 working days after 9am on the next working day (Tuesday), so 9am Thursday
      "2023-06-05T13:00:00Z,2023-06-08T08:00:00Z",
      // Thursday 2pm. 2 working days after 9am on the next working day (Friday), so 9am Tuesday
      "2023-06-08T13:00:00Z,2023-06-13T08:00:00Z",
      // Friday 2pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-09T13:00:00Z,2023-06-14T08:00:00Z",

      // Multi-timezone drifting!!
      // GMT -> BST: Friday 1pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-03-24T13:00:00Z,2023-03-29T08:00:00Z",
      // BST -> GMT: Friday 1pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-10-27T12:00:00Z,2023-11-01T09:00:00Z",
    )
    fun `getDeadline for a short notice placement application returns submitted date plus 2 working days, or 2 working days after 9am on the next working day if after 1pm or a non-working day`(
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
      // Winter (GMT/UTC), i.e. noon local time is at 12:00Z
      // Sunday 11am. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-01T11:00:00Z,2023-01-04T09:00:00Z",
      // Sunday 12:59pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-01T12:59:59Z,2023-01-04T09:00:00Z",
      // Sunday 1pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-01T13:00:00Z,2023-01-04T09:00:00Z",
      // Sunday 2pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-01T14:00:00Z,2023-01-04T09:00:00Z",
      // Monday 11am. 2 working days later (Wednesday)
      "2023-01-02T11:00:00Z,2023-01-04T11:00:00Z",
      // Monday 12:59pm. 2 working days later (Wednesday)
      "2023-01-02T12:59:59Z,2023-01-04T12:59:59Z",
      // Monday 1pm. 2 working days after 9am on the next working day (Tuesday), so 9am Thursday
      "2023-01-02T13:00:00Z,2023-01-05T09:00:00Z",
      // Monday 2pm. 2 working days after 9am on the next working day (Tuesday), so 9am Thursday
      "2023-01-02T14:00:00Z,2023-01-05T09:00:00Z",
      // Thursday 2pm. 2 working days after 9am on the next working day (Friday), so 9am Tuesday
      "2023-01-05T14:00:00Z,2023-01-10T09:00:00Z",
      // Friday 2pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-01-06T14:00:00Z,2023-01-11T09:00:00Z",

      // Summer (BST/UTC+1), i.e. noon local time is at 11:00Z
      // Sunday 11am. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-04T10:00:00Z,2023-06-07T08:00:00Z",
      // Sunday 12:59pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-04T11:59:59Z,2023-06-07T08:00:00Z",
      // Sunday 1pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-04T12:00:00Z,2023-06-07T08:00:00Z",
      // Sunday 2pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-04T13:00:00Z,2023-06-07T08:00:00Z",
      // Monday 11am. 2 working days later (Wednesday)
      "2023-06-05T10:00:00Z,2023-06-07T10:00:00Z",
      // Monday 12:59pm. 2 working days later (Wednesday)
      "2023-06-05T11:59:59Z,2023-06-07T11:59:59Z",
      // Monday 1pm. 2 working days after 9am on the next working day (Tuesday), so 9am Thursday
      "2023-06-05T12:00:00Z,2023-06-08T08:00:00Z",
      // Monday 2pm. 2 working days after 9am on the next working day (Tuesday), so 9am Thursday
      "2023-06-05T13:00:00Z,2023-06-08T08:00:00Z",
      // Thursday 2pm. 2 working days after 9am on the next working day (Friday), so 9am Tuesday
      "2023-06-08T13:00:00Z,2023-06-13T08:00:00Z",
      // Friday 2pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-06-09T13:00:00Z,2023-06-14T08:00:00Z",

      // Multi-timezone drifting!!
      // GMT -> BST: Friday 1pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-03-24T13:00:00Z,2023-03-29T08:00:00Z",
      // BST -> GMT: Friday 1pm. 2 working days after 9am on the next working day (Monday), so 9am Wednesday
      "2023-10-27T12:00:00Z,2023-11-01T09:00:00Z",
    )
    fun `getDeadline for an emergency placement application returns submitted date plus 2 working days, or 2 working days after 9am on the next working day if after 1pm or a non-working day`(
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
