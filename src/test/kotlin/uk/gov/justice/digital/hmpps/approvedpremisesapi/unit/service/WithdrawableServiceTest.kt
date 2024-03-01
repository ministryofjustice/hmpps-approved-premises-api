package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WithdrawableDatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WithdrawableState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WithdrawableTreeBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WithdrawableTreeNode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WithdrawalContext
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class WithdrawableServiceTest {
  private val mockPlacementRequestService = mockk<PlacementRequestService>()
  private val mockBookingService = mockk<BookingService>()
  private val mockBookingRepository = mockk<BookingRepository>()
  private val mockPlacementApplicationService = mockk<PlacementApplicationService>()
  private val mockWithdrawableTreeBuilder = mockk<WithdrawableTreeBuilder>()

  private val withdrawableService = WithdrawableService(
    mockPlacementRequestService,
    mockBookingService,
    mockBookingRepository,
    mockPlacementApplicationService,
    mockWithdrawableTreeBuilder,
  )

  val probationRegion = ProbationRegionEntityFactory()
    .withYieldedApArea { ApAreaEntityFactory().produce() }
    .produce()

  val user = UserEntityFactory().withProbationRegion(probationRegion).produce()

  val application = ApprovedPremisesApplicationEntityFactory()
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

  val placementRequests = PlacementRequestEntityFactory()
    .withApplication(application)
    .withAssessment(assessment)
    .withPlacementRequirements(placementRequirements)
    .produceMany()
    .take(5)
    .toList()

  val placementApplications = PlacementApplicationEntityFactory()
    .withCreatedByUser(user)
    .withApplication(application)
    .withSubmittedAt(OffsetDateTime.now())
    .produceMany()
    .take(2)
    .toList()

  val approvedPremises = ApprovedPremisesEntityFactory()
    .withProbationRegion(probationRegion)
    .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
    .produce()

  val bookings = BookingEntityFactory()
    .withYieldedPremises {
      approvedPremises
    }
    .produceMany()
    .take(3)
    .toList()

  @BeforeEach
  fun setup() {
    every {
      mockPlacementRequestService.getWithdrawablePlacementRequestsForUser(user, application)
    } returns placementRequests
    every {
      mockPlacementApplicationService.getWithdrawablePlacementApplicationsForUser(user, application)
    } returns placementApplications
    every {
      mockBookingService.getCancelleableCas1BookingsForUser(user, application)
    } returns bookings
  }

  @Test
  fun `allWithdrawables correctly maps information`() {
    val appId = UUID.randomUUID()

    every {
      mockWithdrawableTreeBuilder.treeForApp(application, user)
    } returns
      WithdrawableTreeNode(
        entityType = WithdrawableEntityType.PlacementApplication,
        entityId = appId,
        status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
        dates = listOf(
          WithdrawableDatePeriod(LocalDate.of(2021, 1, 2), LocalDate.of(2021, 2, 3)),
          WithdrawableDatePeriod(LocalDate.of(2021, 3, 4), LocalDate.of(2021, 4, 5)),
        ),
      )

    val result = withdrawableService.allWithdrawables(application, user)

    assertThat(result).hasSize(1)

    val withdrawableEntity = result.first()
    assertThat(withdrawableEntity.id).isEqualTo(appId)
    assertThat(withdrawableEntity.type).isEqualTo(WithdrawableEntityType.PlacementApplication)
    assertThat(withdrawableEntity.dates).isEqualTo(
      listOf(
        WithdrawableDatePeriod(LocalDate.of(2021, 1, 2), LocalDate.of(2021, 2, 3)),
        WithdrawableDatePeriod(LocalDate.of(2021, 3, 4), LocalDate.of(2021, 4, 5)),
      ),
    )
  }

  @Test
  fun `allWithdrawables only returns entities the user can withdraw`() {
    val appWithdrawableId = UUID.randomUUID()
    val placementRequest1WithdrawableId = UUID.randomUUID()
    val placementRequestWithdrawableButNotPermittedId = UUID.randomUUID()
    val placementRequest2WithdrawableId = UUID.randomUUID()
    val placementApplication1WithdrawableId = UUID.randomUUID()
    val placementApplication2NotWithdrawableId = UUID.randomUUID()
    val placementWithdrawableId = UUID.randomUUID()
    val placementNotWithdrawableId = UUID.randomUUID()

    every {
      mockWithdrawableTreeBuilder.treeForApp(application, user)
    } returns
      WithdrawableTreeNode(
        entityType = WithdrawableEntityType.Application,
        entityId = appWithdrawableId,
        status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
        children = listOf(
          WithdrawableTreeNode(
            entityType = WithdrawableEntityType.PlacementRequest,
            entityId = placementRequest1WithdrawableId,
            status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
            children = listOf(
              WithdrawableTreeNode(
                entityType = WithdrawableEntityType.Booking,
                entityId = placementWithdrawableId,
                status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
              ),
            ),
          ),
          WithdrawableTreeNode(
            entityType = WithdrawableEntityType.PlacementRequest,
            entityId = placementRequestWithdrawableButNotPermittedId,
            status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false),
          ),
          WithdrawableTreeNode(
            entityType = WithdrawableEntityType.PlacementApplication,
            entityId = placementApplication1WithdrawableId,
            status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
            children = listOf(
              WithdrawableTreeNode(
                entityType = WithdrawableEntityType.PlacementRequest,
                entityId = placementRequest2WithdrawableId,
                status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
              ),
            ),
          ),
          WithdrawableTreeNode(
            entityType = WithdrawableEntityType.PlacementApplication,
            entityId = placementApplication2NotWithdrawableId,
            status = WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = true),
          ),
          WithdrawableTreeNode(
            entityType = WithdrawableEntityType.Booking,
            entityId = placementNotWithdrawableId,
            status = WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = true),
          ),
        ),
      )

    val result = withdrawableService.allWithdrawables(application, user)

    assertThat(result).hasSize(5)
    assertThat(result).anyMatch { it.id == appWithdrawableId }
    assertThat(result).anyMatch { it.id == placementRequest1WithdrawableId }
    assertThat(result).anyMatch { it.id == placementRequest2WithdrawableId }
    assertThat(result).anyMatch { it.id == placementApplication1WithdrawableId }
    assertThat(result).anyMatch { it.id == placementWithdrawableId }
  }

  @Test
  fun `withdrawDescendants for application success`() {
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

    every {
      mockWithdrawableTreeBuilder.treeForPlacementApp(placementApplication, user)
    } returns
      WithdrawableTreeNode(
        entityType = WithdrawableEntityType.Application,
        entityId = application.id,
        status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
        children = listOf(
          WithdrawableTreeNode(
            entityType = WithdrawableEntityType.PlacementApplication,
            entityId = placementApplication.id,
            status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
            children = listOf(
              WithdrawableTreeNode(
                entityType = WithdrawableEntityType.PlacementRequest,
                entityId = placementRequestWithdrawable.id,
                status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
              ),
              WithdrawableTreeNode(
                entityType = WithdrawableEntityType.PlacementRequest,
                entityId = placementRequestNotWithdrawable.id,
                status = WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = true),
              ),
            ),
          ),
          WithdrawableTreeNode(
            entityType = WithdrawableEntityType.Booking,
            entityId = bookingWithdrawable.id,
            status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false),
          ),
          WithdrawableTreeNode(
            entityType = WithdrawableEntityType.Booking,
            entityId = bookingNotWithdrawable.id,
            status = WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = false),
          ),
        ),
      )

    val context = WithdrawalContext(
      triggeringUser = user,
      triggeringEntityType = WithdrawableEntityType.Application,
      triggeringEntityId = application.id,
    )

    every {
      mockPlacementApplicationService.withdrawPlacementApplication(any(), any(), any())
    } returns mockk<AuthorisableActionResult<ValidatableActionResult<PlacementApplicationEntity>>>()

    every {
      mockPlacementRequestService.withdrawPlacementRequest(any(), any(), any())
    } returns AuthorisableActionResult.Success(mockk<PlacementRequestService.PlacementRequestAndCancellations>())

    every {
      mockBookingService.createCas1Cancellation(any(), any(), null, any(), any())
    } returns mockk<ValidatableActionResult.Success<CancellationEntity>>()

    every { mockBookingRepository.findByIdOrNull(bookingWithdrawable.id) } returns bookingWithdrawable

    withdrawableService.withdrawPlacementApplicationDescendants(placementApplication, context)

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
        context,
      )
    }
  }

  @Test
  fun `withdrawDescendants for application reports errors if can't withdrawn children`() {
    val logger = mockk<Logger>()
    withdrawableService.log = logger

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

    every {
      mockWithdrawableTreeBuilder.treeForPlacementApp(placementApplication, user)
    } returns
      WithdrawableTreeNode(
        entityType = WithdrawableEntityType.Application,
        entityId = application.id,
        status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
        children = listOf(
          WithdrawableTreeNode(
            entityType = WithdrawableEntityType.PlacementApplication,
            entityId = placementApplication.id,
            status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
            children = listOf(
              WithdrawableTreeNode(
                entityType = WithdrawableEntityType.PlacementRequest,
                entityId = placementRequestWithdrawable.id,
                status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
              ),
              WithdrawableTreeNode(
                entityType = WithdrawableEntityType.PlacementRequest,
                entityId = placementRequestNotWithdrawable.id,
                status = WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = true),
              ),
            ),
          ),
          WithdrawableTreeNode(
            entityType = WithdrawableEntityType.Booking,
            entityId = bookingWithdrawable.id,
            status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false),
          ),
          WithdrawableTreeNode(
            entityType = WithdrawableEntityType.Booking,
            entityId = bookingNotWithdrawable.id,
            status = WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = false),
          ),
        ),
      )

    val context = WithdrawalContext(
      triggeringUser = user,
      triggeringEntityType = WithdrawableEntityType.Application,
      triggeringEntityId = application.id,
    )

    every {
      mockPlacementApplicationService.withdrawPlacementApplication(any(), any(), any())
    } returns AuthorisableActionResult.Unauthorised()

    every {
      mockPlacementRequestService.withdrawPlacementRequest(any(), any(), any())
    } returns AuthorisableActionResult.Unauthorised()

    every {
      mockBookingService.createCas1Cancellation(any(), any(), null, any(), any())
    } returns ValidatableActionResult.GeneralValidationError("oh dear")

    every { mockBookingRepository.findByIdOrNull(bookingWithdrawable.id) } returns bookingWithdrawable

    withdrawableService.withdrawPlacementApplicationDescendants(placementApplication, context)

    verify {
      logger.error(
        "Failed to automatically withdraw PlacementApplication ${placementApplication.id} " +
          "when withdrawing Application ${application.id} " +
          "with error type class uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult\$Unauthorised",
      )
    }

    verify {
      logger.error(
        "Failed to automatically withdraw PlacementRequest ${placementRequestWithdrawable.id} " +
          "when withdrawing Application ${application.id} " +
          "with error type class uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult\$Unauthorised",
      )
    }

    verify {
      logger.error(
        "Failed to automatically withdraw Booking ${bookingWithdrawable.id} " +
          "when withdrawing Application ${application.id} " +
          "with message oh dear",
      )
    }
  }

  @Test
  fun `withdrawDescendants for placement application success`() {
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

    every {
      mockWithdrawableTreeBuilder.treeForPlacementApp(placementApplication, user)
    } returns
      WithdrawableTreeNode(
        entityType = WithdrawableEntityType.PlacementApplication,
        entityId = placementApplication.id,
        status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
        children = listOf(
          WithdrawableTreeNode(
            entityType = WithdrawableEntityType.PlacementRequest,
            entityId = placementRequestWithdrawable.id,
            status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
            children = listOf(
              WithdrawableTreeNode(
                entityType = WithdrawableEntityType.Booking,
                entityId = bookingWithdrawable.id,
                status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false),
              ),
              WithdrawableTreeNode(
                entityType = WithdrawableEntityType.Booking,
                entityId = bookingNotWithdrawable.id,
                status = WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = false),
              ),
            ),
          ),
          WithdrawableTreeNode(
            entityType = WithdrawableEntityType.PlacementRequest,
            entityId = placementRequestNotWithdrawable.id,
            status = WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = true),
          ),
        ),
      )

    val context = WithdrawalContext(
      triggeringUser = user,
      triggeringEntityType = WithdrawableEntityType.PlacementApplication,
      triggeringEntityId = placementApplication.id,
    )

    every {
      mockPlacementRequestService.withdrawPlacementRequest(any(), any(), any())
    } returns AuthorisableActionResult.Success(mockk<PlacementRequestService.PlacementRequestAndCancellations>())

    every {
      mockBookingService.createCas1Cancellation(any(), any(), null, any(), any())
    } returns mockk<ValidatableActionResult.Success<CancellationEntity>>()

    every { mockBookingRepository.findByIdOrNull(bookingWithdrawable.id) } returns bookingWithdrawable

    withdrawableService.withdrawPlacementApplicationDescendants(placementApplication, context)

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
        "Automatically withdrawn as Request for Placement was withdrawn",
        context,
      )
    }
  }

  @Test
  fun `withdrawDescendants for placement application reports errors if can't withdrawn children`() {
    val logger = mockk<Logger>()
    withdrawableService.log = logger

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

    val bookingWithdrawable = BookingEntityFactory().withPremises(approvedPremises).produce()

    every {
      mockWithdrawableTreeBuilder.treeForPlacementApp(placementApplication, user)
    } returns
      WithdrawableTreeNode(
        entityType = WithdrawableEntityType.PlacementApplication,
        entityId = placementApplication.id,
        status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
        children = listOf(
          WithdrawableTreeNode(
            entityType = WithdrawableEntityType.PlacementRequest,
            entityId = placementRequestWithdrawable.id,
            status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
            children = listOf(
              WithdrawableTreeNode(
                entityType = WithdrawableEntityType.Booking,
                entityId = bookingWithdrawable.id,
                status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false),
              ),
            ),
          ),
        ),
      )

    val context = WithdrawalContext(
      triggeringUser = user,
      triggeringEntityType = WithdrawableEntityType.PlacementApplication,
      triggeringEntityId = placementApplication.id,
    )

    every {
      mockPlacementRequestService.withdrawPlacementRequest(any(), any(), any())
    } returns AuthorisableActionResult.Unauthorised()

    every {
      mockBookingService.createCas1Cancellation(any(), any(), null, any(), any())
    } returns ValidatableActionResult.GeneralValidationError("oh dear")

    every { mockBookingRepository.findByIdOrNull(bookingWithdrawable.id) } returns bookingWithdrawable

    withdrawableService.withdrawPlacementApplicationDescendants(placementApplication, context)

    verify {
      logger.error(
        "Failed to automatically withdraw PlacementRequest ${placementRequestWithdrawable.id} " +
          "when withdrawing PlacementApplication ${placementApplication.id} " +
          "with error type class uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult\$Unauthorised",
      )
    }

    verify {
      logger.error(
        "Failed to automatically withdraw Booking ${bookingWithdrawable.id} " +
          "when withdrawing PlacementApplication ${placementApplication.id} " +
          "with message oh dear",
      )
    }
  }

  @Test
  fun `withdrawDescendants for placement request success`() {
    val placementRequest = PlacementRequestEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .withPlacementRequirements(placementRequirements)
      .produce()

    val bookingWithdrawable = BookingEntityFactory().withPremises(approvedPremises).produce()
    val bookingNotWithdrawable = BookingEntityFactory().withPremises(approvedPremises).produce()

    every {
      mockWithdrawableTreeBuilder.treeForPlacementReq(placementRequest, user)
    } returns
      WithdrawableTreeNode(
        entityType = WithdrawableEntityType.PlacementRequest,
        entityId = placementRequest.id,
        status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
        children = listOf(
          WithdrawableTreeNode(
            entityType = WithdrawableEntityType.Booking,
            entityId = bookingWithdrawable.id,
            status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false),
          ),
          WithdrawableTreeNode(
            entityType = WithdrawableEntityType.Booking,
            entityId = bookingNotWithdrawable.id,
            status = WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = false),
          ),
        ),
      )

    val context = WithdrawalContext(
      triggeringUser = user,
      triggeringEntityType = WithdrawableEntityType.PlacementRequest,
      triggeringEntityId = placementRequest.id,
    )

    every {
      mockBookingService.createCas1Cancellation(any(), any(), null, any(), any())
    } returns mockk<ValidatableActionResult.Success<CancellationEntity>>()

    every { mockBookingRepository.findByIdOrNull(bookingWithdrawable.id) } returns bookingWithdrawable

    withdrawableService.withdrawPlacementRequestDescendants(placementRequest, context)

    verify {
      mockBookingService.createCas1Cancellation(
        bookingWithdrawable,
        any(),
        null,
        "Automatically withdrawn as Request for Placement was withdrawn",
        context,
      )
    }
  }

  @Test
  fun `withdrawDescendants for placement request reports errors if can't withdraw children`() {
    val logger = mockk<Logger>()
    withdrawableService.log = logger

    every { logger.isDebugEnabled() } returns true
    every { logger.debug(any<String>()) } returns Unit
    every { logger.error(any<String>()) } returns Unit

    val placementRequest = PlacementRequestEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .withPlacementRequirements(placementRequirements)
      .produce()

    val bookingWithdrawable = BookingEntityFactory().withPremises(approvedPremises).produce()

    every {
      mockWithdrawableTreeBuilder.treeForPlacementReq(placementRequest, user)
    } returns
      WithdrawableTreeNode(
        entityType = WithdrawableEntityType.PlacementRequest,
        entityId = placementRequest.id,
        status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
        children = listOf(
          WithdrawableTreeNode(
            entityType = WithdrawableEntityType.Booking,
            entityId = bookingWithdrawable.id,
            status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false),
          ),
        ),
      )

    val context = WithdrawalContext(
      triggeringUser = user,
      triggeringEntityType = WithdrawableEntityType.PlacementRequest,
      triggeringEntityId = placementRequest.id,
    )

    every {
      mockBookingService.createCas1Cancellation(any(), any(), null, any(), any())
    } returns ValidatableActionResult.GeneralValidationError("oh dear")

    every { mockBookingRepository.findByIdOrNull(bookingWithdrawable.id) } returns bookingWithdrawable

    withdrawableService.withdrawPlacementRequestDescendants(placementRequest, context)

    verify {
      logger.error(
        "Failed to automatically withdraw Booking ${bookingWithdrawable.id} " +
          "when withdrawing PlacementRequest ${placementRequest.id} " +
          "with message oh dear",
      )
    }
  }
}
