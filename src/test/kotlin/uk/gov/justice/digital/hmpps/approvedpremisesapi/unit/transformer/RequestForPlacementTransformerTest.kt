package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RequestForPlacementTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

class RequestForPlacementTransformerTest {
  private val objectMapper = mockk<ObjectMapper>()

  private val requestForPlacementTransformer = RequestForPlacementTransformer(objectMapper)

  @BeforeEach
  fun setupObjectMapperMock() {
    every { objectMapper.readTree(any<String>()) } returns mockk()
  }

  companion object {
    private val user = UserEntityFactory()
      .withDefaults()
      .produce()

    private fun assertPlacementDatesMatchPlacementDateEntities(
      actual: List<PlacementDates>,
      expected: List<PlacementDateEntity>,
    ) {
      assertThat(actual).hasSize(expected.size)
      actual.zip(expected).forEach { (a, e) -> assertPlacementDateMatchesPlacementDateEntity(a, e) }
    }

    private fun assertPlacementDateMatchesPlacementDateEntity(
      actual: PlacementDates,
      expected: PlacementDateEntity,
    ) {
      assertThat(actual.expectedArrival).isEqualTo(expected.expectedArrival)
      assertThat(actual.duration).isEqualTo(expected.duration)
    }
  }

  @Nested
  inner class TransformPlacementApplicationEntityToApi {
    @Test
    fun `Transforms the placement application correctly`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withDefaults()
        .withApplication(application)
        .withSubmittedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        .withDecisionMadeAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        .withWithdrawalReason(randomOf(PlacementApplicationWithdrawalReason.entries))
        .produce()

      val result = requestForPlacementTransformer.transformPlacementApplicationEntityToApi(placementApplication)

      assertThat(result.id).isEqualTo(placementApplication.id)
      assertThat(result.createdByUserId).isEqualTo(placementApplication.createdByUser.id)
      assertThat(result.createdAt).isEqualTo(placementApplication.createdAt.toInstant())
      assertThat(result.isWithdrawn).isEqualTo(placementApplication.isWithdrawn())
      assertThat(result.type).isEqualTo(RequestForPlacementType.manual)
      assertPlacementDatesMatchPlacementDateEntities(result.placementDates, placementApplication.placementDates)
      assertThat(result.submittedAt).isEqualTo(placementApplication.submittedAt?.toInstant())
      assertThat(result.requestReviewedAt).isEqualTo(placementApplication.decisionMadeAt?.toInstant())
      assertThat(result.document).isNotNull
      assertThat(result.withdrawalReason).isEqualTo(placementApplication.withdrawalReason?.apiValue)

      verify(exactly = 1) { objectMapper.readTree(placementApplication.document) }
    }
  }

  @Nested
  inner class TransformPlacementRequestEntityToApi {
    @Test
    fun `Transforms the placement request correctly`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withSubmittedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        .produce()

      val placementRequirements = PlacementRequirementsEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .withPlacementRequirements(placementRequirements)
        .withWithdrawalReason(randomOf(PlacementRequestWithdrawalReason.entries))
        .produce()

      val result = requestForPlacementTransformer.transformPlacementRequestEntityToApi(placementRequest)

      assertThat(result.id).isEqualTo(placementRequest.id)
      assertThat(result.createdByUserId).isEqualTo(placementRequest.application.createdByUser.id)
      assertThat(result.createdAt).isEqualTo(placementRequest.createdAt.toInstant())
      assertThat(result.isWithdrawn).isEqualTo(placementRequest.isWithdrawn)
      assertThat(result.type).isEqualTo(RequestForPlacementType.automatic)
      assertThat(result.placementDates).hasSize(1)
      assertThat(result.placementDates[0].expectedArrival).isEqualTo(placementRequest.expectedArrival)
      assertThat(result.placementDates[0].duration).isEqualTo(placementRequest.duration)
      assertThat(result.submittedAt).isEqualTo(placementRequest.createdAt.toInstant())
      assertThat(result.requestReviewedAt).isEqualTo(placementRequest.assessment.submittedAt?.toInstant())
      assertThat(result.document).isNull()
      assertThat(result.withdrawalReason).isEqualTo(placementRequest.withdrawalReason?.apiValue)
    }
  }
}
