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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2SubmittedApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NomisUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NomisUserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.AssessmentsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.StatusUpdateTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.SubmissionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.TimelineEventsTransformer
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class SubmissionsTransformerTest {
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockNomisUserTransformer = mockk<NomisUserTransformer>()
  private val mockStatusUpdateTransformer = mockk<StatusUpdateTransformer>()
  private val mockTimelineEventsTransformer = mockk<TimelineEventsTransformer>()
  private val mockAssessmentsTransformer = mockk<AssessmentsTransformer>()

  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val applicationTransformer = SubmissionsTransformer(
    objectMapper,
    mockPersonTransformer,
    mockNomisUserTransformer,
    mockStatusUpdateTransformer,
    mockTimelineEventsTransformer,
    mockAssessmentsTransformer,
  )

  private val user = NomisUserEntityFactory().produce()

  private val cas2ApplicationFactory = Cas2ApplicationEntityFactory().withCreatedByUser(user)

  private val mockStatusUpdateEntity = mockk<Cas2StatusUpdateEntity>()
  private val mockStatusUpdateApi = mockk<Cas2StatusUpdate>()
  private val mockAssessment = mockk<Cas2Assessment>()
  private val submittedCas2ApplicationFactory = cas2ApplicationFactory
    .withStatusUpdates(mutableListOf(mockStatusUpdateEntity, mockStatusUpdateEntity))
    .withSubmittedAt(OffsetDateTime.now())

  private val mockNomisUser = mockk<NomisUser>()

  @BeforeEach
  fun setup() {
    every { mockPersonTransformer.transformModelToPersonApi(any()) } returns mockk<Person>()
    every { mockNomisUserTransformer.transformJpaToApi(any()) } returns mockNomisUser
    every { mockStatusUpdateTransformer.transformJpaToApi(any()) } returns mockStatusUpdateApi
    every { mockTimelineEventsTransformer.transformApplicationToTimelineEvents(any()) } returns listOf(mockk<Cas2TimelineEvent>())
    every { mockAssessmentsTransformer.transformJpaToApiRepresentation(any()) } returns mockAssessment
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

      val transformation = applicationTransformer.transformJpaToApiRepresentation(jpaEntity, mockk())

      assertThat(transformation.submittedBy).isEqualTo(mockNomisUser)

      assertThat(transformation.statusUpdates).isEqualTo(
        listOf(mockStatusUpdateApi, mockStatusUpdateApi),
      )

      assertThat(transformation).hasOnlyFields(
        "createdAt",
        "document",
        "id",
        "outdatedSchema",
        "person",
        "schemaVersion",
        "statusUpdates",
        "submittedAt",
        "submittedBy",
        "telephoneNumber",
        "timelineEvents",
        "assessment",
      )
    }
  }

  @Nested
  inner class TransformJpaSummaryToCas2SubmittedSummary {
    @Test
    fun `transforms submitted summary application to API summary representation `() {
      val applicationSummary = object : Cas2ApplicationSummary {
        override fun getId() = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809")
        override fun getCrn() = "CRN123"
        override fun getNomsNumber() = "NOMS456"
        override fun getCreatedByUserId() = UUID.fromString("836a9460-b177-433a-a0d9-262509092c9f")
        override fun getCreatedAt() = Timestamp(Instant.parse("2023-04-19T13:25:00+01:00").toEpochMilli())
        override fun getSubmittedAt() = Timestamp(Instant.parse("2023-04-19T13:25:30+01:00").toEpochMilli())
        override fun getHdcEligibilityDate() = LocalDate.parse("2023-04-29")
      }

      val expectedSubmittedApplicationSummary = Cas2SubmittedApplicationSummary(
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
