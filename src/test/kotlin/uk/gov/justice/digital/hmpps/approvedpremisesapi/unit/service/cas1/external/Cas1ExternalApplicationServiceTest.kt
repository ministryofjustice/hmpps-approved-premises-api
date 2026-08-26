package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.external

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1RequestedPlacementPeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalApplicationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalAssessmentDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPlacementDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPremisesDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalRequestForPlacementDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1PlacementPairDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingShortSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1StaffDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingShortSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RequestForPlacementFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1RequestForPlacementService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external.Cas1ExternalApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.external.Cas1ExternalApplicationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.stream.Stream
import kotlin.collections.emptyList

@SuppressWarnings("UnusedPrivateProperty")
@ExtendWith(MockKExtension::class)
class Cas1ExternalApplicationServiceTest {
  @MockK
  private lateinit var approvedPremisesApplicationRepository: ApprovedPremisesApplicationRepository

  @MockK
  private lateinit var cas1RequestForPlacementService: Cas1RequestForPlacementService

  @MockK
  private lateinit var cas1PremisesService: Cas1PremisesService

  @MockK
  private lateinit var cas1ExternalApplicationTransformer: Cas1ExternalApplicationTransformer

  @InjectMockKs
  private lateinit var service: Cas1ExternalApplicationService

  fun transformToStaffDto(user: UserEntity) = Cas1StaffDto(user.name, user.deliusUsername, user.deliusStaffCode)

  @Nested
  inner class GetPlacementHistory {

    @Test
    fun `getPlacementHistory returns requestForPlacement status and no placement status when no placements`() {
      val awaitingPlacementApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(CRN)
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

      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement1) } returns transformToPlacementHistory(requestForPlacement1)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement2) } returns transformToPlacementHistory(requestForPlacement2)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement3) } returns transformToPlacementHistory(requestForPlacement3)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement4) } returns transformToPlacementHistory(requestForPlacement4)

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
          transformToPlacementHistory(requestForPlacement2),
          transformToPlacementHistory(requestForPlacement4),
          transformToPlacementHistory(requestForPlacement1),
          transformToPlacementHistory(requestForPlacement3),
        ),
      )
    }

    @Test
    fun `getPlacementHistory returns requestForPlacement status and placement status when placements`() {
      val premisesEntity = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withSupportsSpaceBookings(true)
        .produce()
      val awaitingPlacementApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(CRN)
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
      val requestForPlacement4 = RequestForPlacementFactory().withStatusSetDate(
        LocalDate.now().minusDays(2),
      ).withStatus(RequestForPlacementStatus.requestRejected)
        .withDecision(PlacementApplicationDecision.REJECTED).produce()
      val requestForPlacement5 = RequestForPlacementFactory().withStatusSetDate(
        LocalDate.now().minusDays(50),
      ).withStatus(RequestForPlacementStatus.requestWithdrawn)
        .withIsWithdrawn(true).produce()

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
      val withdrawalDate = LocalDate.now().minusDays(10)
      val rejectionReason = "No space"
      every { cas1RequestForPlacementService.getRequestForPlacementWithdrawalDate(requestForPlacement1) } returns null
      every { cas1RequestForPlacementService.getRequestForPlacementWithdrawalDate(requestForPlacement2) } returns null
      every { cas1RequestForPlacementService.getRequestForPlacementWithdrawalDate(requestForPlacement3) } returns null
      every { cas1RequestForPlacementService.getRequestForPlacementWithdrawalDate(requestForPlacement4) } returns null
      every { cas1RequestForPlacementService.getRequestForPlacementWithdrawalDate(requestForPlacement5) } returns withdrawalDate

      every { cas1RequestForPlacementService.getRequestForPlacementRejectionReason(requestForPlacement1) } returns null
      every { cas1RequestForPlacementService.getRequestForPlacementRejectionReason(requestForPlacement2) } returns null
      every { cas1RequestForPlacementService.getRequestForPlacementRejectionReason(requestForPlacement3) } returns null
      every { cas1RequestForPlacementService.getRequestForPlacementRejectionReason(requestForPlacement4) } returns rejectionReason
      every { cas1RequestForPlacementService.getRequestForPlacementRejectionReason(requestForPlacement5) } returns null

      every {
        cas1ExternalApplicationTransformer
          .transformToCas1PlacementPair(requestForPlacement2, null, null, placement8)
      } returns
        transformToPlacementHistory(requestForPlacement2, placement8)
      every {
        cas1ExternalApplicationTransformer
          .transformToCas1PlacementPair(requestForPlacement2, null, null, placement10)
      } returns
        transformToPlacementHistory(requestForPlacement2, placement10)
      every {
        cas1ExternalApplicationTransformer
          .transformToCas1PlacementPair(requestForPlacement2, null, null, placement9)
      } returns
        transformToPlacementHistory(requestForPlacement2, placement9)
      every {
        cas1ExternalApplicationTransformer
          .transformToCas1PlacementPair(requestForPlacement3, null, null, placement7, premisesEntity)
      } returns
        transformToPlacementHistory(requestForPlacement3, placement7, premisesEntity)
      every {
        cas1ExternalApplicationTransformer
          .transformToCas1PlacementPair(requestForPlacement1, null, null, placement1)
      } returns
        transformToPlacementHistory(requestForPlacement1, placement1)
      every {
        cas1ExternalApplicationTransformer
          .transformToCas1PlacementPair(requestForPlacement4, rejectionReason, null)
      } returns
        transformToPlacementHistory(requestForPlacement4, rejectionReason = rejectionReason)
      every {
        cas1ExternalApplicationTransformer
          .transformToCas1PlacementPair(requestForPlacement5, null, withdrawalDate)
      } returns
        transformToPlacementHistory(requestForPlacement5, withdrawalDate = withdrawalDate)
      every {
        cas1ExternalApplicationTransformer
          .transformToCas1PlacementPair(requestForPlacement1, null, null, placement2)
      } returns
        transformToPlacementHistory(requestForPlacement1, placement2)
      every {
        cas1ExternalApplicationTransformer
          .transformToCas1PlacementPair(requestForPlacement1, null, null, placement3)
      } returns
        transformToPlacementHistory(requestForPlacement1, placement3)
      every {
        cas1ExternalApplicationTransformer
          .transformToCas1PlacementPair(requestForPlacement3, null, null, placement4)
      } returns
        transformToPlacementHistory(requestForPlacement3, placement4)
      every {
        cas1ExternalApplicationTransformer
          .transformToCas1PlacementPair(requestForPlacement3, null, null, placement6)
      } returns
        transformToPlacementHistory(requestForPlacement3, placement6)
      every {
        cas1ExternalApplicationTransformer
          .transformToCas1PlacementPair(requestForPlacement3, null, null, placement5)
      } returns
        transformToPlacementHistory(requestForPlacement3, placement5)

      val result = service.getPlacementPairs(awaitingPlacementApplication.id)
      assertThat(result).isEqualTo(
        listOf(
          // placement8 (+3 days)
          transformToPlacementHistory(requestForPlacement2, placement8),
          // placement10 (+2 days)
          transformToPlacementHistory(requestForPlacement2, placement10),
          // placement9 (+1 day)
          transformToPlacementHistory(requestForPlacement2, placement9),
          // placement7 (0 days)
          transformToPlacementHistory(requestForPlacement3, placement7, premisesEntity),
          // placement1 (-1)
          transformToPlacementHistory(requestForPlacement1, placement1),
          // rfp4 (no placements) (-2)
          transformToPlacementHistory(requestForPlacement4, rejectionReason = rejectionReason),
          // placement2 (-3)
          transformToPlacementHistory(requestForPlacement1, placement2),
          // placement3 (-4)
          transformToPlacementHistory(requestForPlacement1, placement3),
          // placement4 (-10)
          transformToPlacementHistory(requestForPlacement3, placement4),
          // placement6 (-15)
          transformToPlacementHistory(requestForPlacement3, placement6),
          // placement5 (-20)
          transformToPlacementHistory(requestForPlacement3, placement5),
          // rfp5 (no placements) (-50)
          transformToPlacementHistory(requestForPlacement5, withdrawalDate = withdrawalDate),
        ),
      )
    }
  }

  @Nested
  inner class GetSuitableApplicationByCrn {
    private val crn = "ABC123"
    private val user = UserEntityFactory()
      .withDefaults()
      .produce()

    @Test
    fun `getSuitableApplicationByCrn returns null as no applications of that crn`() {
      every { approvedPremisesApplicationRepository.findByCrn(crn) } returns emptyList()

      val result = service.getSuitableApplicationByCrn(crn)

      assertThat(result).isNull()
    }

    @Test
    fun `getSuitableApplicationByCrn returns requestForPlacement status and no placement status when no placements`() {
      val awaitingPlacementApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(CRN)
        .withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
        .produce()

      val requestForPlacement1 = RequestForPlacementFactory().withStatusSetDate(
        LocalDate.now().minusDays(4),
      ).withStatus(RequestForPlacementStatus.placementBooked).produce()
      val requestForPlacement2 = RequestForPlacementFactory().withStatusSetDate(
        LocalDate.now().minusDays(1),
      ).withStatus(RequestForPlacementStatus.requestUnsubmitted).produce()
      val requestForPlacement3 = RequestForPlacementFactory().withStatusSetDate(
        LocalDate.now().minusDays(5),
      ).withStatus(RequestForPlacementStatus.requestSubmitted).produce()
      val requestForPlacement4 = RequestForPlacementFactory().withStatusSetDate(
        LocalDate.now().minusDays(2),
      ).withStatus(RequestForPlacementStatus.awaitingMatch).produce()

      every { approvedPremisesApplicationRepository.findByCrn(awaitingPlacementApplication.crn) } returns listOf(awaitingPlacementApplication)
      every { cas1RequestForPlacementService.getRequestsForPlacementByApplication(awaitingPlacementApplication.id, null) } returns CasResult.Success(
        listOf(
          requestForPlacement1,
          requestForPlacement2,
          requestForPlacement3,
          requestForPlacement4,
        ),
      )
      val suitableApplication = transformToSuitableApplication(awaitingPlacementApplication, requestForPlacement2)
      val suitablePlacementPair = transformToPlacementHistory(requestForPlacement2)
      val placementHistory = listOf(
        transformToPlacementHistory(requestForPlacement4),
        transformToPlacementHistory(requestForPlacement1),
        transformToPlacementHistory(requestForPlacement3),
      )
      every { cas1RequestForPlacementService.getRequestForPlacementWithdrawalDate(any()) } returns null
      every { cas1RequestForPlacementService.getRequestForPlacementRejectionReason(any()) } returns null
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement1) } returns transformToPlacementHistory(requestForPlacement1)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement2) } returns suitablePlacementPair
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement3) } returns transformToPlacementHistory(requestForPlacement3)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement4) } returns transformToPlacementHistory(requestForPlacement4)
      every { cas1ExternalApplicationTransformer.transformToCas1SuitableApplication(awaitingPlacementApplication, suitablePlacementPair, placementHistory) } returns
        transformToSuitableApplication(awaitingPlacementApplication, requestForPlacement2)

      val result = service.getSuitableApplicationByCrn(awaitingPlacementApplication.crn)

      assertThat(result).isEqualTo(
        suitableApplication,
      )
    }

    @Test
    fun `getSuitableApplicationByCrn returns requestForPlacement status and placement status when placements`() {
      val premisesEntity = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withSupportsSpaceBookings(true)
        .produce()
      val awaitingPlacementApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(CRN)
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
      val requestForPlacement4 = RequestForPlacementFactory().withStatusSetDate(
        LocalDate.now().minusDays(2),
      ).withStatus(RequestForPlacementStatus.awaitingMatch).produce()

      every { approvedPremisesApplicationRepository.findByCrn(awaitingPlacementApplication.crn) } returns listOf(awaitingPlacementApplication)
      every { cas1RequestForPlacementService.getRequestsForPlacementByApplication(awaitingPlacementApplication.id, null) } returns CasResult.Success(
        listOf(
          requestForPlacement1,
          requestForPlacement2,
          requestForPlacement3,
          requestForPlacement4,
        ),
      )
      val suitableApplication = transformToSuitableApplication(awaitingPlacementApplication, requestForPlacement3, placement7, premisesEntity)
      val suitablePlacementPair = transformToPlacementHistory(requestForPlacement3, placement7, premisesEntity)
      val placementHistory = listOf(
        transformToPlacementHistory(requestForPlacement1, placement1),
        transformToPlacementHistory(requestForPlacement4),
        transformToPlacementHistory(requestForPlacement1, placement2),
        transformToPlacementHistory(requestForPlacement1, placement3),
        transformToPlacementHistory(requestForPlacement3, placement4),
        transformToPlacementHistory(requestForPlacement3, placement6),
        transformToPlacementHistory(requestForPlacement3, placement5),
      )
      every {
        cas1PremisesService.findPremisesById(match { it == premisesEntity.id })
      } returns premisesEntity
      every { cas1RequestForPlacementService.getRequestForPlacementWithdrawalDate(any()) } returns null
      every { cas1RequestForPlacementService.getRequestForPlacementRejectionReason(any()) } returns null
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement1, null, null, placement1) } returns transformToPlacementHistory(requestForPlacement1, placement1)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement1, null, null, placement2) } returns transformToPlacementHistory(requestForPlacement1, placement2)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement1, null, null, placement3) } returns transformToPlacementHistory(requestForPlacement1, placement3)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement3, null, null, placement4) } returns transformToPlacementHistory(requestForPlacement3, placement4)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement3, null, null, placement5) } returns transformToPlacementHistory(requestForPlacement3, placement5)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement3, null, null, placement6) } returns transformToPlacementHistory(requestForPlacement3, placement6)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement3, null, null, placement7, premisesEntity) } returns suitablePlacementPair
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement2, null, null, placement8) } returns transformToPlacementHistory(requestForPlacement2, placement8)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement2, null, null, placement9) } returns transformToPlacementHistory(requestForPlacement2, placement9)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement2, null, null, placement10) } returns transformToPlacementHistory(requestForPlacement2, placement10)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement4) } returns transformToPlacementHistory(requestForPlacement4)
      every { cas1ExternalApplicationTransformer.transformToCas1SuitableApplication(awaitingPlacementApplication, suitablePlacementPair, placementHistory) } returns suitableApplication

      every {
        cas1PremisesService.findPremisesById(match { it != premisesEntity.id })
      } returns null
      val result = service.getSuitableApplicationByCrn(awaitingPlacementApplication.crn)

      assertThat(result).isEqualTo(
        suitableApplication,
      )
    }

    @Test
    fun `getSuitableApplicationByCrn returns requestForPlacement status and placement status UPCOMING and premises`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(CRN)
        .withStatus(ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED)
        .produce()

      val premisesEntity = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withSupportsSpaceBookings(true)
        .produce()

      val booking = Cas1SpaceBookingEntityFactory()
        .withPremises(premisesEntity)
        .withExpectedArrivalDate(LocalDate.now().plusDays(1))
        .withExpectedDepartureDate(LocalDate.now().plusDays(10))
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withSpaceBookings(mutableListOf(booking))
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withApplication(application)
        .withAssessment(assessment)
        .produce()
      application.placementRequests = mutableListOf(placementRequest)

      val placement = Cas1SpaceBookingShortSummaryFactory()
        .withStatusSetDate(booking.expectedArrivalDate)
        .withExpectedDepartureDate(booking.expectedDepartureDate)
        .withExpectedArrivalDate(booking.expectedArrivalDate)
        .withStatus(Cas1SpaceBookingStatus.UPCOMING)
        .withId(booking.id)
        .withPremises(
          NamedId(
            id = premisesEntity.id,
            name = premisesEntity.name,
            code = null,
          ),
        )
        .produce()

      val requestForPlacement = RequestForPlacementFactory().withStatusSetDate(
        LocalDate.now().minusDays(4),
      ).withPlacements(
        listOf(
          placement,
        ),
      )
        .withStatus(RequestForPlacementStatus.placementBooked).produce()
      val suitableApplication = transformToSuitableApplication(application, requestForPlacement, placement, premisesEntity)
      val suitablePlacementPair = transformToPlacementHistory(requestForPlacement, placement, premisesEntity)
      every { cas1RequestForPlacementService.getRequestForPlacementWithdrawalDate(any()) } returns null
      every { cas1RequestForPlacementService.getRequestForPlacementRejectionReason(any()) } returns null
      every { cas1ExternalApplicationTransformer.transformToCas1SuitableApplication(application, suitablePlacementPair, emptyList()) } returns suitableApplication
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement, null, null, placement, premisesEntity) } returns suitablePlacementPair
      every { approvedPremisesApplicationRepository.findByCrn(application.crn) } returns listOf(application)
      every { cas1RequestForPlacementService.getRequestsForPlacementByApplication(application.id, null) } returns CasResult.Success(
        listOf(
          requestForPlacement,
        ),
      )
      every { cas1PremisesService.findPremisesById(premisesEntity.id) } returns premisesEntity

      val result = service.getSuitableApplicationByCrn(application.crn)

      assertThat(result).isEqualTo(suitableApplication)
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.external.Cas1ExternalApplicationServiceTest#provideApplications")
    fun `getSuitableApplicationByCrn returns appropriate suitable application`(
      applications: List<ApprovedPremisesApplicationEntity>,
      suitableApprovedPremisesApplication: ApprovedPremisesApplicationEntity,
    ) {
      every { approvedPremisesApplicationRepository.findByCrn(suitableApprovedPremisesApplication.crn) } returns applications

      val suitableApplication = transformToSuitableApplication(suitableApprovedPremisesApplication)

      every { cas1RequestForPlacementService.getRequestsForPlacementByApplication(suitableApplication.application.id, null) } returns CasResult.Success(emptyList())
      every { cas1ExternalApplicationTransformer.transformToCas1SuitableApplication(suitableApprovedPremisesApplication, null, emptyList()) } returns suitableApplication

      val result = service.getSuitableApplicationByCrn(suitableApprovedPremisesApplication.crn)
      assertThat(result).isEqualTo(suitableApplication)
    }

    @Test
    fun `getSuitableApplicationByCrn returns application with latest submitted date as suitable application when some have same status`() {
      val awaitingPlacementApplication1 = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now().minusDays(1))
        .withCrn(crn)
        .withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
        .produce()
      val awaitingPlacementApplication2 = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now().minusDays(2))
        .withCrn(crn)
        .withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
        .produce()
      val unallocatedAssessment = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now().plusDays(2))
        .withCrn(crn)
        .withStatus(ApprovedPremisesApplicationStatus.UNALLOCATED_ASSESSMENT)
        .produce()
      val latestAwaitingPlacementApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .withCrn(crn)
        .withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
        .produce()

      every { approvedPremisesApplicationRepository.findByCrn(crn) } returns listOf(
        awaitingPlacementApplication1,
        latestAwaitingPlacementApplication,
        awaitingPlacementApplication2,
        unallocatedAssessment,
      )
      val suitableApplication = transformToSuitableApplication(latestAwaitingPlacementApplication)

      every { cas1RequestForPlacementService.getRequestsForPlacementByApplication(latestAwaitingPlacementApplication.id, null) } returns CasResult.Success(emptyList())
      every { cas1PremisesService.findPremisesById(any()) } returns null
      every { cas1RequestForPlacementService.getRequestForPlacementWithdrawalDate(any()) } returns null
      every { cas1RequestForPlacementService.getRequestForPlacementRejectionReason(any()) } returns null
      every { cas1ExternalApplicationTransformer.transformToCas1SuitableApplication(latestAwaitingPlacementApplication, null, emptyList()) } returns suitableApplication

      val result = service.getSuitableApplicationByCrn(crn)

      assertThat(result).isEqualTo(suitableApplication)
    }

    @Test
    fun `getSuitableApplicationByCrn returns application with latest created date as suitable application when some have same status but no submitted at date`() {
      val startedApplication1 = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCreatedAt(OffsetDateTime.now().minusDays(1))
        .withCrn(crn)
        .withStatus(ApprovedPremisesApplicationStatus.STARTED)
        .produce()
      val startedApplication2 = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCreatedAt(OffsetDateTime.now().minusDays(2))
        .withCrn(crn)
        .withStatus(ApprovedPremisesApplicationStatus.STARTED)
        .produce()
      val inapplicableAssessment = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCreatedAt(OffsetDateTime.now().plusDays(2))
        .withCrn(crn)
        .withStatus(ApprovedPremisesApplicationStatus.INAPPLICABLE)
        .produce()
      val latestStartedApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCreatedAt(OffsetDateTime.now())
        .withCrn(crn)
        .withStatus(ApprovedPremisesApplicationStatus.STARTED)
        .produce()

      every { approvedPremisesApplicationRepository.findByCrn(crn) } returns listOf(
        startedApplication1,
        latestStartedApplication,
        startedApplication2,
        inapplicableAssessment,
      )
      val suitableApplication = transformToSuitableApplication(latestStartedApplication)

      every { cas1RequestForPlacementService.getRequestsForPlacementByApplication(latestStartedApplication.id, null) } returns CasResult.Success(emptyList())
      every { cas1PremisesService.findPremisesById(any()) } returns null
      every { cas1RequestForPlacementService.getRequestForPlacementWithdrawalDate(any()) } returns null
      every { cas1RequestForPlacementService.getRequestForPlacementRejectionReason(any()) } returns null
      every { cas1ExternalApplicationTransformer.transformToCas1SuitableApplication(latestStartedApplication, null, emptyList()) } returns suitableApplication

      val result = service.getSuitableApplicationByCrn(crn)

      assertThat(result).isEqualTo(suitableApplication)
    }
  }

  @Nested
  inner class GetCurrentPremisesByCrn {
    private val crn = "ABC123"

    @Test
    fun `getArrivedApplicationByCrn returns null as no applications of that crn`() {
      every { approvedPremisesApplicationRepository.findByCrn(crn) } returns emptyList()

      val result = service.getCurrentPremisesByCrn(crn)

      assertThat(result).isNull()
    }

    @Test
    fun `getArrivedApplicationByCrn returns null when no placements`() {
      val awaitingPlacementApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(CRN)
        .withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
        .produce()

      val requestForPlacement1 = RequestForPlacementFactory().withStatusSetDate(
        LocalDate.now().minusDays(4),
      ).withStatus(RequestForPlacementStatus.placementBooked).produce()
      val requestForPlacement2 = RequestForPlacementFactory().withStatusSetDate(
        LocalDate.now().minusDays(1),
      ).withStatus(RequestForPlacementStatus.requestUnsubmitted).produce()
      val requestForPlacement3 = RequestForPlacementFactory().withStatusSetDate(
        LocalDate.now().minusDays(5),
      ).withStatus(RequestForPlacementStatus.requestSubmitted).produce()
      val requestForPlacement4 = RequestForPlacementFactory().withStatusSetDate(
        LocalDate.now().minusDays(2),
      ).withStatus(RequestForPlacementStatus.awaitingMatch).produce()

      every { approvedPremisesApplicationRepository.findByCrn(awaitingPlacementApplication.crn) } returns listOf(awaitingPlacementApplication)
      every { cas1RequestForPlacementService.getRequestsForPlacementByApplication(awaitingPlacementApplication.id, null) } returns CasResult.Success(
        listOf(
          requestForPlacement1,
          requestForPlacement2,
          requestForPlacement3,
          requestForPlacement4,
        ),
      )
      every { cas1RequestForPlacementService.getRequestForPlacementWithdrawalDate(any()) } returns null
      every { cas1RequestForPlacementService.getRequestForPlacementRejectionReason(any()) } returns null
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement1) } returns transformToPlacementHistory(requestForPlacement1)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement2) } returns transformToPlacementHistory(requestForPlacement2)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement3) } returns transformToPlacementHistory(requestForPlacement3)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement4) } returns transformToPlacementHistory(requestForPlacement4)

      val result = service.getCurrentPremisesByCrn(awaitingPlacementApplication.crn)

      assertThat(result).isNull()
    }

    @Test
    fun `getArrivedApplicationByCrn returns requestForPlacement status and placement status when arrived placement`() {
      val premisesEntity = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withSupportsSpaceBookings(true)
        .produce()
      val awaitingPlacementApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(CRN)
        .withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
        .produce()
      val placement1 = Cas1SpaceBookingShortSummaryFactory()
        .withStatusSetDate(LocalDate.now().minusDays(1))
        .withStatus(Cas1SpaceBookingStatus.DEPARTED)
        .produce()
      val placement2 = Cas1SpaceBookingShortSummaryFactory()
        .withStatusSetDate(LocalDate.now().minusDays(3))
        .withStatus(Cas1SpaceBookingStatus.ARRIVED)
        .withPremises(
          NamedId(
            id = premisesEntity.id,
            name = premisesEntity.name,
            code = null,
          ),
        )
        .produce()
      val placement3 = Cas1SpaceBookingShortSummaryFactory()
        .withStatusSetDate(LocalDate.now().minusDays(4))
        .withStatus(Cas1SpaceBookingStatus.ARRIVED)
        .withPremises(
          NamedId(
            id = premisesEntity.id,
            name = premisesEntity.name,
            code = null,
          ),
        )
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
        .withStatus(Cas1SpaceBookingStatus.UPCOMING)
        .withPremises(
          NamedId(
            id = premisesEntity.id,
            name = premisesEntity.name,
            code = null,
          ),
        )
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
      val requestForPlacement4 = RequestForPlacementFactory().withStatusSetDate(
        LocalDate.now().minusDays(2),
      ).withStatus(RequestForPlacementStatus.awaitingMatch).produce()

      every { approvedPremisesApplicationRepository.findByCrn(awaitingPlacementApplication.crn) } returns listOf(awaitingPlacementApplication)
      every { cas1RequestForPlacementService.getRequestsForPlacementByApplication(awaitingPlacementApplication.id, null) } returns CasResult.Success(
        listOf(
          requestForPlacement1,
          requestForPlacement2,
          requestForPlacement3,
          requestForPlacement4,
        ),
      )
      every {
        cas1PremisesService.findPremisesById(match { it == premisesEntity.id })
      } returns premisesEntity
      every { cas1RequestForPlacementService.getRequestForPlacementWithdrawalDate(any()) } returns null
      every { cas1RequestForPlacementService.getRequestForPlacementRejectionReason(any()) } returns null
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement1, null, null, placement1) } returns
        transformToPlacementHistory(requestForPlacement1, placement1)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement1, null, null, placement2, premisesEntity) } returns
        transformToPlacementHistory(requestForPlacement1, placement2, premisesEntity)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement1, null, null, placement3, premisesEntity) } returns
        transformToPlacementHistory(requestForPlacement1, placement3, premisesEntity)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement3, null, null, placement4) } returns
        transformToPlacementHistory(requestForPlacement3, placement4)
      every {
        cas1ExternalApplicationTransformer.transformToCas1PlacementPair(
          requestForPlacement3,
          null,
          null,
          placement5,
        )
      } returns
        transformToPlacementHistory(requestForPlacement3, placement5)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement3, null, null, placement6) } returns
        transformToPlacementHistory(requestForPlacement3, placement6)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement3, null, null, placement7, premisesEntity) } returns
        transformToPlacementHistory(requestForPlacement3, placement7, premisesEntity)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement2, null, null, placement8) } returns
        transformToPlacementHistory(requestForPlacement2, placement8)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement2, null, null, placement9, premisesEntity) } returns
        transformToPlacementHistory(requestForPlacement2, placement9, premisesEntity)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement2, null, null, placement10) } returns
        transformToPlacementHistory(requestForPlacement2, placement10)
      every { cas1ExternalApplicationTransformer.transformToCas1PlacementPair(requestForPlacement4) } returns
        transformToPlacementHistory(requestForPlacement4)

      every {
        cas1PremisesService.findPremisesById(match { it != premisesEntity.id })
      } returns null
      val result = service.getCurrentPremisesByCrn(awaitingPlacementApplication.crn)

      assertThat(result).isEqualTo(
        Cas1ExternalPremisesDto(
          startDate = placement3.expectedArrivalDate,
          endDate = placement3.expectedDepartureDate,
          addressLine1 = premisesEntity.addressLine1,
          addressLine2 = premisesEntity.addressLine2,
          town = premisesEntity.town,
          postcode = premisesEntity.postcode,
        ),
      )
    }

    @Test
    fun `getArrivedApplicationByCrn returns null when no arrived placements`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(CRN)
        .withStatus(ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED)
        .produce()

      val premisesEntity = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withSupportsSpaceBookings(true)
        .produce()

      val booking = Cas1SpaceBookingEntityFactory()
        .withPremises(premisesEntity)
        .withExpectedArrivalDate(LocalDate.now().plusDays(1))
        .withExpectedDepartureDate(LocalDate.now().plusDays(10))
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withSpaceBookings(mutableListOf(booking))
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withApplication(application)
        .withAssessment(assessment)
        .produce()
      application.placementRequests = mutableListOf(placementRequest)

      val placement = Cas1SpaceBookingShortSummaryFactory()
        .withStatusSetDate(booking.expectedArrivalDate)
        .withExpectedDepartureDate(booking.expectedDepartureDate)
        .withExpectedArrivalDate(booking.expectedArrivalDate)
        .withStatus(Cas1SpaceBookingStatus.UPCOMING)
        .withId(booking.id)
        .withPremises(
          NamedId(
            id = premisesEntity.id,
            name = premisesEntity.name,
            code = null,
          ),
        )
        .produce()

      val requestForPlacement = RequestForPlacementFactory().withStatusSetDate(
        LocalDate.now().minusDays(4),
      ).withPlacements(
        listOf(
          placement,
        ),
      )
        .withStatus(RequestForPlacementStatus.placementBooked).produce()

      every { approvedPremisesApplicationRepository.findByCrn(application.crn) } returns listOf(application)
      every { cas1RequestForPlacementService.getRequestsForPlacementByApplication(application.id, null) } returns CasResult.Success(
        listOf(
          requestForPlacement,
        ),
      )
      every { cas1PremisesService.findPremisesById(premisesEntity.id) } returns premisesEntity
      every { cas1RequestForPlacementService.getRequestForPlacementWithdrawalDate(any()) } returns null
      every { cas1RequestForPlacementService.getRequestForPlacementRejectionReason(any()) } returns null
      every {
        cas1ExternalApplicationTransformer.transformToCas1PlacementPair(
          requestForPlacement,
          null,
          null,
          placement,
          premisesEntity,
        )
      } returns
        transformToPlacementHistory(requestForPlacement, placement, premisesEntity)

      val result = service.getCurrentPremisesByCrn(application.crn)

      assertThat(result).isNull()
    }
  }

  private fun transformToPlacementHistory(
    requestForPlacement: RequestForPlacement,
    placement: Cas1SpaceBookingShortSummary? = null,
    premisesEntity: ApprovedPremisesEntity? = null,
    rejectionReason: String? = null,
    withdrawalDate: LocalDate? = null,
  ) = Cas1PlacementPairDto(
    dateApplied = placement?.statusSetDate ?: requestForPlacement.statusSetDate,
    requestForPlacement = Cas1ExternalRequestForPlacementDto(
      decision = requestForPlacement.decision?.apiValue,
      submittedBy = requestForPlacement.submittedBy,
      submittedAt = requestForPlacement.submittedAt?.toLocalDate(),
      withdrawalReason = requestForPlacement.withdrawalReason,
      withdrawalDate = withdrawalDate,
      rejectionReason = rejectionReason,
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
      premises = premisesEntity?.let {
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
    ),
  )

  private fun transformToSuitableApplication(
    applicationEntity: ApprovedPremisesApplicationEntity,
    requestForPlacement: RequestForPlacement? = null,
    placement: Cas1SpaceBookingShortSummary? = null,
    premisesEntity: ApprovedPremisesEntity? = null,
  ) = Cas1SuitableApplication(
    requestForPlacement = Cas1ExternalRequestForPlacementDto(
      decision = requestForPlacement?.decision?.apiValue,
      rejectionReason = null,
      submittedBy = requestForPlacement?.submittedBy,
      submittedAt = requestForPlacement?.submittedAt?.toLocalDate(),
      withdrawalReason = requestForPlacement?.withdrawalReason,
      withdrawalDate = null,
      expectedArrivalDate = placement?.expectedArrivalDate ?: requestForPlacement?.canonicalPlacementPeriod?.arrival,
      durationDays = requestForPlacement?.canonicalPlacementPeriod?.duration,
      status = requestForPlacement?.status,
    ),
    placement = Cas1ExternalPlacementDto(
      actualArrivalDate = placement?.actualArrivalDate,
      actualDepartureDate = placement?.actualDepartureDate,
      cancellationReason = placement?.cancellation?.reason?.name,
      premises = premisesEntity?.let {
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
    ),
    uiUrl = "http://localhost:3000/applications/${applicationEntity.id}",
    application = Cas1ExternalApplicationDto(
      createdAt = applicationEntity.createdAt,
      createdBy = transformToStaffDto(applicationEntity.createdByUser),
      submittedAt = applicationEntity.submittedAt,
      expiresAt = if (applicationEntity.getLatestAssessment()?.decision == AssessmentDecision.ACCEPTED) applicationEntity.getLatestAssessment()?.submittedAt?.toLocalDate()?.plusDays(365) else null,
      status = applicationEntity.status,
      id = applicationEntity.id,
    ),
    assessment = Cas1ExternalAssessmentDto(
      decision = applicationEntity.getLatestAssessment()?.decision?.apiValue,
      rejectionRationale = applicationEntity.getLatestAssessment()?.rejectionRationale,
    ),
    placementHistory = emptyList(),
  )

  private companion object {
    const val CRN = "X99999"
    private val user = UserEntityFactory()
      .withDefaults()
      .produce()
    private val pendingPlacementRequestApplication = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .withCrn(CRN)
      .withStatus(ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST)
      .produce()
    private val requestedFurtherInformationApplication = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .withCrn(CRN)
      .withStatus(ApprovedPremisesApplicationStatus.REQUESTED_FURTHER_INFORMATION)
      .produce()
    private val assessmentInProgressApplication = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .withCrn(CRN)
      .withStatus(ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS)
      .produce()
    private val unallocatedAssessmentApplication = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .withCrn(CRN)
      .withStatus(ApprovedPremisesApplicationStatus.UNALLOCATED_ASSESSMENT)
      .produce()
    private val awaitingAssessmentApplication = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .withCrn(CRN)
      .withStatus(ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT)
      .produce()
    private val awaitingPlacementApplication = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .withCrn(CRN)
      .withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
      .produce()
    private val startedApplication = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .withCrn(CRN)
      .withStatus(ApprovedPremisesApplicationStatus.STARTED)
      .produce()
    private val rejectedApplication = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .withCrn(CRN)
      .withStatus(ApprovedPremisesApplicationStatus.REJECTED)
      .produce()
    private val inapplicableApplication = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .withCrn(CRN)
      .withStatus(ApprovedPremisesApplicationStatus.INAPPLICABLE)
      .produce()
    private val expiredApplication = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .withCrn(CRN)
      .withStatus(ApprovedPremisesApplicationStatus.EXPIRED)
      .produce()
    private val withdrawnApplication = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .withCrn(CRN)
      .withStatus(ApprovedPremisesApplicationStatus.WITHDRAWN)
      .produce()

    @JvmStatic
    fun provideApplications(): Stream<Arguments> = Stream.of(
      Arguments.of(
        listOf(
          inapplicableApplication,
          expiredApplication,
          withdrawnApplication,
          rejectedApplication,
          startedApplication,
          unallocatedAssessmentApplication,
          awaitingAssessmentApplication,
          assessmentInProgressApplication,
          requestedFurtherInformationApplication,
          pendingPlacementRequestApplication,
          awaitingPlacementApplication,
          pendingPlacementRequestApplication,
          requestedFurtherInformationApplication,
          assessmentInProgressApplication,
          awaitingAssessmentApplication,
          unallocatedAssessmentApplication,
          startedApplication,
          rejectedApplication,
          withdrawnApplication,
          expiredApplication,
          inapplicableApplication,
        ),
        awaitingPlacementApplication,
      ),
      Arguments.of(
        listOf(
          inapplicableApplication,
          expiredApplication,
          withdrawnApplication,
          rejectedApplication,
          startedApplication,
          unallocatedAssessmentApplication,
          awaitingAssessmentApplication,
          assessmentInProgressApplication,
          requestedFurtherInformationApplication,
          pendingPlacementRequestApplication,
          requestedFurtherInformationApplication,
          assessmentInProgressApplication,
          awaitingAssessmentApplication,
          unallocatedAssessmentApplication,
          startedApplication,
          rejectedApplication,
          withdrawnApplication,
          expiredApplication,
          inapplicableApplication,
        ),
        pendingPlacementRequestApplication,
      ),
      Arguments.of(
        listOf(
          inapplicableApplication,
          expiredApplication,
          withdrawnApplication,
          rejectedApplication,
          startedApplication,
          unallocatedAssessmentApplication,
          awaitingAssessmentApplication,
          assessmentInProgressApplication,
          requestedFurtherInformationApplication,
          assessmentInProgressApplication,
          awaitingAssessmentApplication,
          unallocatedAssessmentApplication,
          startedApplication,
          rejectedApplication,
          withdrawnApplication,
          expiredApplication,
          inapplicableApplication,
        ),
        requestedFurtherInformationApplication,
      ),
      Arguments.of(
        listOf(
          inapplicableApplication,
          expiredApplication,
          withdrawnApplication,
          rejectedApplication,
          startedApplication,
          unallocatedAssessmentApplication,
          awaitingAssessmentApplication,
          assessmentInProgressApplication,
          awaitingAssessmentApplication,
          unallocatedAssessmentApplication,
          startedApplication,
          rejectedApplication,
          withdrawnApplication,
          expiredApplication,
          inapplicableApplication,
        ),
        assessmentInProgressApplication,
      ),
      Arguments.of(
        listOf(
          inapplicableApplication,
          expiredApplication,
          withdrawnApplication,
          rejectedApplication,
          startedApplication,
          unallocatedAssessmentApplication,
          awaitingAssessmentApplication,
          unallocatedAssessmentApplication,
          startedApplication,
          rejectedApplication,
          withdrawnApplication,
          expiredApplication,
          inapplicableApplication,
        ),
        awaitingAssessmentApplication,
      ),
      Arguments.of(
        listOf(
          inapplicableApplication,
          expiredApplication,
          withdrawnApplication,
          rejectedApplication,
          startedApplication,
          unallocatedAssessmentApplication,
          startedApplication,
          rejectedApplication,
          withdrawnApplication,
          expiredApplication,
          inapplicableApplication,
        ),
        unallocatedAssessmentApplication,
      ),
      Arguments.of(
        listOf(
          inapplicableApplication,
          expiredApplication,
          withdrawnApplication,
          rejectedApplication,
          startedApplication,
          rejectedApplication,
          withdrawnApplication,
          expiredApplication,
          inapplicableApplication,
        ),
        startedApplication,
      ),
      Arguments.of(
        listOf(
          inapplicableApplication,
          expiredApplication,
          withdrawnApplication,
          rejectedApplication,
          withdrawnApplication,
          expiredApplication,
          inapplicableApplication,
        ),
        rejectedApplication,
      ),
      Arguments.of(
        listOf(
          inapplicableApplication,
          expiredApplication,
          withdrawnApplication,
          expiredApplication,
          inapplicableApplication,
        ),
        withdrawnApplication,
      ),
      Arguments.of(listOf(inapplicableApplication, expiredApplication, inapplicableApplication), expiredApplication),
      Arguments.of(listOf(inapplicableApplication), inapplicableApplication),
    )
  }
}
