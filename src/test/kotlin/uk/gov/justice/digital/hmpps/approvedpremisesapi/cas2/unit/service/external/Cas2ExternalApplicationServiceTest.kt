package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.service.external

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.external.Cas2ArrivalEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.external.Cas2ArrivalEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ExternalSubmittedApplicationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2StaffDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2UserTypeDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.external.Cas2ExternalApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.external.Cas2ExternalDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2StatusUpdateDetailEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2Cohort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DomainEventEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.JsonMapperFactory
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas2ExternalApplicationServiceTest {
  private val mockCas2ApplicationRepository = mockk<Cas2ApplicationRepository>()
  private val mockDomainEventRepository = mockk<DomainEventRepository>()
  private val mockCas2ExternalDomainEventService = mockk<Cas2ExternalDomainEventService>()
  private val jsonMapper = JsonMapperFactory.createJackson3JsonMapper()

  private val cas2ExternalApplicationService = Cas2ExternalApplicationService(
    mockCas2ApplicationRepository,
    mockDomainEventRepository,
    mockCas2ExternalDomainEventService,
    jsonMapper,
    "http://frontend/applications/#id",
    "http://frontend/assess/applications/#applicationId/overview",
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
        markedAsArrivedDateTime = null,
      )

      val expected = setUpExpectedApplication(
        cas2applicationEntity = cas2applicationEntity,
        submittedApplication = submittedApplication,
        uiUrl = "http://frontend/assess/applications/${cas2applicationEntity.id}/overview",
      )

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `returns current application (withdrawn, with statusDetails), providing view submitted url`() {
      val statusId = UUID.fromString("004e2419-9614-4c1e-a207-a8418009f23d")
      val statusDetailId = UUID.fromString("e4a2391e-e847-427a-a913-51e0b0ad9f52")

      val cas2applicationEntity = setUpApplicationWithStatusDetail(statusId, statusDetailId)

      val result = cas2ExternalApplicationService.getSuitableApplicationByCrn(crn)

      val submittedApplication = Cas2ExternalSubmittedApplicationDto(
        latestAssessmentStatus = Cas2AssessmentStatus.WITHDRAWN.apiName,
        submittedAt = cas2applicationEntity.submittedAt!!,
        offerDeclinedReason = null,
        cancelledReason = null,
        markedAsArrivedDateTime = null,
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
        latestAssessmentStatus = Cas2AssessmentStatus.AWAITING_DECISION.apiName,
        submittedAt = cas2applicationEntity.submittedAt!!,
        offerDeclinedReason = null,
        cancelledReason = null,
        markedAsArrivedDateTime = null,
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
        latestAssessmentStatus = Cas2AssessmentStatus.CANCELLED.apiName,
        submittedAt = cas2applicationEntity.submittedAt!!,
        offerDeclinedReason = null,
        cancelledReason = Cas2AssessmentStatusDetail.CREATED_IN_ERROR.name,
        markedAsArrivedDateTime = null,
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
        latestAssessmentStatus = Cas2AssessmentStatus.OFFER_DECLINED.apiName,
        submittedAt = cas2applicationEntity.submittedAt!!,
        offerDeclinedReason = Cas2AssessmentStatusDetail.AREA_UNSUITABLE.name,
        cancelledReason = null,
        markedAsArrivedDateTime = null,
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

      every { mockCas2ApplicationRepository.findLatestApplication(crn, Cas2Cohort.isr()) } returns cas2applicationEntity

      val result = cas2ExternalApplicationService.getSuitableApplicationByCrn(crn)

      val expected = setUpExpectedApplication(
        cas2applicationEntity = cas2applicationEntity,
        uiUrl = "http://frontend/applications/${cas2applicationEntity.id}",
      )

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `returns no application when none exists for crn`() {
      every { mockCas2ApplicationRepository.findLatestApplication(crn, Cas2Cohort.isr()) } returns null
      val result = cas2ExternalApplicationService.getSuitableApplicationByCrn(crn)
      assertThat(result).isEqualTo(null)
    }

    @Test
    fun `Returns null markedAsArrivedDateTime when latestAssessmentStatus is AWAITING_ARRIVAL but no arrival event exists`() {
      val awaitingArrivalStatusId = UUID.fromString("89458555-3219-44a2-9584-c4f715d6b565")

      val cas2ApplicationEntity = Cas2ApplicationEntityFactory()
        .withCreatedByUser(Cas2UserEntityFactory().produce())
        .withCrn(crn)
        .withSubmittedAt(OffsetDateTime.parse("2025-12-03T10:15:30+01:00"))
        .withStatusUpdates(mutableListOf())
        .produce()

      cas2ApplicationEntity.statusUpdates!!.add(
        Cas2StatusUpdateEntityFactory()
          .withApplication(cas2ApplicationEntity)
          .withAssessor(Cas2UserEntityFactory().produce())
          .withStatusId(awaitingArrivalStatusId)
          .produce(),
      )

      every { mockCas2ApplicationRepository.findLatestApplication(crn, Cas2Cohort.isr()) } returns cas2ApplicationEntity
      every {
        mockDomainEventRepository.findByApplicationIdAndType(cas2ApplicationEntity.id, DomainEventType.CAS2_PERSON_ARRIVED)
      } returns emptyList()

      val result = cas2ExternalApplicationService.getSuitableApplicationByCrn(crn)

      assertThat(result!!.submittedApplication!!.markedAsArrivedDateTime).isNull()
    }

    @Test
    fun `Returns null markedAsArrivedDateTime when latestAssessmentStatus is not AWAITING_ARRIVAL and arrival event exists for the application`() {
      val placeOfferedStatusId = UUID.fromString("176bbda0-0766-4d77-8d56-18ed8f9a4ef2")

      val cas2ApplicationEntity = Cas2ApplicationEntityFactory()
        .withCreatedByUser(Cas2UserEntityFactory().produce())
        .withCrn(crn)
        .withSubmittedAt(OffsetDateTime.parse("2025-12-03T10:15:30+01:00"))
        .withStatusUpdates(mutableListOf())
        .produce()

      cas2ApplicationEntity.statusUpdates!!.add(
        Cas2StatusUpdateEntityFactory()
          .withApplication(cas2ApplicationEntity)
          .withAssessor(Cas2UserEntityFactory().produce())
          .withStatusId(placeOfferedStatusId)
          .produce(),
      )

      val arrivalEvent = mockk<DomainEventEntity>()

      every { mockCas2ApplicationRepository.findLatestApplication(crn, Cas2Cohort.isr()) } returns cas2ApplicationEntity
      every {
        mockDomainEventRepository.findByApplicationIdAndType(cas2ApplicationEntity.id, DomainEventType.CAS2_PERSON_ARRIVED)
      } returns listOf(arrivalEvent)

      val result = cas2ExternalApplicationService.getSuitableApplicationByCrn(crn)

      assertThat(result!!.submittedApplication!!.markedAsArrivedDateTime).isNull()
    }

    @Test
    fun `Returns non null markedAsArrivedDateTime when latestAssessmentStatus status is AWAITING_ARRIVAL and arrival event exists`() {
      val awaitingArrivalStatusId = UUID.fromString("89458555-3219-44a2-9584-c4f715d6b565")
      val expectedArrivedDateTime = Instant.parse("2025-12-03T10:15:30Z")
      val eventId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")

      val cas2ApplicationEntity = Cas2ApplicationEntityFactory()
        .withCreatedByUser(Cas2UserEntityFactory().produce())
        .withCrn(crn)
        .withSubmittedAt(OffsetDateTime.parse("2024-08-17T11:25:31+01:00"))
        .withStatusUpdates(mutableListOf())
        .produce()

      cas2ApplicationEntity.statusUpdates!!.add(
        Cas2StatusUpdateEntityFactory()
          .withApplication(cas2ApplicationEntity)
          .withAssessor(Cas2UserEntityFactory().produce())
          .withStatusId(awaitingArrivalStatusId)
          .produce(),
      )

      val cas2ArrivalEvent = Cas2ArrivalEvent(
        id = eventId,
        timestamp = expectedArrivedDateTime,
        eventType = EventType.arrived,
        eventDetails = Cas2ArrivalEventDetails(
          markedAsArrivedDateTime = expectedArrivedDateTime,
          arrivedByUsername = "testuser",
        ),
      )

      val arrivalEvent = DomainEventEntityFactory()
        .withApplicationId(cas2ApplicationEntity.id)
        .withType(DomainEventType.CAS2_PERSON_ARRIVED)
        .withCrn(crn)
        .withData(jsonMapper.writeValueAsString(cas2ArrivalEvent))
        .produce()

      every { mockCas2ApplicationRepository.findLatestApplication(crn, Cas2Cohort.isr()) } returns cas2ApplicationEntity
      every {
        mockDomainEventRepository.findByApplicationIdAndType(cas2ApplicationEntity.id, DomainEventType.CAS2_PERSON_ARRIVED)
      } returns listOf(arrivalEvent)

      val result = cas2ExternalApplicationService.getSuitableApplicationByCrn(crn)

      assertThat(result!!.submittedApplication!!.markedAsArrivedDateTime).isEqualTo(expectedArrivedDateTime)
    }
  }

  @Nested
  inner class PostArrival {
    @Test
    fun `returns not found when no valid applications exist for the crn`() {
      val applicationId = UUID.randomUUID()

      every {
        mockCas2ApplicationRepository.findAllByCrnAndSubmittedAtIsNotNullAndAssessmentIdIsNotNull(crn)
      } returns emptyList()

      val result = cas2ExternalApplicationService.recordArrival(crn, applicationId, "username", Instant.now())

      assertThat(result).isEqualTo(CasResult.NotFound<Unit>("CRN", crn))
    }

    @Test
    fun `returns not found when application is not associated with the crn`() {
      val applicationId = UUID.randomUUID()
      val otherApplication = Cas2ApplicationEntityFactory()
        .withCreatedByUser(Cas2UserEntityFactory().produce())
        .withCrn(crn)
        .withId(UUID.randomUUID())
        .withSubmittedAt(OffsetDateTime.now())
        .withStatusUpdates(mutableListOf())
        .produce()

      every {
        mockCas2ApplicationRepository.findAllByCrnAndSubmittedAtIsNotNullAndAssessmentIdIsNotNull(crn)
      } returns listOf(otherApplication)

      val result = cas2ExternalApplicationService.recordArrival(crn, applicationId, "username", Instant.now())

      assertThat(result).isEqualTo(CasResult.NotFound<Unit>("application", applicationId.toString()))
    }

    @Test
    fun `returns fail when the arrival datetime is in the future`() {
      val applicationId = UUID.randomUUID()
      val application = Cas2ApplicationEntityFactory()
        .withCreatedByUser(Cas2UserEntityFactory().produce())
        .withCrn(crn)
        .withId(applicationId)
        .withSubmittedAt(OffsetDateTime.now())
        .withStatusUpdates(mutableListOf())
        .produce()

      every {
        mockCas2ApplicationRepository.findAllByCrnAndSubmittedAtIsNotNullAndAssessmentIdIsNotNull(crn)
      } returns listOf(application)

      every { mockCas2ExternalDomainEventService.saveArrivalDomainEvent(any()) } answers { mockk() }

      val futureDatetime = Instant.now().plusSeconds(3600)

      val result = cas2ExternalApplicationService.recordArrival(crn, applicationId, "username", futureDatetime)

      assertThat(result).isEqualTo(CasResult.FieldValidationError<Unit>(mapOf(futureDatetime.toString() to "cannot be in the future")))
    }

    @Test
    fun `returns success when the crn has the matching application`() {
      val applicationId = UUID.randomUUID()
      val application = Cas2ApplicationEntityFactory()
        .withCreatedByUser(Cas2UserEntityFactory().produce())
        .withCrn(crn)
        .withId(applicationId)
        .withSubmittedAt(OffsetDateTime.now())
        .withStatusUpdates(mutableListOf())
        .produce()

      every {
        mockCas2ApplicationRepository.findAllByCrnAndSubmittedAtIsNotNullAndAssessmentIdIsNotNull(crn)
      } returns listOf(application)

      every { mockCas2ExternalDomainEventService.saveArrivalDomainEvent(any()) } answers { mockk() }

      val result = cas2ExternalApplicationService.recordArrival(crn, applicationId, "username", Instant.now())

      assertThat(result).isEqualTo(CasResult.Success(Unit))
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

    every { mockCas2ApplicationRepository.findLatestApplication(crn, Cas2Cohort.isr()) } returns cas2applicationEntity

    return cas2applicationEntity
  }
}
