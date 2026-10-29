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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingShortSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RequestForPlacementFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external.Cas1ExternalApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external.Cas1PlacementPairService
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
  private lateinit var cas1PlacementPairService: Cas1PlacementPairService

  @MockK
  private lateinit var cas1ExternalApplicationTransformer: Cas1ExternalApplicationTransformer

  @InjectMockKs
  private lateinit var service: Cas1ExternalApplicationService

  fun transformToStaffDto(user: UserEntity) = Cas1StaffDto(user.name, user.deliusUsername, user.deliusStaffCode)

  @Nested
  inner class GetSuitableApplicationByCrn {
    private val crn = "ABC123"
    private val user = UserEntityFactory()
      .withDefaults()
      .produce()

    @Test
    fun `returns null as no applications of that crn`() {
      every { approvedPremisesApplicationRepository.findByCrn(crn) } returns emptyList()

      val result = service.getSuitableApplicationByCrn(crn)

      assertThat(result).isNull()
    }

    @Nested
    inner class SuitablePlacementPair {
      @Test
      fun `uses the future placement pair closest to today as suitable and returns past pairs as history`() {
        val application = ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(user)
          .withCrn(CRN)
          .withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
          .produce()

        val future2 = placementPair(LocalDate.now().plusDays(3))
        val future1 = placementPair(LocalDate.now().plusDays(1))
        val today = placementPair(LocalDate.now())
        val past1 = placementPair(LocalDate.now().minusDays(1))
        val past2 = placementPair(LocalDate.now().minusDays(5))

        val suitableApplication = transformToSuitableApplication(application)

        every { approvedPremisesApplicationRepository.findByCrn(application.crn) } returns listOf(application)
        every { cas1PlacementPairService.getPlacementPairs(application.id) } returns listOf(future2, future1, today, past1, past2)
        every {
          cas1ExternalApplicationTransformer.transformToCas1SuitableApplication(application, today, listOf(past1, past2))
        } returns suitableApplication

        val result = service.getSuitableApplicationByCrn(application.crn)

        assertThat(result).isEqualTo(suitableApplication)
      }

      @Test
      fun `uses the most recent past placement pair as suitable when there are no future pairs`() {
        val application = ApprovedPremisesApplicationEntityFactory()
          .withCreatedByUser(user)
          .withCrn(CRN)
          .withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
          .produce()

        val past1 = placementPair(LocalDate.now().minusDays(1))
        val past2 = placementPair(LocalDate.now().minusDays(5))

        val suitableApplication = transformToSuitableApplication(application)

        every { approvedPremisesApplicationRepository.findByCrn(application.crn) } returns listOf(application)
        every { cas1PlacementPairService.getPlacementPairs(application.id) } returns listOf(past1, past2)
        every {
          cas1ExternalApplicationTransformer.transformToCas1SuitableApplication(application, past1, listOf(past2))
        } returns suitableApplication

        val result = service.getSuitableApplicationByCrn(application.crn)

        assertThat(result).isEqualTo(suitableApplication)
      }
    }

    @Nested
    inner class MostSuitableApplication {
      @ParameterizedTest
      @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.external.Cas1ExternalApplicationServiceTest#suitableApplicationScenarioArguments")
      fun `returns application with most advanced status`(
        applications: List<ApprovedPremisesApplicationEntity>,
        expectedSuitableApplication: ApprovedPremisesApplicationEntity,
      ) {
        every { approvedPremisesApplicationRepository.findByCrn(expectedSuitableApplication.crn) } returns applications

        val suitableApplication = transformToSuitableApplication(expectedSuitableApplication)

        every { cas1PlacementPairService.getPlacementPairs(suitableApplication.application.id) } returns emptyList()
        every { cas1ExternalApplicationTransformer.transformToCas1SuitableApplication(expectedSuitableApplication, null, emptyList()) } returns suitableApplication

        val result = service.getSuitableApplicationByCrn(expectedSuitableApplication.crn)
        assertThat(result).isEqualTo(suitableApplication)
      }

      @Test
      fun `returns application with latest submitted date as suitable application when some have same status`() {
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

        every { cas1PlacementPairService.getPlacementPairs(latestAwaitingPlacementApplication.id) } returns emptyList()
        every { cas1ExternalApplicationTransformer.transformToCas1SuitableApplication(latestAwaitingPlacementApplication, null, emptyList()) } returns suitableApplication

        val result = service.getSuitableApplicationByCrn(crn)

        assertThat(result).isEqualTo(suitableApplication)
      }

      @Test
      fun `returns application with latest created date as suitable application when some have same status but no submitted at date`() {
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

        every { cas1PlacementPairService.getPlacementPairs(latestStartedApplication.id) } returns emptyList()
        every { cas1ExternalApplicationTransformer.transformToCas1SuitableApplication(latestStartedApplication, null, emptyList()) } returns suitableApplication

        val result = service.getSuitableApplicationByCrn(crn)

        assertThat(result).isEqualTo(suitableApplication)
      }
    }
  }

  @Nested
  inner class GetCurrentPremisesByCrn {
    private val crn = "ABC123"

    @Test
    fun `returns null as no applications of that crn`() {
      every { approvedPremisesApplicationRepository.findByCrn(crn) } returns emptyList()

      val result = service.getCurrentPremisesByCrn(crn)

      assertThat(result).isNull()
    }

    @Test
    fun `returns null when no placement pairs`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(CRN)
        .withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
        .produce()

      every { approvedPremisesApplicationRepository.findByCrn(application.crn) } returns listOf(application)
      every { cas1PlacementPairService.getPlacementPairs(application.id) } returns listOf(
        placementPair(LocalDate.now().minusDays(1)),
        placementPair(LocalDate.now().minusDays(2)),
      )

      val result = service.getCurrentPremisesByCrn(application.crn)

      assertThat(result).isNull()
    }

    @Test
    fun `returns premises of the most recent arrived placement`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(CRN)
        .withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
        .produce()

      val premises1Entity = ApprovedPremisesEntityFactory().withDefaults().produce()
      val premises2Entity = ApprovedPremisesEntityFactory().withDefaults().produce()
      val premises3Entity = ApprovedPremisesEntityFactory().withDefaults().produce()

      val upcoming = placementPair(LocalDate.now().plusDays(1), Cas1SpaceBookingStatus.UPCOMING, premises1Entity)
      val arrived = placementPair(LocalDate.now().minusDays(3), Cas1SpaceBookingStatus.ARRIVED, premises2Entity)
      val departed = placementPair(LocalDate.now().minusDays(10), Cas1SpaceBookingStatus.DEPARTED, premises3Entity)

      every { approvedPremisesApplicationRepository.findByCrn(application.crn) } returns listOf(application)
      every { cas1PlacementPairService.getPlacementPairs(application.id) } returns listOf(upcoming, arrived, departed)

      val result = service.getCurrentPremisesByCrn(application.crn)

      assertThat(result).isEqualTo(arrived.placement!!.premises)
    }

    @Test
    fun `returns null when no arrived placements`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(CRN)
        .withStatus(ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED)
        .produce()
      val premisesEntity = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withSupportsSpaceBookings(true)
        .produce()

      every { approvedPremisesApplicationRepository.findByCrn(application.crn) } returns listOf(application)
      every { cas1PlacementPairService.getPlacementPairs(application.id) } returns listOf(
        placementPair(LocalDate.now().plusDays(1), Cas1SpaceBookingStatus.UPCOMING, premisesEntity),
      )

      val result = service.getCurrentPremisesByCrn(application.crn)

      assertThat(result).isNull()
    }
  }

  private fun placementPair(
    dateApplied: LocalDate,
    status: Cas1SpaceBookingStatus? = null,
    premises: ApprovedPremisesEntity? = null,
  ): Cas1PlacementPairDto {
    val requestForPlacement = RequestForPlacementFactory()
      .withStatusSetDate(dateApplied)
      .withStatus(RequestForPlacementStatus.placementBooked)
      .produce()
    val placement = status?.let {
      Cas1SpaceBookingShortSummaryFactory()
        .withStatusSetDate(dateApplied)
        .withStatus(it)
        .produce()
    }
    return transformToPlacementHistory(requestForPlacement, placement, premises)
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
      expectedDepartureDate = placement?.expectedDepartureDate,
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
      expectedDepartureDate = placement?.expectedDepartureDate,
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
    fun suitableApplicationScenarioArguments(): Stream<Arguments> = Stream.of(
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
