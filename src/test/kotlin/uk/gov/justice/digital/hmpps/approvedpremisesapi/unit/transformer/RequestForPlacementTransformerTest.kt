package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SituationOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1StaffDtoFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ReleaseType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RequestForPlacementTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

class RequestForPlacementTransformerTest {
  private val jsonMapper = mockk<JsonMapper>()
  private val cas1AssessmentTransformer = mockk<Cas1AssessmentTransformer>()
  private val requestForPlacementTransformer = RequestForPlacementTransformer(jsonMapper, cas1AssessmentTransformer)

  @BeforeEach
  fun setupObjectMapperMock() {
    every { jsonMapper.readTree(any<String>()) } returns mockk()
  }

  companion object {
    private val user = UserEntityFactory()
      .withDefaults()
      .produce()
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
        .withExpectedArrival(LocalDate.of(2012, 9, 9))
        .withExpectedArrivalFlexible(false)
        .withRequestedDuration(47)
        .withRequestedDuration(48)
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withAuthorisedDuration(49)
        .withSentenceType(SentenceTypeOption.bailPlacement.name)
        .withReleaseType(Cas1ReleaseType.licence)
        .withSituation(SituationOption.bailSentence.name)
        .produce()
      every { cas1AssessmentTransformer.transformToStaffDto(placementApplication.createdByUser) } returns Cas1StaffDtoFactory()
        .withName(placementApplication.createdByUser.name)
        .withUsername(placementApplication.createdByUser.deliusUsername)
        .withStaffCode(placementApplication.createdByUser.deliusStaffCode)
        .produce()
      val result = requestForPlacementTransformer.transformPlacementApplicationEntityToApi(placementApplication, true)

      assertThat(result.id).isEqualTo(placementApplication.id)
      assertThat(result.createdByUserId).isEqualTo(placementApplication.createdByUser.id)
      assertThat(result.createdAt).isEqualTo(placementApplication.createdAt.toInstant())
      assertThat(result.isWithdrawn).isEqualTo(placementApplication.isWithdrawn)
      assertThat(result.type).isEqualTo(RequestForPlacementType.manual)
      assertThat(result.submittedAt).isEqualTo(placementApplication.submittedAt?.toInstant())
      assertThat(result.requestReviewedAt).isEqualTo(placementApplication.decisionMadeAt?.toInstant())
      assertThat(result.document).isNotNull
      assertThat(result.withdrawalReason).isEqualTo(placementApplication.withdrawalReason?.apiValue)
      assertThat(result.sentenceType).isEqualTo(SentenceTypeOption.bailPlacement)
      assertThat(result.releaseType).isEqualTo(ReleaseTypeOption.licence)
      assertThat(result.situation).isEqualTo(SituationOption.bailSentence)
      assertThat(result.requestedPlacementPeriod.arrival).isEqualTo(LocalDate.of(2012, 9, 9))
      assertThat(result.requestedPlacementPeriod.arrivalFlexible).isFalse
      assertThat(result.requestedPlacementPeriod.duration).isEqualTo(48)
      assertThat(result.authorisedPlacementPeriod!!.arrival).isEqualTo(LocalDate.of(2012, 9, 9))
      assertThat(result.authorisedPlacementPeriod.arrivalFlexible).isFalse
      assertThat(result.authorisedPlacementPeriod.duration).isEqualTo(49)
      assertThat(result.decision).isEqualTo(PlacementApplicationDecision.ACCEPTED)
      assertThat(result.submittedBy?.name).isEqualTo(placementApplication.createdByUser.name)

      assertThat(result.status).isEqualTo(RequestForPlacementStatus.awaitingMatch)
      assertThat(result.statusSetDate).isEqualTo(placementApplication.decisionMadeAt!!.toLocalDate())

      verify(exactly = 1) { jsonMapper.readTree(placementApplication.document) }
    }

    @Test
    fun `Derives the correct type for an automatic placement application`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withDefaults()
        .withApplication(application)
        .withSubmittedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        .withExpectedArrival(LocalDate.of(2012, 9, 9))
        .withRequestedDuration(47)
        .withAutomatic(true)
        .produce()
      every { cas1AssessmentTransformer.transformToStaffDto(placementApplication.createdByUser) } returns Cas1StaffDtoFactory()
        .withName(placementApplication.createdByUser.name)
        .withUsername(placementApplication.createdByUser.deliusUsername)
        .withStaffCode(placementApplication.createdByUser.deliusStaffCode)
        .produce()
      val result = requestForPlacementTransformer.transformPlacementApplicationEntityToApi(placementApplication, true)

      assertThat(result.type).isEqualTo(RequestForPlacementType.automatic)
    }

    @ParameterizedTest
    @CsvSource("true", "false")
    fun `canBeDirectlyWithdrawn is derived from the provided argument`(canBeDirectlyWithdrawn: Boolean) {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withDefaults()
        .withApplication(application)
        .withSubmittedAt(OffsetDateTime.now())
        .withExpectedArrival(LocalDate.now())
        .withRequestedDuration(5)
        .withRequestedDuration(6)
        .withAuthorisedDuration(7)
        .withAutomatic(false)
        .produce()

      every { cas1AssessmentTransformer.transformToStaffDto(placementApplication.createdByUser) } returns Cas1StaffDtoFactory()
        .withName(placementApplication.createdByUser.name)
        .withUsername(placementApplication.createdByUser.deliusUsername)
        .withStaffCode(placementApplication.createdByUser.deliusStaffCode)
        .produce()

      val result = requestForPlacementTransformer.transformPlacementApplicationEntityToApi(
        placementApplication,
        canBeDirectlyWithdrawn = canBeDirectlyWithdrawn,
      )

      assertThat(result.canBeDirectlyWithdrawn).isEqualTo(canBeDirectlyWithdrawn)
    }
  }

  @Nested
  inner class TransformPlacementRequestEntityToApi {
    @Test
    fun `Transforms the placement request correctly`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSentenceType(SentenceTypeOption.bailPlacement.name)
        .withReleaseType(Cas1ReleaseType.rotl)
        .withSituation(SituationOption.bailAssessment.name)
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
      every { cas1AssessmentTransformer.transformToStaffDto(application.createdByUser) } returns Cas1StaffDtoFactory()
        .withName(application.createdByUser.name)
        .withUsername(application.createdByUser.deliusUsername)
        .withStaffCode(application.createdByUser.deliusStaffCode)
        .produce()
      val result = requestForPlacementTransformer.transformPlacementRequestEntityToApi(placementRequest, true)

      assertThat(result.id).isEqualTo(placementRequest.id)
      assertThat(result.createdByUserId).isEqualTo(placementRequest.application.createdByUser.id)
      assertThat(result.createdAt).isEqualTo(placementRequest.createdAt.toInstant())
      assertThat(result.isWithdrawn).isEqualTo(placementRequest.isWithdrawn)
      assertThat(result.type).isEqualTo(RequestForPlacementType.automatic)
      assertThat(result.requestedPlacementPeriod.arrival).isEqualTo(placementRequest.expectedArrival)
      assertThat(result.requestedPlacementPeriod.arrivalFlexible).isNull()
      assertThat(result.requestedPlacementPeriod.duration).isEqualTo(placementRequest.duration)
      assertThat(result.authorisedPlacementPeriod!!.arrival).isEqualTo(placementRequest.expectedArrival)
      assertThat(result.authorisedPlacementPeriod.arrivalFlexible).isNull()
      assertThat(result.authorisedPlacementPeriod.duration).isEqualTo(placementRequest.duration)
      assertThat(result.submittedAt).isEqualTo(placementRequest.createdAt.toInstant())
      assertThat(result.requestReviewedAt).isEqualTo(placementRequest.assessment.submittedAt?.toInstant())
      assertThat(result.document).isNull()
      assertThat(result.withdrawalReason).isEqualTo(placementRequest.withdrawalReason?.apiValue)
      assertThat(result.sentenceType).isEqualTo(SentenceTypeOption.bailPlacement)
      assertThat(result.releaseType).isEqualTo(ReleaseTypeOption.rotl)
      assertThat(result.situation).isEqualTo(SituationOption.bailAssessment)
      assertThat(result.decision).isEqualTo(PlacementApplicationDecision.ACCEPTED)
      assertThat(result.submittedBy?.name).isEqualTo(application.createdByUser.name)
      assertThat(result.status).isEqualTo(RequestForPlacementStatus.awaitingMatch)
      assertThat(result.statusSetDate).isEqualTo(placementRequest.createdAt.toLocalDate())
    }

    @ParameterizedTest
    @CsvSource("true", "false")
    fun `canBeDirectlyWithdrawn is derived from the provided argument`(canBeDirectlyWithdrawn: Boolean) {
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
        .produce()
      every { cas1AssessmentTransformer.transformToStaffDto(application.createdByUser) } returns Cas1StaffDtoFactory()
        .withName(application.createdByUser.name)
        .withUsername(application.createdByUser.deliusUsername)
        .withStaffCode(application.createdByUser.deliusStaffCode)
        .produce()
      val result = requestForPlacementTransformer.transformPlacementRequestEntityToApi(
        placementRequest,
        canBeDirectlyWithdrawn = canBeDirectlyWithdrawn,
      )

      assertThat(result.canBeDirectlyWithdrawn).isEqualTo(canBeDirectlyWithdrawn)
    }
  }
}
