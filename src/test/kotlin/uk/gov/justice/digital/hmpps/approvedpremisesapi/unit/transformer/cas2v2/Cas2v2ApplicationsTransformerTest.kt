package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas2v2

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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LatestCas2v2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2.Cas2v2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2.Cas2v2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2.Cas2v2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2.Cas2v2ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2.Cas2v2AssessmentsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2.Cas2v2StatusUpdateTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2.Cas2v2TimelineEventsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2.Cas2v2UserTransformer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas2v2ApplicationsTransformerTest {
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockCas2v2UserTransformer = mockk<Cas2v2UserTransformer>()
  private val mockCas2v2StatusUpdateTransformer = mockk<Cas2v2StatusUpdateTransformer>()
  private val mockCas2v2TimelineEventsTransformer = mockk<Cas2v2TimelineEventsTransformer>()
  private val mockCas2v2AssessmentsTransformer = mockk<Cas2v2AssessmentsTransformer>()

  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val cas2v2ApplicationsTransformer = Cas2v2ApplicationsTransformer(
    objectMapper,
    mockPersonTransformer,
    mockCas2v2UserTransformer,
    mockCas2v2StatusUpdateTransformer,
    mockCas2v2TimelineEventsTransformer,
    mockCas2v2AssessmentsTransformer,
  )

  private val user = Cas2v2UserEntityFactory().produce()

  private val cas2v2ApplicationFactory = Cas2v2ApplicationEntityFactory().withCreatedByUser(user)

  private val submittedCas2v2ApplicationFactory = cas2v2ApplicationFactory
    .withSubmittedAt(OffsetDateTime.now())

  @BeforeEach
  fun setup() {
    every { mockPersonTransformer.transformModelToPersonApi(any()) } returns mockk<Person>()
    every { mockCas2v2UserTransformer.transformJpaToApi(any()) } returns Cas2v2User(
      id = user.id,
      name = user.name,
      username = user.username,
      authSource = Cas2v2User.AuthSource.nomis,
      isActive = user.isActive,
    )
    every { mockCas2v2StatusUpdateTransformer.transformJpaToApi(any()) } returns mockk<Cas2v2StatusUpdate>()
    every { mockCas2v2StatusUpdateTransformer.transformJpaSummaryToLatestStatusUpdateApi(any()) } returns mockk<LatestCas2v2StatusUpdate>()
    every { mockCas2v2TimelineEventsTransformer.transformApplicationToTimelineEvents(any()) } returns listOf()
  }

  @Nested
  inner class TransformJpaToApi {

    @Test
    fun `transformJpaToApi transforms an in progress CAS-2v2 application correctly`() {
      val application = cas2v2ApplicationFactory
        .withSubmittedAt(null)
        .produce()

      application.applicationOrigin = ApplicationOrigin.prisonBail

      val result = cas2v2ApplicationsTransformer.transformJpaToApi(application, mockk())

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
        "applicationOrigin",
        "bailHearingDate",
      )

      assertThat(result.id).isEqualTo(application.id)
      assertThat(result.createdBy.id).isEqualTo(user.id)
      assertThat(result.status).isEqualTo(ApplicationStatus.inProgress)
      assertThat(result.timelineEvents).isEqualTo(listOf<Cas2TimelineEvent>())
      assertThat(result.applicationOrigin).isEqualTo(ApplicationOrigin.prisonBail)
    }

    @Test
    fun `transformJpaToApi transforms a submitted CAS2v2 application correctly without status updates`() {
      val assessment = Cas2v2Assessment(id = UUID.fromString("3adc18ec-3d0d-4d0f-8b31-6f08e2591c35"))
      every { mockCas2v2AssessmentsTransformer.transformJpaToApiRepresentation(any()) } returns assessment

      val application = submittedCas2v2ApplicationFactory
        .withAssessment(Cas2v2AssessmentEntityFactory().produce()).produce()

      val result = cas2v2ApplicationsTransformer.transformJpaToApi(application, mockk())

      assertThat(result.id).isEqualTo(application.id)
      assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
      assertThat(result.telephoneNumber).isEqualTo(application.telephoneNumber)
      assertThat(result.assessment!!.id).isEqualTo(assessment.id)
    }

    @Test
    fun `transformJpaToApi transforms a submitted CAS2v2 application correctly with status updates`() {
      val mockStatusUpdate = Cas2v2StatusUpdate(
        id = UUID.fromString("c426c63a-be35-421f-a1a0-fc286b60da41"),
        description = "On Waiting List",
        label = "On Waiting List",
        name = "onWaitingList",
      )
      val mockAssessment = Cas2v2Assessment(
        id = UUID.fromString("6e631a8c-a013-4bb4-812c-886c8fc25354"),
        statusUpdates = listOf(mockStatusUpdate),
      )
      every { mockCas2v2AssessmentsTransformer.transformJpaToApiRepresentation(any()) } returns mockAssessment

      val application = submittedCas2v2ApplicationFactory.withAssessment(Cas2v2AssessmentEntityFactory().produce()).produce()

      val result = cas2v2ApplicationsTransformer.transformJpaToApi(application, mockk())

      assertThat(result.id).isEqualTo(application.id)
      assertThat(result.assessment!!.statusUpdates).hasSize(1).containsExactly(mockStatusUpdate)
    }
  }

  @Nested
  inner class TransformJpaSummaryToSummary {

    @Test
    fun `transforms an in progress CAS2v2 application correctly`() {
      val application = Cas2v2ApplicationSummaryEntity(
        id = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809"),
        crn = "CRNNUM",
        nomsNumber = "NOMNUM",
        userId = "836a9460-b177-433a-a0d9-262509092c9f",
        userName = "first last",
        createdAt = OffsetDateTime.parse("2023-04-19T13:25:00+01:00"),
        submittedAt = null,
        hdcEligibilityDate = null,
        latestStatusUpdateLabel = null,
        latestStatusUpdateStatusId = null,
        prisonCode = "BRI",
      )

      every { mockCas2v2StatusUpdateTransformer.transformJpaSummaryToLatestStatusUpdateApi(any()) } returns null

      val result = cas2v2ApplicationsTransformer.transformJpaSummaryToSummary(
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
      assertThat(result.createdByUserName).isEqualTo("first last")
    }

    @Test
    fun `transforms a submitted CAS2v2 application correctly`() {
      val application = Cas2v2ApplicationSummaryEntity(
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
      )

      every { mockCas2v2StatusUpdateTransformer.transformJpaSummaryToLatestStatusUpdateApi(any()) } returns LatestCas2v2StatusUpdate(
        statusId = UUID.fromString(application.latestStatusUpdateStatusId),
        label = application.latestStatusUpdateLabel!!,
      )

      val result = cas2v2ApplicationsTransformer.transformJpaSummaryToSummary(
        application,
        "firstName surname",
      )

      assertThat(result.id).isEqualTo(application.id)
      assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
      assertThat(result.hdcEligibilityDate).isEqualTo("2023-04-29")
      assertThat(result.personName).isEqualTo("firstName surname")
      assertThat(result.crn).isEqualTo(application.crn)
      assertThat(result.nomsNumber).isEqualTo(application.nomsNumber)
      assertThat(result.latestStatusUpdate?.label).isEqualTo("my latest status update")
      assertThat(result.latestStatusUpdate?.statusId).isEqualTo(UUID.fromString("ae544aee-7170-4794-99fb-703090cbc7db"))
      assertThat(result.createdByUserName).isEqualTo("first last")
    }
  }
}
