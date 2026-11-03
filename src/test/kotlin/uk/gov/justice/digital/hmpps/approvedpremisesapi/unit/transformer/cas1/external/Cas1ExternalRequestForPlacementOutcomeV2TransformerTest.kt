package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1.external

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1RequestedPlacementPeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CancellationReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPremisesV2Dto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalRequestForPlacementWithdrawalDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.PlacementApplicationDecisionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingShortSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RequestForPlacementFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1StaffDtoFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.external.Cas1ExternalRequestForPlacementOutcomeV2Transformer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class Cas1ExternalRequestForPlacementOutcomeV2TransformerTest {
  private val transformer = Cas1ExternalRequestForPlacementOutcomeV2Transformer()

  @Nested
  inner class TransformToRequestForPlacement {

    @Test
    fun `maps all fields`() {
      val submittedBy = Cas1StaffDtoFactory().produce()
      val rfp = RequestForPlacementFactory()
        .withStatus(RequestForPlacementStatus.requestWithdrawn)
        .withStatusSetDateTime(LocalDateTime.parse("2024-12-03T10:15:30"))
        .withSubmittedBy(submittedBy)
        .withSubmittedAt(Instant.parse("2026-02-03T10:00:00Z"))
        .withCanonicalPlacementPeriod(Cas1RequestedPlacementPeriod(arrival = LocalDate.of(2026, 6, 1), arrivalFlexible = false, duration = 12))
        .withDecision(PlacementApplicationDecision.REJECTED)
        .withRejectionReason("No space")
        .withIsWithdrawn(true)
        .withWithdrawalReason(WithdrawPlacementRequestReason.noCapacityDueToLostBed)
        .withWithdrawalDate(OffsetDateTime.of(2026, 4, 5, 9, 0, 0, 0, ZoneOffset.UTC))
        .produce()

      val result = transformer.transformToRequestForPlacement(rfp)

      assertThat(result.status).isEqualTo(RequestForPlacementStatus.requestWithdrawn)
      assertThat(result.statusLastUpdated).isEqualTo(LocalDateTime.parse("2024-12-03T10:15:30"))
      assertThat(result.submittedBy).isEqualTo(submittedBy)
      assertThat(result.submittedAt).isEqualTo(LocalDate.of(2026, 2, 3))
      assertThat(result.expectedArrivalDate).isEqualTo(LocalDate.of(2026, 6, 1))
      assertThat(result.durationDays).isEqualTo(12)
      assertThat(result.decision).isEqualTo(PlacementApplicationDecisionDto.rejected)
      assertThat(result.rejectionReason).isEqualTo("No space")
      assertThat(result.withdrawal).isEqualTo(
        Cas1ExternalRequestForPlacementWithdrawalDto(WithdrawPlacementRequestReason.noCapacityDueToLostBed, LocalDate.of(2026, 4, 5)),
      )
    }

    @Test
    fun `has null withdrawal when not withdrawn`() {
      val rfp = RequestForPlacementFactory()
        .withIsWithdrawn(false)
        .withWithdrawalReason(WithdrawPlacementRequestReason.noCapacityDueToLostBed)
        .withWithdrawalDate(OffsetDateTime.of(2026, 4, 5, 9, 0, 0, 0, ZoneOffset.UTC))
        .produce()

      assertThat(transformer.transformToRequestForPlacement(rfp).withdrawal).isNull()
    }
  }

  @Nested
  inner class TransformToPlacement {
    private val premises = ApprovedPremisesEntityFactory().withDefaults().produce()

    private val placement = Cas1SpaceBookingShortSummaryFactory()
      .withStatus(Cas1SpaceBookingStatus.UPCOMING)
      .withExpectedArrivalDate(LocalDate.of(2026, 7, 1))
      .withExpectedDepartureDate(LocalDate.of(2026, 7, 20))
      .withActualArrivalDate(LocalDate.of(2026, 7, 2))
      .withActualDepartureDate(LocalDate.of(2026, 7, 19))
      .withCancellation(null)
      .produce()

    @Test
    fun `maps all fields`() {
      val result = transformer.transformToPlacement(placement, premises)

      assertThat(result.status).isEqualTo(Cas1SpaceBookingStatus.UPCOMING)
      assertThat(result.canonicalArrivalDate).isEqualTo(LocalDate.of(2026, 7, 2))
      assertThat(result.canonicalDepartureDate).isEqualTo(LocalDate.of(2026, 7, 19))
      assertThat(result.expectedArrivalDate).isEqualTo(LocalDate.of(2026, 7, 1))
      assertThat(result.expectedDepartureDate).isEqualTo(LocalDate.of(2026, 7, 20))
      assertThat(result.actualArrivalDate).isEqualTo(LocalDate.of(2026, 7, 2))
      assertThat(result.actualDepartureDate).isEqualTo(LocalDate.of(2026, 7, 19))
      assertThat(result.premises).isEqualTo(
        Cas1ExternalPremisesV2Dto(premises.addressLine1, premises.addressLine2, premises.town, premises.postcode),
      )
      assertThat(result.cancellationReason).isNull()
    }

    @Test
    fun `uses cancellation reason name when reason is not other`() {
      val cancelled = placementCancelledWith(reasonName = "Oops", reasonNotes = "Because")

      assertThat(transformer.transformToPlacement(cancelled, premises).cancellationReason).isEqualTo("Oops")
    }

    @Test
    fun `uses cancellation reason notes when reason is other`() {
      val cancelled = placementCancelledWith(reasonName = CancellationReasonRepository.CAS1_OTHER_NAME, reasonNotes = "Because")

      assertThat(transformer.transformToPlacement(cancelled, premises).cancellationReason).isEqualTo("Because")
    }

    @Test
    fun `use expected arrival and departure for canonical if no actual arrival and departure`() {
      val placementWithoutActual = Cas1SpaceBookingShortSummaryFactory()
        .withStatus(Cas1SpaceBookingStatus.UPCOMING)
        .withExpectedArrivalDate(LocalDate.of(2026, 7, 1))
        .withExpectedDepartureDate(LocalDate.of(2026, 7, 20))
        .withActualArrivalDate(null)
        .withActualDepartureDate(null)
        .withCancellation(null)
        .produce()

      val result = transformer.transformToPlacement(placementWithoutActual, premises)

      assertThat(result.canonicalArrivalDate).isEqualTo(LocalDate.of(2026, 7, 1))
      assertThat(result.canonicalDepartureDate).isEqualTo(LocalDate.of(2026, 7, 20))
    }

    private fun placementCancelledWith(reasonName: String, reasonNotes: String) = Cas1SpaceBookingShortSummaryFactory()
      .withCancellation(
        Cas1SpaceBookingCancellation(
          occurredAt = LocalDate.of(2026, 6, 1),
          recordedAt = Instant.parse("2026-06-01T10:00:00Z"),
          reason = CancellationReason(id = UUID.randomUUID(), name = reasonName, isActive = true, serviceScope = "approved-premises"),
          reasonNotes = reasonNotes,
        ),
      )
      .produce()
  }
}
