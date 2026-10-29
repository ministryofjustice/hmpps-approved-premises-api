package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.external

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1RequestedPlacementPeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPlacementDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPremisesDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalRequestForPlacementDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1PlacementPairDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingShortSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingShortSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RequestForPlacementFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1RequestForPlacementService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external.Cas1PlacementPairService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.external.Cas1ExternalApplicationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate
import java.time.LocalDate
import java.time.ZoneOffset

@SuppressWarnings("UnusedPrivateProperty")
@ExtendWith(MockKExtension::class)
class Cas1PlacementPairServiceTest {
  @MockK
  private lateinit var cas1RequestForPlacementService: Cas1RequestForPlacementService

  @MockK
  private lateinit var cas1PremisesService: Cas1PremisesService

  @MockK
  private lateinit var cas1ExternalApplicationTransformer: Cas1ExternalApplicationTransformer

  @InjectMockKs
  private lateinit var service: Cas1PlacementPairService

  private val user = UserEntityFactory()
    .withDefaults()
    .produce()

  @Nested
  inner class GetPlacementPairs {

    @Test
    fun `getPlacementPairs returns requestForPlacement status and no placement status when no placements`() {
      val awaitingPlacementApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn("X99999")
        .withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
        .produce()

      val requestForPlacement1 = RequestForPlacementFactory().withStatusSetDate(
        LocalDate.now().minusDays(4),
      ).withStatus(RequestForPlacementStatus.placementBooked)
        .withCanonicalPlacementPeriod(
          Cas1RequestedPlacementPeriod(
            arrival = LocalDate.now().plusDays(3),
            arrivalFlexible = true,
            duration = 10,
          ),
        )
        .produce()
      val requestForPlacement2 = RequestForPlacementFactory().withStatusSetDate(
        LocalDate.now().minusDays(1),
      ).withStatus(RequestForPlacementStatus.requestUnsubmitted)
        .withCanonicalPlacementPeriod(
          Cas1RequestedPlacementPeriod(
            arrival = LocalDate.now().plusDays(1),
            arrivalFlexible = true,
            duration = 5,
          ),
        ).produce()
      val requestForPlacement3 = RequestForPlacementFactory().withStatusSetDate(
        LocalDate.now().minusDays(5),
      ).withStatus(RequestForPlacementStatus.requestSubmitted)
        .withCanonicalPlacementPeriod(
          Cas1RequestedPlacementPeriod(
            arrival = LocalDate.now().plusDays(2),
            arrivalFlexible = true,
            duration = 8,
          ),
        ).produce()
      val requestForPlacement4 = RequestForPlacementFactory().withStatusSetDate(
        LocalDate.now().minusDays(2),
      ).withStatus(RequestForPlacementStatus.awaitingMatch).withCanonicalPlacementPeriod(
        Cas1RequestedPlacementPeriod(
          arrival = LocalDate.now().plusDays(1),
          arrivalFlexible = true,
          duration = 11,
        ),
      ).produce()

      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement1) } returns toPlacementPair(requestForPlacement1)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement2) } returns toPlacementPair(requestForPlacement2)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement3) } returns toPlacementPair(requestForPlacement3)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement4) } returns toPlacementPair(requestForPlacement4)

      every { cas1RequestForPlacementService.getRequestsForPlacementByApplication(awaitingPlacementApplication.id, null) } returns CasResult.Success(
        listOf(
          requestForPlacement1,
          requestForPlacement2,
          requestForPlacement3,
          requestForPlacement4,
        ),
      )

      val result = service.getPlacementPairs(awaitingPlacementApplication.id)

      assertThat(result).isEqualTo(
        listOf(
          toPlacementPair(requestForPlacement2),
          toPlacementPair(requestForPlacement4),
          toPlacementPair(requestForPlacement1),
          toPlacementPair(requestForPlacement3),
        ),
      )
    }

    @Test
    fun `getPlacementPairs returns requestForPlacement status and placement status when placements`() {
      val premisesEntity = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withSupportsSpaceBookings(true)
        .produce()
      val awaitingPlacementApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn("X99999")
        .withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
        .produce()
      val placement1 = Cas1SpaceBookingShortSummaryFactory()
        .withStatusSetDate(LocalDate.now().minusDays(1))
        .withStatus(Cas1SpaceBookingStatus.DEPARTED)
        .produce()
      val placement2 = Cas1SpaceBookingShortSummaryFactory()
        .withStatusSetDate(LocalDate.now().minusDays(3))
        .withStatus(Cas1SpaceBookingStatus.CANCELLED)
        .produce()
      val placement3 = Cas1SpaceBookingShortSummaryFactory()
        .withStatusSetDate(LocalDate.now().minusDays(4))
        .withStatus(Cas1SpaceBookingStatus.ARRIVED)
        .produce()
      val placement4 = Cas1SpaceBookingShortSummaryFactory()
        .withStatusSetDate(LocalDate.now().minusDays(10))
        .withStatus(Cas1SpaceBookingStatus.UPCOMING)
        .produce()
      val placement5 = Cas1SpaceBookingShortSummaryFactory()
        .withStatusSetDate(LocalDate.now().minusDays(20))
        .withStatus(Cas1SpaceBookingStatus.NOT_ARRIVED)
        .produce()
      val placement6 = Cas1SpaceBookingShortSummaryFactory()
        .withStatusSetDate(LocalDate.now().minusDays(15))
        .withStatus(Cas1SpaceBookingStatus.NOT_ARRIVED)
        .produce()
      val placement7 = Cas1SpaceBookingShortSummaryFactory()
        .withStatusSetDate(LocalDate.now())
        .withStatus(Cas1SpaceBookingStatus.UPCOMING)
        .withPremises(
          NamedId(
            id = premisesEntity.id,
            name = premisesEntity.name,
            code = null,
          ),
        )
        .produce()
      val placement8 = Cas1SpaceBookingShortSummaryFactory()
        .withStatusSetDate(LocalDate.now().plusDays(3))
        .withStatus(Cas1SpaceBookingStatus.DEPARTED)
        .produce()
      val placement9 = Cas1SpaceBookingShortSummaryFactory()
        .withStatusSetDate(LocalDate.now().plusDays(1))
        .withStatus(Cas1SpaceBookingStatus.ARRIVED)
        .produce()
      val placement10 = Cas1SpaceBookingShortSummaryFactory()
        .withStatusSetDate(LocalDate.now().plusDays(2))
        .withStatus(Cas1SpaceBookingStatus.CANCELLED)
        .produce()
      val requestForPlacement1 = RequestForPlacementFactory().withStatusSetDate(
        LocalDate.now().minusDays(4),
      ).withPlacements(
        listOf(
          placement1,
          placement2,
          placement3,
        ),
      )
        .withStatus(RequestForPlacementStatus.placementBooked).produce()
      val requestForPlacement2 = RequestForPlacementFactory().withStatusSetDate(
        LocalDate.now().minusDays(1),
      ).withPlacements(
        listOf(
          placement8,
          placement9,
          placement10,
        ),
      ).withStatus(RequestForPlacementStatus.requestUnsubmitted).produce()
      val requestForPlacement3 = RequestForPlacementFactory().withStatusSetDate(
        LocalDate.now().minusDays(5),
      ).withPlacements(
        listOf(
          placement5,
          placement6,
          placement7,
          placement4,
        ),
      ).withStatus(RequestForPlacementStatus.requestSubmitted).produce()

      val withdrawalDate = LocalDate.now().minusDays(10)
      val rejectionReason = "No space"

      val requestForPlacement4 = RequestForPlacementFactory().withStatusSetDate(
        LocalDate.now().minusDays(2),
      ).withStatus(RequestForPlacementStatus.requestRejected)
        .withDecision(PlacementApplicationDecision.REJECTED).produce()
        .copy(rejectionReason = rejectionReason)
      val requestForPlacement5 = RequestForPlacementFactory().withStatusSetDate(
        LocalDate.now().minusDays(50),
      ).withStatus(RequestForPlacementStatus.requestWithdrawn)
        .withIsWithdrawn(true).produce()
        .copy(withdrawalDate = withdrawalDate.atStartOfDay().atOffset(ZoneOffset.UTC))

      every { cas1RequestForPlacementService.getRequestsForPlacementByApplication(awaitingPlacementApplication.id, null) } returns CasResult.Success(
        listOf(
          requestForPlacement1,
          requestForPlacement2,
          requestForPlacement3,
          requestForPlacement4,
          requestForPlacement5,
        ),
      )
      every {
        cas1PremisesService.findPremisesById(match { it == premisesEntity.id })
      } returns premisesEntity

      every {
        cas1PremisesService.findPremisesById(match { it != premisesEntity.id })
      } returns null

      every {
        cas1ExternalApplicationTransformer
          .transformToCas1PlacementPair(requestForPlacement2, null, null, placement8)
      } returns
        toPlacementPair(requestForPlacement2, placement8)
      every {
        cas1ExternalApplicationTransformer
          .transformToCas1PlacementPair(requestForPlacement2, null, null, placement10)
      } returns
        toPlacementPair(requestForPlacement2, placement10)
      every {
        cas1ExternalApplicationTransformer
          .transformToCas1PlacementPair(requestForPlacement2, null, null, placement9)
      } returns
        toPlacementPair(requestForPlacement2, placement9)
      every {
        cas1ExternalApplicationTransformer
          .transformToCas1PlacementPair(requestForPlacement3, null, null, placement7, premisesEntity)
      } returns
        toPlacementPair(requestForPlacement3, placement7, premisesEntity)
      every {
        cas1ExternalApplicationTransformer
          .transformToCas1PlacementPair(requestForPlacement1, null, null, placement1)
      } returns
        toPlacementPair(requestForPlacement1, placement1)
      every {
        cas1ExternalApplicationTransformer
          .transformToCas1PlacementPair(requestForPlacement4, rejectionReason, null)
      } returns
        toPlacementPair(requestForPlacement4, requestForPlacementRejectionReason = rejectionReason)
      every {
        cas1ExternalApplicationTransformer
          .transformToCas1PlacementPair(requestForPlacement5, null, withdrawalDate)
      } returns
        toPlacementPair(requestForPlacement5, requestForPlacementWithdrawalDate = withdrawalDate)
      every {
        cas1ExternalApplicationTransformer
          .transformToCas1PlacementPair(requestForPlacement1, null, null, placement2)
      } returns
        toPlacementPair(requestForPlacement1, placement2)
      every {
        cas1ExternalApplicationTransformer
          .transformToCas1PlacementPair(requestForPlacement1, null, null, placement3)
      } returns
        toPlacementPair(requestForPlacement1, placement3)
      every {
        cas1ExternalApplicationTransformer
          .transformToCas1PlacementPair(requestForPlacement3, null, null, placement4)
      } returns
        toPlacementPair(requestForPlacement3, placement4)
      every {
        cas1ExternalApplicationTransformer
          .transformToCas1PlacementPair(requestForPlacement3, null, null, placement6)
      } returns
        toPlacementPair(requestForPlacement3, placement6)
      every {
        cas1ExternalApplicationTransformer
          .transformToCas1PlacementPair(requestForPlacement3, null, null, placement5)
      } returns
        toPlacementPair(requestForPlacement3, placement5)

      val result = service.getPlacementPairs(awaitingPlacementApplication.id)
      assertThat(result).isEqualTo(
        listOf(
          // placement8 (+3 days)
          toPlacementPair(requestForPlacement2, placement8),
          // placement10 (+2 days)
          toPlacementPair(requestForPlacement2, placement10),
          // placement9 (+1 day)
          toPlacementPair(requestForPlacement2, placement9),
          // placement7 (0 days)
          toPlacementPair(requestForPlacement3, placement7, premisesEntity),
          // placement1 (-1)
          toPlacementPair(requestForPlacement1, placement1),
          // rfp4 (no placements) (-2)
          toPlacementPair(requestForPlacement4, requestForPlacementRejectionReason = rejectionReason),
          // placement2 (-3)
          toPlacementPair(requestForPlacement1, placement2),
          // placement3 (-4)
          toPlacementPair(requestForPlacement1, placement3),
          // placement4 (-10)
          toPlacementPair(requestForPlacement3, placement4),
          // placement6 (-15)
          toPlacementPair(requestForPlacement3, placement6),
          // placement5 (-20)
          toPlacementPair(requestForPlacement3, placement5),
          // rfp5 (no placements) (-50)
          toPlacementPair(requestForPlacement5, requestForPlacementWithdrawalDate = withdrawalDate),
        ),
      )
    }
  }

  private fun toPlacementPair(
    requestForPlacement: RequestForPlacement,
    placement: Cas1SpaceBookingShortSummary? = null,
    placementPremises: ApprovedPremisesEntity? = null,
    requestForPlacementRejectionReason: String? = null,
    requestForPlacementWithdrawalDate: LocalDate? = null,
  ) = Cas1PlacementPairDto(
    dateApplied = placement?.statusSetDate ?: requestForPlacement.statusSetDate,
    requestForPlacement = Cas1ExternalRequestForPlacementDto(
      decision = requestForPlacement.decision?.apiValue,
      submittedBy = requestForPlacement.submittedBy,
      submittedAt = requestForPlacement.submittedAt?.toLocalDate(),
      withdrawalReason = requestForPlacement.withdrawalReason,
      withdrawalDate = requestForPlacementWithdrawalDate,
      rejectionReason = requestForPlacementRejectionReason,
      expectedArrivalDate =
      placement?.expectedArrivalDate
        ?: requestForPlacement.canonicalPlacementPeriod.arrival,
      durationDays = requestForPlacement.canonicalPlacementPeriod.duration,
      status = requestForPlacement.status,
    ),
    placement = Cas1ExternalPlacementDto(
      actualArrivalDate = placement?.actualArrivalDate,
      actualDepartureDate = placement?.actualDepartureDate,
      cancellationReason = placement?.cancellation?.reason?.name,
      premises = placementPremises?.let {
        Cas1ExternalPremisesDto(
          startDate = placement?.expectedArrivalDate,
          endDate = placement?.expectedDepartureDate,
          addressLine1 = it.addressLine1,
          addressLine2 = it.addressLine2,
          town = it.town,
          postcode = it.postcode,
        )
      },
      status = placement?.status,
      expectedDepartureDate = placement?.expectedDepartureDate,
    ),
  )
}
