package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredBySeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredByUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PaginationConfig
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

class Cas1PlacementRequestServiceTest {
  private val placementRequestRepository = mockk<PlacementRequestRepository>()
  private val bookingNotMadeRepository = mockk<BookingNotMadeRepository>()
  private val placementRequirementsRepository = mockk<PlacementRequirementsRepository>()
  private val cancellationRepository = mockk<CancellationRepository>()
  private val userAccessService = mockk<Cas1UserAccessService>()
  private val applicationService = mockk<ApplicationService>()
  private val cas1PlacementRequestEmailService = mockk<Cas1PlacementRequestEmailService>()
  private val cas1PlacementRequestDomainEventService = mockk<Cas1PlacementRequestDomainEventService>()
  private val cas1BookingDomainEventService = mockk<Cas1BookingDomainEventService>()
  private val offenderService = mockk<OffenderService>()

  private val placementRequestService = Cas1PlacementRequestService(
    placementRequestRepository,
    bookingNotMadeRepository,
    placementRequirementsRepository,
    cancellationRepository,
    userAccessService,
    applicationService,
    cas1PlacementRequestEmailService,
    cas1PlacementRequestDomainEventService,
    cas1BookingDomainEventService,
    offenderService,
    clock = Clock.systemDefaultZone(),
  )

  private val assigneeUser = UserEntityFactory()
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
    }
    .produce()

  @BeforeEach
  fun before() {
    PaginationConfig(defaultPageSize = 10).postInit()
  }

  @Nested
  inner class CreatePlacementRequest {

    @Test
    fun `createPlacementRequest creates a placement request with the correct deadline`() {
      every { placementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(assigneeUser)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(assigneeUser)
        .produce()

      val placementRequirements = PlacementRequirementsEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .produce()

      val placementDates = PlacementDates(
        expectedArrival = LocalDate.now(),
        duration = 12,
      )

      val placementRequest = placementRequestService.createPlacementRequest(
        placementRequirements,
        placementDates,
        "Some notes",
        false,
        null,
      )

      assertThat(placementRequest.duration).isEqualTo(placementDates.duration)
      assertThat(placementRequest.expectedArrival).isEqualTo(placementDates.expectedArrival)
      assertThat(placementRequest.placementRequirements).isEqualTo(placementRequirements)
      assertThat(placementRequest.assessment.id).isEqualTo(assessment.id)
      assertThat(placementRequest.application.id).isEqualTo(application.id)
      assertThat(placementRequest.isParole).isFalse()
    }
  }

  @Nested
  inner class GetPlacementRequestForUser {

    @Test
    fun `returns NotFound when PlacementRequest doesn't exist`() {
      val placementRequestId = UUID.fromString("72f15a57-8f3a-48bc-abc7-be09fe548fea")

      val requestingUser = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      every { placementRequestRepository.findByIdOrNull(placementRequestId) } returns null

      val result = placementRequestService.getPlacementRequestForUser(requestingUser, placementRequestId)

      assertThat(result is CasResult.NotFound).isTrue()
    }

    @Test
    fun `returns Success when user can access offender`() {
      val requestingUser = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val otherUser = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(assigneeUser)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(assigneeUser)
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withApplication(application)
        .withAssessment(assessment)
        .produce()

      val mockCancellations = mockk<List<CancellationEntity>>()

      every {
        offenderService.canAccessOffender(application.crn, requestingUser.cas1LaoStrategy())
      } returns true
      every { placementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { cancellationRepository.getCancellationsForApplicationId(application.id) } returns mockCancellations

      val result = placementRequestService.getPlacementRequestForUser(requestingUser, placementRequest.id)

      assertThat(result is CasResult.Success).isTrue()

      val (expectedPlacementRequest, expectedCancellations) = (result as CasResult.Success).value

      assertThat(expectedPlacementRequest).isEqualTo(placementRequest)
      assertThat(expectedCancellations).isEqualTo(mockCancellations)
    }
  }

  @Nested
  inner class CreateBookingNotMade {

    @Test
    fun `createBookingNotMade returns Not Found when Placement Request doesn't exist`() {
      val requestingUser = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val placementRequestId = UUID.fromString("25dd65b1-38b5-47bc-a00b-f2df228ed06b")

      every { placementRequestRepository.findByIdOrNull(placementRequestId) } returns null

      val result = placementRequestService.createBookingNotMade(requestingUser, placementRequestId, null)
      assertThatCasResult(result).isNotFound("PlacementRequest", "25dd65b1-38b5-47bc-a00b-f2df228ed06b")
    }

    @Test
    fun `createBookingNotMade returns Success, saves Booking Not Made and saves domain event`() {
      val requestingUser = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val otherUser = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(otherUser)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(otherUser)
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withApplication(application)
        .withAssessment(assessment)
        .produce()

      every { cas1BookingDomainEventService.bookingNotMade(any(), any(), any(), any()) } just Runs

      every { placementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { bookingNotMadeRepository.save(any()) } answers { it.invocation.args[0] as BookingNotMadeEntity }

      val result = placementRequestService.createBookingNotMade(requestingUser, placementRequest.id, "some notes")
      assertThatCasResult(result).isSuccess().with { bookingNotMade ->
        assertThat(bookingNotMade.placementRequest).isEqualTo(placementRequest)
        assertThat(bookingNotMade.notes).isEqualTo("some notes")

        verify(exactly = 1) { bookingNotMadeRepository.save(match { it.notes == "some notes" && it.placementRequest == placementRequest }) }

        verify(exactly = 1) {
          cas1BookingDomainEventService.bookingNotMade(
            user = requestingUser,
            placementRequest = placementRequest,
            bookingNotCreatedAt = any(),
            notes = "some notes",
          )
        }
      }
    }
  }

  @Nested
  inner class GetWithdrawableState {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(UserEntityFactory().withUnitTestControlProbationRegion().produce())
      .produce()

    @Test
    fun `getWithdrawableState not withdrawable if already withdrawn`() {
      val placementRequest = createValidPlacementRequest(application, user)
      placementRequest.isWithdrawn = true

      every { userAccessService.userMayWithdrawPlacementRequest(user, placementRequest) } returns true

      val result = placementRequestService.getWithdrawableState(placementRequest, user)

      assertThat(result.withdrawn).isTrue()
      assertThat(result.withdrawable).isFalse()
    }

    @Test
    fun `getWithdrawableState withdrawable if not already withdrawn and not reallocated`() {
      val placementRequest = createValidPlacementRequest(application, user)
      placementRequest.isWithdrawn = false

      every { userAccessService.userMayWithdrawPlacementRequest(user, placementRequest) } returns true

      val result = placementRequestService.getWithdrawableState(placementRequest, user)

      assertThat(result.withdrawn).isFalse()
      assertThat(result.withdrawable).isTrue()
    }

    @ParameterizedTest
    @CsvSource("true", "false")
    fun `getWithdrawableState userMayDirectlyWithdraw delegates to user access service`(canWithdraw: Boolean) {
      val placementRequest = createValidPlacementRequest(application, user)

      every { userAccessService.userMayWithdrawPlacementRequest(user, placementRequest) } returns canWithdraw

      val result = placementRequestService.getWithdrawableState(placementRequest, user)

      assertThat(result.userMayDirectlyWithdraw).isEqualTo(canWithdraw)
    }

    @Test
    fun `getWithdrawableState userMayDirectlyWithdraw returns false if not for original app dates`() {
      val placementRequest = createValidPlacementRequest(
        application,
        user,
        placementApplication = PlacementApplicationEntityFactory()
          .withCreatedByUser(user)
          .withApplication(application)
          .produce(),
      )

      every { userAccessService.userMayWithdrawPlacementRequest(user, placementRequest) } returns true

      val result = placementRequestService.getWithdrawableState(placementRequest, user)

      assertThat(result.userMayDirectlyWithdraw).isFalse()
    }
  }

  @Nested
  inner class WithdrawPlacementRequest {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .produce()

    val placementRequest = createValidPlacementRequest(application, user)
    val placementRequestId = placementRequest.id

    @Test
    fun `withdrawPlacementRequest returns Not Found if no Placement Request with ID exists`() {
      every { placementRequestRepository.findByIdForUpdate(placementRequestId) } returns null

      val result = placementRequestService.withdrawPlacementRequest(
        placementRequestId,
        PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST,
        WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.PlacementRequest,
          placementRequestId,
        ),
      )

      assertThat(result is CasResult.NotFound).isTrue
    }

    @ParameterizedTest
    @EnumSource(PlacementRequestWithdrawalReason::class)
    @NullSource
    fun `withdrawPlacementRequest returns Success and saves withdrawn PlacementRequest, triggering emails and domain events and cascades`(
      reason: PlacementRequestWithdrawalReason?,
    ) {
      every { userAccessService.userMayWithdrawPlacementRequest(user, placementRequest) } returns true
      every { placementRequestRepository.findByIdForUpdate(placementRequestId) } returns placementRequest
      every { placementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }
      every { placementRequestRepository.findByApplication(application) } returns listOf(placementRequest)
      every { cas1PlacementRequestEmailService.placementRequestWithdrawn(any(), any()) } returns Unit
      every { cas1PlacementRequestDomainEventService.placementRequestWithdrawn(any(), any()) } returns Unit
      every {
        applicationService.updateApprovedPremisesApplicationStatus(application.id, PENDING_PLACEMENT_REQUEST)
      } returns Unit
      every { cancellationRepository.getCancellationsForApplicationId(any()) } returns emptyList()

      val withdrawalContext = WithdrawalContext(
        WithdrawalTriggeredByUser(user),
        WithdrawableEntityType.PlacementRequest,
        placementRequestId,
      )

      val result = placementRequestService.withdrawPlacementRequest(
        placementRequestId,
        reason,
        withdrawalContext,
      )

      assertThat(result is CasResult.Success).isTrue

      verify {
        placementRequestRepository.save(
          match {
            it.id == placementRequestId &&
              it.isWithdrawn &&
              it.withdrawalReason == reason
          },
        )
      }

      verify { cas1PlacementRequestEmailService.placementRequestWithdrawn(placementRequest, WithdrawalTriggeredByUser(user)) }
      verify { cas1PlacementRequestDomainEventService.placementRequestWithdrawn(placementRequest, withdrawalContext) }
    }

    @Test
    fun `withdrawPlacementRequest updates application status to 'PENDING_PLACEMENT_REQUEST' if no other non-active placement requests`() {
      every { userAccessService.userMayWithdrawPlacementRequest(user, placementRequest) } returns true
      every { placementRequestRepository.findByIdForUpdate(placementRequestId) } returns placementRequest
      every { placementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }
      every { cas1PlacementRequestEmailService.placementRequestWithdrawn(any(), any()) } returns Unit
      every { cas1PlacementRequestDomainEventService.placementRequestWithdrawn(any(), any()) } returns Unit
      every { cancellationRepository.getCancellationsForApplicationId(any()) } returns emptyList()

      val withdrawnPlacementRequest = createValidPlacementRequest(application, user)
      withdrawnPlacementRequest.isWithdrawn = true

      every {
        placementRequestRepository.findByApplication(application)
      } returns listOf(
        withdrawnPlacementRequest,
      )
      every {
        applicationService.updateApprovedPremisesApplicationStatus(application.id, PENDING_PLACEMENT_REQUEST)
      } returns Unit

      val result = placementRequestService.withdrawPlacementRequest(
        placementRequestId,
        PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST,
        WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.PlacementRequest,
          placementRequestId,
        ),
      )

      assertThat(result is CasResult.Success).isTrue

      verify {
        applicationService.updateApprovedPremisesApplicationStatus(
          application.id,
          PENDING_PLACEMENT_REQUEST,
        )
      }
    }

    @Test
    fun `withdrawPlacementRequest does not update application status to 'PENDING_PLACEMENT_REQUEST' if there are other active placement requests`() {
      every { userAccessService.userMayWithdrawPlacementRequest(user, placementRequest) } returns true
      every { placementRequestRepository.findByIdForUpdate(placementRequestId) } returns placementRequest
      every { placementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }
      every { cas1PlacementRequestEmailService.placementRequestWithdrawn(any(), any()) } returns Unit
      every { cas1PlacementRequestDomainEventService.placementRequestWithdrawn(any(), any()) } returns Unit
      every { cancellationRepository.getCancellationsForApplicationId(any()) } returns emptyList()

      val withdrawnPlacementRequest = createValidPlacementRequest(application, user)
      withdrawnPlacementRequest.isWithdrawn = true

      val activePlacementRequest = createValidPlacementRequest(application, user)

      every {
        placementRequestRepository.findByApplication(application)
      } returns listOf(
        withdrawnPlacementRequest,
        activePlacementRequest,
      )

      val result = placementRequestService.withdrawPlacementRequest(
        placementRequestId,
        PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST,
        WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.PlacementRequest,
          placementRequestId,
        ),
      )

      assertThat(result is CasResult.Success).isTrue

      verify { applicationService wasNot Called }
    }

    @Test
    fun `withdrawPlacementRequest doesnt updates application status if user didn't trigger withdrawal`() {
      every { userAccessService.userMayWithdrawPlacementRequest(user, placementRequest) } returns true
      every { placementRequestRepository.findByIdForUpdate(placementRequestId) } returns placementRequest
      every { placementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }
      every { cas1PlacementRequestEmailService.placementRequestWithdrawn(any(), any()) } returns Unit
      every { cas1PlacementRequestDomainEventService.placementRequestWithdrawn(any(), any()) } returns Unit
      every { cancellationRepository.getCancellationsForApplicationId(any()) } returns emptyList()

      val withdrawnPlacementRequest = createValidPlacementRequest(application, user)
      withdrawnPlacementRequest.isWithdrawn = true

      every {
        placementRequestRepository.findByApplication(application)
      } returns listOf(
        withdrawnPlacementRequest,
      )

      val result = placementRequestService.withdrawPlacementRequest(
        placementRequestId,
        PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST,
        WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.Application,
          placementRequestId,
        ),
      )

      assertThat(result is CasResult.Success).isTrue

      verify { applicationService wasNot Called }
    }

    @Test
    fun `withdrawPlacementRequest doesnt updates application status if triggered by seed job`() {
      every { userAccessService.userMayWithdrawPlacementRequest(user, placementRequest) } returns true
      every { placementRequestRepository.findByIdForUpdate(placementRequestId) } returns placementRequest
      every { placementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }
      every { cas1PlacementRequestEmailService.placementRequestWithdrawn(any(), any()) } returns Unit
      every { cas1PlacementRequestDomainEventService.placementRequestWithdrawn(any(), any()) } returns Unit
      every { cancellationRepository.getCancellationsForApplicationId(any()) } returns emptyList()

      val withdrawnPlacementRequest = createValidPlacementRequest(application, user)
      withdrawnPlacementRequest.isWithdrawn = true

      every {
        placementRequestRepository.findByApplication(application)
      } returns listOf(
        withdrawnPlacementRequest,
      )

      val result = placementRequestService.withdrawPlacementRequest(
        placementRequestId,
        PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST,
        WithdrawalContext(
          WithdrawalTriggeredBySeedJob,
          WithdrawableEntityType.PlacementRequest,
          placementRequestId,
        ),
      )

      assertThat(result is CasResult.Success).isTrue

      verify { applicationService wasNot Called }
    }

    @ParameterizedTest
    @EnumSource(value = WithdrawableEntityType::class, names = ["PlacementRequest", "SpaceBooking"], mode = EnumSource.Mode.EXCLUDE)
    fun `withdrawPlacementRequest sets correct reason if withdrawal triggered by other entity and user permissions not checked`(triggeringEntity: WithdrawableEntityType) {
      every { placementRequestRepository.findByIdForUpdate(placementRequestId) } returns placementRequest
      every { placementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }
      every { cas1PlacementRequestEmailService.placementRequestWithdrawn(any(), any()) } returns Unit
      every { cas1PlacementRequestDomainEventService.placementRequestWithdrawn(any(), any()) } returns Unit
      every { cancellationRepository.getCancellationsForApplicationId(any()) } returns emptyList()

      val providedReason = PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST
      val result = placementRequestService.withdrawPlacementRequest(
        placementRequestId,
        providedReason,
        WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          triggeringEntity,
          placementRequestId,
        ),
      )

      assertThat(result is CasResult.Success).isTrue

      val expectedWithdrawalReason = when (triggeringEntity) {
        WithdrawableEntityType.Application -> PlacementRequestWithdrawalReason.RELATED_APPLICATION_WITHDRAWN
        WithdrawableEntityType.PlacementApplication -> PlacementRequestWithdrawalReason.RELATED_PLACEMENT_APPLICATION_WITHDRAWN
        WithdrawableEntityType.PlacementRequest -> providedReason
        WithdrawableEntityType.SpaceBooking -> providedReason
      }

      verify { userAccessService wasNot Called }

      verify {
        placementRequestRepository.save(
          match {
            it.id == placementRequestId &&
              it.isWithdrawn &&
              it.withdrawalReason == expectedWithdrawalReason
          },
        )
      }
    }

    @Test
    fun `withdrawPlacementRequest is idempotent if placement request already withdrawn`() {
      placementRequest.isWithdrawn = true

      every { placementRequestRepository.findByIdForUpdate(placementRequestId) } returns placementRequest
      every { placementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }
      every { cancellationRepository.getCancellationsForApplicationId(any()) } returns emptyList()

      val result = placementRequestService.withdrawPlacementRequest(
        placementRequestId,
        PlacementRequestWithdrawalReason.WITHDRAWN_BY_PP,
        WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.PlacementRequest,
          placementRequestId,
        ),
      )

      assertThat(result is CasResult.Success).isTrue

      verify(exactly = 0) { placementRequestRepository.save(any()) }
    }

    @Test
    fun `withdrawPlacementRequest success if user directly requested withdrawal`() {
      every { userAccessService.userMayWithdrawPlacementRequest(user, placementRequest) } returns true
      every { placementRequestRepository.findByIdForUpdate(placementRequestId) } returns placementRequest
      every { placementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }
      every { placementRequestRepository.findByApplication(application) } returns listOf(placementRequest)
      every {
        applicationService.updateApprovedPremisesApplicationStatus(application.id, PENDING_PLACEMENT_REQUEST)
      } returns Unit
      every { cas1PlacementRequestEmailService.placementRequestWithdrawn(any(), any()) } returns Unit
      every { cas1PlacementRequestDomainEventService.placementRequestWithdrawn(any(), any()) } returns Unit
      every { cancellationRepository.getCancellationsForApplicationId(any()) } returns emptyList()

      val result = placementRequestService.withdrawPlacementRequest(
        placementRequestId,
        PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST,
        WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.PlacementRequest,
          placementRequestId,
        ),
      )

      assertThat(result is CasResult.Success).isTrue

      verify {
        placementRequestRepository.save(
          match {
            it.id == placementRequestId &&
              it.isWithdrawn &&
              it.withdrawalReason == PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST
          },
        )
      }
    }
  }

  @Nested
  inner class GetPlacementRequest {

    @Test
    fun `returns NotFound when PlacementRequest doesn't exist`() {
      val placementRequestId = UUID.fromString("72f15a57-8f3a-48bc-abc7-be09fe548fea")

      val requestingUser = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      every { placementRequestRepository.findByIdOrNull(placementRequestId) } returns null

      val result = placementRequestService.getPlacementRequest(requestingUser, placementRequestId)

      assertThat(result is CasResult.NotFound).isTrue()
    }

    @Test
    fun `returns Success when user can access offender`() {
      val requestingUser = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val otherUser = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(assigneeUser)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(assigneeUser)
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withApplication(application)
        .withAssessment(assessment)
        .produce()

      every {
        offenderService.canAccessOffender(application.crn, requestingUser.cas1LaoStrategy())
      } returns true
      every { placementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest

      val result = placementRequestService.getPlacementRequest(requestingUser, placementRequest.id)

      assertThat(result is CasResult.Success).isTrue()

      val expectedPlacementRequest = (result as CasResult.Success).value

      assertThat(expectedPlacementRequest).isEqualTo(placementRequest)
    }
  }

  private fun createValidPlacementRequest(
    application: ApprovedPremisesApplicationEntity,
    user: UserEntity,
    placementApplication: PlacementApplicationEntity? = null,
  ): PlacementRequestEntity {
    val placementRequestId = UUID.fromString("49f3eef9-4770-4f00-8f31-8e6f4cb4fd9e")

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(user)
      .produce()

    val placementRequest = PlacementRequestEntityFactory()
      .withId(placementRequestId)
      .withPlacementRequirements(
        PlacementRequirementsEntityFactory()
          .withApplication(application)
          .withAssessment(assessment)
          .produce(),
      )
      .withApplication(application)
      .withAssessment(assessment)
      .withPlacementApplication(placementApplication)
      .produce()

    return placementRequest
  }
}
