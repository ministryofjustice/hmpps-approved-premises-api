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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2SubmittedApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NomisUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2ApplicationSummaryEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.AssessmentsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.NomisUserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.SubmissionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.TimelineEventsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderManagementUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OffenderManagementUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import java.time.OffsetDateTime
import java.util.UUID

class SubmissionsTransformerTest {
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockNomisUserTransformer = mockk<NomisUserTransformer>()
  private val mockTimelineEventsTransformer = mockk<TimelineEventsTransformer>()
  private val mockAssessmentsTransformer = mockk<AssessmentsTransformer>()
  private val mockOffenderManagementUnitRepository = mockk<OffenderManagementUnitRepository>()
  private val mockCas2UserService = mockk<Cas2UserService>()

  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val applicationTransformer = SubmissionsTransformer(
    objectMapper,
    mockPersonTransformer,
    mockNomisUserTransformer,
    mockTimelineEventsTransformer,
    mockAssessmentsTransformer,
    mockOffenderManagementUnitRepository,
    mockCas2UserService,
  )

  private val nomisUserEntity = NomisUserEntityFactory().produce()
  private val cas2ApplicationFactory = Cas2ApplicationEntityFactory().withCreatedByUser(nomisUserEntity)

  private val submittedCas2ApplicationFactory = cas2ApplicationFactory
    .withSubmittedAt(OffsetDateTime.now())
  private val mockStatusUpdate = Cas2StatusUpdate(
    id = UUID.fromString("c426c63a-be35-421f-a1a0-fc286b60da41"),
    description = "On Waiting List",
    label = "On Waiting List",
    name = "onWaitingList",
  )
  private val mockAssessment = Cas2Assessment(
    id = UUID.fromString("6e631a8c-a013-4bb4-812c-886c8fc25354"),
    statusUpdates = listOf(mockStatusUpdate),
  )

  private val mockNomisUser = mockk<NomisUser>()
  private val prison = OffenderManagementUnitEntityFactory().produce()

  @BeforeEach
  fun setup() {
    every { mockPersonTransformer.transformModelToPersonApi(any()) } returns mockk<Person>()
    every {
      mockNomisUserTransformer.transformJpaToApi(
        nomisUserEntity,
      )
    } returns mockNomisUser
    every { mockTimelineEventsTransformer.transformApplicationToTimelineEvents(any()) } returns listOf(mockk<Cas2TimelineEvent>())
    every { mockAssessmentsTransformer.transformJpaToApiRepresentation(any()) } returns mockAssessment
    every { mockOffenderManagementUnitRepository.findByPrisonCode(any()) } returns prison
    every { mockCas2UserService.getNomisUserById(any()) } returns nomisUserEntity
  }

  @Nested
  inner class TransformJpaToApi {
    @Test
    fun `transforms to API representation with NomisUser, no data, status updates and assessment`() {
      val assessmentEntity = Cas2AssessmentEntityFactory()
        .withApplication(submittedCas2ApplicationFactory.produce())
        .withNacroReferralId("OH123")
        .withAssessorName("Assessor name")
        .produce()

      val jpaEntity = submittedCas2ApplicationFactory.withAssessment(assessmentEntity).produce()

      every { mockAssessmentsTransformer.transformJpaToApiRepresentation(assessmentEntity) } returns mockAssessment
      every { mockCas2UserService.getNomisUserById(any()) } returns nomisUserEntity

      every { mockNomisUserTransformer.transformJpaToApi(any<Cas2ApplicationEntity>()) } returns mockNomisUser

      val transformation = applicationTransformer.transformJpaToApiRepresentation(jpaEntity, mockk())

      assertThat(transformation.submittedBy).isEqualTo(mockNomisUser)

      assertThat(transformation.assessment.statusUpdates).isEqualTo(
        listOf(mockStatusUpdate),
      )

      assertThat(transformation).hasOnlyFields(
        "createdAt",
        "document",
        "id",
        "person",
        "submittedAt",
        "submittedBy",
        "telephoneNumber",
        "timelineEvents",
        "assessment",
        "allocatedPomEmailAddress",
        "allocatedPomName",
        "assignmentDate",
        "currentPrisonName",
        "isTransferredApplication",
        "omuEmailAddress",
      )
    }
  }

  @Nested
  inner class TransformJpaSummaryToCas2SubmittedSummary {
    @Test
    fun `transforms submitted summary application to API summary representation `() {
      val applicationSummary = Cas2ApplicationSummaryEntityFactory.produce()

      val expectedSubmittedApplicationSummary = Cas2SubmittedApplicationSummary(
        id = applicationSummary.id,
        crn = applicationSummary.crn,
        nomsNumber = applicationSummary.nomsNumber,
        createdByUserId = UUID.fromString(applicationSummary.getCreatedById()),
        createdAt = applicationSummary.createdAt.toInstant(),
        submittedAt = applicationSummary.submittedAt!!.toInstant(),
        personName = "Example Offender",
      )

      val transformation = applicationTransformer.transformJpaSummaryToApiRepresentation(
        applicationSummary,
        "Example Offender",
      )

      assertThat(transformation).isEqualTo(expectedSubmittedApplicationSummary)
    }
  }
}
