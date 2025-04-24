package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LatestCas2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NomisUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderManagementUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2.Cas2ApplicationSummaryEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2.Cas2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OffenderManagementUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.NomisUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NomisUserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.AssessmentsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.StatusUpdateTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.TimelineEventsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ApplicationsTransformerTest {
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockNomisTransformer = mockk<NomisUserTransformer>()
  private val mockStatusUpdateTransformer = mockk<StatusUpdateTransformer>()
  private val mockTimelineEventsTransformer = mockk<TimelineEventsTransformer>()
  private val mockAssessmentsTransformer = mockk<AssessmentsTransformer>()
  private val nomisUserService = mockk<NomisUserService>()
  private val offenderManagementUnitRepository = mockk<OffenderManagementUnitRepository>()

  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val applicationsTransformer = ApplicationsTransformer(
    objectMapper,
    mockPersonTransformer,
    mockNomisTransformer,
    mockStatusUpdateTransformer,
    mockTimelineEventsTransformer,
    mockAssessmentsTransformer,
    nomisUserService,
    offenderManagementUnitRepository,
  )

  private val user = NomisUserEntityFactory().produce()

  private val cas2ApplicationFactory = Cas2ApplicationEntityFactory().withCreatedByUser(user)

  private val submittedCas2ApplicationFactory = cas2ApplicationFactory
    .withSubmittedAt(OffsetDateTime.now())

  @BeforeEach
  fun setup() {
    every { mockPersonTransformer.transformModelToPersonApi(any()) } returns mockk<Person>()
    every { mockNomisTransformer.transformJpaToApi(any()) } returns NomisUser(
      id = user.id,
      name = user.name,
      nomisUsername = user.nomisUsername,
      isActive = user.isActive,
    )
    every { mockStatusUpdateTransformer.transformJpaToApi(any()) } returns mockk<Cas2StatusUpdate>()
    every { mockStatusUpdateTransformer.transformJpaSummaryToLatestStatusUpdateApi(any()) } returns mockk<LatestCas2StatusUpdate>()
    every { mockTimelineEventsTransformer.transformApplicationToTimelineEvents(any()) } returns listOf()
  }

  @Nested
  inner class TransformJpaToApi {

    @Test
    fun `transformJpaToApi transforms an in progress CAS-2 application correctly`() {
      val application = cas2ApplicationFactory
        .withSubmittedAt(null)
        .produce()

      val result = applicationsTransformer.transformJpaToApi(application, mockk())

      assertThat(result).hasOnlyFields(
        "id",
        "person",
        "createdBy",
        "schemaVersion",
        "outdatedSchema",
        "createdAt",
        "submittedAt",
        "data",
        "document",
        "status",
        "type",
        "telephoneNumber",
        "assessment",
        "timelineEvents",
        "allocatedPomEmailAddress",
        "allocatedPomName",
        "assignmentDate",
        "currentPrisonName",
        "isTransferredApplication",
        "omuEmailAddress",
      )

      assertThat(result.id).isEqualTo(application.id)
      assertThat(result.createdBy.id).isEqualTo(user.id)
      assertThat(result.status).isEqualTo(ApplicationStatus.inProgress)
      assertThat(result.timelineEvents).isEqualTo(listOf<Cas2TimelineEvent>())

      // these are assigned after an application is submitted
      assertThat(result.submittedAt).isNull()
      assertThat(result.allocatedPomEmailAddress).isNull()
      assertThat(result.allocatedPomName).isNull()
      assertThat(result.assignmentDate).isNull()
      assertThat(result.currentPrisonName).isNull()
      assertThat(result.isTransferredApplication).isFalse()
      assertThat(result.omuEmailAddress).isNull()
    }

    @Test
    fun `transformJpaToApi transforms a submitted CAS2 application correctly without status updates`() {
      val assessment = Cas2Assessment(id = UUID.fromString("3adc18ec-3d0d-4d0f-8b31-6f08e2591c35"))
      every { mockAssessmentsTransformer.transformJpaToApiRepresentation(any()) } returns assessment
      every { nomisUserService.getNomisUserById(any()) } returns user
      val prison = OffenderManagementUnitEntityFactory().produce()
      every { offenderManagementUnitRepository.findByPrisonCode(any()) } returns prison

      val application = submittedCas2ApplicationFactory
        .withAssessment(Cas2AssessmentEntityFactory().produce())
        .withReferringPrisonCode("PRI")
        .withApplicationAssignments().produce()

      val result = applicationsTransformer.transformJpaToApi(application, mockk())

      assertThat(result.id).isEqualTo(application.id)
      assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
      assertThat(result.telephoneNumber).isEqualTo(application.telephoneNumber)
      assertThat(result.assessment!!.id).isEqualTo(assessment.id)

      // these are assigned after an application is submitted
      assertThat(result.submittedAt).isEqualTo(application.submittedAt!!.toInstant())
      assertThat(result.allocatedPomEmailAddress).isEqualTo(user.email)
      assertThat(result.allocatedPomName).isEqualTo(user.name)
      assertThat(result.assignmentDate).isEqualTo(application.currentAssignmentDate)
      assertThat(result.currentPrisonName).isEqualTo(prison.prisonName)
      assertThat(result.isTransferredApplication).isFalse()
      assertThat(result.omuEmailAddress).isEqualTo(prison.email)
    }

    @Test
    fun `transformJpaToApi transforms a submitted CAS2 application correctly with status updates`() {
      val mockStatusUpdate = Cas2StatusUpdate(
        id = UUID.fromString("c426c63a-be35-421f-a1a0-fc286b60da41"),
        description = "On Waiting List",
        label = "On Waiting List",
        name = "onWaitingList",
      )
      val mockAssessment = Cas2Assessment(
        id = UUID.fromString("6e631a8c-a013-4bb4-812c-886c8fc25354"),
        statusUpdates = listOf(mockStatusUpdate),
      )

      val prison = OffenderManagementUnitEntityFactory().produce()
      every { mockAssessmentsTransformer.transformJpaToApiRepresentation(any()) } returns mockAssessment
      every { offenderManagementUnitRepository.findByPrisonCode(any()) } returns prison
      every { nomisUserService.getNomisUserById(any()) } returns user

      val application = submittedCas2ApplicationFactory.withAssessment(Cas2AssessmentEntityFactory().produce())
        .withReferringPrisonCode("PRI")
        .withApplicationAssignments()
        .produce()

      val result = applicationsTransformer.transformJpaToApi(application, mockk())

      assertThat(result.id).isEqualTo(application.id)
      assertThat(result.assessment!!.statusUpdates).hasSize(1).containsExactly(mockStatusUpdate)

      // these are assigned after an application is submitted
      assertThat(result.submittedAt).isEqualTo(application.submittedAt!!.toInstant())
      assertThat(result.allocatedPomEmailAddress).isEqualTo(user.email)
      assertThat(result.allocatedPomName).isEqualTo(user.name)
      assertThat(result.assignmentDate).isEqualTo(application.currentAssignmentDate)
      assertThat(result.currentPrisonName).isEqualTo(prison.prisonName)
      assertThat(result.isTransferredApplication).isFalse()
      assertThat(result.omuEmailAddress).isEqualTo(prison.email)
    }

    @Test
    fun `transformJpaToApi transforms a submitted CAS2 application correctly which has been transferred`() {
      val assessment = Cas2Assessment(id = UUID.fromString("3adc18ec-3d0d-4d0f-8b31-6f08e2591c35"))
      every { mockAssessmentsTransformer.transformJpaToApiRepresentation(any()) } returns assessment
      every { nomisUserService.getNomisUserById(any()) } returns user
      val prison = OffenderManagementUnitEntityFactory().produce()
      val newPrison = OffenderManagementUnitEntityFactory().withPrisonCode("NEW").withPrisonName("New Prison").withEmail("test@test.co.uk").produce()
      every { offenderManagementUnitRepository.findByPrisonCode(eq(prison.prisonCode)) } returns prison
      every { offenderManagementUnitRepository.findByPrisonCode(eq(newPrison.prisonCode)) } returns newPrison

      val application = submittedCas2ApplicationFactory
        .withAssessment(Cas2AssessmentEntityFactory().produce())
        .withReferringPrisonCode(prison.prisonCode)
        .withApplicationAssignments().produce()

      application.applicationAssignments.add(Cas2ApplicationAssignmentEntity(UUID.randomUUID(), application, newPrison.prisonCode, null, OffsetDateTime.now()))

      val result = applicationsTransformer.transformJpaToApi(application, mockk())

      assertThat(result.id).isEqualTo(application.id)
      assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
      assertThat(result.telephoneNumber).isEqualTo(application.telephoneNumber)
      assertThat(result.assessment!!.id).isEqualTo(assessment.id)

      // these are assigned after an application is submitted
      assertThat(result.submittedAt).isEqualTo(application.submittedAt!!.toInstant())
      assertThat(result.allocatedPomEmailAddress).isNull()
      assertThat(result.allocatedPomName).isNull()
      assertThat(result.assignmentDate).isEqualTo(application.currentAssignmentDate)
      assertThat(result.currentPrisonName).isEqualTo(newPrison.prisonName)
      assertThat(result.isTransferredApplication).isTrue()
      assertThat(result.omuEmailAddress).isEqualTo(newPrison.email)
    }
  }

  @Nested
  inner class TransformJpaSummaryToSummary {

    @Test
    fun `transforms an in progress CAS2 application correctly`() {
      val application = Cas2ApplicationSummaryEntityFactory.produce()

      every { mockStatusUpdateTransformer.transformJpaSummaryToLatestStatusUpdateApi(any()) } returns null

      val prison = OffenderManagementUnitEntityFactory().produce()
      every { offenderManagementUnitRepository.findByPrisonCode(any()) } returns prison

      val result = applicationsTransformer.transformJpaSummaryToSummary(
        application,
        "firstName surname",
      )

      assertThat(result.id).isEqualTo(application.id)
      assertThat(result.createdByUserId.toString()).isEqualTo(application.userId)
      assertThat(result.risks).isNull()
      assertThat(result.personName).isEqualTo("firstName surname")
      assertThat(result.crn).isEqualTo(application.crn)
      assertThat(result.nomsNumber).isEqualTo(application.nomsNumber)
      assertThat(result.hdcEligibilityDate).isNull()
      assertThat(result.latestStatusUpdate).isNull()
      assertThat(result.createdByUserName).isEqualTo(application.userName)
    }

    @Test
    fun `transforms a submitted CAS2 application correctly`() {
      val prison = OffenderManagementUnitEntityFactory().produce()
      every { offenderManagementUnitRepository.findByPrisonCode(any()) } returns prison

      val application = Cas2ApplicationSummaryEntity(
        id = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809"),
        crn = "CRNNUM",
        nomsNumber = "NOMNUM",
        userId = "836a9460-b177-433a-a0d9-262509092c9f",
        userName = "first last",
        createdAt = OffsetDateTime.parse("2023-04-19T13:25:00+01:00"),
        submittedAt = OffsetDateTime.parse("2023-04-19T13:25:30+01:00"),
        hdcEligibilityDate = LocalDate.parse("2023-04-29"),
        latestStatusUpdateStatusId = "ae544aee-7170-4794-99fb-703090cbc7db",
        latestStatusUpdateLabel = "my latest status update",
        prisonCode = "BRI",
        allocatedPomUserId = UUID.randomUUID(),
        allocatedPomName = "${randomStringUpperCase(8)} ${randomStringUpperCase(6)}",
        currentPrisonCode = prison.prisonCode,
        assignmentDate = OffsetDateTime.now(),
      )

      every { mockStatusUpdateTransformer.transformJpaSummaryToLatestStatusUpdateApi(any()) } returns LatestCas2StatusUpdate(
        statusId = UUID.fromString(application.latestStatusUpdateStatusId),
        label = application.latestStatusUpdateLabel!!,
      )

      val result = applicationsTransformer.transformJpaSummaryToSummary(
        application,
        "firstName surname",
      )

      assertThat(result.id).isEqualTo(application.id)
      assertThat(result.createdByUserId).isEqualTo(UUID.fromString(application.userId))
      assertThat(result.createdByUserName).isEqualTo(application.userName)
      assertThat(result.allocatedPomUserId).isEqualTo(application.allocatedPomUserId)
      assertThat(result.allocatedPomName).isEqualTo(application.allocatedPomName)
      assertThat(result.currentPrisonName).isEqualTo(prison.prisonName)
      assertThat(result.createdAt).isEqualTo(application.createdAt.toInstant())
      assertThat(result.submittedAt).isEqualTo(application.submittedAt!!.toInstant())
      assertThat(result.type).isEqualTo("CAS2")
      assertThat(result.hdcEligibilityDate).isEqualTo(application.hdcEligibilityDate)
      assertThat(result.crn).isEqualTo(application.crn)
      assertThat(result.nomsNumber).isEqualTo(application.nomsNumber)
      assertThat(result.personName).isEqualTo("firstName surname")
      assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
      assertThat(result.latestStatusUpdate?.label).isEqualTo(application.latestStatusUpdateLabel)
      assertThat(result.latestStatusUpdate?.statusId).isEqualTo(UUID.fromString(application.latestStatusUpdateStatusId))
      assertThat(result.assignmentDate).isEqualTo(application.assignmentDate!!.toLocalDate())
    }
  }
}
