package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.service.external

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ExternalSubmittedApplicationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2StaffDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2UserTypeDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.external.Cas2ExternalApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2StatusUpdateDetailEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2Cohort
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

class Cas2ExternalApplicationServiceTest {
  private val mockCas2ApplicationRepository = mockk<Cas2ApplicationRepository>()

  private val cas2ExternalApplicationService = Cas2ExternalApplicationService(
    mockCas2ApplicationRepository,
    "http://frontend/applications/#id",
    "http://frontend/assess/applications/#applicationId/overview",
    Clock.systemDefaultZone(),
  )

  val crn = "ADAFD"

  @Nested
  inner class GetSuitableApplicationByCrn {
    @Test
    fun `returns current application (submitted), providing view submitted url`() {
      val cas2applicationEntity = setUpApplication()

      val result = cas2ExternalApplicationService.getSuitableApplicationByCrn(crn)

      val submittedApplication = Cas2ExternalSubmittedApplicationDto(
        latestAssessmentStatus = null,
        submittedAt = cas2applicationEntity.submittedAt!!,
        offerDeclinedReason = null,
        cancelledReason = null,
      )

      val expected = setUpExpectedApplication(
        cas2applicationEntity = cas2applicationEntity,
        submittedApplication = submittedApplication,
        uiUrl = "http://frontend/assess/applications/${cas2applicationEntity.id}/overview",
      )

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `returns null when latest application has a conditional release date in the past`() {
      setUpApplication(LocalDate.now().minusDays(1))

      val result = cas2ExternalApplicationService.getSuitableApplicationByCrn(crn)
      assertThat(result).isNull()
    }

    @Test
    fun `returns current application (withdrawn, with statusDetails), providing view submitted url`() {
        val statusId = UUID.fromString("004e2419-9614-4c1e-a207-a8418009f23d")
        val statusDetailId = UUID.fromString("e4a2391e-e847-427a-a913-51e0b0ad9f52")

        val cas2applicationEntity = setUpApplicationWithStatusDetail(statusId, statusDetailId)

        val result = cas2ExternalApplicationService.getSuitableApplicationByCrn(crn)

        val submittedApplication = Cas2ExternalSubmittedApplicationDto(
          latestAssessmentStatus = "withdrawn",
          submittedAt = cas2applicationEntity.submittedAt!!,
          offerDeclinedReason = null,
          cancelledReason = null,
        )

        val expected = setUpExpectedApplication(
          cas2applicationEntity = cas2applicationEntity,
          submittedApplication = submittedApplication,
          uiUrl = "http://frontend/assess/applications/${cas2applicationEntity.id}/overview",
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `returns current application (awaitingDecision), providing view submitted url`() {
      val statusId = UUID.fromString("ba4d8432-250b-4ab9-81ec-7eb4b16e5dd1")

      val cas2applicationEntity = setUpApplication()

      val statusUpdate = Cas2StatusUpdateEntityFactory()
        .withApplication(cas2applicationEntity)
        .withAssessor(cas2applicationEntity.createdByUser)
        .withStatusId(statusId)
        .produce()

      cas2applicationEntity.statusUpdates!!.add(statusUpdate)

      val result = cas2ExternalApplicationService.getSuitableApplicationByCrn(crn)

      val submittedApplication = Cas2ExternalSubmittedApplicationDto(
        latestAssessmentStatus = "awaitingDecision",
        submittedAt = cas2applicationEntity.submittedAt!!,
        offerDeclinedReason = null,
        cancelledReason = null,
      )

      val expected = setUpExpectedApplication(
        cas2applicationEntity = cas2applicationEntity,
        submittedApplication = submittedApplication,
        uiUrl = "http://frontend/assess/applications/${cas2applicationEntity.id}/overview",
      )

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `returns current application (cancelled), providing view submitted url and cancellation reason`() {
      val statusId = UUID.fromString("f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9")
      val statusDetailId = UUID.fromString("d1d96185-d92a-450b-b47f-bcce50356eed")

      val cas2applicationEntity = setUpApplicationWithStatusDetail(statusId, statusDetailId)

      val result = cas2ExternalApplicationService.getSuitableApplicationByCrn(crn)

      val submittedApplication = Cas2ExternalSubmittedApplicationDto(
        latestAssessmentStatus = "cancelled",
        submittedAt = cas2applicationEntity.submittedAt!!,
        offerDeclinedReason = null,
        cancelledReason = "createdInError",
      )

      val expected = setUpExpectedApplication(
        cas2applicationEntity = cas2applicationEntity,
        submittedApplication = submittedApplication,
        uiUrl = "http://frontend/assess/applications/${cas2applicationEntity.id}/overview",
      )

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `returns current application (offerDeclined), providing view submitted url and offerDeclined reason`() {
      val statusId = UUID.fromString("9a381bc6-22d3-41d6-804d-4e49f428c1de")
      val statusDetailId = UUID.fromString("62645779-242d-4601-a8f8-d2cbf1d41dfa")

      val cas2applicationEntity = setUpApplicationWithStatusDetail(statusId, statusDetailId)

      val result = cas2ExternalApplicationService.getSuitableApplicationByCrn(crn)

      val submittedApplication = Cas2ExternalSubmittedApplicationDto(
        latestAssessmentStatus = "offerDeclined",
        submittedAt = cas2applicationEntity.submittedAt!!,
        offerDeclinedReason = "areaUnsuitable",
        cancelledReason = null,
      )

      val expected = setUpExpectedApplication(
        cas2applicationEntity = cas2applicationEntity,
        submittedApplication = submittedApplication,
        uiUrl = "http://frontend/assess/applications/${cas2applicationEntity.id}/overview",
      )

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `returns current application (draft), providing view draft url`() {
      val user = Cas2UserEntityFactory()
        .produce()
      val cas2applicationEntity = Cas2ApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(crn)
        .withStatusUpdates(mutableListOf())
        .produce()

      every { mockCas2ApplicationRepository.findApplicationsByCohortNewestFirst(crn, Cas2Cohort.isr()) } returns listOf(cas2applicationEntity)

      val result = cas2ExternalApplicationService.getSuitableApplicationByCrn(crn)

      val expected = setUpExpectedApplication(
        cas2applicationEntity = cas2applicationEntity,
        uiUrl = "http://frontend/applications/${cas2applicationEntity.id}",
      )

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `returns older application when newer application is ineligible`() {
      val user = Cas2UserEntityFactory()
        .produce()

      val today = LocalDate.now()
      val now = today
        .atStartOfDay(ZoneOffset.UTC)
        .toOffsetDateTime()
        .truncatedTo(ChronoUnit.MICROS)

      val newerCas2applicationEntity = setUpApplication(
        conditionalReleaseDate = today.minusDays(1),
        createdAt = now
      )

      val olderCas2applicationEntity = Cas2ApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCreatedAt(newerCas2applicationEntity.createdAt.minusMonths(1))
        .withCrn(crn)
        .withStatusUpdates(mutableListOf())
        .produce()

      every { mockCas2ApplicationRepository.findApplicationsByCohortNewestFirst(crn, Cas2Cohort.isr()) } returns listOf(
        newerCas2applicationEntity,
        olderCas2applicationEntity,
      )

      val result = cas2ExternalApplicationService.getSuitableApplicationByCrn(crn)

      val expected = setUpExpectedApplication(
        cas2applicationEntity = olderCas2applicationEntity,
        uiUrl = "http://frontend/applications/${olderCas2applicationEntity.id}",
      )

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `returns null when latest draft application is more than 2 months old`() {
      val user = Cas2UserEntityFactory()
        .produce()
      val cas2applicationEntity = Cas2ApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(crn)
        .withStatusUpdates(mutableListOf())
        .withCreatedAt(OffsetDateTime.now().minusMonths(2))
        .produce()

      every { mockCas2ApplicationRepository.findApplicationsByCohortNewestFirst(crn, Cas2Cohort.isr()) } returns listOf(cas2applicationEntity)

      val result = cas2ExternalApplicationService.getSuitableApplicationByCrn(crn)

      assertThat(result).isNull()
    }

    @Test
    fun `returns no application when none exists for crn`() {
      every { mockCas2ApplicationRepository.findApplicationsByCohortNewestFirst(crn, Cas2Cohort.isr()) } returns listOf()
      val result = cas2ExternalApplicationService.getSuitableApplicationByCrn(crn)
      assertThat(result).isEqualTo(null)
    }
  }

  private fun setUpApplicationWithStatusDetail(statusId: UUID, statusDetailId: UUID): Cas2ApplicationEntity {
    val cas2applicationEntity = setUpApplication()

    val statusUpdate = Cas2StatusUpdateEntityFactory()
      .withApplication(cas2applicationEntity)
      .withAssessor(cas2applicationEntity.createdByUser)
      .withStatusId(statusId)
      .withStatusUpdateDetails(mutableListOf())
      .produce()

    val statusUpdateDetail = Cas2StatusUpdateDetailEntityFactory()
      .withStatusDetailId(statusDetailId)
      .withStatusUpdate(statusUpdate)
      .produce()

    statusUpdate.statusUpdateDetails!!.add(statusUpdateDetail)

    cas2applicationEntity.statusUpdates!!.add(statusUpdate)

    return cas2applicationEntity
  }

  private fun setUpCas2StaffDto(cas2applicationEntity: Cas2ApplicationEntity) = Cas2StaffDto(
    username = cas2applicationEntity.createdByUser.username,
    deliusStaffCode = cas2applicationEntity.createdByUser.deliusStaffCode,
    name = cas2applicationEntity.createdByUser.name,
    nomisStaffId = cas2applicationEntity.createdByUser.nomisStaffId,
    userType = Cas2UserTypeDto.valueOf(cas2applicationEntity.createdByUser.userType.name),
  )

  private fun setUpExpectedApplication(
    cas2applicationEntity: Cas2ApplicationEntity,
    submittedApplication: Cas2ExternalSubmittedApplicationDto? = null,
    uiUrl: String,
  ) = Cas2SuitableApplication(
    uiUrl = uiUrl,
    id = cas2applicationEntity.id,
    submittedApplication = submittedApplication,
    cohort = cas2applicationEntity.cohort?.apiType,
    createdAt = cas2applicationEntity.createdAt,
    createdBy = setUpCas2StaffDto(cas2applicationEntity),
  )

  private fun setUpApplication(
    conditionalReleaseDate: LocalDate = LocalDate.now(),
    createdAt: OffsetDateTime = OffsetDateTime.now(),
  ): Cas2ApplicationEntity {
    val user = Cas2UserEntityFactory()
      .produce()
    val submittedAt = OffsetDateTime.now()
    val cas2applicationEntity = Cas2ApplicationEntityFactory()
      .withConditionalReleaseDate(conditionalReleaseDate)
      .withCreatedByUser(user)
      .withCreatedAt(createdAt)
      .withSubmittedAt(submittedAt)
      .withCrn(crn)
      .withId(UUID.randomUUID())
      .withStatusUpdates(mutableListOf())
      .produce()

    every { mockCas2ApplicationRepository.findApplicationsByCohortNewestFirst(crn, Cas2Cohort.isr()) } returns listOf(cas2applicationEntity)

    return cas2applicationEntity
  }
}
