package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.transformer

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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NomisUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2ApplicationSummaryEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.LatestCas2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.AssessmentsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.Cas2ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.NomisUserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.StatusUpdateTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.TimelineEventsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderManagementUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OffenderManagementUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas2ApplicationsTransformerTest {
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockNomisUserTransformer = mockk<NomisUserTransformer>()
  private val mockStatusUpdateTransformer = mockk<StatusUpdateTransformer>()
  private val mockTimelineEventsTransformer = mockk<TimelineEventsTransformer>()
  private val mockAssessmentsTransformer = mockk<AssessmentsTransformer>()
  private val cas2UserService = mockk<Cas2UserService>()
  private val offenderManagementUnitRepository = mockk<OffenderManagementUnitRepository>()

  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val cas2ApplicationsTransformer = Cas2ApplicationsTransformer(
    objectMapper,
    mockPersonTransformer,
    mockNomisUserTransformer,
    mockStatusUpdateTransformer,
    mockTimelineEventsTransformer,
    mockAssessmentsTransformer,
    cas2UserService,
    offenderManagementUnitRepository,
  )

  private val nomisUserEntity = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).produce()
  private val nomisUser = NomisUser(
    id = this.nomisUserEntity.id,
    name = this.nomisUserEntity.name,
    nomisUsername = this.nomisUserEntity.username,
    isActive = this.nomisUserEntity.isActive,
  )

  private val cas2ApplicationFactory = Cas2ApplicationEntityFactory().withCreatedByUser(this.nomisUserEntity)

  private val submittedCas2ApplicationFactory = cas2ApplicationFactory
    .withSubmittedAt(OffsetDateTime.now())

  @BeforeEach
  fun setup() {
    every { mockPersonTransformer.transformModelToPersonApi(any()) } returns mockk<Person>()
    every {
      mockNomisUserTransformer.transformJpaToApi(
        nomisUserEntity,
      )
    } returns nomisUser
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
        .withCreatedByUser(nomisUserEntity)
        .produce()

      every { mockNomisUserTransformer.transformJpaToApi(any<Cas2ApplicationEntity>()) } returns nomisUser
      val result = cas2ApplicationsTransformer.transformJpaToApi(application, mockk())

      assertThat(result).hasOnlyFields(
        "id",
        "person",
        "createdBy",
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
        "applicationOrigin",
        "bailHearingDate",
      )

      assertThat(result.id).isEqualTo(application.id)
      assertThat(result.createdBy.id).isEqualTo(nomisUserEntity.id)
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
      assertThat(result.applicationOrigin).isEqualTo(ApplicationOrigin.homeDetentionCurfew)
      assertThat(result.bailHearingDate).isNull()
    }

    @Test
    fun `transformJpaToApi transforms a submitted CAS2 application correctly without status updates`() {
      val assessment = Cas2Assessment(id = UUID.fromString("3adc18ec-3d0d-4d0f-8b31-6f08e2591c35"))
      every { mockAssessmentsTransformer.transformJpaToApiRepresentation(any()) } returns assessment
      every { cas2UserService.getNomisUserById(any()) } returns nomisUserEntity
      val prison = OffenderManagementUnitEntityFactory().produce()
      every { offenderManagementUnitRepository.findByPrisonCode(any()) } returns prison

      val application = submittedCas2ApplicationFactory
        .withAssessment(Cas2AssessmentEntityFactory().produce())
        .withReferringPrisonCode("PRI")
        .withApplicationAssignments().produce()

      every { mockNomisUserTransformer.transformJpaToApi(any<Cas2ApplicationEntity>()) } returns nomisUser
      val result = cas2ApplicationsTransformer.transformJpaToApi(application, mockk())

      assertThat(result.id).isEqualTo(application.id)
      assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
      assertThat(result.telephoneNumber).isEqualTo(application.telephoneNumber)
      assertThat(result.assessment!!.id).isEqualTo(assessment.id)

      // these are assigned after an application is submitted
      assertThat(result.submittedAt).isEqualTo(application.submittedAt!!.toInstant())
      assertThat(result.allocatedPomEmailAddress).isEqualTo(nomisUserEntity.email)
      assertThat(result.allocatedPomName).isEqualTo(nomisUserEntity.name)
      assertThat(result.assignmentDate).isEqualTo(application.currentAssignmentDate)
      assertThat(result.currentPrisonName).isEqualTo(prison.prisonName)
      assertThat(result.isTransferredApplication).isFalse()
      assertThat(result.omuEmailAddress).isEqualTo(prison.email)
      assertThat(result.applicationOrigin).isEqualTo(ApplicationOrigin.homeDetentionCurfew)
      assertThat(result.bailHearingDate).isNull()
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
      every { cas2UserService.getNomisUserById(any()) } returns nomisUserEntity

      val application = submittedCas2ApplicationFactory.withAssessment(Cas2AssessmentEntityFactory().produce())
        .withReferringPrisonCode("PRI")
        .withApplicationAssignments()
        .produce()

      every { mockNomisUserTransformer.transformJpaToApi(any<Cas2ApplicationEntity>()) } returns nomisUser
      val result = cas2ApplicationsTransformer.transformJpaToApi(application, mockk())

      assertThat(result.id).isEqualTo(application.id)
      assertThat(result.assessment!!.statusUpdates).hasSize(1).containsExactly(mockStatusUpdate)

      // these are assigned after an application is submitted
      assertThat(result.submittedAt).isEqualTo(application.submittedAt!!.toInstant())
      assertThat(result.allocatedPomEmailAddress).isEqualTo(nomisUserEntity.email)
      assertThat(result.allocatedPomName).isEqualTo(nomisUserEntity.name)
      assertThat(result.assignmentDate).isEqualTo(application.currentAssignmentDate)
      assertThat(result.currentPrisonName).isEqualTo(prison.prisonName)
      assertThat(result.isTransferredApplication).isFalse()
      assertThat(result.omuEmailAddress).isEqualTo(prison.email)
    }

    @Test
    fun `transformJpaToApi transforms a submitted CAS2 application correctly which has been transferred`() {
      val assessment = Cas2Assessment(id = UUID.fromString("3adc18ec-3d0d-4d0f-8b31-6f08e2591c35"))
      every { mockAssessmentsTransformer.transformJpaToApiRepresentation(any()) } returns assessment
      every { cas2UserService.getNomisUserById(any()) } returns nomisUserEntity
      val prison = OffenderManagementUnitEntityFactory().produce()
      val newPrison = OffenderManagementUnitEntityFactory().withPrisonCode("NEW").withPrisonName("New Prison")
        .withEmail("test@test.co.uk").produce()
      every { offenderManagementUnitRepository.findByPrisonCode(eq(prison.prisonCode)) } returns prison
      every { offenderManagementUnitRepository.findByPrisonCode(eq(newPrison.prisonCode)) } returns newPrison

      val application = submittedCas2ApplicationFactory
        .withAssessment(Cas2AssessmentEntityFactory().produce())
        .withReferringPrisonCode(prison.prisonCode)
        .withApplicationAssignments().produce()

      application.applicationAssignments.add(
        Cas2ApplicationAssignmentEntity(
          UUID.randomUUID(),
          application,
          newPrison.prisonCode,
          null,
          OffsetDateTime.now(),
        ),
      )

      every { mockNomisUserTransformer.transformJpaToApi(any<Cas2ApplicationEntity>()) } returns nomisUser
      val result = cas2ApplicationsTransformer.transformJpaToApi(application, mockk())

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

    @Test
    fun `check bail fields transformed correctly`() {
      val now = OffsetDateTime.now().toLocalDate()
      val application = cas2ApplicationFactory
        .withSubmittedAt(null)
        .withApplicationOrigin(ApplicationOrigin.courtBail)
        .withBailHearingDate(now)
        .produce()

      every { mockNomisUserTransformer.transformJpaToApi(any<Cas2ApplicationEntity>()) } returns nomisUser
      val result = cas2ApplicationsTransformer.transformJpaToApi(application, mockk())

      assertThat(result).hasOnlyFields(
        "id",
        "person",
        "createdBy",
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
        "applicationOrigin",
        "bailHearingDate",
      )

      assertThat(result.id).isEqualTo(application.id)
      assertThat(result.createdBy.id).isEqualTo(nomisUserEntity.id)
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
      assertThat(result.applicationOrigin).isEqualTo(ApplicationOrigin.courtBail)
      assertThat(result.bailHearingDate).isEqualTo(now)
    }
  }

  @Nested
  inner class TransformJpaSummaryToSummary {

    fun createApplication(prisonCode: String) = Cas2ApplicationSummaryEntity(
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
      currentPrisonCode = prisonCode,
      assignmentDate = OffsetDateTime.now(),
      // BAIL-WIP - come back and check application summary view
      applicationOrigin = ApplicationOrigin.prisonBail.toString(),
    )

    @Test
    fun `transforms an in progress CAS2 application correctly`() {
      val application = Cas2ApplicationSummaryEntityFactory.produce()

      every { mockStatusUpdateTransformer.transformJpaSummaryToLatestStatusUpdateApi(any()) } returns null

      val prison = OffenderManagementUnitEntityFactory().produce()
      every { offenderManagementUnitRepository.findByPrisonCode(any()) } returns prison

      val result = cas2ApplicationsTransformer.transformJpaSummaryToSummary(
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
      assertThat(result.applicationOrigin).isEqualTo(ApplicationOrigin.homeDetentionCurfew)
      assertThat(result.bailHearingDate).isNull()
    }

    @Test
    fun `transforms a submitted CAS2 application correctly`() {
      val prison = OffenderManagementUnitEntityFactory().produce()
      every { offenderManagementUnitRepository.findByPrisonCode(any()) } returns prison

      val application = createApplication(prisonCode = "BRI")
      every { mockStatusUpdateTransformer.transformJpaSummaryToLatestStatusUpdateApi(any()) } returns LatestCas2StatusUpdate(
        statusId = UUID.fromString(application.latestStatusUpdateStatusId),
        label = application.latestStatusUpdateLabel!!,
      )

      val result = cas2ApplicationsTransformer.transformJpaSummaryToSummary(
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

    @Test
    fun `uses prison code when name not available`() {
      every { offenderManagementUnitRepository.findByPrisonCode(any()) } returns null

      val application = createApplication(prisonCode = "BRI")

      every { mockStatusUpdateTransformer.transformJpaSummaryToLatestStatusUpdateApi(any()) } returns LatestCas2StatusUpdate(
        statusId = UUID.fromString(application.latestStatusUpdateStatusId),
        label = application.latestStatusUpdateLabel!!,
      )

      val result = cas2ApplicationsTransformer.transformJpaSummaryToSummary(
        application,
        "firstName surname",
      )

      assertThat(result.currentPrisonName).isEqualTo(application.currentPrisonCode)
    }

    @Test
    fun `check bail fields transformed correctly`() {
      every { offenderManagementUnitRepository.findByPrisonCode(any()) } returns null

      val now = OffsetDateTime.now().toLocalDate()
      val applicationSummary = Cas2ApplicationSummaryEntity(
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
        currentPrisonCode = "BRI",
        assignmentDate = OffsetDateTime.now(),
        applicationOrigin = ApplicationOrigin.prisonBail.toString(),
        bailHearingDate = now,
      )

      val result = cas2ApplicationsTransformer.transformJpaSummaryToSummary(
        applicationSummary,
        "firstName surname",
      )

      assertThat(result.id).isEqualTo(applicationSummary.id)
      assertThat(result.applicationOrigin).isEqualTo(ApplicationOrigin.prisonBail)
      assertThat(result.bailHearingDate).isEqualTo(now)
    }
  }
}
