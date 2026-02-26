package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementHistory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OfflineApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationStatusService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import java.util.stream.Stream
import kotlin.collections.emptyList

@SuppressWarnings("UnusedPrivateProperty")
@ExtendWith(MockKExtension::class)
class Cas1ApplicationServiceTest {
  @MockK
  private lateinit var approvedPremisesApplicationRepository: ApprovedPremisesApplicationRepository

  @MockK
  private lateinit var applicationRepository: ApplicationRepository

  @MockK
  private lateinit var offlineApplicationRepository: OfflineApplicationRepository

  @MockK
  private lateinit var cas1ApplicationStatusService: Cas1ApplicationStatusService

  @MockK
  private lateinit var cas1ApplicationDomainEventService: Cas1ApplicationDomainEventService

  @MockK
  private lateinit var cas1ApplicationEmailService: Cas1ApplicationEmailService

  @MockK
  private lateinit var cas1AssessmentService: Cas1AssessmentService

  @MockK
  private lateinit var cas1UserAccessService: Cas1UserAccessService

  @MockK
  private lateinit var userAccessService: UserAccessService

  @MockK
  private lateinit var offenderService: OffenderService

  @MockK
  private lateinit var userRepository: UserRepository

  @MockK
  private lateinit var placementApplicationRepository: PlacementApplicationRepository

  @InjectMockKs
  private lateinit var service: Cas1ApplicationService

  @Nested
  inner class GetApplicationForUsername {

    @Test
    fun `getApplicationForUsername where user cannot access the application returns Unauthorised result`() {
      val distinguishedName = "SOMEPERSON"
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      every { userRepository.findByDeliusUsername(any()) } returns UserEntityFactory()
        .withDeliusUsername(distinguishedName)
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      every { approvedPremisesApplicationRepository.findByIdOrNull(any()) } returns ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(
          UserEntityFactory()
            .withYieldedProbationRegion {
              ProbationRegionEntityFactory()
                .withYieldedApArea { ApAreaEntityFactory().produce() }
                .produce()
            }
            .produce(),
        )
        .produce()

      every { userAccessService.userCanViewApplication(any(), any()) } returns false

      assertThat(
        service.getApplicationForUsername(
          applicationId,
          distinguishedName,
        ) is CasResult.Unauthorised,
      ).isTrue
    }

    @Test
    fun `getApplicationForUsername where user can access the application returns Success result with entity from db`() {
      val distinguishedName = "SOMEPERSON"
      val userId = UUID.fromString("239b5e41-f83e-409e-8fc0-8f1e058d417e")
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      val userEntity = UserEntityFactory()
        .withId(userId)
        .withDeliusUsername(distinguishedName)
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      val applicationEntity = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(userEntity)
        .produce()

      every { approvedPremisesApplicationRepository.findByIdOrNull(any()) } returns applicationEntity
      every { userRepository.findByDeliusUsername(any()) } returns userEntity
      every { userAccessService.userCanViewApplication(any(), any()) } returns true

      val result = service.getApplicationForUsername(applicationId, distinguishedName)

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).isEqualTo(applicationEntity)
      }
    }
  }

  @Nested
  inner class GetOfflineApplicationForUsername {
    @Test
    fun `getOfflineApplicationForUsername where application does not exist returns NotFound result`() {
      val distinguishedName = "SOMEPERSON"
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      every { offlineApplicationRepository.findByIdOrNull(applicationId) } returns null

      assertThat(
        service.getOfflineApplicationForUsername(
          applicationId,
          distinguishedName,
        ) is CasResult.NotFound,
      ).isTrue
    }

    @Test
    fun `getOfflineApplicationForUsername where where caller is not one of one of roles CAS1_CRU_MEMBER, ASSESSOR, MATCHER, MANAGER returns Unauthorised result`() {
      val distinguishedName = "SOMEPERSON"
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      every { userRepository.findByDeliusUsername(distinguishedName) } returns UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()
      every { offlineApplicationRepository.findByIdOrNull(applicationId) } returns OfflineApplicationEntityFactory()
        .produce()

      assertThat(
        service.getOfflineApplicationForUsername(
          applicationId,
          distinguishedName,
        ) is CasResult.Unauthorised,
      ).isTrue
    }

    @ParameterizedTest
    @EnumSource(
      value = UserRole::class,
      names = ["CAS1_CRU_MEMBER", "CAS1_ASSESSOR", "CAS1_FUTURE_MANAGER"],
    )
    fun `getOfflineApplicationForUsername where user has one of roles CAS1_CRU_MEMBER, ASSESSOR, FUTURE_MANAGER but does not pass LAO check returns Unauthorised result`(
      role: UserRole,
    ) {
      val distinguishedName = "SOMEPERSON"
      val userId = UUID.fromString("239b5e41-f83e-409e-8fc0-8f1e058d417e")
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      val userEntity = UserEntityFactory()
        .withId(userId)
        .withDeliusUsername(distinguishedName)
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()
        .apply {
          roles += UserRoleAssignmentEntityFactory()
            .withUser(this)
            .withRole(role)
            .produce()
        }

      val applicationEntity = OfflineApplicationEntityFactory()
        .produce()

      every { offlineApplicationRepository.findByIdOrNull(applicationId) } returns applicationEntity
      every { userRepository.findByDeliusUsername(distinguishedName) } returns userEntity
      every { offenderService.canAccessOffender(distinguishedName, applicationEntity.crn) } returns false

      val result = service.getOfflineApplicationForUsername(applicationId, distinguishedName)

      assertThat(result is CasResult.Unauthorised).isTrue
    }

    @ParameterizedTest
    @EnumSource(
      value = UserRole::class,
      names = ["CAS1_CRU_MEMBER", "CAS1_ASSESSOR", "CAS1_FUTURE_MANAGER"],
    )
    fun `getOfflineApplicationForUsername where user has permission of roles CAS1_CRU_MEMBER, ASSESSOR, FUTURE_MANAGER and passes LAO check returns Success result with entity from db`(role: UserRole) {
      val distinguishedName = "SOMEPERSON"
      val userId = UUID.fromString("239b5e41-f83e-409e-8fc0-8f1e058d417e")
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      val userEntity = UserEntityFactory()
        .withId(userId)
        .withDeliusUsername(distinguishedName)
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()
        .apply {
          roles += UserRoleAssignmentEntityFactory()
            .withUser(this)
            .withRole(role)
            .produce()
        }

      val applicationEntity = OfflineApplicationEntityFactory()
        .produce()

      every { offlineApplicationRepository.findByIdOrNull(applicationId) } returns applicationEntity
      every { userRepository.findByDeliusUsername(distinguishedName) } returns userEntity
      every { offenderService.canAccessOffender(distinguishedName, applicationEntity.crn) } returns true

      val result = service.getOfflineApplicationForUsername(applicationId, distinguishedName)

      assertThat(result is CasResult.Success).isTrue
      result as CasResult.Success

      assertThat(result.value).isEqualTo(applicationEntity)
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
    fun `getSuitableApplicationByCrn returns placementAllocatedApplication as suitable application when they all have suitable status`() {
      val placementAllocatedApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(crn)
        .withStatus(ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED)
        .produce()
      val pendingPlacementRequestApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(crn)
        .withStatus(ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST)
        .produce()
      val requestedFurtherInformationApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(crn)
        .withStatus(ApprovedPremisesApplicationStatus.REQUESTED_FURTHER_INFORMATION)
        .produce()
      val assessmentInProgressApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(crn)
        .withStatus(ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS)
        .produce()
      val unallocatedAssessmentApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(crn)
        .withStatus(ApprovedPremisesApplicationStatus.UNALLOCATED_ASSESSMENT)
        .produce()
      val awaitingAssessmentApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(crn)
        .withStatus(ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT)
        .produce()
      val awaitingPlacementApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(crn)
        .withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withDefaults()
        .withApplication(placementAllocatedApplication)
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withApplication(placementAllocatedApplication)
        .withPlacementApplication(placementApplication)
        .produce()

      val upcomingBooking = Cas1SpaceBookingEntityFactory()
        .withCancellationOccurredAt(null)
        .withActualDepartureDate(null)
        .withActualArrivalDate(null)
        .withNonArrivalConfirmedAt(null)
        .withCreatedAt(OffsetDateTime.now())
        .withApplication(placementAllocatedApplication)
        .withPlacementRequest(placementRequest)
        .produce()

      placementRequest.spaceBookings.add(upcomingBooking)
      placementApplication.placementRequest = placementRequest
      placementAllocatedApplication.placementRequests.add(placementRequest)

      every { approvedPremisesApplicationRepository.findByCrn(crn) } returns listOf(
        assessmentInProgressApplication,
        awaitingAssessmentApplication,
        placementAllocatedApplication,
        unallocatedAssessmentApplication,
        awaitingPlacementApplication,
        requestedFurtherInformationApplication,
        pendingPlacementRequestApplication,
      )

      every { placementApplicationRepository.findByApplication(placementAllocatedApplication) } returns listOf(placementApplication)

      val suitableApplication = Cas1SuitableApplication(
        id = placementAllocatedApplication.id,
        applicationStatus = ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED,
        placementStatus = Cas1SpaceBookingStatus.UPCOMING,
        requestForPlacementStatus = RequestForPlacementStatus.placementBooked,
        placementHistories = listOf(
          Cas1PlacementHistory(
            dateApplied = LocalDate.now(),
            requestForPlacementStatus = RequestForPlacementStatus.placementBooked,
            placementStatus = Cas1SpaceBookingStatus.UPCOMING,
          ),
        ),
      )

      val result = service.getSuitableApplicationByCrn(crn)

      assertThat(result).isEqualTo(suitableApplication)
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1ApplicationServiceTest#providePlacementApplications")
    fun `getSuitableApplicationByCrn returns suitable application with correct placement application but not placement requests`(
      application: ApprovedPremisesApplicationEntity,
      placementApplications: List<PlacementApplicationEntity>,
      requestForPlacementStatus: RequestForPlacementStatus,
      placementHistories: List<Cas1PlacementHistory>,
    ) {
      every { placementApplicationRepository.findByApplication(application) } returns placementApplications
      every { approvedPremisesApplicationRepository.findByCrn(application.crn) } returns listOf(application)

      val suitableApplication = Cas1SuitableApplication(
        id = application.id,
        applicationStatus = ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST,
        requestForPlacementStatus = requestForPlacementStatus,
        placementStatus = null,
        placementHistories = placementHistories,
      )

      val result = service.getSuitableApplicationByCrn(application.crn)

      assertThat(result).isEqualTo(suitableApplication)
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1ApplicationServiceTest#providePlacementRequests")
    fun `getSuitableApplicationByCrn returns suitable application with correct placement request but no placements`(
      application: ApprovedPremisesApplicationEntity,
      placementApplications: List<PlacementApplicationEntity>,
      requestForPlacementStatus: RequestForPlacementStatus,
      placementHistories: List<Cas1PlacementHistory>,
    ) {
      every { placementApplicationRepository.findByApplication(application) } returns placementApplications
      every { approvedPremisesApplicationRepository.findByCrn(application.crn) } returns listOf(application)

      val suitableApplication = Cas1SuitableApplication(
        id = application.id,
        applicationStatus = ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT,
        requestForPlacementStatus = requestForPlacementStatus,
        placementStatus = null,
        placementHistories = placementHistories,
      )

      val result = service.getSuitableApplicationByCrn(application.crn)

      assertThat(result).isEqualTo(suitableApplication)
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1ApplicationServiceTest#providePlacements")
    fun `getSuitableApplicationByCrn returns suitable application with correct placement`(
      application: ApprovedPremisesApplicationEntity,
      placementApplications: List<PlacementApplicationEntity>,
      requestForPlacementStatus: RequestForPlacementStatus,
      placementStatus: Cas1SpaceBookingStatus,
      placementHistories: List<Cas1PlacementHistory>,
    ) {
      every { placementApplicationRepository.findByApplication(application) } returns placementApplications
      every { approvedPremisesApplicationRepository.findByCrn(application.crn) } returns listOf(application)

      val suitableApplication = Cas1SuitableApplication(
        id = application.id,
        applicationStatus = ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED,
        requestForPlacementStatus = requestForPlacementStatus,
        placementStatus = placementStatus,
        placementHistories = placementHistories,
      )

      val result = service.getSuitableApplicationByCrn(application.crn)

      assertThat(result).isEqualTo(suitableApplication)
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1ApplicationServiceTest#provideApplications")
    fun `getSuitableApplicationByCrn returns appropriate suitable application`(
      applications: List<ApprovedPremisesApplicationEntity>,
      suitableApprovedPremisesApplication: ApprovedPremisesApplicationEntity,
    ) {
      every { approvedPremisesApplicationRepository.findByCrn(suitableApprovedPremisesApplication.crn) } returns applications
      val suitableApplication = Cas1SuitableApplication(
        id = suitableApprovedPremisesApplication.id,
        applicationStatus = suitableApprovedPremisesApplication.status,
        requestForPlacementStatus = null,
        placementStatus = null,
        placementHistories = emptyList(),
      )
      every { placementApplicationRepository.findByApplication(suitableApprovedPremisesApplication) } returns emptyList()

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

      every { placementApplicationRepository.findByApplication(latestAwaitingPlacementApplication) } returns emptyList()

      val suitableApplication = Cas1SuitableApplication(
        id = latestAwaitingPlacementApplication.id,
        applicationStatus = ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT,
        placementStatus = null,
        requestForPlacementStatus = null,
        placementHistories = emptyList(),
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
      every { placementApplicationRepository.findByApplication(latestStartedApplication) } returns emptyList()

      val suitableApplication = Cas1SuitableApplication(
        id = latestStartedApplication.id,
        applicationStatus = ApprovedPremisesApplicationStatus.STARTED,
        placementStatus = null,
        requestForPlacementStatus = null,
        placementHistories = emptyList(),
      )

      val result = service.getSuitableApplicationByCrn(crn)

      assertThat(result).isEqualTo(suitableApplication)
    }
  }

  @Nested
  inner class WithdrawApprovedPremisesApplication {

    @Test
    fun `withdrawApprovedPremisesApplication returns NotFound if Application does not exist`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val applicationId = UUID.fromString("bb13d346-f278-43d7-9c23-5c4077c031ca")

      every { approvedPremisesApplicationRepository.findByIdOrNull(applicationId) } returns null

      val result = service.withdrawApprovedPremisesApplication(
        applicationId,
        user,
        "alternative_identified_placement_no_longer_required",
        null,
      )

      assertThat(result is CasResult.NotFound).isTrue
    }

    @Test
    fun `withdrawApprovedPremisesApplication is idempotent and returns success if already withdrawn`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withIsWithdrawn(true)
        .produce()

      every { approvedPremisesApplicationRepository.findByIdOrNull(application.id) } returns application
      every { cas1UserAccessService.userMayWithdrawApplication(user, application) } returns true

      val result = service.withdrawApprovedPremisesApplication(application.id, user, "other", null)

      assertThat(result is CasResult.Success).isTrue
    }

    @Test
    fun `withdrawApprovedPremisesApplication returns Success and saves Application with isWithdrawn set to true, triggers domain event and email`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      application.assessments.add(assessment)

      every { approvedPremisesApplicationRepository.findByIdOrNull(application.id) } returns application
      every { cas1UserAccessService.userMayWithdrawApplication(user, application) } returns true
      every { approvedPremisesApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesApplicationEntity }
      every { cas1ApplicationStatusService.applicationWithdrawn(any()) } just Runs
      every { cas1ApplicationDomainEventService.applicationWithdrawn(any(), any()) } just Runs
      every { cas1ApplicationEmailService.applicationWithdrawn(any(), any()) } just Runs
      every { cas1AssessmentService.updateAssessmentWithdrawn(any(), any()) } just Runs

      val result = service.withdrawApprovedPremisesApplication(
        application.id,
        user,
        "alternative_identified_placement_no_longer_required",
        null,
      )

      assertThat(result is CasResult.Success).isTrue

      verify {
        approvedPremisesApplicationRepository.save(
          match {
            it.id == application.id &&
              it is ApprovedPremisesApplicationEntity &&
              it.isWithdrawn &&
              it.withdrawalReason == "alternative_identified_placement_no_longer_required"
          },
        )
      }

      verify { cas1ApplicationStatusService.applicationWithdrawn(application) }
      verify { cas1ApplicationDomainEventService.applicationWithdrawn(application, user) }
      verify { cas1ApplicationEmailService.applicationWithdrawn(application, user) }
      verify { cas1AssessmentService.updateAssessmentWithdrawn(assessment.id, user) }
    }

    @Test
    fun `withdrawApprovedPremisesApplication returns Success and saves Application with isWithdrawn set to true, triggers domain event when other reason is set`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      every { approvedPremisesApplicationRepository.findByIdOrNull(application.id) } returns application
      every { cas1UserAccessService.userMayWithdrawApplication(user, application) } returns true
      every { approvedPremisesApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesApplicationEntity }
      every { cas1ApplicationStatusService.applicationWithdrawn(any()) } just Runs
      every { cas1ApplicationDomainEventService.applicationWithdrawn(any(), any()) } just Runs
      every { cas1ApplicationEmailService.applicationWithdrawn(any(), any()) } just Runs

      val result = service.withdrawApprovedPremisesApplication(application.id, user, "other", "Some other reason")

      assertThat(result is CasResult.Success).isTrue

      verify {
        approvedPremisesApplicationRepository.save(
          match {
            it.id == application.id &&
              it is ApprovedPremisesApplicationEntity &&
              it.isWithdrawn &&
              it.withdrawalReason == "other" &&
              it.otherWithdrawalReason == "Some other reason"
          },
        )
      }

      verify { cas1ApplicationStatusService.applicationWithdrawn(application) }
      verify { cas1ApplicationDomainEventService.applicationWithdrawn(application, user) }
      verify { cas1ApplicationEmailService.applicationWithdrawn(application, user) }
    }

    @Test
    fun `withdrawApprovedPremisesApplication does not persist otherWithdrawalReason if withdrawlReason is not other`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      every { approvedPremisesApplicationRepository.findByIdOrNull(application.id) } returns application
      every { cas1UserAccessService.userMayWithdrawApplication(user, application) } returns true
      every { approvedPremisesApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesApplicationEntity }
      every { cas1ApplicationStatusService.applicationWithdrawn(any()) } just Runs
      every { cas1ApplicationEmailService.applicationWithdrawn(any(), any()) } returns Unit
      every { cas1ApplicationDomainEventService.applicationWithdrawn(any(), any()) } just Runs

      service.withdrawApprovedPremisesApplication(
        application.id,
        user,
        "alternative_identified_placement_no_longer_required",
        "Some other reason",
      )

      verify {
        approvedPremisesApplicationRepository.save(
          match {
            it.id == application.id &&
              it is ApprovedPremisesApplicationEntity &&
              it.isWithdrawn &&
              it.withdrawalReason == "alternative_identified_placement_no_longer_required" &&
              it.otherWithdrawalReason == null
          },
        )
      }

      verify { cas1ApplicationStatusService.applicationWithdrawn(application) }
      verify { cas1ApplicationDomainEventService.applicationWithdrawn(application, user) }
      verify { cas1ApplicationEmailService.applicationWithdrawn(application, user) }
    }
  }

  @Nested
  inner class ExpireApprovedPremisesApplication {

    @Test
    fun `expireApprovedPremisesApplication returns NotFound if Application does not exist`() {
      val applicationId = UUID.fromString("bb13d346-f278-43d7-9c23-5c4077c031ca")

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      every { approvedPremisesApplicationRepository.findByIdOrNull(applicationId) } returns null

      val result = service.expireApprovedPremisesApplication(
        applicationId,
        user,
        "Expired reason",
      )

      assertThat(result is CasResult.NotFound).isTrue
    }

    @Test
    fun `expireApprovedPremisesApplication is idempotent and returns success if already expired`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withStatus(ApprovedPremisesApplicationStatus.EXPIRED)
        .produce()

      every { approvedPremisesApplicationRepository.findByIdOrNull(application.id) } returns application

      val result = service.expireApprovedPremisesApplication(
        application.id,
        user,
        "Expire reason.",
      )

      assertThat(result is CasResult.Success).isTrue
    }

    @Test
    fun `expireApprovedPremisesApplication returns Success and sets status to EXPIRED and saves reason and triggers domain event`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      application.assessments.add(assessment)

      every { approvedPremisesApplicationRepository.findByIdOrNull(application.id) } returns application
      every { approvedPremisesApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesApplicationEntity }
      every { cas1ApplicationDomainEventService.applicationExpiredManually(any(), user, "Expired reason.") } just Runs

      val result = service.expireApprovedPremisesApplication(
        application.id,
        user,
        "Expired reason.",
      )

      assertThat(result is CasResult.Success).isTrue

      verify {
        approvedPremisesApplicationRepository.save(
          match {
            it.id == application.id &&
              it is ApprovedPremisesApplicationEntity &&
              it.status == ApprovedPremisesApplicationStatus.EXPIRED &&
              it.expiredReason == "Expired reason."
          },
        )
      }

      verify { cas1ApplicationDomainEventService.applicationExpiredManually(application, user, "Expired reason.") }
    }
  }

  @Nested
  inner class GetWithdrawableState {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    @Test
    fun `getWithdrawableState withdrawable if application not withdrawn`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withIsWithdrawn(false)
        .produce()

      every { cas1UserAccessService.userMayWithdrawApplication(user, application) } returns true

      val result = service.getWithdrawableState(application, user)

      assertThat(result.withdrawn).isFalse()
      assertThat(result.withdrawable).isTrue()
    }

    @Test
    fun `getWithdrawableState not withdrawable if application already withdrawn `() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withIsWithdrawn(true)
        .produce()

      every { cas1UserAccessService.userMayWithdrawApplication(user, application) } returns true

      val result = service.getWithdrawableState(application, user)

      assertThat(result.withdrawn).isTrue()
      assertThat(result.withdrawable).isFalse()
    }

    @ParameterizedTest
    @CsvSource("true", "false")
    fun `getWithdrawableState userMayDirectlyWithdraw delegates to user access service`(canWithdraw: Boolean) {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withIsWithdrawn(false)
        .produce()

      every { cas1UserAccessService.userMayWithdrawApplication(user, application) } returns canWithdraw

      val result = service.getWithdrawableState(application, user)

      assertThat(result.userMayDirectlyWithdraw).isEqualTo(canWithdraw)
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

    @JvmStatic
    fun providePlacementApplications(): Stream<Arguments> {
      // REQUEST UNSUBMITTED
      val requestUnsubmittedDateApplied = LocalDate.now()
      val requestUnsubmittedApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(CRN)
        .withStatus(ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST)
        .produce()
      val requestUnsubmittedPlacementApplication = PlacementApplicationEntityFactory()
        .withCreatedByUser(user)
        .withApplication(requestUnsubmittedApplication)
        .withCreatedAt(requestUnsubmittedDateApplied.toLocalDateTime())
        .produce()

      // REQUEST REJECTED
      val requestRejectedDateApplied = LocalDate.now()
      val requestRejectedApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(CRN)
        .withStatus(ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST)
        .produce()
      val requestRejectedPlacementApplication = PlacementApplicationEntityFactory()
        .withCreatedByUser(user)
        .withApplication(requestRejectedApplication)
        .withSubmittedAt(requestRejectedDateApplied.toLocalDateTime())
        .withDecision(PlacementApplicationDecision.REJECTED)
        .produce()

      // REQUEST SUBMITTED
      val requestSubmittedDateApplied = LocalDate.now()
      val requestSubmittedApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(CRN)
        .withStatus(ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST)
        .produce()
      val requestSubmittedPlacementApplication = PlacementApplicationEntityFactory()
        .withCreatedByUser(user)
        .withApplication(requestSubmittedApplication)
        .withSubmittedAt(requestSubmittedDateApplied.toLocalDateTime())
        .produce()

      // AWAITING MATCH
      val awaitingMatchDateApplied = LocalDate.now()
      val awaitingMatchApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(CRN)
        .withStatus(ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST)
        .produce()
      val awaitingMatchPlacementApplication = PlacementApplicationEntityFactory()
        .withCreatedByUser(user)
        .withApplication(awaitingMatchApplication)
        .withSubmittedAt(awaitingMatchDateApplied.toLocalDateTime())
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .produce()

      // REQUEST WITHDRAWN
      val requestWithdrawnDateApplied = LocalDate.now()
      val requestWithdrawnApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(CRN)
        .withStatus(ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST)
        .produce()
      val requestWithdrawnPlacementApplication = PlacementApplicationEntityFactory()
        .withCreatedByUser(user)
        .withApplication(requestWithdrawnApplication)
        .withSubmittedAt(requestWithdrawnDateApplied.toLocalDateTime())
        .withIsWithdrawn(true)
        .produce()

      return Stream.of(
        Arguments.of(
          requestUnsubmittedApplication,
          listOf(requestUnsubmittedPlacementApplication),
          RequestForPlacementStatus.requestUnsubmitted,
          listOf(
            Cas1PlacementHistory(
              dateApplied = requestUnsubmittedDateApplied,
              requestForPlacementStatus = RequestForPlacementStatus.requestUnsubmitted,
              placementStatus = null,
            ),
          ),
        ),
        Arguments.of(
          requestRejectedApplication,
          listOf(requestRejectedPlacementApplication),
          RequestForPlacementStatus.requestRejected,
          listOf(
            Cas1PlacementHistory(
              dateApplied = requestRejectedDateApplied,
              requestForPlacementStatus = RequestForPlacementStatus.requestRejected,
              placementStatus = null,
            ),
          ),
        ),
        Arguments.of(
          requestSubmittedApplication,
          listOf(requestSubmittedPlacementApplication),
          RequestForPlacementStatus.requestSubmitted,
          listOf(
            Cas1PlacementHistory(
              dateApplied = requestSubmittedDateApplied,
              requestForPlacementStatus = RequestForPlacementStatus.requestSubmitted,
              placementStatus = null,
            ),
          ),
        ),
        Arguments.of(
          awaitingMatchApplication,
          listOf(awaitingMatchPlacementApplication),
          RequestForPlacementStatus.awaitingMatch,
          listOf(
            Cas1PlacementHistory(
              dateApplied = awaitingMatchDateApplied,
              requestForPlacementStatus = RequestForPlacementStatus.awaitingMatch,
              placementStatus = null,
            ),
          ),
        ),
        Arguments.of(
          requestWithdrawnApplication,
          listOf(requestWithdrawnPlacementApplication),
          RequestForPlacementStatus.requestWithdrawn,
          listOf(
            Cas1PlacementHistory(
              dateApplied = requestWithdrawnDateApplied,
              requestForPlacementStatus = RequestForPlacementStatus.requestWithdrawn,
              placementStatus = null,
            ),
          ),
        ),
      )
    }

    @JvmStatic
    fun providePlacementRequests(): Stream<Arguments> {
      // AWAITING MATCH
      val awaitingMatchDateApplied = LocalDate.now()
      val awaitingMatchApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(CRN)
        .withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
        .produce()
      val awaitingMatchPlacementApplication = PlacementApplicationEntityFactory()
        .withCreatedByUser(user)
        .withApplication(awaitingMatchApplication)
        .withSubmittedAt(awaitingMatchDateApplied.toLocalDateTime())
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withPlacementRequest(
          PlacementRequestEntityFactory()
            .withDefaults()
            .withIsWithdrawn(false)
            .withApplication(awaitingMatchApplication)
            .produce(),
        )
        .produce()
      awaitingMatchApplication.placementRequests.add(awaitingMatchPlacementApplication.placementRequest!!)

      // REQUEST WITHDRAWN
      val requestWithdrawnDateApplied = LocalDate.now()
      val requestWithdrawnApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(CRN)
        .withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
        .produce()
      val requestWithdrawnPlacementApplication = PlacementApplicationEntityFactory()
        .withCreatedByUser(user)
        .withApplication(requestWithdrawnApplication)
        .withSubmittedAt(requestWithdrawnDateApplied.toLocalDateTime())
        .withPlacementRequest(
          PlacementRequestEntityFactory()
            .withDefaults()
            .withIsWithdrawn(true)
            .withApplication(requestWithdrawnApplication)
            .produce(),
        )
        .produce()
      requestWithdrawnApplication.placementRequests.add(requestWithdrawnPlacementApplication.placementRequest!!)

      return Stream.of(
        Arguments.of(
          awaitingMatchApplication,
          listOf(awaitingMatchPlacementApplication),
          RequestForPlacementStatus.awaitingMatch,
          listOf(
            Cas1PlacementHistory(
              dateApplied = awaitingMatchDateApplied,
              requestForPlacementStatus = RequestForPlacementStatus.awaitingMatch,
              placementStatus = null,
            ),
          ),
        ),
        Arguments.of(
          requestWithdrawnApplication,
          listOf(requestWithdrawnPlacementApplication),
          RequestForPlacementStatus.requestWithdrawn,
          listOf(
            Cas1PlacementHistory(
              dateApplied = requestWithdrawnDateApplied,
              requestForPlacementStatus = RequestForPlacementStatus.requestWithdrawn,
              placementStatus = null,
            ),
          ),
        ),
      )
    }

    @JvmStatic
    fun providePlacements(): Stream<Arguments> {
      val upcomingDateApplied = LocalDate.now()
      val upcomingApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(CRN)
        .withStatus(ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED)
        .produce()
      val upcomingPlacementApplication = PlacementApplicationEntityFactory()
        .withCreatedByUser(user)
        .withApplication(upcomingApplication)
        .withSubmittedAt(upcomingDateApplied.toLocalDateTime())
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withPlacementRequest(
          PlacementRequestEntityFactory()
            .withDefaults()
            .withIsWithdrawn(false)
            .withApplication(upcomingApplication)
            .produce(),
        )
        .produce()
      val upcomingBooking = Cas1SpaceBookingEntityFactory()
        .withCancellationOccurredAt(null)
        .withActualDepartureDate(null)
        .withActualArrivalDate(null)
        .withNonArrivalConfirmedAt(null)
        .withCreatedAt(OffsetDateTime.now())
        .withApplication(upcomingApplication)
        .withPlacementRequest(upcomingPlacementApplication.placementRequest!!)
        .produce()
      upcomingPlacementApplication.placementRequest!!.spaceBookings.add(upcomingBooking)
      upcomingApplication.placementRequests.add(upcomingPlacementApplication.placementRequest!!)

      val arrivedDateApplied = LocalDate.now()
      val arrivedApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(CRN)
        .withStatus(ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED)
        .produce()
      val arrivedPlacementApplication = PlacementApplicationEntityFactory()
        .withCreatedByUser(user)
        .withApplication(arrivedApplication)
        .withSubmittedAt(arrivedDateApplied.toLocalDateTime())
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withPlacementRequest(
          PlacementRequestEntityFactory()
            .withDefaults()
            .withIsWithdrawn(false)
            .withApplication(arrivedApplication)
            .produce(),
        )
        .produce()
      val arrivedBooking = Cas1SpaceBookingEntityFactory()
        .withCancellationOccurredAt(null)
        .withActualDepartureDate(null)
        .withActualArrivalDate(arrivedDateApplied)
        .withNonArrivalConfirmedAt(null)
        .withCreatedAt(OffsetDateTime.now())
        .withApplication(arrivedApplication)
        .withPlacementRequest(arrivedPlacementApplication.placementRequest!!)
        .produce()
      arrivedPlacementApplication.placementRequest!!.spaceBookings.add(arrivedBooking)
      arrivedApplication.placementRequests.add(arrivedPlacementApplication.placementRequest!!)

      val departedDateApplied = LocalDate.now()
      val departedApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(CRN)
        .withStatus(ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED)
        .produce()
      val departedPlacementApplication = PlacementApplicationEntityFactory()
        .withCreatedByUser(user)
        .withApplication(departedApplication)
        .withSubmittedAt(departedDateApplied.toLocalDateTime())
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withPlacementRequest(
          PlacementRequestEntityFactory()
            .withDefaults()
            .withIsWithdrawn(false)
            .withApplication(departedApplication)
            .produce(),
        )
        .produce()
      val departedBooking = Cas1SpaceBookingEntityFactory()
        .withCancellationOccurredAt(null)
        .withActualDepartureDate(departedDateApplied)
        .withActualArrivalDate(LocalDate.now().minusDays(1))
        .withNonArrivalConfirmedAt(null)
        .withCreatedAt(OffsetDateTime.now())
        .withApplication(departedApplication)
        .withPlacementRequest(departedPlacementApplication.placementRequest!!)
        .produce()
      departedPlacementApplication.placementRequest!!.spaceBookings.add(departedBooking)
      departedApplication.placementRequests.add(departedPlacementApplication.placementRequest!!)

      val notArrivedDateApplied = LocalDate.now()
      val notArrivedApplication = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(CRN)
        .withStatus(ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED)
        .produce()
      val notArrivedPlacementApplication = PlacementApplicationEntityFactory()
        .withCreatedByUser(user)
        .withApplication(notArrivedApplication)
        .withSubmittedAt(notArrivedDateApplied.toLocalDateTime())
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withPlacementRequest(
          PlacementRequestEntityFactory()
            .withDefaults()
            .withIsWithdrawn(false)
            .withApplication(notArrivedApplication)
            .produce(),
        )
        .produce()
      val notArrivedBooking = Cas1SpaceBookingEntityFactory()
        .withCancellationOccurredAt(null)
        .withActualDepartureDate(null)
        .withActualArrivalDate(null)
        .withNonArrivalConfirmedAt(notArrivedDateApplied.toLocalDateTime().toInstant())
        .withCreatedAt(OffsetDateTime.now())
        .withApplication(notArrivedApplication)
        .withPlacementRequest(notArrivedPlacementApplication.placementRequest!!)
        .produce()
      notArrivedPlacementApplication.placementRequest!!.spaceBookings.add(notArrivedBooking)
      notArrivedApplication.placementRequests.add(notArrivedPlacementApplication.placementRequest!!)

      return Stream.of(
        Arguments.of(
          upcomingApplication,
          listOf(upcomingPlacementApplication),
          RequestForPlacementStatus.placementBooked,
          Cas1SpaceBookingStatus.UPCOMING,
          listOf(
            Cas1PlacementHistory(
              dateApplied = upcomingDateApplied,
              requestForPlacementStatus = RequestForPlacementStatus.placementBooked,
              placementStatus = Cas1SpaceBookingStatus.UPCOMING,
            ),
          ),
        ),
        Arguments.of(
          arrivedApplication,
          listOf(arrivedPlacementApplication),
          RequestForPlacementStatus.placementBooked,
          Cas1SpaceBookingStatus.ARRIVED,
          listOf(
            Cas1PlacementHistory(
              dateApplied = arrivedDateApplied,
              requestForPlacementStatus = RequestForPlacementStatus.placementBooked,
              placementStatus = Cas1SpaceBookingStatus.ARRIVED,
            ),
          ),
        ),
        Arguments.of(
          departedApplication,
          listOf(departedPlacementApplication),
          RequestForPlacementStatus.placementBooked,
          Cas1SpaceBookingStatus.DEPARTED,
          listOf(
            Cas1PlacementHistory(
              dateApplied = departedDateApplied,
              requestForPlacementStatus = RequestForPlacementStatus.placementBooked,
              placementStatus = Cas1SpaceBookingStatus.DEPARTED,
            ),
          ),
        ),
        Arguments.of(
          notArrivedApplication,
          listOf(notArrivedPlacementApplication),
          RequestForPlacementStatus.placementBooked,
          Cas1SpaceBookingStatus.NOT_ARRIVED,
          listOf(
            Cas1PlacementHistory(
              dateApplied = notArrivedDateApplied,
              requestForPlacementStatus = RequestForPlacementStatus.placementBooked,
              placementStatus = Cas1SpaceBookingStatus.NOT_ARRIVED,
            ),
          ),
        ),
      )
    }
  }
}
