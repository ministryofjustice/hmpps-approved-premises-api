package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RequestForPlacementTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import java.time.LocalDate
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
        .withAuthorisedDuration(49)
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
      assertThat(result.requestedPlacementPeriod.arrival).isEqualTo(LocalDate.of(2012, 9, 9))
      assertThat(result.requestedPlacementPeriod.arrivalFlexible).isFalse
      assertThat(result.requestedPlacementPeriod.duration).isEqualTo(48)
      assertThat(result.authorisedPlacementPeriod!!.arrival).isEqualTo(LocalDate.of(2012, 9, 9))
      assertThat(result.authorisedPlacementPeriod.arrivalFlexible).isFalse
      assertThat(result.authorisedPlacementPeriod.duration).isEqualTo(49)
      assertThat(result.placementDates).hasSize(1)
      assertThat(result.placementDates[0].expectedArrival).isEqualTo(LocalDate.of(2012, 9, 9))
      assertThat(result.placementDates[0].duration).isEqualTo(49)

      verify(exactly = 1) { objectMapper.readTree(placementApplication.document) }
    }

    @Test
    fun `Maintains the correct status for a withdrawn placement application`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withDefaults()
        .withApplication(application)
        .withSubmittedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        .withDecisionMadeAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        .withIsWithdrawn(true)
        .withWithdrawalReason(randomOf(PlacementApplicationWithdrawalReason.entries))
        .withExpectedArrival(LocalDate.of(2012, 9, 9))
        .withRequestedDuration(47)
        .withRequestedDuration(6)
        .withAuthorisedDuration(7)
        .withAutomatic(false)
        .produce()

      val result = requestForPlacementTransformer.transformPlacementApplicationEntityToApi(placementApplication, true)

      assertThat(result.status).isEqualTo(RequestForPlacementStatus.requestWithdrawn)
    }

    @Test
    fun `Derives the correct status for a placement application with a booking`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withDefaults()
        .withApplication(application)
        .withSubmittedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        .withDecisionMadeAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withExpectedArrival(LocalDate.of(2012, 9, 9))
        .withRequestedDuration(47)
        .withRequestedDuration(6)
        .withAuthorisedDuration(7)
        .withAutomatic(false)
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
        .apply {
          placementApplication.placementRequest = this
        }

      Cas1SpaceBookingEntityFactory()
        .produce()
        .apply {
          placementRequest.spaceBookings = mutableListOf(this)
        }

      val result = requestForPlacementTransformer.transformPlacementApplicationEntityToApi(placementApplication, true)

      assertThat(result.status).isEqualTo(RequestForPlacementStatus.placementBooked)
    }

    @Test
    fun `Derives the correct status for a rejected placement application`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withDefaults()
        .withApplication(application)
        .withSubmittedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        .withDecisionMadeAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        .withDecision(PlacementApplicationDecision.REJECTED)
        .withExpectedArrival(LocalDate.of(2012, 9, 9))
        .withRequestedDuration(47)
        .withRequestedDuration(6)
        .withAuthorisedDuration(7)
        .withAutomatic(false)
        .produce()

      val result = requestForPlacementTransformer.transformPlacementApplicationEntityToApi(placementApplication, true)

      assertThat(result.status).isEqualTo(RequestForPlacementStatus.requestRejected)
    }

    @Test
    fun `Derives the correct status for an accepted placement application`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withDefaults()
        .withApplication(application)
        .withSubmittedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        .withDecisionMadeAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withExpectedArrival(LocalDate.of(2012, 9, 9))
        .withRequestedDuration(47)
        .withRequestedDuration(6)
        .withAuthorisedDuration(7)
        .withAutomatic(false)
        .produce()

      val result = requestForPlacementTransformer.transformPlacementApplicationEntityToApi(placementApplication, true)

      assertThat(result.status).isEqualTo(RequestForPlacementStatus.awaitingMatch)
    }

    @Test
    fun `Derives the correct status for a submitted placement application`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withDefaults()
        .withApplication(application)
        .withSubmittedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS))
        .withExpectedArrival(LocalDate.of(2012, 9, 9))
        .withRequestedDuration(47)
        .withRequestedDuration(6)
        .withAuthorisedDuration(7)
        .withAutomatic(false)
        .produce()

      val result = requestForPlacementTransformer.transformPlacementApplicationEntityToApi(placementApplication, true)

      assertThat(result.status).isEqualTo(RequestForPlacementStatus.requestSubmitted)
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
      assertThat(result.placementDates).hasSize(1)
      assertThat(result.placementDates[0].expectedArrival).isEqualTo(placementRequest.expectedArrival)
      assertThat(result.placementDates[0].duration).isEqualTo(placementRequest.duration)
      assertThat(result.submittedAt).isEqualTo(placementRequest.createdAt.toInstant())
      assertThat(result.requestReviewedAt).isEqualTo(placementRequest.assessment.submittedAt?.toInstant())
      assertThat(result.document).isNull()
      assertThat(result.withdrawalReason).isEqualTo(placementRequest.withdrawalReason?.apiValue)
    }

    @Test
    fun `Derives the correct status for a withdrawn placement request`() {
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
        .withIsWithdrawn(true)
        .withWithdrawalReason(randomOf(PlacementRequestWithdrawalReason.entries))
        .produce()

      val result = requestForPlacementTransformer.transformPlacementRequestEntityToApi(placementRequest, true)

      assertThat(result.status).isEqualTo(RequestForPlacementStatus.requestWithdrawn)
    }

    @Test
    fun `Derives the correct status for a placement request with a booking`() {
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

      Cas1SpaceBookingEntityFactory()
        .produce()
        .apply {
          placementRequest.spaceBookings = mutableListOf(this)
        }

      val result = requestForPlacementTransformer.transformPlacementRequestEntityToApi(placementRequest, true)

      assertThat(result.status).isEqualTo(RequestForPlacementStatus.placementBooked)
    }

    @Test
    fun `Derives the correct status for a placement request awaiting match`() {
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
        .withIsWithdrawn(false)
        .produce()

      val result = requestForPlacementTransformer.transformPlacementRequestEntityToApi(placementRequest, true)

      assertThat(result.status).isEqualTo(RequestForPlacementStatus.awaitingMatch)
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

      val result = requestForPlacementTransformer.transformPlacementRequestEntityToApi(
        placementRequest,
        canBeDirectlyWithdrawn = canBeDirectlyWithdrawn,
      )

      assertThat(result.canBeDirectlyWithdrawn).isEqualTo(canBeDirectlyWithdrawn)
    }
  }
}
