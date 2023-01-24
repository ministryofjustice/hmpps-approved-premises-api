package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentClarificationNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentClarificationNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision as ApiAssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision as JpaAssessmentDecision

class AssessmentTransformerTest {
  private val mockApplicationsTransformer = mockk<ApplicationsTransformer>()
  private val mockAssessmentClarificationNoteTransformer = mockk<AssessmentClarificationNoteTransformer>()
  private val assessmentTransformer = AssessmentTransformer(
    jacksonObjectMapper(),
    mockApplicationsTransformer,
    mockAssessmentClarificationNoteTransformer
  )

  private val allocatedToUser = UserEntityFactory().produce()

  private val assessmentFactory = AssessmentEntityFactory()
    .withApplication(mockk<ApprovedPremisesApplicationEntity>())
    .withId(UUID.fromString("7d0d3b38-5bc3-45c7-95eb-4d714cbd0db1"))
    .withAssessmentSchema(
      ApprovedPremisesAssessmentJsonSchemaEntity(
        id = UUID.fromString("aeeb6992-6485-4600-9c35-19479819c544"),
        addedAt = OffsetDateTime.now(),
        schema = "{}"
      )
    )
    .withDecision(JpaAssessmentDecision.REJECTED)
    .withRejectionRationale("reasoning")
    .withData("{\"data\": \"something\"}")
    .withCreatedAt(OffsetDateTime.parse("2022-12-14T12:05:00Z"))
    .withSubmittedAt(OffsetDateTime.parse("2022-12-14T12:06:00Z"))
    .withAllocatedToUser(allocatedToUser)

  @BeforeEach
  fun setup() {
    every { mockApplicationsTransformer.transformJpaToApi(any<ApplicationEntity>(), any(), any()) } returns mockk<ApprovedPremisesApplication>()
    every { mockAssessmentClarificationNoteTransformer.transformJpaToApi(any()) } returns mockk()
  }

  @Test
  fun `transformJpaToApi transforms correctly`() {
    val assessment = assessmentFactory.produce()

    val result = assessmentTransformer.transformJpaToApi(assessment, mockk(), mockk())

    assertThat(result.id).isEqualTo(UUID.fromString("7d0d3b38-5bc3-45c7-95eb-4d714cbd0db1"))
    assertThat(result.schemaVersion).isEqualTo(UUID.fromString("aeeb6992-6485-4600-9c35-19479819c544"))
    assertThat(result.decision).isEqualTo(ApiAssessmentDecision.rejected)
    assertThat(result.rejectionRationale).isEqualTo("reasoning")
    assertThat(result.createdAt).isEqualTo(OffsetDateTime.parse("2022-12-14T12:05:00Z"))
    assertThat(result.submittedAt).isEqualTo(OffsetDateTime.parse("2022-12-14T12:06:00Z"))
    assertThat(result.allocatedToStaffMemberId).isEqualTo(allocatedToUser.id)
  }

  @Test
  fun `transformJpaToApi sets a pending status when there is a clarification note with no response`() {
    val assessment = assessmentFactory.withDecision(null).produce()

    val clarificationNotes = mutableListOf(
      AssessmentClarificationNoteEntityFactory()
        .withAssessment(assessment)
        .withResponse("Some text")
        .withCreatedBy(UserEntityFactory().produce())
        .produce(),
      AssessmentClarificationNoteEntityFactory()
        .withAssessment(assessment)
        .withCreatedBy(UserEntityFactory().produce())
        .produce()
    )

    assessment.clarificationNotes = clarificationNotes

    val result = assessmentTransformer.transformJpaToApi(assessment, mockk(), mockk())

    assertThat(result.status).isEqualTo(AssessmentStatus.pending)
  }

  @Test
  fun `transformJpaToApi sets a completed status when there is a decision`() {
    val assessment = assessmentFactory
      .withDecision(AssessmentDecision.ACCEPTED)
      .produce()

    val result = assessmentTransformer.transformJpaToApi(assessment, mockk(), mockk())

    assertThat(result.status).isEqualTo(AssessmentStatus.completed)
  }

  @Test
  fun `transformJpaToApi sets a deallocated status when there is a deallocated timestamp`() {
    val assessment = assessmentFactory
      .withDecision(null)
      .withReallocatedAt(OffsetDateTime.now())
      .produce()

    val result = assessmentTransformer.transformJpaToApi(assessment, mockk(), mockk())

    assertThat(result.status).isEqualTo(AssessmentStatus.reallocated)
  }

  @Test
  fun `transformJpaToApi sets an active status when there is no decision`() {
    val assessment = assessmentFactory
      .withDecision(null)
      .produce()

    val result = assessmentTransformer.transformJpaToApi(assessment, mockk(), mockk())

    assertThat(result.status).isEqualTo(AssessmentStatus.active)
  }
}
