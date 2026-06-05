package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.unit.transformer

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NomisUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcSubmittedApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcTimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2ApplicationSummaryEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcAssessmentsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcNomisUserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcSubmissionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcTimelineEventsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderManagementUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OffenderManagementUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.JsonMapperFactory
import java.time.OffsetDateTime
import java.util.UUID

class SubmissionsTransformerTest {
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockCas2HdcNomisUserTransformer = mockk<Cas2HdcNomisUserTransformer>()
  private val mockCas2HdcTimelineEventsTransformer = mockk<Cas2HdcTimelineEventsTransformer>()
  private val mockCas2HdcAssessmentsTransformer = mockk<Cas2HdcAssessmentsTransformer>()
  private val mockOffenderManagementUnitRepository = mockk<OffenderManagementUnitRepository>()
  private val mockCas2HdcUserService = mockk<Cas2HdcUserService>()

  private val jsonMapper = JsonMapperFactory.createJackson3JsonMapper()

  private val applicationTransformer = Cas2HdcSubmissionsTransformer(
    jsonMapper,
    mockPersonTransformer,
    mockCas2HdcNomisUserTransformer,
    mockCas2HdcTimelineEventsTransformer,
    mockCas2HdcAssessmentsTransformer,
    mockOffenderManagementUnitRepository,
    mockCas2HdcUserService,
  )

  private val nomisUserEntity = Cas2UserEntityFactory().produce()
  private val cas2ApplicationFactory = Cas2ApplicationEntityFactory().withCreatedByUser(nomisUserEntity)

  private val submittedCas2ApplicationFactory = cas2ApplicationFactory
    .withSubmittedAt(OffsetDateTime.now())
  private val mockStatusUpdate = Cas2HdcStatusUpdate(
    id = UUID.fromString("c426c63a-be35-421f-a1a0-fc286b60da41"),
    description = "On Waiting List",
    label = "On Waiting List",
    name = "onWaitingList",
  )
  private val mockAssessment = Cas2HdcAssessment(
    id = UUID.fromString("6e631a8c-a013-4bb4-812c-886c8fc25354"),
    statusUpdates = listOf(mockStatusUpdate),
  )

  private val mockNomisUser = mockk<NomisUser>()
  private val prison = OffenderManagementUnitEntityFactory().produce()

  @BeforeEach
  fun setup() {
    every { mockPersonTransformer.transformModelToPersonApi(any()) } returns mockk<Person>()
    every {
      mockCas2HdcNomisUserTransformer.transformJpaToApi(
        nomisUserEntity,
      )
    } returns mockNomisUser
    every { mockCas2HdcTimelineEventsTransformer.transformApplicationToTimelineEvents(any()) } returns listOf(mockk<Cas2HdcTimelineEvent>())
    every { mockCas2HdcAssessmentsTransformer.transformJpaToApiRepresentation(any()) } returns mockAssessment
    every { mockOffenderManagementUnitRepository.findByPrisonCode(any()) } returns prison
    every { mockCas2HdcUserService.getNomisUserById(any(), eq(Cas2HdcServiceOrigin.HDC)) } returns nomisUserEntity
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

      every { mockCas2HdcAssessmentsTransformer.transformJpaToApiRepresentation(assessmentEntity) } returns mockAssessment
      every { mockCas2HdcUserService.getNomisUserById(any(), eq(Cas2HdcServiceOrigin.HDC)) } returns nomisUserEntity

      every { mockCas2HdcNomisUserTransformer.transformJpaToApi(any<Cas2ApplicationEntity>()) } returns mockNomisUser

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

      val expectedSubmittedApplicationSummary = Cas2HdcSubmittedApplicationSummary(
        id = applicationSummary.id,
        crn = applicationSummary.crn,
        nomsNumber = applicationSummary.nomsNumber,
        createdByUserId = UUID.fromString(applicationSummary.userId),
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
