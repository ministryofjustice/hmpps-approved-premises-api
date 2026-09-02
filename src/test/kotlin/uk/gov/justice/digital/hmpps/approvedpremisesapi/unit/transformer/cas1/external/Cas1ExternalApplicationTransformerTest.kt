package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1.external

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1RequestedPlacementPeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CancellationReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalApplicationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalAssessmentDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPlacementDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPremisesDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalRequestForPlacementDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1PlacementPairDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1StaffDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.PlacementApplicationDecisionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingShortSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RequestForPlacementFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.external.Cas1ExternalApplicationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.minusDays
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision as AssessmentDecisionApi
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision as AssessmentDecisionJpa

class Cas1ExternalApplicationTransformerTest {
  private val cas1ApplicationUrlTemplate: String = "http://localhost:3000/applications/#id"
  private val mockCas1AssessmentTransformer = mockk<Cas1AssessmentTransformer>()
  private val mockCas1ApplicationTransformer = mockk<Cas1ApplicationService>()
  private val cas1ExternalApplicationTransformer = Cas1ExternalApplicationTransformer(
    mockCas1AssessmentTransformer,
    mockCas1ApplicationTransformer,
    cas1ApplicationUrlTemplate,
  )

  @Nested
  inner class TransformToCas1SuitableApplication {
    @Test
    fun `Transforms to suitable application`() {
      val premises = Cas1ExternalPremisesDto(
        startDate = LocalDate.now().minusDays(1),
        endDate = LocalDate.now().plusDays(12),
        addressLine1 = "Test House",
        addressLine2 = "Test Lane",
        town = "Test Town",
        postcode = "TE57 8PP",
      )
      val user = UserEntityFactory()
        .withDefaults()
        .withName("Tom")
        .produce()

      val requestSubmittedBy = Cas1StaffDto(
        name = "Bob",
        username = "bob1",
        staffCode = "bob123",
      )

      val userStaff = Cas1StaffDto(
        name = user.name,
        username = user.deliusUsername,
        staffCode = user.deliusStaffCode,
      )

      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withStatus(ApprovedPremisesApplicationStatus.EXPIRED)
        .withCreatedAt(OffsetDateTime.now().minusDays(100))
        .withSubmittedAt(OffsetDateTime.now().minusDays(50))
        .withCreatedByUser(user)
        .withArrivalDate(OffsetDateTime.now().plusDays(20))
        .withRequestedPlacementDuration(20)
        .produce()
      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withDefaults()
        .withDecision(AssessmentDecisionJpa.ACCEPTED)
        .withApplication(application)
        .withRejectionRationale("Great")
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      application.assessments.add(assessment)

      val suitablePlacementPair = Cas1PlacementPairDto(
        requestForPlacement = Cas1ExternalRequestForPlacementDto(
          decision = PlacementApplicationDecisionDto.rejected,
          rejectionReason = "No space",
          submittedBy = requestSubmittedBy,
          submittedAt = LocalDate.now(),
          withdrawalReason = WithdrawPlacementRequestReason.noCapacityDueToLostBed,
          withdrawalDate = LocalDate.now().minusDays(1),
          expectedArrivalDate = LocalDate.now().minusDays(2),
          durationDays = 12,
          status = RequestForPlacementStatus.awaitingMatch,
        ),
        placement = Cas1ExternalPlacementDto(
          actualArrivalDate = LocalDate.now().minusDays(3),
          actualDepartureDate = LocalDate.now().plusDays(3),
          cancellationReason = "Other",
          premises = premises,
          status = Cas1SpaceBookingStatus.CANCELLED,
        ),
        dateApplied = LocalDate.now(),
      )
      val expected = Cas1SuitableApplication(
        uiUrl = "http://localhost:3000/applications/${application.id}",
        application = Cas1ExternalApplicationDto(
          createdAt = application.createdAt,
          createdBy = userStaff,
          submittedAt = application.submittedAt,
          expiresAt = assessment.submittedAt?.toLocalDate()?.plusDays(365),
          status = application.status,
          id = application.id,
        ),
        assessment = Cas1ExternalAssessmentDto(
          decision = AssessmentDecisionApi.accepted,
          rejectionRationale = assessment.rejectionRationale,
        ),
        requestForPlacement = suitablePlacementPair.requestForPlacement,
        placement = suitablePlacementPair.placement,
        placementHistory = emptyList(),
      )

      every { mockCas1AssessmentTransformer.transformToStaffDto(user) } returns userStaff
      every { mockCas1AssessmentTransformer.transformJpaDecisionToApi(assessment.decision) } returns AssessmentDecisionApi.accepted
      every { mockCas1ApplicationTransformer.getApplicationExpiresAt(application) } returns assessment.submittedAt?.toLocalDate()?.plusDays(365)

      val result = cas1ExternalApplicationTransformer.transformToCas1SuitableApplication(application, suitablePlacementPair, emptyList())

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `Transforms to suitable application with no placements`() {
      val user = UserEntityFactory()
        .withDefaults()
        .withName("Tom")
        .produce()
      val userStaff = Cas1StaffDto(
        name = user.name,
        username = user.deliusUsername,
        staffCode = user.deliusStaffCode,
      )
      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withStatus(ApprovedPremisesApplicationStatus.EXPIRED)
        .withCreatedAt(OffsetDateTime.now().minusDays(100))
        .withSubmittedAt(OffsetDateTime.now().minusDays(50))
        .withCreatedByUser(user)
        .withArrivalDate(OffsetDateTime.now().plusDays(20))
        .withRequestedPlacementDuration(20)
        .produce()
      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withDefaults()
        .withDecision(AssessmentDecisionJpa.ACCEPTED)
        .withApplication(application)
        .withRejectionRationale("Great")
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      application.assessments.add(assessment)

      val expected = Cas1SuitableApplication(
        uiUrl = "http://localhost:3000/applications/${application.id}",
        application = Cas1ExternalApplicationDto(
          createdAt = application.createdAt,
          createdBy = userStaff,
          submittedAt = application.submittedAt,
          expiresAt = assessment.submittedAt?.toLocalDate()?.plusDays(365),
          status = application.status,
          id = application.id,
        ),
        assessment = Cas1ExternalAssessmentDto(
          decision = AssessmentDecisionApi.accepted,
          rejectionRationale = assessment.rejectionRationale,
        ),
        requestForPlacement = null,
        placement = null,
        placementHistory = emptyList(),
      )

      every { mockCas1AssessmentTransformer.transformToStaffDto(user) } returns userStaff
      every { mockCas1AssessmentTransformer.transformJpaDecisionToApi(assessment.decision) } returns AssessmentDecisionApi.accepted
      every { mockCas1ApplicationTransformer.getApplicationExpiresAt(application) } returns assessment.submittedAt?.toLocalDate()?.plusDays(365)

      val result = cas1ExternalApplicationTransformer.transformToCas1SuitableApplication(application, null, emptyList())

      assertThat(result).isEqualTo(expected)
    }
  }

  @Nested
  inner class TransformToCas1PlacementPair {
    @Test
    fun `Transforms to placement history that is departed`() {
      val duration = 12
      val expectedArrivalDate = LocalDate.now().minusDays(100)
      val expectedDepartureDate = expectedArrivalDate.plusWeeks(duration.toLong())
      val actualDepartureDate = expectedDepartureDate.minusDays(1)
      val rejectionReason = null
      val withdrawalDate = null
      val user = UserEntityFactory()
        .withDefaults()
        .withName("Bob")
        .produce()
      val requestSubmittedBy = Cas1StaffDto(
        name = user.name,
        username = user.deliusUsername,
        staffCode = user.deliusStaffCode,
      )
      val canonicalPlacementPeriod = Cas1RequestedPlacementPeriod(
        arrival = expectedArrivalDate,
        arrivalFlexible = false,
        duration = duration,
      )
      val requestForPlacement = RequestForPlacementFactory()
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withStatus(RequestForPlacementStatus.placementBooked)
        .withSubmittedBy(requestSubmittedBy)
        .withSubmittedAt(Instant.now().minusDays(110))
        .withWithdrawalReason(null)
        .withCanonicalPlacementPeriod(canonicalPlacementPeriod)
        .produce()

      val placement = Cas1SpaceBookingShortSummaryFactory()
        .withStatus(Cas1SpaceBookingStatus.DEPARTED)
        .withStatusSetDate(actualDepartureDate)
        .withExpectedArrivalDate(expectedArrivalDate)
        .withActualArrivalDate(expectedArrivalDate.plusDays(1))
        .withActualDepartureDate(actualDepartureDate)
        .produce()
      val premisesEntity = ApprovedPremisesEntityFactory()
        .withAddressLine1("Test House")
        .withAddressLine2("Test Lane")
        .withTown("Test Town")
        .withPostcode("TE57 8PP")
        .withDefaults()
        .produce()

      val premises = Cas1ExternalPremisesDto(
        startDate = placement.actualArrivalDate,
        endDate = placement.actualDepartureDate,
        addressLine1 = premisesEntity.addressLine1,
        addressLine2 = premisesEntity.addressLine2,
        town = premisesEntity.town,
        postcode = premisesEntity.postcode,
      )

      val expected = Cas1PlacementPairDto(
        requestForPlacement = Cas1ExternalRequestForPlacementDto(
          decision = requestForPlacement.decision?.apiValue,
          rejectionReason = rejectionReason,
          submittedBy = requestSubmittedBy,
          submittedAt = requestForPlacement.submittedAt?.toLocalDate(),
          withdrawalReason = requestForPlacement.withdrawalReason,
          withdrawalDate = withdrawalDate,
          expectedArrivalDate = placement.expectedArrivalDate,
          durationDays = duration,
          status = requestForPlacement.status,
        ),

        placement = Cas1ExternalPlacementDto(
          actualArrivalDate = placement.actualArrivalDate,
          actualDepartureDate = placement.actualDepartureDate,
          cancellationReason = placement.cancellation?.reason?.name,
          status = placement.status,
          premises = premises,
        ),
        dateApplied = placement.statusSetDate!!,
      )

      every { mockCas1AssessmentTransformer.transformToStaffDto(user) } returns requestSubmittedBy

      val result = cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement, rejectionReason, withdrawalDate, placement, premisesEntity)

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `Transforms to placement history that is arrived`() {
      val duration = 12
      val expectedArrivalDate = LocalDate.now().minusDays(100)
      val actualArrivalDate = expectedArrivalDate.plusDays(1)
      val expectedDepartureDate = expectedArrivalDate.plusWeeks(duration.toLong())
      val actualDepartureDate = null
      val canonicalPlacementPeriod = Cas1RequestedPlacementPeriod(
        arrival = expectedArrivalDate,
        arrivalFlexible = false,
        duration = duration,
      )
      val rejectionReason = null
      val withdrawalDate = null
      val user = UserEntityFactory()
        .withDefaults()
        .withName("Bob")
        .produce()
      val requestSubmittedBy = Cas1StaffDto(
        name = user.name,
        username = user.deliusUsername,
        staffCode = user.deliusStaffCode,
      )
      val requestForPlacement = RequestForPlacementFactory()
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withStatus(RequestForPlacementStatus.placementBooked)
        .withSubmittedBy(requestSubmittedBy)
        .withSubmittedAt(Instant.now().minusDays(110))
        .withWithdrawalReason(null)
        .withCanonicalPlacementPeriod(canonicalPlacementPeriod)
        .produce()

      val placement = Cas1SpaceBookingShortSummaryFactory()
        .withStatus(Cas1SpaceBookingStatus.ARRIVED)
        .withStatusSetDate(actualArrivalDate)
        .withExpectedArrivalDate(expectedArrivalDate)
        .withActualArrivalDate(actualArrivalDate)
        .withExpectedDepartureDate(expectedDepartureDate)
        .withActualDepartureDate(actualDepartureDate)
        .produce()
      val premisesEntity = ApprovedPremisesEntityFactory()
        .withAddressLine1("Test House")
        .withAddressLine2("Test Lane")
        .withTown("Test Town")
        .withPostcode("TE57 8PP")
        .withDefaults()
        .produce()

      val premises = Cas1ExternalPremisesDto(
        startDate = placement.actualArrivalDate,
        endDate = placement.expectedDepartureDate,
        addressLine1 = premisesEntity.addressLine1,
        addressLine2 = premisesEntity.addressLine2,
        town = premisesEntity.town,
        postcode = premisesEntity.postcode,
      )

      val expected = Cas1PlacementPairDto(
        requestForPlacement = Cas1ExternalRequestForPlacementDto(
          status = requestForPlacement.status,
          decision = requestForPlacement.decision?.apiValue,
          rejectionReason = rejectionReason,
          submittedBy = requestSubmittedBy,
          submittedAt = requestForPlacement.submittedAt?.toLocalDate(),
          withdrawalReason = requestForPlacement.withdrawalReason,
          withdrawalDate = withdrawalDate,
          expectedArrivalDate = placement.expectedArrivalDate,
          durationDays = duration,
        ),

        placement = Cas1ExternalPlacementDto(
          actualArrivalDate = placement.actualArrivalDate,
          actualDepartureDate = placement.actualDepartureDate,
          cancellationReason = placement.cancellation?.reason?.name,
          status = placement.status,
          premises = premises,
        ),
        dateApplied = placement.statusSetDate!!,
      )
      every { mockCas1AssessmentTransformer.transformToStaffDto(user) } returns requestSubmittedBy

      val result = cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement, rejectionReason, withdrawalDate, placement, premisesEntity)

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `Transforms to placement history that is withdrawn`() {
      val duration = 12
      val expectedArrivalDate = LocalDate.now().minusDays(100)
      val actualArrivalDate = null
      val expectedDepartureDate = expectedArrivalDate.plusWeeks(duration.toLong())
      val actualDepartureDate = null
      val canonicalPlacementPeriod = Cas1RequestedPlacementPeriod(
        arrival = expectedArrivalDate,
        arrivalFlexible = false,
        duration = duration,
      )
      val rejectionReason = null
      val withdrawalDate = expectedArrivalDate.minusDays(1)
      val user = UserEntityFactory()
        .withDefaults()
        .withName("Bob")
        .produce()
      val requestSubmittedBy = Cas1StaffDto(
        name = user.name,
        username = user.deliusUsername,
        staffCode = user.deliusStaffCode,
      )
      val requestForPlacement = RequestForPlacementFactory()
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withStatus(RequestForPlacementStatus.requestWithdrawn)
        .withSubmittedBy(requestSubmittedBy)
        .withSubmittedAt(Instant.now().minusDays(110))
        .withIsWithdrawn(true)
        .withWithdrawalReason(WithdrawPlacementRequestReason.noCapacityDueToLostBed)
        .withCanonicalPlacementPeriod(canonicalPlacementPeriod)
        .produce()
      val reason = CancellationReason(
        id = UUID.randomUUID(),
        name = "Oops",
        isActive = true,
        serviceScope = "approved-premises",
      )
      val cancellation = Cas1SpaceBookingCancellation(
        occurredAt = expectedArrivalDate.minusDays(1),
        recordedAt = Instant.now(),
        reason = reason,
        reasonNotes = "Because",
      )
      val placement = Cas1SpaceBookingShortSummaryFactory()
        .withStatus(Cas1SpaceBookingStatus.ARRIVED)
        .withStatusSetDate(withdrawalDate)
        .withExpectedArrivalDate(expectedArrivalDate)
        .withActualArrivalDate(actualArrivalDate)
        .withExpectedDepartureDate(expectedDepartureDate)
        .withActualDepartureDate(actualDepartureDate)
        .withCancellation(cancellation)
        .produce()
      val premisesEntity = ApprovedPremisesEntityFactory()
        .withAddressLine1("Test House")
        .withAddressLine2("Test Lane")
        .withTown("Test Town")
        .withPostcode("TE57 8PP")
        .withDefaults()
        .produce()

      val premises = Cas1ExternalPremisesDto(
        startDate = placement.expectedArrivalDate,
        endDate = placement.expectedDepartureDate,
        addressLine1 = premisesEntity.addressLine1,
        addressLine2 = premisesEntity.addressLine2,
        town = premisesEntity.town,
        postcode = premisesEntity.postcode,
      )

      val expected = Cas1PlacementPairDto(
        requestForPlacement = Cas1ExternalRequestForPlacementDto(
          status = requestForPlacement.status,
          decision = requestForPlacement.decision?.apiValue,
          rejectionReason = rejectionReason,
          submittedBy = requestSubmittedBy,
          submittedAt = requestForPlacement.submittedAt?.toLocalDate(),
          withdrawalReason = requestForPlacement.withdrawalReason,
          withdrawalDate = withdrawalDate,
          expectedArrivalDate = placement.expectedArrivalDate,
          durationDays = duration,
        ),

        placement = Cas1ExternalPlacementDto(
          actualArrivalDate = placement.actualArrivalDate,
          actualDepartureDate = placement.actualDepartureDate,
          cancellationReason = placement.cancellation?.reason?.name,
          status = placement.status,
          premises = premises,
        ),
        dateApplied = placement.statusSetDate!!,
      )
      every { mockCas1AssessmentTransformer.transformToStaffDto(user) } returns requestSubmittedBy

      val result = cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement, rejectionReason, withdrawalDate, placement, premisesEntity)

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `Transforms to placement history that is withdrawn with Other reason`() {
      val duration = 12
      val expectedArrivalDate = LocalDate.now().minusDays(100)
      val actualArrivalDate = null
      val expectedDepartureDate = expectedArrivalDate.plusWeeks(duration.toLong())
      val actualDepartureDate = null
      val canonicalPlacementPeriod = Cas1RequestedPlacementPeriod(
        arrival = expectedArrivalDate,
        arrivalFlexible = false,
        duration = duration,
      )
      val rejectionReason = null
      val withdrawalDate = expectedArrivalDate.minusDays(1)
      val user = UserEntityFactory()
        .withDefaults()
        .withName("Bob")
        .produce()
      val requestSubmittedBy = Cas1StaffDto(
        name = user.name,
        username = user.deliusUsername,
        staffCode = user.deliusStaffCode,
      )
      val requestForPlacement = RequestForPlacementFactory()
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withStatus(RequestForPlacementStatus.requestWithdrawn)
        .withSubmittedBy(requestSubmittedBy)
        .withSubmittedAt(Instant.now().minusDays(110))
        .withIsWithdrawn(true)
        .withWithdrawalReason(WithdrawPlacementRequestReason.noCapacityDueToLostBed)
        .withCanonicalPlacementPeriod(canonicalPlacementPeriod)
        .produce()
      val reason = CancellationReason(
        id = UUID.randomUUID(),
        name = "Other",
        isActive = true,
        serviceScope = "approved-premises",
      )
      val cancellation = Cas1SpaceBookingCancellation(
        occurredAt = expectedArrivalDate.minusDays(1),
        recordedAt = Instant.now(),
        reason = reason,
        reasonNotes = "Because",
      )
      val placement = Cas1SpaceBookingShortSummaryFactory()
        .withStatus(Cas1SpaceBookingStatus.ARRIVED)
        .withStatusSetDate(withdrawalDate)
        .withExpectedArrivalDate(expectedArrivalDate)
        .withActualArrivalDate(actualArrivalDate)
        .withExpectedDepartureDate(expectedDepartureDate)
        .withActualDepartureDate(actualDepartureDate)
        .withCancellation(cancellation)
        .produce()
      val premisesEntity = ApprovedPremisesEntityFactory()
        .withAddressLine1("Test House")
        .withAddressLine2("Test Lane")
        .withTown("Test Town")
        .withPostcode("TE57 8PP")
        .withDefaults()
        .produce()

      val premises = Cas1ExternalPremisesDto(
        startDate = placement.expectedArrivalDate,
        endDate = placement.expectedDepartureDate,
        addressLine1 = premisesEntity.addressLine1,
        addressLine2 = premisesEntity.addressLine2,
        town = premisesEntity.town,
        postcode = premisesEntity.postcode,
      )

      val expected = Cas1PlacementPairDto(
        requestForPlacement = Cas1ExternalRequestForPlacementDto(
          decision = requestForPlacement.decision?.apiValue,
          rejectionReason = rejectionReason,
          submittedBy = requestSubmittedBy,
          submittedAt = requestForPlacement.submittedAt?.toLocalDate(),
          withdrawalReason = requestForPlacement.withdrawalReason,
          withdrawalDate = withdrawalDate,
          expectedArrivalDate = placement.expectedArrivalDate,
          durationDays = duration,
          status = requestForPlacement.status,
        ),
        placement = Cas1ExternalPlacementDto(
          status = placement.status,
          actualArrivalDate = placement.actualArrivalDate,
          actualDepartureDate = placement.actualDepartureDate,
          cancellationReason = placement.cancellation?.reasonNotes,
          premises = premises,
        ),
        dateApplied = placement.statusSetDate!!,
      )
      every { mockCas1AssessmentTransformer.transformToStaffDto(user) } returns requestSubmittedBy

      val result = cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement, rejectionReason, withdrawalDate, placement, premisesEntity)

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `Transforms to placement history that's rejected`() {
      val duration = 12
      val expectedArrivalDate = LocalDate.now().minusDays(100)
      val canonicalPlacementPeriod = Cas1RequestedPlacementPeriod(
        arrival = expectedArrivalDate,
        arrivalFlexible = false,
        duration = duration,
      )
      val rejectionReason = "Oh no"
      val withdrawalDate = null
      val user = UserEntityFactory()
        .withDefaults()
        .withName("Bob")
        .produce()
      val requestSubmittedBy = Cas1StaffDto(
        name = user.name,
        username = user.deliusUsername,
        staffCode = user.deliusStaffCode,
      )
      val requestForPlacement = RequestForPlacementFactory()
        .withDecision(PlacementApplicationDecision.REJECTED)
        .withStatus(RequestForPlacementStatus.requestRejected)
        .withSubmittedBy(requestSubmittedBy)
        .withSubmittedAt(Instant.now().minusDays(110))
        .withCanonicalPlacementPeriod(canonicalPlacementPeriod)
        .withStatusSetDate(expectedArrivalDate)
        .produce()
      val expected = Cas1PlacementPairDto(
        requestForPlacement = Cas1ExternalRequestForPlacementDto(
          decision = requestForPlacement.decision?.apiValue,
          rejectionReason = rejectionReason,
          submittedBy = requestSubmittedBy,
          submittedAt = requestForPlacement.submittedAt?.toLocalDate(),
          withdrawalReason = requestForPlacement.withdrawalReason,
          withdrawalDate = withdrawalDate,
          expectedArrivalDate = expectedArrivalDate,
          durationDays = duration,
          status = requestForPlacement.status,
        ),
        placement = null,
        dateApplied = requestForPlacement.statusSetDate,
      )
      every { mockCas1AssessmentTransformer.transformToStaffDto(user) } returns requestSubmittedBy

      val result = cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement, rejectionReason, withdrawalDate, null, null)

      assertThat(result).isEqualTo(expected)
    }
  }
}
