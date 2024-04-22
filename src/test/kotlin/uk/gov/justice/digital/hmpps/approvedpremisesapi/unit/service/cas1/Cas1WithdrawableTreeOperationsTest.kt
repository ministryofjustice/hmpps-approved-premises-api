package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableTreeOperations
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableTreeNode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredByUser
import java.util.UUID

class Cas1WithdrawableTreeOperationsTest {
  private val mockPlacementRequestService = mockk<PlacementRequestService>()
  private val mockBookingService = mockk<BookingService>()
  private val mockBookingRepository = mockk<BookingRepository>()
  private val mockPlacementApplicationService = mockk<PlacementApplicationService>()

  private val service = Cas1WithdrawableTreeOperations(
    mockPlacementRequestService,
    mockBookingService,
    mockBookingRepository,
    mockPlacementApplicationService,
  )

  val probationRegion = ProbationRegionEntityFactory()
    .withYieldedApArea { ApAreaEntityFactory().produce() }
    .produce()

  val user = UserEntityFactory().withProbationRegion(probationRegion).produce()

  val application = ApprovedPremisesApplicationEntityFactory()
    .withId(UUID.fromString("fb724580-d6df-4e0e-92bb-54573f396202"))
    .withCreatedByUser(user)
    .produce()

  val assessment = ApprovedPremisesAssessmentEntityFactory()
    .withAllocatedToUser(user)
    .withApplication(application)
    .produce()

  val placementRequirements = PlacementRequirementsEntityFactory()
    .withApplication(application)
    .withAssessment(assessment)
    .produce()

  val premises = ApprovedPremisesEntityFactory()
    .withProbationRegion(probationRegion)
    .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
    .produce()

  val approvedPremises = ApprovedPremisesEntityFactory()
    .withProbationRegion(probationRegion)
    .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
    .produce()

  @Test
  fun `withdrawDescendantsOfRootNode ignores non withdrawable nodes`() {
    val placementApplication = PlacementApplicationEntityFactory()
      .withApplication(application)
      .withCreatedByUser(user)
      .produce()

    val placementRequestWithdrawable = PlacementRequestEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .withPlacementRequirements(placementRequirements)
      .withPlacementApplication(placementApplication)
      .produce()

    val placementRequestNotWithdrawable = PlacementRequestEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .withPlacementRequirements(placementRequirements)
      .withPlacementApplication(placementApplication)
      .produce()

    val bookingWithdrawable = BookingEntityFactory().withPremises(approvedPremises).produce()
    val bookingNotWithdrawable = BookingEntityFactory().withPremises(approvedPremises).produce()

    val adhocBookingWithdrawable = BookingEntityFactory().withPremises(approvedPremises).produce()
    val adhocBookingNotWithdrawable = BookingEntityFactory().withPremises(approvedPremises).produce()

    val tree = WithdrawableTreeNode(
      applicationId = application.id,
      entityType = WithdrawableEntityType.Application,
      entityId = application.id,
      status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
      children = listOf(
        WithdrawableTreeNode(
          applicationId = application.id,
          entityType = WithdrawableEntityType.PlacementApplication,
          entityId = placementApplication.id,
          status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
          children = listOf(
            WithdrawableTreeNode(
              applicationId = application.id,
              entityType = WithdrawableEntityType.PlacementRequest,
              entityId = placementRequestWithdrawable.id,
              status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
              children = listOf(
                WithdrawableTreeNode(
                  applicationId = application.id,
                  entityType = WithdrawableEntityType.Booking,
                  entityId = bookingWithdrawable.id,
                  status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false),
                ),
                WithdrawableTreeNode(
                  applicationId = application.id,
                  entityType = WithdrawableEntityType.Booking,
                  entityId = bookingNotWithdrawable.id,
                  status = WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = false),
                ),
              ),
            ),
            WithdrawableTreeNode(
              applicationId = application.id,
              entityType = WithdrawableEntityType.PlacementRequest,
              entityId = placementRequestNotWithdrawable.id,
              status = WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = true),
            ),
          ),
        ),
        WithdrawableTreeNode(
          applicationId = application.id,
          entityType = WithdrawableEntityType.Booking,
          entityId = adhocBookingWithdrawable.id,
          status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false),
        ),
        WithdrawableTreeNode(
          applicationId = application.id,
          entityType = WithdrawableEntityType.Booking,
          entityId = adhocBookingNotWithdrawable.id,
          status = WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = false),
        ),
      ),
    )

    val context = WithdrawalContext(
      withdrawalTriggeredBy = WithdrawalTriggeredByUser(user),
      triggeringEntityType = WithdrawableEntityType.Application,
      triggeringEntityId = application.id,
    )

    every {
      mockPlacementApplicationService.withdrawPlacementApplication(any(), any(), any())
    } returns mockk<CasResult<PlacementApplicationEntity>>()

    every {
      mockPlacementRequestService.withdrawPlacementRequest(any(), any(), any())
    } returns CasResult.Success(mockk<PlacementRequestService.PlacementRequestAndCancellations>())

    every {
      mockBookingService.createCas1Cancellation(any(), any(), null, any(), any(), any())
    } returns mockk<CasResult.Success<CancellationEntity>>()

    every { mockBookingRepository.findByIdOrNull(bookingWithdrawable.id) } returns bookingWithdrawable
    every { mockBookingRepository.findByIdOrNull(adhocBookingWithdrawable.id) } returns adhocBookingWithdrawable

    service.withdrawDescendantsOfRootNode(tree, context)

    verify {
      mockPlacementApplicationService.withdrawPlacementApplication(
        placementApplication.id,
        null,
        context,
      )
    }

    verify {
      mockPlacementRequestService.withdrawPlacementRequest(
        placementRequestWithdrawable.id,
        null,
        context,
      )
    }

    verify {
      mockBookingService.createCas1Cancellation(
        bookingWithdrawable,
        any(),
        null,
        "Automatically withdrawn as Application was withdrawn",
        null,
        context,
      )
    }

    verify {
      mockBookingService.createCas1Cancellation(
        adhocBookingWithdrawable,
        any(),
        null,
        "Automatically withdrawn as Application was withdrawn",
        null,
        context,
      )
    }

    confirmVerified(mockPlacementApplicationService)
    confirmVerified(mockPlacementRequestService)
    confirmVerified(mockBookingService)
  }

  @Test
  fun `withdrawDescendantsOfRootNode ignores nodes that are blocking, or ancestors of blocking`() {
    val placementApplicationWithdrawable = PlacementApplicationEntityFactory()
      .withApplication(application)
      .withCreatedByUser(user)
      .produce()

    val placementApplicationWithdrawableButBlocked = PlacementApplicationEntityFactory()
      .withApplication(application)
      .withCreatedByUser(user)
      .produce()

    val placementRequestWithdrawable = PlacementRequestEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .withPlacementRequirements(placementRequirements)
      .withPlacementApplication(placementApplicationWithdrawable)
      .produce()

    val placementRequestWithdrawableButBlocked = PlacementRequestEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .withPlacementRequirements(placementRequirements)
      .withPlacementApplication(placementApplicationWithdrawableButBlocked)
      .produce()

    val placementRequestNotWithdrawable = PlacementRequestEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .withPlacementRequirements(placementRequirements)
      .withPlacementApplication(placementApplicationWithdrawableButBlocked)
      .produce()

    val bookingWithdrawableButBlocking = BookingEntityFactory().withPremises(approvedPremises).produce()
    val bookingNotWithdrawable = BookingEntityFactory().withPremises(approvedPremises).produce()

    val adhocBookingWithdrawable = BookingEntityFactory().withPremises(approvedPremises).produce()
    val adhocBookingWithdrawableButBlocked = BookingEntityFactory().withPremises(approvedPremises).produce()

    val tree = WithdrawableTreeNode(
      applicationId = application.id,
      entityType = WithdrawableEntityType.Application,
      entityId = application.id,
      status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
      children = listOf(
        WithdrawableTreeNode(
          applicationId = application.id,
          entityType = WithdrawableEntityType.PlacementApplication,
          entityId = placementApplicationWithdrawable.id,
          status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
          children = listOf(
            WithdrawableTreeNode(
              applicationId = application.id,
              entityType = WithdrawableEntityType.PlacementRequest,
              entityId = placementRequestWithdrawable.id,
              status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
            ),
          ),
        ),
        WithdrawableTreeNode(
          applicationId = application.id,
          entityType = WithdrawableEntityType.PlacementApplication,
          entityId = placementApplicationWithdrawableButBlocked.id,
          status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
          children = listOf(
            WithdrawableTreeNode(
              applicationId = application.id,
              entityType = WithdrawableEntityType.PlacementRequest,
              entityId = placementRequestWithdrawableButBlocked.id,
              status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
              children = listOf(
                WithdrawableTreeNode(
                  applicationId = application.id,
                  entityType = WithdrawableEntityType.Booking,
                  entityId = bookingWithdrawableButBlocking.id,
                  status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false, blockAncestorWithdrawals = true),
                ),
                WithdrawableTreeNode(
                  applicationId = application.id,
                  entityType = WithdrawableEntityType.Booking,
                  entityId = bookingNotWithdrawable.id,
                  status = WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = false, blockAncestorWithdrawals = false),
                ),
              ),
            ),
            WithdrawableTreeNode(
              applicationId = application.id,
              entityType = WithdrawableEntityType.PlacementRequest,
              entityId = placementRequestNotWithdrawable.id,
              status = WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = true),
            ),
          ),
        ),
        WithdrawableTreeNode(
          applicationId = application.id,
          entityType = WithdrawableEntityType.Booking,
          entityId = adhocBookingWithdrawable.id,
          status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false, blockAncestorWithdrawals = false),
        ),
        WithdrawableTreeNode(
          applicationId = application.id,
          entityType = WithdrawableEntityType.Booking,
          entityId = adhocBookingWithdrawableButBlocked.id,
          status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false, blockAncestorWithdrawals = true),
        ),
      ),
    )

    val context = WithdrawalContext(
      withdrawalTriggeredBy = WithdrawalTriggeredByUser(user),
      triggeringEntityType = WithdrawableEntityType.Application,
      triggeringEntityId = application.id,
    )

    every {
      mockPlacementApplicationService.withdrawPlacementApplication(any(), any(), any())
    } returns mockk<CasResult<PlacementApplicationEntity>>()

    every {
      mockPlacementRequestService.withdrawPlacementRequest(any(), any(), any())
    } returns CasResult.Success(mockk<PlacementRequestService.PlacementRequestAndCancellations>())

    every {
      mockBookingService.createCas1Cancellation(any(), any(), null, any(), any(), any())
    } returns mockk<CasResult.Success<CancellationEntity>>()

    every { mockBookingRepository.findByIdOrNull(bookingWithdrawableButBlocking.id) } returns bookingWithdrawableButBlocking
    every { mockBookingRepository.findByIdOrNull(adhocBookingWithdrawable.id) } returns adhocBookingWithdrawable

    service.withdrawDescendantsOfRootNode(tree, context)

    verify {
      mockPlacementApplicationService.withdrawPlacementApplication(
        placementApplicationWithdrawable.id,
        null,
        context,
      )
    }

    verify {
      mockPlacementRequestService.withdrawPlacementRequest(
        placementRequestWithdrawable.id,
        null,
        context,
      )
    }

    verify {
      mockBookingService.createCas1Cancellation(
        adhocBookingWithdrawable,
        any(),
        null,
        "Automatically withdrawn as Application was withdrawn",
        null,
        context,
      )
    }

    confirmVerified(mockPlacementApplicationService)
    confirmVerified(mockPlacementRequestService)
    confirmVerified(mockBookingService)
  }

  @Test
  fun `withdrawDescendantsOfRootNode for application reports errors if can't withdrawn children`() {
    val logger = mockk<Logger>()
    service.log = logger

    every { logger.isDebugEnabled() } returns true
    every { logger.debug(any<String>()) } returns Unit
    every { logger.error(any<String>()) } returns Unit

    val placementApplication = PlacementApplicationEntityFactory()
      .withApplication(application)
      .withCreatedByUser(user)
      .produce()

    val placementRequestWithdrawable = PlacementRequestEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .withPlacementRequirements(placementRequirements)
      .withPlacementApplication(placementApplication)
      .produce()

    val placementRequestNotWithdrawable = PlacementRequestEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .withPlacementRequirements(placementRequirements)
      .withPlacementApplication(placementApplication)
      .produce()

    val bookingWithdrawable = BookingEntityFactory().withPremises(approvedPremises).produce()
    val bookingNotWithdrawable = BookingEntityFactory().withPremises(approvedPremises).produce()

    val adhocBookingWithdrawable = BookingEntityFactory().withPremises(approvedPremises).produce()
    val adhocBookingNotWithdrawable = BookingEntityFactory().withPremises(approvedPremises).produce()

    val tree = WithdrawableTreeNode(
      applicationId = application.id,
      entityType = WithdrawableEntityType.Application,
      entityId = application.id,
      status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
      children = listOf(
        WithdrawableTreeNode(
          applicationId = application.id,
          entityType = WithdrawableEntityType.PlacementApplication,
          entityId = placementApplication.id,
          status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
          children = listOf(
            WithdrawableTreeNode(
              applicationId = application.id,
              entityType = WithdrawableEntityType.PlacementRequest,
              entityId = placementRequestWithdrawable.id,
              status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
              children = listOf(
                WithdrawableTreeNode(
                  applicationId = application.id,
                  entityType = WithdrawableEntityType.Booking,
                  entityId = bookingWithdrawable.id,
                  status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false),
                ),
                WithdrawableTreeNode(
                  applicationId = UUID.randomUUID(),
                  entityType = WithdrawableEntityType.Booking,
                  entityId = bookingNotWithdrawable.id,
                  status = WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = false),
                ),
              ),
            ),
            WithdrawableTreeNode(
              applicationId = application.id,
              entityType = WithdrawableEntityType.PlacementRequest,
              entityId = placementRequestNotWithdrawable.id,
              status = WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = true),
            ),
          ),
        ),
        WithdrawableTreeNode(
          applicationId = application.id,
          entityType = WithdrawableEntityType.Booking,
          entityId = adhocBookingWithdrawable.id,
          status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false),
        ),
        WithdrawableTreeNode(
          applicationId = application.id,
          entityType = WithdrawableEntityType.Booking,
          entityId = adhocBookingNotWithdrawable.id,
          status = WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = false),
        ),
      ),
    )

    val context = WithdrawalContext(
      withdrawalTriggeredBy = WithdrawalTriggeredByUser(user),
      triggeringEntityType = WithdrawableEntityType.Application,
      triggeringEntityId = application.id,
    )

    every {
      mockPlacementApplicationService.withdrawPlacementApplication(any(), any(), any())
    } returns CasResult.Unauthorised()

    every {
      mockPlacementRequestService.withdrawPlacementRequest(any(), any(), any())
    } returns CasResult.Unauthorised()

    every {
      mockBookingService.createCas1Cancellation(any(), any(), null, any(), any(), any())
    } returns CasResult.GeneralValidationError("oh dear")

    every { mockBookingRepository.findByIdOrNull(bookingWithdrawable.id) } returns bookingWithdrawable
    every { mockBookingRepository.findByIdOrNull(adhocBookingWithdrawable.id) } returns adhocBookingWithdrawable

    service.withdrawDescendantsOfRootNode(tree, context)

    verify {
      logger.error(
        "Failed to automatically withdraw PlacementApplication ${placementApplication.id} " +
          "when withdrawing Application ${application.id} " +
          "with error type class uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult\$Unauthorised",
      )
    }

    verify {
      logger.error(
        "Failed to automatically withdraw PlacementRequest ${placementRequestWithdrawable.id} " +
          "when withdrawing Application ${application.id} " +
          "with error type class uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult\$Unauthorised",
      )
    }

    verify {
      logger.error(
        "Failed to automatically withdraw Booking ${bookingWithdrawable.id} " +
          "when withdrawing Application ${application.id} " +
          "with message oh dear",
      )
    }

    verify {
      logger.error(
        "Failed to automatically withdraw Booking ${adhocBookingWithdrawable.id} " +
          "when withdrawing Application ${application.id} " +
          "with message oh dear",
      )
    }
  }

  @Test
  fun `withdrawDescendantsOfRootNode throws exception if any nodes don't belong to same application as root node`() {
    val otherApplicationId = UUID.fromString("4071072a-3d52-4904-b5bd-32b6420b105a")

    val placementApplicationOtherApp = PlacementApplicationEntityFactory()
      .withId(UUID.fromString("db8c102a-4062-4f8e-ab0f-d5f8953f447c"))
      .withApplication(application)
      .withCreatedByUser(user)
      .produce()

    val placementRequestWithdrawable = PlacementRequestEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .withPlacementRequirements(placementRequirements)
      .withPlacementApplication(placementApplicationOtherApp)
      .produce()

    val placementRequestNotWithdrawable = PlacementRequestEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .withPlacementRequirements(placementRequirements)
      .withPlacementApplication(placementApplicationOtherApp)
      .produce()

    val bookingWithdrawableOtherApp = BookingEntityFactory()
      .withId(UUID.fromString("843de779-0b23-4517-8dea-f749596e0666"))
      .withPremises(approvedPremises)
      .produce()
    val bookingNotWithdrawable = BookingEntityFactory().withPremises(approvedPremises).produce()

    val adhocBookingWithdrawableOtherApp = BookingEntityFactory()
      .withId(UUID.fromString("3a159ffe-d22e-4a04-9432-c7df4f836098"))
      .withPremises(approvedPremises)
      .produce()
    val adhocBookingNotWithdrawable = BookingEntityFactory().withPremises(approvedPremises).produce()

    val tree = WithdrawableTreeNode(
      applicationId = application.id,
      entityType = WithdrawableEntityType.Application,
      entityId = application.id,
      status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
      children = listOf(
        WithdrawableTreeNode(
          applicationId = otherApplicationId,
          entityType = WithdrawableEntityType.PlacementApplication,
          entityId = placementApplicationOtherApp.id,
          status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
          children = listOf(
            WithdrawableTreeNode(
              applicationId = application.id,
              entityType = WithdrawableEntityType.PlacementRequest,
              entityId = placementRequestWithdrawable.id,
              status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
              children = listOf(
                WithdrawableTreeNode(
                  applicationId = otherApplicationId,
                  entityType = WithdrawableEntityType.Booking,
                  entityId = bookingWithdrawableOtherApp.id,
                  status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false),
                ),
                WithdrawableTreeNode(
                  applicationId = otherApplicationId,
                  entityType = WithdrawableEntityType.Booking,
                  entityId = bookingNotWithdrawable.id,
                  status = WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = false),
                ),
              ),
            ),
            WithdrawableTreeNode(
              applicationId = application.id,
              entityType = WithdrawableEntityType.PlacementRequest,
              entityId = placementRequestNotWithdrawable.id,
              status = WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = true),
            ),
          ),
        ),
        WithdrawableTreeNode(
          applicationId = otherApplicationId,
          entityType = WithdrawableEntityType.Booking,
          entityId = adhocBookingWithdrawableOtherApp.id,
          status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false),
        ),
        WithdrawableTreeNode(
          applicationId = application.id,
          entityType = WithdrawableEntityType.Booking,
          entityId = adhocBookingNotWithdrawable.id,
          status = WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = false),
        ),
      ),
    )

    val context = WithdrawalContext(
      withdrawalTriggeredBy = WithdrawalTriggeredByUser(user),
      triggeringEntityType = WithdrawableEntityType.Application,
      triggeringEntityId = application.id,
    )

    assertThatThrownBy {
      service.withdrawDescendantsOfRootNode(tree, context)
    }.hasMessage(
      "Cascade withdrawal for root node Application fb724580-d6df-4e0e-92bb-54573f396202 (application fb724580-d6df-4e0e-92bb-54573f396202) would " +
        "remove the following nodes belonging to other applications " +
        "[PlacementApplication db8c102a-4062-4f8e-ab0f-d5f8953f447c (application 4071072a-3d52-4904-b5bd-32b6420b105a), " +
        "Booking 843de779-0b23-4517-8dea-f749596e0666 (application 4071072a-3d52-4904-b5bd-32b6420b105a), " +
        "Booking 3a159ffe-d22e-4a04-9432-c7df4f836098 (application 4071072a-3d52-4904-b5bd-32b6420b105a)]",
    )
  }

  @Test
  fun `withdrawDescendantsOfRootNode throws exception if attempting to withdraw more than 50 descendants`() {
    val childenNodes = (0..105).map {
      WithdrawableTreeNode(
        applicationId = application.id,
        entityType = WithdrawableEntityType.PlacementRequest,
        entityId = UUID.randomUUID(),
        status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
      )
    }

    val tree = WithdrawableTreeNode(
      applicationId = application.id,
      entityType = WithdrawableEntityType.Application,
      entityId = application.id,
      status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
      children = childenNodes,
    )

    val context = WithdrawalContext(
      withdrawalTriggeredBy = WithdrawalTriggeredByUser(user),
      triggeringEntityType = WithdrawableEntityType.Application,
      triggeringEntityId = application.id,
    )

    assertThatThrownBy {
      service.withdrawDescendantsOfRootNode(tree, context)
    }.hasMessage(
      "Cascade withdrawal for root node Application fb724580-d6df-4e0e-92bb-54573f396202 (application fb724580-d6df-4e0e-92bb-54573f396202) " +
        "will lead to an unexpectedly high number of withdrawals (106)",
    )
  }
}
