package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
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
  private val mockPlacementApplicationService = mockk<PlacementApplicationService>()
  private val mockWithdrawableTreeBuilder = mockk<WithdrawableTreeBuilder>()

  private val withdrawableService = WithdrawableService(
    mockPlacementRequestService,
    mockBookingService,
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

  val bookings = BookingEntityFactory()
    .withYieldedPremises {
      ApprovedPremisesEntityFactory()
        .withProbationRegion(probationRegion)
        .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
        .produce()
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
  fun `withdrawAllForApplication cascades to placement requests, placement applications and adhoc bookings`() {
    application.placementRequests.addAll(placementRequests)
    every {
      mockPlacementApplicationService.getAllPlacementApplicationEntitiesForApplicationId(application.id)
    } returns placementApplications

    every {
      mockBookingService.getAllForApplication(application)
    } returns bookings

    every {
      mockPlacementRequestService.withdrawPlacementRequest(
        any(),
        userProvidedReason = null,
        WithdrawalContext(
          user,
          WithdrawableEntityType.Application,
        ),
      )
    } returns mockk<AuthorisableActionResult<PlacementRequestService.PlacementRequestAndCancellations>>()

    every {
      mockPlacementApplicationService.withdrawPlacementApplication(
        any(),
        userProvidedReason = null,
        WithdrawalContext(
          user,
          WithdrawableEntityType.Application,
        ),
      )
    } returns mockk<AuthorisableActionResult<ValidatableActionResult<PlacementApplicationEntity>>>()

    every {
      mockBookingService.createCas1Cancellation(
        booking = any(),
        cancelledAt = any(),
        userProvidedReason = null,
        notes = any(),
        WithdrawalContext(
          user,
          WithdrawableEntityType.Application,
        ),
      )
    } returns mockk<ValidatableActionResult.Success<CancellationEntity>>()

    withdrawableService.withdrawAllForApplication(application, user)

    placementRequests.forEach {
      verify {
        mockPlacementRequestService.withdrawPlacementRequest(
          it.id,
          userProvidedReason = null,
          WithdrawalContext(
            user,
            WithdrawableEntityType.Application,
          ),
        )
      }
    }

    placementApplications.forEach {
      verify {
        mockPlacementApplicationService.withdrawPlacementApplication(
          it.id,
          userProvidedReason = null,
          WithdrawalContext(
            user,
            WithdrawableEntityType.Application,
          ),
        )
      }
    }

    bookings.forEach {
      verify {
        mockBookingService.createCas1Cancellation(
          booking = it,
          cancelledAt = any(),
          userProvidedReason = null,
          notes = "Automatically withdrawn as placement request was withdrawn",
          WithdrawalContext(
            user,
            WithdrawableEntityType.Application,
          ),
        )
      }
    }
  }

  @Test
  fun `withdrawAllForApplication reports errors if can't withdraw children`() {
    val logger = mockk<Logger>()
    withdrawableService.log = logger

    val placementRequest = placementRequests[0]
    application.placementRequests.add(placementRequest)

    val placementApplication = placementApplications[0]
    every {
      mockPlacementApplicationService.getAllPlacementApplicationEntitiesForApplicationId(application.id)
    } returns listOf(placementApplication)

    val booking = bookings[0]
    every {
      mockBookingService.getAllForApplication(application)
    } returns listOf(booking)

    every {
      mockPlacementRequestService.withdrawPlacementRequest(
        any(),
        userProvidedReason = null,
        WithdrawalContext(
          user,
          WithdrawableEntityType.Application,
        ),
      )
    } returns AuthorisableActionResult.Unauthorised()

    every {
      mockPlacementApplicationService.withdrawPlacementApplication(
        any(),
        userProvidedReason = null,
        WithdrawalContext(
          user,
          WithdrawableEntityType.Application,
        ),
      )
    } returns AuthorisableActionResult.Unauthorised()

    every {
      mockBookingService.createCas1Cancellation(
        booking = any(),
        cancelledAt = any(),
        userProvidedReason = null,
        notes = any(),
        WithdrawalContext(
          user,
          WithdrawableEntityType.Application,
        ),
      )
    } returns ValidatableActionResult.GeneralValidationError("oh dear")

    every { logger.error(any<String>()) } returns Unit

    withdrawableService.withdrawAllForApplication(application, user)

    verify {
      logger.error(
        "Failed to automatically withdraw placement request ${placementRequest.id} " +
          "when withdrawing application ${application.id} " +
          "with error type class uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult\$Unauthorised",
      )
    }

    verify {
      logger.error(
        "Failed to automatically withdraw placement application ${placementApplication.id} " +
          "when withdrawing application ${application.id} " +
          "with error type class uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult\$Unauthorised",
      )
    }

    verify {
      logger.error(
        "Failed to automatically withdraw booking ${booking.id} " +
          "when withdrawing application ${application.id} " +
          "with message oh dear",
      )
    }
  }
}
