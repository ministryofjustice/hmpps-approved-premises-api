package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentClarificationNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer
import java.time.OffsetDateTime

class ApplicationsTransformerTest {
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockRisksTransformer = mockk<RisksTransformer>()

  private val applicationsTransformer = ApplicationsTransformer(
    jacksonObjectMapper(),
    mockPersonTransformer,
    mockRisksTransformer
  )

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

  private val approvedPremisesApplicationFactory = ApprovedPremisesApplicationEntityFactory()
    .withCreatedByUser(user)

  private val temporaryAccommodationApplicationEntityFactory = TemporaryAccommodationApplicationEntityFactory()
    .withCreatedByUser(user)

  private val assessmentFactory = AssessmentEntityFactory()
    .withAllocatedToUser(allocatedToUser)

  private val completedClarificationNoteFactory = AssessmentClarificationNoteEntityFactory()
    .withResponse("Response")
    .withCreatedBy(allocatedToUser)

  private val awaitingClarificationNoteFactory = AssessmentClarificationNoteEntityFactory()
    .withCreatedBy(allocatedToUser)

  private val unSubmittedApprovedPremisesApplicationFactory = approvedPremisesApplicationFactory
    .withSubmittedAt(null)

  private val submittedApprovedPremisesApplicationFactory = approvedPremisesApplicationFactory
    .withSubmittedAt(OffsetDateTime.now())

  private val submittedTemporaryAccommodationApplicationFactory = temporaryAccommodationApplicationEntityFactory
    .withSubmittedAt(OffsetDateTime.now())

  @BeforeEach
  fun setup() {
    every { mockPersonTransformer.transformModelToApi(any<OffenderDetailSummary>(), any()) } returns mockk<Person>()
    every { mockRisksTransformer.transformDomainToApi(any<PersonRisks>(), any<String>()) } returns mockk()
  }

  @Test
  fun `transformJpaToApi transforms an in progress Approved Premises application correctly`() {
    val application = approvedPremisesApplicationFactory.withSubmittedAt(null).produce()

    val result = applicationsTransformer.transformJpaToApi(application, mockk(), mockk()) as ApprovedPremisesApplication

    assertThat(result.id).isEqualTo(application.id)
    assertThat(result.createdByUserId).isEqualTo(user.id)
    assertThat(result.status).isEqualTo(ApplicationStatus.inProgress)
  }

  @Test
  fun `transformJpaToApi transforms an in progress Temporary Accommodation application correctly`() {
    val application = temporaryAccommodationApplicationEntityFactory.withSubmittedAt(null).produce()

    val result = applicationsTransformer.transformJpaToApi(application, mockk(), mockk()) as TemporaryAccommodationApplication

    assertThat(result.id).isEqualTo(application.id)
    assertThat(result.createdByUserId).isEqualTo(user.id)
    assertThat(result.status).isEqualTo(ApplicationStatus.inProgress)
  }

  @Test
  fun `transformJpaToApi transforms a submitted Approved Premises application correctly`() {
    val application = submittedApprovedPremisesApplicationFactory.produce()

    val result = applicationsTransformer.transformJpaToApi(application, mockk(), mockk()) as ApprovedPremisesApplication

    assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
  }

  @Test
  fun `transformJpaToApi transforms a submitted Temporary Accommodation application correctly`() {
    val application = submittedTemporaryAccommodationApplicationFactory.produce()

    val result = applicationsTransformer.transformJpaToApi(application, mockk(), mockk()) as TemporaryAccommodationApplication

    assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
  }

  @Test
  fun `transformJpaToApi transforms an Approved Premises application with requested clarification notes correctly`() {
    val application = submittedApprovedPremisesApplicationFactory.produce()
    val assessment = assessmentFactory.withApplication(application).produce()

    application.assessments = mutableListOf(assessment)
    assessment.clarificationNotes = mutableListOf(
      completedClarificationNoteFactory
        .withAssessment(assessment)
        .produce(),
      awaitingClarificationNoteFactory
        .withAssessment(assessment)
        .produce()
    )

    val result = applicationsTransformer.transformJpaToApi(application, mockk(), mockk()) as ApprovedPremisesApplication

    assertThat(result.status).isEqualTo(ApplicationStatus.requestedFurtherInformation)
  }

  @Test
  fun `transformJpaToApi transforms a Temporary Accommodation application with requested clarification notes correctly`() {
    val application = submittedTemporaryAccommodationApplicationFactory.produce()
    val assessment = assessmentFactory.withApplication(application).produce()

    application.assessments = mutableListOf(assessment)
    assessment.clarificationNotes = mutableListOf(
      completedClarificationNoteFactory
        .withAssessment(assessment)
        .produce(),
      awaitingClarificationNoteFactory
        .withAssessment(assessment)
        .produce()
    )

    val result = applicationsTransformer.transformJpaToApi(application, mockk(), mockk()) as TemporaryAccommodationApplication

    assertThat(result.status).isEqualTo(ApplicationStatus.requestedFurtherInformation)
  }

  @Test
  fun `transformJpaToApi transforms an Approved Premises application with a completed clarification note correctly`() {
    val application = submittedApprovedPremisesApplicationFactory.produce()
    val assessment = assessmentFactory.withApplication(application).produce()

    assessment.clarificationNotes = mutableListOf(
      completedClarificationNoteFactory
        .withAssessment(assessment)
        .produce()
    )

    application.assessments = mutableListOf(assessment)

    val result = applicationsTransformer.transformJpaToApi(application, mockk(), mockk()) as ApprovedPremisesApplication

    assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
  }

  @Test
  fun `transformJpaToApi transforms a Temporary Accommodation application with a completed clarification note correctly`() {
    val application = submittedTemporaryAccommodationApplicationFactory.produce()
    val assessment = assessmentFactory.withApplication(application).produce()

    assessment.clarificationNotes = mutableListOf(
      completedClarificationNoteFactory
        .withAssessment(assessment)
        .produce()
    )

    application.assessments = mutableListOf(assessment)

    val result = applicationsTransformer.transformJpaToApi(application, mockk(), mockk()) as TemporaryAccommodationApplication

    assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
  }

  @Test
  fun `transformJpaToApi uses the latest assessment`() {
    val application = submittedTemporaryAccommodationApplicationFactory.produce()
    val oldAssessment = assessmentFactory.withApplication(application)
      .withCreatedAt(OffsetDateTime.parse("2022-09-01T12:34:56.789Z"))
      .produce()
    val latestAssessment = assessmentFactory.withApplication(application)
      .withCreatedAt(OffsetDateTime.now())
      .produce()

    oldAssessment.clarificationNotes = mutableListOf(
      awaitingClarificationNoteFactory
        .withAssessment(oldAssessment)
        .produce()
    )

    latestAssessment.clarificationNotes = mutableListOf(
      completedClarificationNoteFactory
        .withAssessment(latestAssessment)
        .produce()
    )

    application.assessments = mutableListOf(oldAssessment, latestAssessment)

    val result = applicationsTransformer.transformJpaToApi(application, mockk(), mockk()) as TemporaryAccommodationApplication

    assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
  }
}
