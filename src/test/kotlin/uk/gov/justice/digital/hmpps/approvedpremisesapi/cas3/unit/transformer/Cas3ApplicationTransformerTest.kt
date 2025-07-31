package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3ApplicationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentClarificationNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

class Cas3ApplicationTransformerTest {
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockRisksTransformer = mockk<RisksTransformer>()

  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val user = UserEntityFactory()
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
    }
    .produce()

  private val allocatedToUser = UserEntityFactory()
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
    }
    .produce()

  private val temporaryAccommodationApplicationEntityFactory = TemporaryAccommodationApplicationEntityFactory()
    .withCreatedByUser(user)

  private val submittedTemporaryAccommodationApplicationFactory = temporaryAccommodationApplicationEntityFactory
    .withArrivalDate(OffsetDateTime.now().toLocalDate().plusDays(7))
    .withSubmittedAt(OffsetDateTime.now())

  private val awaitingClarificationNoteFactory = AssessmentClarificationNoteEntityFactory()
    .withCreatedBy(allocatedToUser)

  private val completedClarificationNoteFactory = AssessmentClarificationNoteEntityFactory()
    .withResponse("Response")
    .withCreatedBy(allocatedToUser)

  private val assessmentFactory = ApprovedPremisesAssessmentEntityFactory()
    .withAllocatedToUser(allocatedToUser)

  private val cas3ApplicationsTransformer = Cas3ApplicationTransformer(objectMapper, mockPersonTransformer, mockRisksTransformer)

  @BeforeEach
  fun setup() {
    every { mockPersonTransformer.transformModelToPersonApi(any()) } returns mockk<Person>()
    every { mockRisksTransformer.transformDomainToApi(any<PersonRisks>(), any<String>()) } returns mockk()
  }

  @Test
  fun `transformJpaToApi transforms an in progress application correctly`() {
    val application = temporaryAccommodationApplicationEntityFactory
      .withSubmittedAt(null)
      .withArrivalDate(null)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withApArea(
            ApAreaEntityFactory()
              .produce(),
          )
          .produce()
      }
      .produce()

    val result = cas3ApplicationsTransformer.transformJpaToApi(application, mockk())

    assertThat(result.id).isEqualTo(application.id)
    assertThat(result.createdByUserId).isEqualTo(user.id)
    assertThat(result.status).isEqualTo(ApplicationStatus.inProgress)
    assertThat(result.risks).isNotNull
    assertThat(result.arrivalDate).isNull()
    assertThat(result.offenceId).isEqualTo(application.offenceId)
    assertThat(result.assessmentId).isNull()
  }

  @Test
  fun `transformJpaToApi populates assessmentId`() {
    val application = temporaryAccommodationApplicationEntityFactory.withDefaults().produce()
    val assessment = TemporaryAccommodationAssessmentEntityFactory().withApplication(application).produce()
    application.assessments = mutableListOf(assessment)
    val result = cas3ApplicationsTransformer.transformJpaToApi(application, mockk())

    assertThat(result.assessmentId).isEqualTo(assessment.id)
  }

  @Test
  fun `transformJpaToApi transforms a submitted application correctly`() {
    val application = submittedTemporaryAccommodationApplicationFactory
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withApArea(
            ApAreaEntityFactory()
              .produce(),
          )
          .produce()
      }
      .produce()

    val result = cas3ApplicationsTransformer.transformJpaToApi(application, mockk())

    assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
    assertThat(result.arrivalDate).isEqualTo(application.arrivalDate!!.toInstant())
    assertThat(result.offenceId).isEqualTo(application.offenceId)
  }

  @Test
  fun `transformJpaToApi sets status as 'requested further information' when transforming a application with requested clarification notes`() {
    val application = submittedTemporaryAccommodationApplicationFactory
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withApArea(
            ApAreaEntityFactory()
              .produce(),
          )
          .produce()
      }
      .produce()
    val assessment = assessmentFactory.withApplication(application).produce()

    application.assessments = mutableListOf(assessment)
    assessment.clarificationNotes = mutableListOf(
      completedClarificationNoteFactory
        .withAssessment(assessment)
        .produce(),
      awaitingClarificationNoteFactory
        .withAssessment(assessment)
        .produce(),
    )

    val result = cas3ApplicationsTransformer.transformJpaToApi(application, mockk())

    assertThat(result.status).isEqualTo(ApplicationStatus.requestedFurtherInformation)
  }

  @Test
  fun `transformJpaToApi sets status as 'submitted' when transforming application with a completed clarification note`() {
    val application = submittedTemporaryAccommodationApplicationFactory
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withApArea(
            ApAreaEntityFactory()
              .produce(),
          )
          .produce()
      }
      .produce()
    val assessment = assessmentFactory.withApplication(application).produce()

    assessment.clarificationNotes = mutableListOf(
      completedClarificationNoteFactory
        .withAssessment(assessment)
        .produce(),
    )

    application.assessments = mutableListOf(assessment)

    val result = cas3ApplicationsTransformer.transformJpaToApi(application, mockk())

    assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
  }

  @Test
  fun `transformJpaToApi uses the latest assessment`() {
    val application = submittedTemporaryAccommodationApplicationFactory
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withApArea(
            ApAreaEntityFactory()
              .produce(),
          )
          .produce()
      }
      .produce()
    val oldAssessment = assessmentFactory.withApplication(application)
      .withCreatedAt(OffsetDateTime.parse("2022-09-01T12:34:56.789Z"))
      .produce()
    val latestAssessment = assessmentFactory.withApplication(application)
      .withCreatedAt(OffsetDateTime.now())
      .produce()

    oldAssessment.clarificationNotes = mutableListOf(
      awaitingClarificationNoteFactory
        .withAssessment(oldAssessment)
        .produce(),
    )

    latestAssessment.clarificationNotes = mutableListOf(
      completedClarificationNoteFactory
        .withAssessment(latestAssessment)
        .produce(),
    )

    application.assessments = mutableListOf(oldAssessment, latestAssessment)

    val result = cas3ApplicationsTransformer.transformJpaToApi(application, mockk())

    assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
  }

  @Test
  fun `transformJpaToApiSummary transforms an in progress application correctly`() {
    val application = object : TemporaryAccommodationApplicationSummary {
      override fun getRiskRatings() = objectMapper.writeValueAsString(PersonRisksFactory().produce())
      override fun getId() = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809")
      override fun getCrn() = randomStringMultiCaseWithNumbers(6)
      override fun getCreatedByUserId() = UUID.fromString("836a9460-b177-433a-a0d9-262509092c9f")
      override fun getCreatedAt() = Instant.now().randomDateTimeBefore(10)
      override fun getSubmittedAt() = null
      override fun getLatestAssessmentSubmittedAt() = null
      override fun getLatestAssessmentDecision() = null
      override fun getLatestAssessmentHasClarificationNotesWithoutResponse() = false
      override fun getHasBooking() = false
    }

    val result = cas3ApplicationsTransformer.transformDomainToCas3ApplicationSummary(
      application,
      mockk(),
    )

    assertThat(result.id).isEqualTo(application.getId())
    assertThat(result.createdByUserId).isEqualTo(application.getCreatedByUserId())
    assertThat(result.status).isEqualTo(ApplicationStatus.inProgress)
    assertThat(result.risks).isNotNull
  }

  @Test
  fun `transformJpaToApiSummary transforms a submitted application correctly`() {
    val application = object : TemporaryAccommodationApplicationSummary {
      override fun getRiskRatings() = objectMapper.writeValueAsString(PersonRisksFactory().produce())
      override fun getId() = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809")
      override fun getCrn() = randomStringMultiCaseWithNumbers(6)
      override fun getCreatedByUserId() = UUID.fromString("836a9460-b177-433a-a0d9-262509092c9f")
      override fun getCreatedAt() = Instant.now().randomDateTimeBefore(30)
      override fun getSubmittedAt() = Instant.now().randomDateTimeBefore(3)
      override fun getLatestAssessmentSubmittedAt() = null
      override fun getLatestAssessmentDecision() = null
      override fun getLatestAssessmentHasClarificationNotesWithoutResponse() = false
      override fun getHasBooking() = false
    }

    val result = cas3ApplicationsTransformer.transformDomainToCas3ApplicationSummary(
      application,
      mockk(),
    )

    assertThat(result.id).isEqualTo(application.getId())
    assertThat(result.createdByUserId).isEqualTo(application.getCreatedByUserId())
    assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
    assertThat(result.risks).isNotNull
  }

  @Test
  fun `transformJpaToApiSummary transforms application with rejected assessment correctly`() {
    val application = object : TemporaryAccommodationApplicationSummary {
      override fun getRiskRatings() = objectMapper.writeValueAsString(PersonRisksFactory().produce())
      override fun getId() = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809")
      override fun getCrn() = randomStringMultiCaseWithNumbers(6)
      override fun getCreatedByUserId() = UUID.fromString("836a9460-b177-433a-a0d9-262509092c9f")
      override fun getCreatedAt() = Instant.parse("2025-04-19T13:25:00+01:00")
      override fun getSubmittedAt() = Instant.parse("2025-04-19T13:25:30+01:00")
      override fun getLatestAssessmentSubmittedAt() = Instant.parse("2025-04-23T09:01:31+05:00")
      override fun getLatestAssessmentDecision() = AssessmentDecision.REJECTED
      override fun getLatestAssessmentHasClarificationNotesWithoutResponse() = false
      override fun getHasBooking() = false
    }

    val result = cas3ApplicationsTransformer.transformDomainToCas3ApplicationSummary(
      application,
      mockk(),
    )

    assertThat(result.id).isEqualTo(application.getId())
    assertThat(result.createdByUserId).isEqualTo(application.getCreatedByUserId())
    assertThat(result.status).isEqualTo(ApplicationStatus.rejected)
    assertThat(result.risks).isNotNull
  }
}
