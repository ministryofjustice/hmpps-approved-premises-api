package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.unit.transformer

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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2SubmittedApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.Cas2v2AssessmentsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.Cas2v2SubmissionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.Cas2v2TimelineEventsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.Cas2v2UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas2v2SubmissionsTransformerTest {

  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockCas2v2UserTransformer = mockk<Cas2v2UserTransformer>()
  private val mockCas2v2TimelineEventsTransformer = mockk<Cas2v2TimelineEventsTransformer>()
  private val mockCas2v2AssessmentsTransformer = mockk<Cas2v2AssessmentsTransformer>()

  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val applicationTransformer = Cas2v2SubmissionsTransformer(
    objectMapper,
    mockPersonTransformer,
    mockCas2v2UserTransformer,
    mockCas2v2TimelineEventsTransformer,
    mockCas2v2AssessmentsTransformer,
  )

  private val user = Cas2UserEntityFactory()
    .withServiceOrigin(Cas2ServiceOrigin.BAIL)
    .produce()

  private val cas2ApplicationFactory = Cas2ApplicationEntityFactory()
    .withServiceOrigin(Cas2ServiceOrigin.BAIL)
    .withCreatedByUser(user)

  private val submittedCas2ApplicationFactory = cas2ApplicationFactory
    .withSubmittedAt(OffsetDateTime.now())
  private val mockStatusUpdate = Cas2v2StatusUpdate(
    id = UUID.fromString("c426c63a-be35-421f-a1a0-fc286b60da41"),
    description = "On Waiting List",
    label = "On Waiting List",
    name = "onWaitingList",
  )
  private val mockAssessment = Cas2v2Assessment(
    id = UUID.fromString("6e631a8c-a013-4bb4-812c-886c8fc25354"),
    statusUpdates = listOf(mockStatusUpdate),
  )

  private val mockCas2v2User = mockk<Cas2v2User>()

  @BeforeEach
  fun setup() {
    every { mockPersonTransformer.transformModelToPersonApi(any()) } returns mockk<Person>()
    every { mockCas2v2UserTransformer.transformJpaToApi(any()) } returns mockCas2v2User
    every { mockCas2v2TimelineEventsTransformer.transformApplicationToTimelineEvents(any()) } returns listOf(mockk<Cas2TimelineEvent>())
    every { mockCas2v2AssessmentsTransformer.transformJpaToApiRepresentation(any()) } returns mockAssessment
  }

  @Nested
  inner class TransformJpaToApi {
    @Test
    fun `transforms to API representation with NomisUser, no data, status updates and assessment`() {
      val assessmentEntity = Cas2AssessmentEntityFactory()
        .withApplication(submittedCas2ApplicationFactory.produce())
        .withNacroReferralId("OH123")
        .withAssessorName("Assessor name")
        .withServiceOrigin(Cas2ServiceOrigin.BAIL)
        .produce()

      val jpaEntity = submittedCas2ApplicationFactory.withAssessment(assessmentEntity).produce()

      every { mockCas2v2AssessmentsTransformer.transformJpaToApiRepresentation(assessmentEntity) } returns mockAssessment

      val transformation = applicationTransformer.transformJpaToApiRepresentation(jpaEntity, mockk())

      assertThat(transformation.submittedBy).isEqualTo(mockCas2v2User)

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
        "applicationOrigin",
        "assessment",
        "bailHearingDate",
      )
    }
  }

  @Nested
  inner class TransformJpaSummaryToCas2v2SubmittedSummary {
    @Test
    fun `transforms submitted summary application to API summary representation `() {
      val applicationSummary = Cas2ApplicationSummaryEntity(
        id = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809"),
        crn = "CRN123",
        nomsNumber = "NOMS456",
        userId = "836a9460-b177-433a-a0d9-262509092c9f",
        userName = "first last",
        createdAt = OffsetDateTime.parse("2023-04-19T13:25:00+01:00"),
        submittedAt = OffsetDateTime.parse("2023-04-19T13:25:30+01:00"),
        hdcEligibilityDate = LocalDate.parse("2023-04-29"),
        latestStatusUpdateLabel = null,
        latestStatusUpdateStatusId = null,
        prisonCode = "BRI",
        allocatedPomName = null,
        allocatedPomUserId = null,
        currentPrisonCode = null,
        assignmentDate = null,
      )

      val expectedSubmittedApplicationSummary = Cas2v2SubmittedApplicationSummary(
        id = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809"),
        crn = "CRN123",
        nomsNumber = "NOMS456",
        createdByUserId = UUID.fromString("836a9460-b177-433a-a0d9-262509092c9f"),
        createdAt = Instant.parse("2023-04-19T13:25:00+01:00"),
        submittedAt = Instant.parse("2023-04-19T13:25:30+01:00"),
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
