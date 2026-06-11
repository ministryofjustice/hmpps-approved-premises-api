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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ExternalPremisesDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1RequestForPlacementService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external.Cas1ExternalApplicationService
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

  @InjectMockKs
  private lateinit var service: Cas1ExternalApplicationService

  @Nested
  inner class GetPlacementHistories {

    @Test
    fun `getPlacementHistories returns requestForPlacement status and no placement status when no placements`() {
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

      every { cas1RequestForPlacementService.getRequestsForPlacementByApplication(awaitingPlacementApplication.id, null) } returns CasResult.Success(
        listOf(
          requestForPlacement1,
          requestForPlacement2,
          requestForPlacement3,
          requestForPlacement4,
        ),
      )

      val result = service.getPlacementHistories(awaitingPlacementApplication.id)

      assertThat(result).isEqualTo(
        listOf(
          Cas1ExternalApplicationService.Cas1PlacementHistory(
            dateApplied = requestForPlacement2.statusSetDate,
            requestForPlacementStatus = requestForPlacement2.status,
            placementStatus = null,
            premises = null,
          ),
          Cas1ExternalApplicationService.Cas1PlacementHistory(
            dateApplied = requestForPlacement4.statusSetDate,
            requestForPlacementStatus = requestForPlacement4.status,
            placementStatus = null,
            premises = null,
          ),
          Cas1ExternalApplicationService.Cas1PlacementHistory(
            dateApplied = requestForPlacement1.statusSetDate,
            requestForPlacementStatus = requestForPlacement1.status,
            placementStatus = null,
            premises = null,
          ),
          Cas1ExternalApplicationService.Cas1PlacementHistory(
            dateApplied = requestForPlacement3.statusSetDate,
            requestForPlacementStatus = requestForPlacement3.status,
            placementStatus = null,
            premises = null,
          ),
        ),
      )
    }

    @Test
    fun `getPlacementHistories returns requestForPlacement status and placement status when placements`() {
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

      every {
        cas1PremisesService.findPremisesById(match { it != premisesEntity.id })
      } returns null

      val result = service.getPlacementHistories(awaitingPlacementApplication.id)

      assertThat(result).isEqualTo(
        listOf(
          // placement8 (+3 days)
          Cas1ExternalApplicationService.Cas1PlacementHistory(
            dateApplied = placement8.statusSetDate!!,
            requestForPlacementStatus = requestForPlacement2.status,
            placementStatus = placement8.status,
            premises = null,
          ),
          // placement10 (+2 days)
          Cas1ExternalApplicationService.Cas1PlacementHistory(
            dateApplied = placement10.statusSetDate!!,
            requestForPlacementStatus = requestForPlacement2.status,
            placementStatus = placement10.status,
            premises = null,
          ),
          // placement9 (+1 day)
          Cas1ExternalApplicationService.Cas1PlacementHistory(
            dateApplied = placement9.statusSetDate!!,
            requestForPlacementStatus = requestForPlacement2.status,
            placementStatus = placement9.status,
            premises = null,
          ),
          // placement7 (0 days)
          Cas1ExternalApplicationService.Cas1PlacementHistory(
            dateApplied = placement7.statusSetDate!!,
            requestForPlacementStatus = requestForPlacement3.status,
            placementStatus = placement7.status,
            premises = Cas1ExternalPremisesDto(
              startDate = placement7.expectedArrivalDate,
              endDate = placement7.expectedDepartureDate,
              addressLine1 = premisesEntity.addressLine1,
              addressLine2 = premisesEntity.addressLine2,
              town = premisesEntity.town,
              postcode = premisesEntity.postcode,
            ),
          ),
          // placement1 (-1)
          Cas1ExternalApplicationService.Cas1PlacementHistory(
            dateApplied = placement1.statusSetDate!!,
            requestForPlacementStatus = requestForPlacement1.status,
            placementStatus = placement1.status,
            premises = null,
          ),
          // rfp4 (no placements) (-2)
          Cas1ExternalApplicationService.Cas1PlacementHistory(
            dateApplied = requestForPlacement4.statusSetDate,
            requestForPlacementStatus = requestForPlacement4.status,
            placementStatus = null,
            premises = null,
          ),
          // placement2 (-3)
          Cas1ExternalApplicationService.Cas1PlacementHistory(
            dateApplied = placement2.statusSetDate!!,
            requestForPlacementStatus = requestForPlacement1.status,
            placementStatus = placement2.status,
            premises = null,
          ),
          // placement3 (-4)
          Cas1ExternalApplicationService.Cas1PlacementHistory(
            dateApplied = placement3.statusSetDate!!,
            requestForPlacementStatus = requestForPlacement1.status,
            placementStatus = placement3.status,
            premises = null,
          ),
          // placement4 (-10)
          Cas1ExternalApplicationService.Cas1PlacementHistory(
            dateApplied = placement4.statusSetDate!!,
            requestForPlacementStatus = requestForPlacement3.status,
            placementStatus = placement4.status,
            premises = null,
          ),
          // placement6 (-15)
          Cas1ExternalApplicationService.Cas1PlacementHistory(
            dateApplied = placement6.statusSetDate!!,
            requestForPlacementStatus = requestForPlacement3.status,
            placementStatus = placement6.status,
            premises = null,
          ),
          // placement5 (-20)
          Cas1ExternalApplicationService.Cas1PlacementHistory(
            dateApplied = placement5.statusSetDate!!,
            requestForPlacementStatus = requestForPlacement3.status,
            placementStatus = placement5.status,
            premises = null,
          ),
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

      val result = service.getSuitableApplicationByCrn(awaitingPlacementApplication.crn)

      assertThat(result).isEqualTo(
        Cas1SuitableApplication(
          id = awaitingPlacementApplication.id,
          applicationStatus = awaitingPlacementApplication.status,
          requestForPlacementStatus = requestForPlacement2.status,
          placementStatus = null,
          premises = null,
        ),
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
      every {
        cas1PremisesService.findPremisesById(match { it == premisesEntity.id })
      } returns premisesEntity

      every {
        cas1PremisesService.findPremisesById(match { it != premisesEntity.id })
      } returns null
      val result = service.getSuitableApplicationByCrn(awaitingPlacementApplication.crn)

      assertThat(result).isEqualTo(
        Cas1SuitableApplication(
          id = awaitingPlacementApplication.id,
          applicationStatus = awaitingPlacementApplication.status,
          requestForPlacementStatus = requestForPlacement3.status,
          placementStatus = placement7.status,
          premises = Cas1ExternalPremisesDto(
            startDate = placement7.expectedArrivalDate,
            endDate = placement7.expectedDepartureDate,
            addressLine1 = premisesEntity.addressLine1,
            addressLine2 = premisesEntity.addressLine2,
            town = premisesEntity.town,
            postcode = premisesEntity.postcode,
          ),
        ),
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

      val premises = Cas1ExternalPremisesDto(
        startDate = booking.expectedArrivalDate,
        endDate = booking.expectedDepartureDate,
        addressLine1 = premisesEntity.addressLine1,
        addressLine2 = premisesEntity.addressLine2,
        town = premisesEntity.town,
        postcode = premisesEntity.postcode,
      )

      every { approvedPremisesApplicationRepository.findByCrn(application.crn) } returns listOf(application)
      every { cas1RequestForPlacementService.getRequestsForPlacementByApplication(application.id, null) } returns CasResult.Success(
        listOf(
          requestForPlacement,
        ),
      )
      every { cas1PremisesService.findPremisesById(premisesEntity.id) } returns premisesEntity

      val suitableApplication = Cas1SuitableApplication(
        id = application.id,
        applicationStatus = application.status,
        requestForPlacementStatus = requestForPlacement.status,
        placementStatus = placement.status,
        premises = premises,
      )
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
      val suitableApplication = Cas1SuitableApplication(
        id = suitableApprovedPremisesApplication.id,
        applicationStatus = suitableApprovedPremisesApplication.status,
        premises = null,
        requestForPlacementStatus = null,
        placementStatus = null,
      )

      every { cas1RequestForPlacementService.getRequestsForPlacementByApplication(suitableApplication.id, null) } returns CasResult.Success(emptyList())

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

      every { cas1RequestForPlacementService.getRequestsForPlacementByApplication(latestAwaitingPlacementApplication.id, null) } returns CasResult.Success(emptyList())
      every { cas1PremisesService.findPremisesById(any()) } returns null

      val suitableApplication = Cas1SuitableApplication(
        id = latestAwaitingPlacementApplication.id,
        applicationStatus = ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT,
        premises = null,
        requestForPlacementStatus = null,
        placementStatus = null,
      )
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

      every { cas1RequestForPlacementService.getRequestsForPlacementByApplication(latestStartedApplication.id, null) } returns CasResult.Success(emptyList())
      every { cas1PremisesService.findPremisesById(any()) } returns null

      val suitableApplication = Cas1SuitableApplication(
        id = latestStartedApplication.id,
        applicationStatus = ApprovedPremisesApplicationStatus.STARTED,
        premises = null,
        requestForPlacementStatus = null,
        placementStatus = null,
      )

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

      val result = service.getCurrentPremisesByCrn(application.crn)

      assertThat(result).isNull()
    }
  }

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
