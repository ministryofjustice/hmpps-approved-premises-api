package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WithdrawableDatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WithdrawableState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WithdrawableTreeBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WithdrawableTreeNode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WithdrawableTreeOperations
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WithdrawalContext
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class WithdrawableServiceTest {
  private val applicationService = mockk<ApplicationService>()
  private val placementRequestService = mockk<PlacementRequestService>()
  private val placementApplicationService = mockk<PlacementApplicationService>()
  private val bookingService = mockk<BookingService>()
  private val withdrawableTreeBuilder = mockk<WithdrawableTreeBuilder>()
  private val withdrawableTreeOperations = mockk<WithdrawableTreeOperations>()

  private val withdrawableService = WithdrawableService(
    applicationService,
    placementRequestService,
    placementApplicationService,
    bookingService,
    withdrawableTreeBuilder,
    withdrawableTreeOperations,
  )

  val probationRegion = ProbationRegionEntityFactory()
    .withYieldedApArea { ApAreaEntityFactory().produce() }
    .produce()

  val user = UserEntityFactory().withProbationRegion(probationRegion).produce()

  val application = ApprovedPremisesApplicationEntityFactory()
    .withCreatedByUser(user)
    .produce()

  @Test
  fun `allWithdrawables correctly maps information`() {
    val appId = UUID.randomUUID()

    every {
      withdrawableTreeBuilder.treeForApp(application, user)
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
      withdrawableTreeBuilder.treeForApp(application, user)
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

  @Nested
  inner class WithdrawApplication {

    val withdrawalReason = "the reason"
    val withdrawalOtherReason = "the other reason"

    @Test
    fun success() {
      every { applicationService.getApplication(application.id) } returns application

      val tree = WithdrawableTreeNode(
        WithdrawableEntityType.Application,
        application.id,
        WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
      )

      every { withdrawableTreeBuilder.treeForApp(application, user) } returns tree

      every {
        applicationService.withdrawApprovedPremisesApplication(any(), any(), any(), any())
      } returns CasResult.Success(Unit)

      every {
        withdrawableTreeOperations.withdrawDescendantsOfRootNode(
          tree,
          WithdrawalContext(user, WithdrawableEntityType.Application, application.id),
        )
      } returns Unit

      val result = withdrawableService.withdrawApplication(application.id, user, withdrawalReason, withdrawalOtherReason)

      assertThat(result is CasResult.Success)

      verify {
        applicationService.withdrawApprovedPremisesApplication(
          application.id,
          user,
          withdrawalReason,
          withdrawalOtherReason,
        )
      }

      verify {
        withdrawableTreeOperations.withdrawDescendantsOfRootNode(
          tree,
          WithdrawalContext(user, WithdrawableEntityType.Application, application.id),
        )
      }
    }

    @Test
    fun `fails if not CAS1 application()`() {
      every { applicationService.getApplication(application.id) } returns
        TemporaryAccommodationApplicationEntityFactory()
          .withProbationRegion(ProbationRegionEntityFactory().withDefaults().produce())
          .withCreatedByUser(user)
          .produce()

      val result = withdrawableService.withdrawApplication(application.id, user, withdrawalReason, withdrawalOtherReason)

      assertThat(result is CasResult.GeneralValidationError)
    }

    @Test
    fun `fails if user may not directly withdraw()`() {
      every { applicationService.getApplication(application.id) } returns application

      val tree = WithdrawableTreeNode(
        WithdrawableEntityType.Application,
        application.id,
        WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false),
      )

      every { withdrawableTreeBuilder.treeForApp(application, user) } returns tree

      val result = withdrawableService.withdrawApplication(application.id, user, withdrawalReason, withdrawalOtherReason)

      assertThat(result is CasResult.Unauthorised).isTrue()
    }

    @Test
    fun `fails if not withdrawable()`() {
      every { applicationService.getApplication(application.id) } returns application

      val tree = WithdrawableTreeNode(
        WithdrawableEntityType.Application,
        application.id,
        WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = true),
      )

      every { withdrawableTreeBuilder.treeForApp(application, user) } returns tree

      val result = withdrawableService.withdrawApplication(application.id, user, withdrawalReason, withdrawalOtherReason)

      assertThat(result is CasResult.GeneralValidationError).isTrue()
      assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Application is not in a withdrawable state")
    }
  }

  @Nested
  inner class WithdrawPlacementRequest {
    val withdrawalReason = PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withAllocatedToUser(user)
      .withApplication(application)
      .produce()

    val placementRequest = PlacementRequestEntityFactory()
      .withApplication(application)
      .withPlacementRequirements(
        PlacementRequirementsEntityFactory()
          .withApplication(application)
          .withAssessment(assessment)
          .produce(),
      )
      .withAssessment(assessment)
      .produce()

    @Test
    fun success() {
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest

      val tree = WithdrawableTreeNode(
        WithdrawableEntityType.PlacementRequest,
        placementRequest.id,
        WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
      )

      every { withdrawableTreeBuilder.treeForPlacementReq(placementRequest, user) } returns tree

      every {
        placementRequestService.withdrawPlacementRequest(any(), any(), any())
      } returns CasResult.Success(
        PlacementRequestService.PlacementRequestAndCancellations(
          placementRequest,
          emptyList(),
        ),
      )

      val context = WithdrawalContext(user, WithdrawableEntityType.PlacementRequest, placementRequest.id)

      every {
        withdrawableTreeOperations.withdrawDescendantsOfRootNode(tree, context)
      } returns Unit

      val result = withdrawableService.withdrawPlacementRequest(placementRequest.id, user, withdrawalReason)

      assertThat(result is CasResult.Success)

      verify {
        placementRequestService.withdrawPlacementRequest(
          placementRequest.id,
          withdrawalReason,
          context,
        )
      }

      verify {
        withdrawableTreeOperations.withdrawDescendantsOfRootNode(tree, context)
      }
    }

    @Test
    fun `fails if user may not directly withdraw()`() {
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest

      val tree = WithdrawableTreeNode(
        WithdrawableEntityType.PlacementRequest,
        placementRequest.id,
        WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false),
      )

      every { withdrawableTreeBuilder.treeForPlacementReq(placementRequest, user) } returns tree

      val result = withdrawableService.withdrawPlacementRequest(placementRequest.id, user, withdrawalReason)

      assertThat(result is CasResult.Unauthorised).isTrue()
    }

    @Test
    fun `fails if not withdrawable()`() {
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest

      val tree = WithdrawableTreeNode(
        WithdrawableEntityType.PlacementRequest,
        placementRequest.id,
        WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = true),
      )

      every { withdrawableTreeBuilder.treeForPlacementReq(placementRequest, user) } returns tree

      val result = withdrawableService.withdrawPlacementRequest(placementRequest.id, user, withdrawalReason)

      assertThat(result is CasResult.GeneralValidationError).isTrue()
      assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Request for Placement is not in a withdrawable state")
    }
  }

  @Nested
  inner class WithdrawPlacementApplication {

    val withdrawalReason = PlacementApplicationWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST

    val placementApplication = PlacementApplicationEntityFactory()
      .withCreatedByUser(user)
      .withApplication(application)
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    @Test
    fun success() {
      every { placementApplicationService.getApplicationOrNull(placementApplication.id) } returns placementApplication

      val tree = WithdrawableTreeNode(
        WithdrawableEntityType.PlacementApplication,
        placementApplication.id,
        WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
      )

      every { withdrawableTreeBuilder.treeForPlacementApp(placementApplication, user) } returns tree

      every {
        placementApplicationService.withdrawPlacementApplication(any(), any(), any())
      } returns CasResult.Success(placementApplication)

      val context = WithdrawalContext(user, WithdrawableEntityType.PlacementApplication, placementApplication.id)

      every {
        withdrawableTreeOperations.withdrawDescendantsOfRootNode(tree, context)
      } returns Unit

      val result = withdrawableService.withdrawPlacementApplication(placementApplication.id, user, withdrawalReason)

      assertThat(result is CasResult.Success)

      verify {
        placementApplicationService.withdrawPlacementApplication(
          placementApplication.id,
          withdrawalReason,
          context,
        )
      }

      verify {
        withdrawableTreeOperations.withdrawDescendantsOfRootNode(tree, context)
      }
    }

    @Test
    fun `fails if user may not directly withdraw()`() {
      every { placementApplicationService.getApplicationOrNull(placementApplication.id) } returns placementApplication

      val tree = WithdrawableTreeNode(
        WithdrawableEntityType.PlacementApplication,
        placementApplication.id,
        WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false),
      )

      every { withdrawableTreeBuilder.treeForPlacementApp(placementApplication, user) } returns tree

      val result = withdrawableService.withdrawPlacementApplication(placementApplication.id, user, withdrawalReason)

      assertThat(result is CasResult.Unauthorised).isTrue()
    }

    @Test
    fun `fails if not withdrawable()`() {
      every { placementApplicationService.getApplicationOrNull(placementApplication.id) } returns placementApplication

      val tree = WithdrawableTreeNode(
        WithdrawableEntityType.PlacementApplication,
        placementApplication.id,
        WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = true),
      )

      every { withdrawableTreeBuilder.treeForPlacementApp(placementApplication, user) } returns tree

      val result = withdrawableService.withdrawPlacementApplication(placementApplication.id, user, withdrawalReason)

      assertThat(result is CasResult.GeneralValidationError).isTrue()
      assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Request for Placement is not in a withdrawable state")
    }
  }

  @Nested
  inner class WithdrawBooking {

    val cancelledAt = LocalDate.now()
    val userProvidedReason = UUID.randomUUID()
    val notes = "The Notes"

    val booking = BookingEntityFactory()
      .withApplication(application)
      .withDefaultPremises()
      .produce()

    @Test
    fun success() {
      val tree = WithdrawableTreeNode(
        WithdrawableEntityType.Booking,
        booking.id,
        WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
      )

      every { withdrawableTreeBuilder.treeForBooking(booking, user) } returns tree

      every {
        bookingService.createCas1Cancellation(any(), any(), any(), any(), any())
      } returns CasResult.Success(CancellationEntityFactory().withDefaults().withBooking(booking).produce())

      val context = WithdrawalContext(user, WithdrawableEntityType.Booking, booking.id)

      every {
        withdrawableTreeOperations.withdrawDescendantsOfRootNode(tree, context)
      } returns Unit

      val result = withdrawableService.withdrawBooking(booking, user, cancelledAt, userProvidedReason, notes)

      assertThat(result is CasResult.Success)

      verify {
        bookingService.createCas1Cancellation(
          booking,
          cancelledAt,
          userProvidedReason,
          notes,
          context,
        )
      }

      verify {
        withdrawableTreeOperations.withdrawDescendantsOfRootNode(tree, context)
      }
    }

    @Test
    fun `fails if user may not directly withdraw()`() {
      val tree = WithdrawableTreeNode(
        WithdrawableEntityType.Booking,
        booking.id,
        WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false),
      )

      every { withdrawableTreeBuilder.treeForBooking(booking, user) } returns tree

      val result = withdrawableService.withdrawBooking(booking, user, cancelledAt, userProvidedReason, notes)

      assertThat(result is CasResult.Unauthorised).isTrue()
    }

    @Test
    fun `fails if not withdrawable()`() {
      val tree = WithdrawableTreeNode(
        WithdrawableEntityType.Booking,
        booking.id,
        WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = true),
      )

      every { withdrawableTreeBuilder.treeForBooking(booking, user) } returns tree

      val result = withdrawableService.withdrawBooking(booking, user, cancelledAt, userProvidedReason, notes)

      assertThat(result is CasResult.GeneralValidationError).isTrue()
      assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Placement is not in a withdrawable state")
    }
  }
}
