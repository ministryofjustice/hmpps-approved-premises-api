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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPlacementDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPremisesDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalRequestForPlacementDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1RequestForPlacementOutcomeDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingShortSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RequestForPlacementFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1StaffDtoFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.external.Cas1ExternalApplicationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.minusDays
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

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
  inner class TransformToCas1RequestForPlacementOutcomeDto {
    @Test
    fun `If placement is upcoming set dateApplied to expected arrival date`() {
      val expectedArrivalDate = LocalDate.now().minusDays(100)
      val requestForPlacement = RequestForPlacementFactory().produce()

      val placement = Cas1SpaceBookingShortSummaryFactory()
        .withStatus(Cas1SpaceBookingStatus.UPCOMING)
        .withExpectedArrivalDate(expectedArrivalDate)
        .produce()

      val premisesEntity = ApprovedPremisesEntityFactory().withDefaults().produce()

      every { mockCas1AssessmentTransformer.transformToStaffDto(any()) } returns Cas1StaffDtoFactory().produce()

      val result = cas1ExternalApplicationTransformer.transformToCas1PlacementPair(
        rfp = requestForPlacement,
        placement = placement,
        premises = premisesEntity,
      )

      assertThat(result.dateApplied).isEqualTo(expectedArrivalDate)
    }

    @Test
    fun `Transforms to outcome that is departed`() {
      val duration = 12
      val expectedArrivalDate = LocalDate.now().minusDays(100)
      val expectedDepartureDate = expectedArrivalDate.plusWeeks(duration.toLong())
      val actualDepartureDate = expectedDepartureDate.minusDays(1)
      val user = UserEntityFactory()
        .withDefaults()
        .withName("Bob")
        .produce()
      val requestSubmittedBy = Cas1StaffDtoFactory()
        .withName(user.name)
        .withUsername(user.deliusUsername)
        .withStaffCode(user.deliusStaffCode)
        .produce()
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

      val expected = Cas1RequestForPlacementOutcomeDto(
        requestForPlacement = Cas1ExternalRequestForPlacementDto(
          decision = requestForPlacement.decision?.apiValue,
          rejectionReason = requestForPlacement.rejectionReason,
          submittedBy = requestSubmittedBy,
          submittedAt = requestForPlacement.submittedAt?.toLocalDate(),
          withdrawalReason = requestForPlacement.withdrawalReason,
          withdrawalDate = null,
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
          expectedArrivalDate = expectedArrivalDate,
          expectedDepartureDate = placement.expectedDepartureDate,
        ),
        dateApplied = placement.statusSetDate,
        statusLastUpdated = placement.statusSetDate,
      )

      every { mockCas1AssessmentTransformer.transformToStaffDto(user) } returns requestSubmittedBy

      val result = cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement, placement, premisesEntity)

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `Transforms to outcome that is arrived`() {
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
      val user = UserEntityFactory()
        .withDefaults()
        .withName("Bob")
        .produce()
      val requestSubmittedBy = Cas1StaffDtoFactory()
        .withName(user.name)
        .withUsername(user.deliusUsername)
        .withStaffCode(user.deliusStaffCode)
        .produce()
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

      val expected = Cas1RequestForPlacementOutcomeDto(
        requestForPlacement = Cas1ExternalRequestForPlacementDto(
          status = requestForPlacement.status,
          decision = requestForPlacement.decision?.apiValue,
          rejectionReason = requestForPlacement.rejectionReason,
          submittedBy = requestSubmittedBy,
          submittedAt = requestForPlacement.submittedAt?.toLocalDate(),
          withdrawalReason = requestForPlacement.withdrawalReason,
          withdrawalDate = null,
          expectedArrivalDate = placement.expectedArrivalDate,
          durationDays = duration,
        ),

        placement = Cas1ExternalPlacementDto(
          actualArrivalDate = placement.actualArrivalDate,
          actualDepartureDate = placement.actualDepartureDate,
          cancellationReason = placement.cancellation?.reason?.name,
          status = placement.status,
          premises = premises,
          expectedArrivalDate = expectedArrivalDate,
          expectedDepartureDate = placement.expectedDepartureDate,
        ),
        dateApplied = placement.statusSetDate,
        statusLastUpdated = placement.statusSetDate,
      )
      every { mockCas1AssessmentTransformer.transformToStaffDto(user) } returns requestSubmittedBy

      val result = cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement, placement, premisesEntity)

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `Transforms to outcome that is withdrawn`() {
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
      val withdrawalDate = expectedArrivalDate.minusDays(1)
      val user = UserEntityFactory()
        .withDefaults()
        .withName("Bob")
        .produce()
      val requestSubmittedBy = Cas1StaffDtoFactory()
        .withName(user.name)
        .withUsername(user.deliusUsername)
        .withStaffCode(user.deliusStaffCode)
        .produce()
      val requestForPlacement = RequestForPlacementFactory()
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withStatus(RequestForPlacementStatus.requestWithdrawn)
        .withSubmittedBy(requestSubmittedBy)
        .withSubmittedAt(Instant.now().minusDays(110))
        .withIsWithdrawn(true)
        .withWithdrawalDate(withdrawalDate.atStartOfDay().atOffset(ZoneOffset.UTC))
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

      val expected = Cas1RequestForPlacementOutcomeDto(
        requestForPlacement = Cas1ExternalRequestForPlacementDto(
          status = requestForPlacement.status,
          decision = requestForPlacement.decision?.apiValue,
          rejectionReason = requestForPlacement.rejectionReason,
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
          expectedArrivalDate = placement.expectedArrivalDate,
          expectedDepartureDate = placement.expectedDepartureDate,
        ),
        dateApplied = placement.statusSetDate,
        statusLastUpdated = placement.statusSetDate,
      )
      every { mockCas1AssessmentTransformer.transformToStaffDto(user) } returns requestSubmittedBy

      val result = cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement, placement, premisesEntity)

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `Transforms to outcome that is withdrawn with Other reason`() {
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
      val withdrawalDate = expectedArrivalDate.minusDays(1)
      val user = UserEntityFactory()
        .withDefaults()
        .withName("Bob")
        .produce()
      val requestSubmittedBy = Cas1StaffDtoFactory()
        .withName(user.name)
        .withUsername(user.deliusUsername)
        .withStaffCode(user.deliusStaffCode)
        .produce()
      val requestForPlacement = RequestForPlacementFactory()
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withStatus(RequestForPlacementStatus.requestWithdrawn)
        .withSubmittedBy(requestSubmittedBy)
        .withSubmittedAt(Instant.now().minusDays(110))
        .withIsWithdrawn(true)
        .withWithdrawalDate(withdrawalDate.atStartOfDay().atOffset(ZoneOffset.UTC))
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

      val expected = Cas1RequestForPlacementOutcomeDto(
        requestForPlacement = Cas1ExternalRequestForPlacementDto(
          decision = requestForPlacement.decision?.apiValue,
          rejectionReason = requestForPlacement.rejectionReason,
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
          expectedDepartureDate = placement.expectedDepartureDate,
          expectedArrivalDate = placement.expectedArrivalDate,
        ),
        dateApplied = placement.statusSetDate,
        statusLastUpdated = placement.statusSetDate,
      )
      every { mockCas1AssessmentTransformer.transformToStaffDto(user) } returns requestSubmittedBy

      val result = cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement, placement, premisesEntity)

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `Transforms to outcome that's rejected`() {
      val duration = 12
      val expectedArrivalDate = LocalDate.now().minusDays(100)
      val canonicalPlacementPeriod = Cas1RequestedPlacementPeriod(
        arrival = expectedArrivalDate,
        arrivalFlexible = false,
        duration = duration,
      )
      val user = UserEntityFactory()
        .withDefaults()
        .withName("Bob")
        .produce()
      val requestSubmittedBy = Cas1StaffDtoFactory()
        .withName(user.name)
        .withUsername(user.deliusUsername)
        .withStaffCode(user.deliusStaffCode)
        .produce()
      val requestForPlacement = RequestForPlacementFactory()
        .withDecision(PlacementApplicationDecision.REJECTED)
        .withRejectionReason("Oh no")
        .withStatus(RequestForPlacementStatus.requestRejected)
        .withSubmittedBy(requestSubmittedBy)
        .withSubmittedAt(Instant.now().minusDays(110))
        .withCanonicalPlacementPeriod(canonicalPlacementPeriod)
        .withStatusSetDate(expectedArrivalDate)
        .produce()
      val expected = Cas1RequestForPlacementOutcomeDto(
        requestForPlacement = Cas1ExternalRequestForPlacementDto(
          decision = requestForPlacement.decision?.apiValue,
          rejectionReason = requestForPlacement.rejectionReason,
          submittedBy = requestSubmittedBy,
          submittedAt = requestForPlacement.submittedAt?.toLocalDate(),
          withdrawalReason = requestForPlacement.withdrawalReason,
          withdrawalDate = null,
          expectedArrivalDate = expectedArrivalDate,
          durationDays = duration,
          status = requestForPlacement.status,
        ),
        placement = null,
        dateApplied = requestForPlacement.statusSetDate,
        statusLastUpdated = requestForPlacement.statusSetDate,
      )
      every { mockCas1AssessmentTransformer.transformToStaffDto(user) } returns requestSubmittedBy

      val result = cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement, null, null)

      assertThat(result).isEqualTo(expected)
    }
  }
}
