package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1.external

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalAssessmentDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalRequestForPlacementOutcomeDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1StaffDtoFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.external.Cas1ExternalApplicationV2Transformer
import java.time.LocalDate
import java.time.OffsetDateTime
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision as AssessmentDecisionJpa

@ExtendWith(MockKExtension::class)
class Cas1ExternalApplicationV2TransformerTest {
  @MockK
  private lateinit var assessmentTransformer: Cas1AssessmentTransformer

  @MockK
  private lateinit var applicationService: Cas1ApplicationService

  private lateinit var transformer: Cas1ExternalApplicationV2Transformer

  private val user = UserEntityFactory().withDefaults().produce()
  private val staff = Cas1StaffDtoFactory().produce()

  @BeforeEach
  fun setup() {
    transformer = Cas1ExternalApplicationV2Transformer(
      assessmentTransformer,
      applicationService,
      "http://localhost:3000/applications/#id",
    )

    every { assessmentTransformer.transformToStaffDto(user) } returns staff
    every { applicationService.getApplicationExpiresAt(any()) } returns null
  }

  @Nested
  inner class TransformToApplicationV2 {
    private val application = ApprovedPremisesApplicationEntityFactory()
      .withDefaults()
      .withCreatedByUser(user)
      .withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    @Test
    fun `maps all fields`() {
      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withDefaults()
        .withApplication(application)
        .withDecision(AssessmentDecisionJpa.REJECTED)
        .withRejectionRationale("Not suitable")
        .withSubmittedAt(OffsetDateTime.now())
        .produce()
      application.assessments.add(assessment)
      every { assessmentTransformer.transformJpaDecisionToApi(AssessmentDecisionJpa.REJECTED) } returns AssessmentDecision.rejected
      every { applicationService.getApplicationExpiresAt(application) } returns LocalDate.of(2027, 1, 1)
      val upcoming = mockk<Cas1ExternalRequestForPlacementOutcomeDto>()
      val history = listOf(mockk<Cas1ExternalRequestForPlacementOutcomeDto>(), mockk<Cas1ExternalRequestForPlacementOutcomeDto>())

      val result = transform(application, upcoming, history)

      assertThat(result.id).isEqualTo(application.id)
      assertThat(result.uiUrl).isEqualTo("http://localhost:3000/applications/${application.id}")
      assertThat(result.status).isEqualTo(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
      assertThat(result.createdAt).isEqualTo(application.createdAt)
      assertThat(result.createdBy).isEqualTo(staff)
      assertThat(result.expiresAt).isEqualTo(LocalDate.of(2027, 1, 1))
      val submitted = result.submittedApplication!!
      assertThat(submitted.submittedAt).isEqualTo(application.submittedAt)
      assertThat(submitted.assessment).isEqualTo(Cas1ExternalAssessmentDto(AssessmentDecision.rejected, "Not suitable"))
      assertThat(submitted.upcomingRequestForPlacement).isSameAs(upcoming)
      assertThat(submitted.requestForPlacementHistory).containsExactlyElementsOf(history)
    }

    @Test
    fun `has null assessment when application has no assessment`() {
      val result = transform(application)

      assertThat(result.submittedApplication!!.assessment).isNull()
    }

    @Test
    fun `has no submitted application when application has not been submitted`() {
      val unsubmitted = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()

      val result = transform(unsubmitted)

      assertThat(result.submittedApplication).isNull()
    }

    private fun transform(
      application: ApprovedPremisesApplicationEntity,
      upcoming: Cas1ExternalRequestForPlacementOutcomeDto? = null,
      history: List<Cas1ExternalRequestForPlacementOutcomeDto> = emptyList(),
    ) = transformer.transformToApplicationV2(application, upcoming, history)
  }
}
