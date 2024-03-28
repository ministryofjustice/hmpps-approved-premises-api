package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayCountService
import java.time.LocalDate
import java.time.OffsetDateTime

@SuppressWarnings("MagicNumber")
class TaskDeadlineServiceTest {
  private val workingDayCountService = mockk<WorkingDayCountService>()
  private val taskDeadlineService = TaskDeadlineService(workingDayCountService)

  // Set up a naive implementation of the workingDayCountService
  @BeforeEach
  fun setup() {
    every {
      workingDayCountService.addWorkingDays(any<LocalDate>(), any<Int>())
    } answers {
      firstArg<LocalDate>().plusDays(secondArg<Int>().toLong())
    }
  }

  @Nested
  inner class GetAssessmentDeadline {

    @Test
    fun `getDeadline for a standard assessment returns created date plus 10 working days`() {
      val createdAt = OffsetDateTime.parse("2023-01-01T15:00:00Z")
      val assessment =
        createAssessment(noticeType = Cas1ApplicationTimelinessCategory.standard, isEsap = false, createdAt = createdAt)
      val result = taskDeadlineService.getDeadline(assessment)

      assertThat(result!!.toLocalDate()).isEqualTo(LocalDate.parse("2023-01-11"))

      verify(exactly = 1) {
        workingDayCountService.addWorkingDays(
          date = assessment.createdAt.toLocalDate(),
          daysToAdd = 10,
        )
      }
    }

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

    @Test
    fun `getDeadline for a short notice assessment returns created date plus 2 working days`() {
      val createdAt = OffsetDateTime.parse("2023-01-01T15:00:00Z")
      val assessment = createAssessment(
        noticeType = Cas1ApplicationTimelinessCategory.shortNotice,
        isEsap = false,
        createdAt = createdAt,
      )
      val result = taskDeadlineService.getDeadline(assessment)

      assertThat(result!!.toLocalDate()).isEqualTo(LocalDate.parse("2023-01-03"))

      verify(exactly = 1) {
        workingDayCountService.addWorkingDays(
          date = assessment.createdAt.toLocalDate(),
          daysToAdd = 2,
        )
      }
    }

    @Test
    fun `getDeadline for an emergency assessment created before 1pm returns created date plus two hours`() {
      val createdAt = OffsetDateTime.parse("2023-01-01T11:00:00Z")
      val assessment = createAssessment(
        noticeType = Cas1ApplicationTimelinessCategory.emergency,
        isEsap = false,
        createdAt = createdAt,
      )
      val result = taskDeadlineService.getDeadline(assessment)

      assertThat(result).isEqualTo(OffsetDateTime.parse("2023-01-01T13:00:00Z"))
    }

    @Test
    fun `getDeadline for an emergency assessment created after 1pm returns 11am on the next working day`() {
      val createdAt = OffsetDateTime.parse("2023-01-01T14:00:00Z")
      val assessment = createAssessment(
        noticeType = Cas1ApplicationTimelinessCategory.emergency,
        isEsap = false,
        createdAt = createdAt,
      )

      every { workingDayCountService.nextWorkingDay(any()) } returns LocalDate.parse("2023-01-02")

      val result = taskDeadlineService.getDeadline(assessment)

      assertThat(result).isEqualTo(OffsetDateTime.parse("2023-01-02T11:00:00Z"))

      verify(exactly = 1) {
        workingDayCountService.nextWorkingDay(assessment.createdAt.toLocalDate())
      }
    }
  }

  @Nested
  inner class GetPlacementRequestDeadline {

    @Test
    fun `getDeadline for a standard placement request returns created date plus 5 working days`() {
      val createdAt = OffsetDateTime.parse("2023-01-01T15:00:00Z")
      val placementRequest =
        createPlacementRequest(noticeType = Cas1ApplicationTimelinessCategory.standard, isEsap = false, createdAt)
      val result = taskDeadlineService.getDeadline(placementRequest)

      assertThat(result.toLocalDate()).isEqualTo(LocalDate.parse("2023-01-06"))

      verify(exactly = 1) {
        workingDayCountService.addWorkingDays(
          date = placementRequest.createdAt.toLocalDate(),
          daysToAdd = 5,
        )
      }
    }

    @Test
    fun `getDeadline for a short notice placement request returns created date plus 2 working days `() {
      val createdAt = OffsetDateTime.parse("2023-01-01T15:00:00Z")
      val placementRequest = createPlacementRequest(
        noticeType = Cas1ApplicationTimelinessCategory.shortNotice,
        isEsap = false,
        createdAt = createdAt,
      )
      val result = taskDeadlineService.getDeadline(placementRequest)

      assertThat(result.toLocalDate()).isEqualTo(LocalDate.parse("2023-01-03"))

      verify(exactly = 1) {
        workingDayCountService.addWorkingDays(
          date = placementRequest.createdAt.toLocalDate(),
          daysToAdd = 2,
        )
      }
    }

    @Test
    fun `getDeadline for an emergency placement request returns created date`() {
      val placementRequest = createPlacementRequest(
        noticeType = Cas1ApplicationTimelinessCategory.emergency,
        isEsap = false,
        createdAt = OffsetDateTime.now(),
      )
      val result = taskDeadlineService.getDeadline(placementRequest)

      assertThat(result).isEqualTo(placementRequest.createdAt)
    }

    @Test
    fun `getDeadline for an ESAP placement request returns created date `() {
      val placementRequest = createPlacementRequest(
        noticeType = Cas1ApplicationTimelinessCategory.standard,
        isEsap = true,
        createdAt = OffsetDateTime.now(),
      )

      val result = taskDeadlineService.getDeadline(placementRequest)

      assertThat(result).isEqualTo(placementRequest.createdAt)
    }
  }

  @Nested
  inner class GetPlacementApplicationDeadline {

    @Test
    fun `getDeadline for a standard placement application returns submitted date plus 10 working days`() {
      val submittedAt = OffsetDateTime.parse("2023-01-01T15:00:00Z")

      val placementApplication = createPlacementApplication(
        noticeType = Cas1ApplicationTimelinessCategory.standard,
        isEsap = false,
        submittedAt = submittedAt,
      )

      val result = taskDeadlineService.getDeadline(placementApplication)

      assertThat(result.toLocalDate()).isEqualTo(LocalDate.parse("2023-01-11"))

      verify(exactly = 1) {
        workingDayCountService.addWorkingDays(
          date = submittedAt.toLocalDate(),
          daysToAdd = 10,
        )
      }
    }

    @Test
    fun `getDeadline for a short notice placement application returns submitted date plus 10 working days`() {
      val submittedAt = OffsetDateTime.parse("2023-01-01T15:00:00Z")

      val placementApplication = createPlacementApplication(
        noticeType = Cas1ApplicationTimelinessCategory.shortNotice,
        isEsap = false,
        submittedAt = submittedAt,
      )

      val result = taskDeadlineService.getDeadline(placementApplication)

      assertThat(result.toLocalDate()).isEqualTo(LocalDate.parse("2023-01-11"))

      verify(exactly = 1) {
        workingDayCountService.addWorkingDays(
          date = submittedAt.toLocalDate(),
          daysToAdd = 10,
        )
      }
    }

    @Test
    fun `getDeadline for an emergency placement application returns submitted date plus 10 working days`() {
      val submittedAt = OffsetDateTime.parse("2023-01-01T15:00:00Z")

      val placementApplication = createPlacementApplication(
        noticeType = Cas1ApplicationTimelinessCategory.standard,
        isEsap = true,
        submittedAt = submittedAt,
      )

      val result = taskDeadlineService.getDeadline(placementApplication)

      assertThat(result.toLocalDate()).isEqualTo(LocalDate.parse("2023-01-11"))

      verify(exactly = 1) {
        workingDayCountService.addWorkingDays(
          date = submittedAt.toLocalDate(),
          daysToAdd = 10,
        )
      }
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
