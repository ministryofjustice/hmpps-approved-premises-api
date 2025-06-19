package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.Cas1PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.BlockingReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableTreeBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableTreeOperations
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableDatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableTree
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableTreeNode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredByUser
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class Cas1WithdrawableServiceTest {
  private val applicationService = mockk<ApplicationService>()
  private val placementRequestService = mockk<PlacementRequestService>()
  private val cas1PlacementApplicationService = mockk<Cas1PlacementApplicationService>()
  private val bookingService = mockk<BookingService>()
  private val cas1SpaceBookingService = mockk<Cas1SpaceBookingService>()
  private val cas1WithdrawableTreeBuilder = mockk<Cas1WithdrawableTreeBuilder>()
  private val cas1WithdrawableTreeOperations = mockk<Cas1WithdrawableTreeOperations>()

  private val cas1WithdrawableService = Cas1WithdrawableService(
    applicationService,
    placementRequestService,
    cas1PlacementApplicationService,
    bookingService,
    cas1SpaceBookingService,
    cas1WithdrawableTreeBuilder,
    cas1WithdrawableTreeOperations,
  )

  val probationRegion = ProbationRegionEntityFactory()
    .withYieldedApArea { ApAreaEntityFactory().produce() }
    .produce()

  val user = UserEntityFactory().withProbationRegion(probationRegion).produce()

  val application = ApprovedPremisesApplicationEntityFactory()
    .withCreatedByUser(user)
    .produce()

  @Test
  fun `allDirectlyWithdrawables correctly maps for given tree node`() {
    val appId = UUID.randomUUID()

    every {
      cas1WithdrawableTreeBuilder.treeForApp(application, user)
    } returns
      WithdrawableTree(
        rootNode = WithdrawableTreeNode(
          applicationId = application.id,
          entityType = WithdrawableEntityType.PlacementApplication,
          entityId = appId,
          status = WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true),
          dates = listOf(
            WithdrawableDatePeriod(LocalDate.of(2021, 1, 2), LocalDate.of(2021, 2, 3)),
            WithdrawableDatePeriod(LocalDate.of(2021, 3, 4), LocalDate.of(2021, 4, 5)),
          ),
        ),
      )

    val result = cas1WithdrawableService.allDirectlyWithdrawables(application, user).withdrawables

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
  fun `allDirectlyWithdrawables only returns entities the user can withdraw`() {
    val appWithdrawableId = UUID.randomUUID()
    val placementRequest1WithdrawableId = UUID.randomUUID()
    val placementRequestWithdrawableButNotPermittedId = UUID.randomUUID()
    val placementRequest2WithdrawableId = UUID.randomUUID()
    val placementApplication1WithdrawableId = UUID.randomUUID()
    val placementApplication2NotWithdrawableId = UUID.randomUUID()
    val placementWithdrawableId = UUID.randomUUID()
    val placementNotWithdrawableId = UUID.randomUUID()

    every {
      cas1WithdrawableTreeBuilder.treeForApp(application, user)
    } returns
      WithdrawableTree(
        rootNode = WithdrawableTreeNode(
          applicationId = application.id,
          entityType = WithdrawableEntityType.Application,
          entityId = appWithdrawableId,
          status = WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true),
          children = listOf(
            WithdrawableTreeNode(
              applicationId = application.id,
              entityType = WithdrawableEntityType.PlacementRequest,
              entityId = placementRequest1WithdrawableId,
              status = WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true),
              children = listOf(
                WithdrawableTreeNode(
                  applicationId = application.id,
                  entityType = WithdrawableEntityType.Booking,
                  entityId = placementWithdrawableId,
                  status = WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true),
                ),
              ),
            ),
            WithdrawableTreeNode(
              applicationId = application.id,
              entityType = WithdrawableEntityType.PlacementRequest,
              entityId = placementRequestWithdrawableButNotPermittedId,
              status = WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = false),
            ),
            WithdrawableTreeNode(
              applicationId = application.id,
              entityType = WithdrawableEntityType.PlacementApplication,
              entityId = placementApplication1WithdrawableId,
              status = WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true),
              children = listOf(
                WithdrawableTreeNode(
                  applicationId = application.id,
                  entityType = WithdrawableEntityType.PlacementRequest,
                  entityId = placementRequest2WithdrawableId,
                  status = WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true),
                ),
              ),
            ),
            WithdrawableTreeNode(
              applicationId = application.id,
              entityType = WithdrawableEntityType.PlacementApplication,
              entityId = placementApplication2NotWithdrawableId,
              status = WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = true),
            ),
            WithdrawableTreeNode(
              applicationId = application.id,
              entityType = WithdrawableEntityType.Booking,
              entityId = placementNotWithdrawableId,
              status = WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = true),
            ),
          ),
        ),
      )

    val result = cas1WithdrawableService.allDirectlyWithdrawables(application, user).withdrawables

    assertThat(result).hasSize(5)
    assertThat(result).anyMatch { it.id == appWithdrawableId }
    assertThat(result).anyMatch { it.id == placementRequest1WithdrawableId }
    assertThat(result).anyMatch { it.id == placementRequest2WithdrawableId }
    assertThat(result).anyMatch { it.id == placementApplication1WithdrawableId }
    assertThat(result).anyMatch { it.id == placementWithdrawableId }
  }

  @Test
  fun `allDirectlyWithdrawables doesn't return entities that are blocking, or ancestors of blocking`() {
    val appWithdrawableButBlockedId = UUID.randomUUID()
    val placementRequest1WithdrawableButBlockedId = UUID.randomUUID()
    val placementRequestWithdrawableButNotPermittedId = UUID.randomUUID()
    val placementRequest2WithdrawableId = UUID.randomUUID()
    val placementApplication1WithdrawableId = UUID.randomUUID()
    val placementApplication2NotWithdrawableId = UUID.randomUUID()
    val placementWithdrawableButBlockingId = UUID.randomUUID()
    val placementNotWithdrawableId = UUID.randomUUID()

    every {
      cas1WithdrawableTreeBuilder.treeForApp(application, user)
    } returns
      WithdrawableTree(
        rootNode = WithdrawableTreeNode(
          applicationId = application.id,
          entityType = WithdrawableEntityType.Application,
          entityId = appWithdrawableButBlockedId,
          status = WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true),
          children = listOf(
            WithdrawableTreeNode(
              applicationId = application.id,
              entityType = WithdrawableEntityType.PlacementRequest,
              entityId = placementRequest1WithdrawableButBlockedId,
              status = WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true),
              children = listOf(
                WithdrawableTreeNode(
                  applicationId = application.id,
                  entityType = WithdrawableEntityType.Booking,
                  entityId = placementWithdrawableButBlockingId,
                  status = WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true, blockingReason = BlockingReason.ArrivalRecordedInCas1),
                ),
              ),
            ),
            WithdrawableTreeNode(
              applicationId = application.id,
              entityType = WithdrawableEntityType.PlacementRequest,
              entityId = placementRequestWithdrawableButNotPermittedId,
              status = WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = false),
            ),
            WithdrawableTreeNode(
              applicationId = application.id,
              entityType = WithdrawableEntityType.PlacementApplication,
              entityId = placementApplication1WithdrawableId,
              status = WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true),
              children = listOf(
                WithdrawableTreeNode(
                  applicationId = application.id,
                  entityType = WithdrawableEntityType.PlacementRequest,
                  entityId = placementRequest2WithdrawableId,
                  status = WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true, blockingReason = null),
                ),
              ),
            ),
            WithdrawableTreeNode(
              applicationId = application.id,
              entityType = WithdrawableEntityType.PlacementApplication,
              entityId = placementApplication2NotWithdrawableId,
              status = WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = true),
            ),
            WithdrawableTreeNode(
              applicationId = application.id,
              entityType = WithdrawableEntityType.Booking,
              entityId = placementNotWithdrawableId,
              status = WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = true),
            ),
          ),
        ),
      )

    val result = cas1WithdrawableService.allDirectlyWithdrawables(application, user)

    assertThat(result.notes).contains("1 or more placements cannot be withdrawn as they have an arrival")

    val withdrawables = result.withdrawables
    assertThat(withdrawables).hasSize(2)
    assertThat(withdrawables).anyMatch { it.id == placementRequest2WithdrawableId }
    assertThat(withdrawables).anyMatch { it.id == placementApplication1WithdrawableId }
  }

  @Nested
  inner class WithdrawApplication {

    private val withdrawalReason = "the reason"
    private val withdrawalOtherReason = "the other reason"

    @Test
    fun success() {
      every { applicationService.getApplication(application.id) } returns application

      val tree = WithdrawableTree(
        rootNode = WithdrawableTreeNode(
          applicationId = application.id,
          WithdrawableEntityType.Application,
          application.id,
          WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true),
        ),
      )

      every { cas1WithdrawableTreeBuilder.treeForApp(application, user) } returns tree

      every {
        applicationService.withdrawApprovedPremisesApplication(any(), any(), any(), any())
      } returns CasResult.Success(Unit)

      every {
        cas1WithdrawableTreeOperations.withdrawDescendantsOfRootNode(
          tree.rootNode,
          WithdrawalContext(WithdrawalTriggeredByUser(user), WithdrawableEntityType.Application, application.id),
        )
      } returns Unit

      val result = cas1WithdrawableService.withdrawApplication(application.id, user, withdrawalReason, withdrawalOtherReason)

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
        cas1WithdrawableTreeOperations.withdrawDescendantsOfRootNode(
          tree.rootNode,
          WithdrawalContext(WithdrawalTriggeredByUser(user), WithdrawableEntityType.Application, application.id),
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

      val result = cas1WithdrawableService.withdrawApplication(application.id, user, withdrawalReason, withdrawalOtherReason)

      assertThat(result is CasResult.GeneralValidationError)
    }

    @Test
    fun `fails if user may not directly withdraw()`() {
      every { applicationService.getApplication(application.id) } returns application

      val tree = WithdrawableTree(
        rootNode = WithdrawableTreeNode(
          applicationId = application.id,
          WithdrawableEntityType.Application,
          application.id,
          WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = false),
        ),
      )

      every { cas1WithdrawableTreeBuilder.treeForApp(application, user) } returns tree

      val result = cas1WithdrawableService.withdrawApplication(application.id, user, withdrawalReason, withdrawalOtherReason)

      assertThat(result is CasResult.Unauthorised).isTrue()
    }

    @Test
    fun `fails if not withdrawable()`() {
      every { applicationService.getApplication(application.id) } returns application

      val tree = WithdrawableTree(
        rootNode = WithdrawableTreeNode(
          applicationId = application.id,
          WithdrawableEntityType.Application,
          application.id,
          WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = true),
        ),
      )

      every { cas1WithdrawableTreeBuilder.treeForApp(application, user) } returns tree

      val result = cas1WithdrawableService.withdrawApplication(application.id, user, withdrawalReason, withdrawalOtherReason)

      assertThat(result is CasResult.GeneralValidationError).isTrue()
      assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Application is not in a withdrawable state")
    }

    @Test
    fun `fails if blocked()`() {
      every { applicationService.getApplication(application.id) } returns application

      val tree = WithdrawableTree(
        rootNode = WithdrawableTreeNode(
          applicationId = application.id,
          WithdrawableEntityType.Application,
          application.id,
          WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true),
          children = listOf(
            WithdrawableTreeNode(
              applicationId = application.id,
              WithdrawableEntityType.Booking,
              UUID.randomUUID(),
              WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true, blockingReason = BlockingReason.ArrivalRecordedInCas1),
            ),
          ),
        ),
      )

      every { cas1WithdrawableTreeBuilder.treeForApp(application, user) } returns tree

      val result = cas1WithdrawableService.withdrawApplication(application.id, user, withdrawalReason, withdrawalOtherReason)

      assertThat(result is CasResult.GeneralValidationError).isTrue()
      assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Application withdrawal is blocked")
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

      val tree = WithdrawableTree(
        WithdrawableTreeNode(
          applicationId = application.id,
          WithdrawableEntityType.PlacementRequest,
          placementRequest.id,
          WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true),
        ),
      )

      every { cas1WithdrawableTreeBuilder.treeForPlacementReq(placementRequest, user) } returns tree

      every {
        placementRequestService.withdrawPlacementRequest(any(), any(), any())
      } returns CasResult.Success(
        PlacementRequestService.PlacementRequestAndCancellations(
          placementRequest,
          emptyList(),
        ),
      )

      val context = WithdrawalContext(WithdrawalTriggeredByUser(user), WithdrawableEntityType.PlacementRequest, placementRequest.id)

      every {
        cas1WithdrawableTreeOperations.withdrawDescendantsOfRootNode(tree.rootNode, context)
      } returns Unit

      val result = cas1WithdrawableService.withdrawPlacementRequest(placementRequest.id, user, withdrawalReason)

      assertThat(result is CasResult.Success)

      verify {
        placementRequestService.withdrawPlacementRequest(
          placementRequest.id,
          withdrawalReason,
          context,
        )
      }

      verify {
        cas1WithdrawableTreeOperations.withdrawDescendantsOfRootNode(tree.rootNode, context)
      }
    }

    @Test
    fun `fails if user may not directly withdraw()`() {
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest

      val tree = WithdrawableTree(
        WithdrawableTreeNode(
          applicationId = application.id,
          WithdrawableEntityType.PlacementRequest,
          placementRequest.id,
          WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = false),
        ),
      )

      every { cas1WithdrawableTreeBuilder.treeForPlacementReq(placementRequest, user) } returns tree

      val result = cas1WithdrawableService.withdrawPlacementRequest(placementRequest.id, user, withdrawalReason)

      assertThat(result is CasResult.Unauthorised).isTrue()
    }

    @Test
    fun `fails if not withdrawable()`() {
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest

      val tree = WithdrawableTree(
        WithdrawableTreeNode(
          applicationId = application.id,
          WithdrawableEntityType.PlacementRequest,
          placementRequest.id,
          WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = true),
        ),
      )

      every { cas1WithdrawableTreeBuilder.treeForPlacementReq(placementRequest, user) } returns tree

      val result = cas1WithdrawableService.withdrawPlacementRequest(placementRequest.id, user, withdrawalReason)

      assertThat(result is CasResult.GeneralValidationError).isTrue()
      assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Request for Placement is not in a withdrawable state")
    }

    @Test
    fun `fails if blocked()`() {
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest

      val tree = WithdrawableTree(
        WithdrawableTreeNode(
          applicationId = application.id,
          WithdrawableEntityType.PlacementRequest,
          placementRequest.id,
          WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true),
          children = listOf(
            WithdrawableTreeNode(
              applicationId = application.id,
              WithdrawableEntityType.Booking,
              UUID.randomUUID(),
              WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true, blockingReason = BlockingReason.ArrivalRecordedInCas1),
            ),
          ),
        ),
      )

      every { cas1WithdrawableTreeBuilder.treeForPlacementReq(placementRequest, user) } returns tree

      val result = cas1WithdrawableService.withdrawPlacementRequest(placementRequest.id, user, withdrawalReason)

      assertThat(result is CasResult.GeneralValidationError).isTrue()
      assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Request for Placement withdrawal is blocked")
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
      every { cas1PlacementApplicationService.getApplicationOrNull(placementApplication.id) } returns placementApplication

      val tree = WithdrawableTree(
        WithdrawableTreeNode(
          applicationId = application.id,
          WithdrawableEntityType.PlacementApplication,
          placementApplication.id,
          WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true),
        ),
      )

      every { cas1WithdrawableTreeBuilder.treeForPlacementApp(placementApplication, user) } returns tree

      every {
        cas1PlacementApplicationService.withdrawPlacementApplication(any(), any(), any())
      } returns CasResult.Success(placementApplication)

      val context = WithdrawalContext(WithdrawalTriggeredByUser(user), WithdrawableEntityType.PlacementApplication, placementApplication.id)

      every {
        cas1WithdrawableTreeOperations.withdrawDescendantsOfRootNode(tree.rootNode, context)
      } returns Unit

      val result = cas1WithdrawableService.withdrawPlacementApplication(placementApplication.id, user, withdrawalReason)

      assertThat(result is CasResult.Success)

      verify {
        cas1PlacementApplicationService.withdrawPlacementApplication(
          placementApplication.id,
          withdrawalReason,
          context,
        )
      }

      verify {
        cas1WithdrawableTreeOperations.withdrawDescendantsOfRootNode(tree.rootNode, context)
      }
    }

    @Test
    fun `fails if user may not directly withdraw()`() {
      every { cas1PlacementApplicationService.getApplicationOrNull(placementApplication.id) } returns placementApplication

      val tree = WithdrawableTree(
        WithdrawableTreeNode(
          applicationId = application.id,
          WithdrawableEntityType.PlacementApplication,
          placementApplication.id,
          WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = false),
        ),
      )

      every { cas1WithdrawableTreeBuilder.treeForPlacementApp(placementApplication, user) } returns tree

      val result = cas1WithdrawableService.withdrawPlacementApplication(placementApplication.id, user, withdrawalReason)

      assertThat(result is CasResult.Unauthorised).isTrue()
    }

    @Test
    fun `fails if not withdrawable()`() {
      every { cas1PlacementApplicationService.getApplicationOrNull(placementApplication.id) } returns placementApplication

      val tree = WithdrawableTree(
        WithdrawableTreeNode(
          applicationId = application.id,
          WithdrawableEntityType.PlacementApplication,
          placementApplication.id,
          WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = true),
        ),
      )

      every { cas1WithdrawableTreeBuilder.treeForPlacementApp(placementApplication, user) } returns tree

      val result = cas1WithdrawableService.withdrawPlacementApplication(placementApplication.id, user, withdrawalReason)

      assertThat(result is CasResult.GeneralValidationError).isTrue()
      assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Request for Placement is not in a withdrawable state")
    }

    @Test
    fun `fails if blocked()`() {
      every { cas1PlacementApplicationService.getApplicationOrNull(placementApplication.id) } returns placementApplication

      val tree = WithdrawableTree(
        WithdrawableTreeNode(
          applicationId = application.id,
          WithdrawableEntityType.PlacementApplication,
          placementApplication.id,
          WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true),
          children = listOf(
            WithdrawableTreeNode(
              applicationId = application.id,
              WithdrawableEntityType.PlacementApplication,
              placementApplication.id,
              WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true, blockingReason = BlockingReason.ArrivalRecordedInCas1),
            ),
          ),
        ),
      )

      every { cas1WithdrawableTreeBuilder.treeForPlacementApp(placementApplication, user) } returns tree

      val result = cas1WithdrawableService.withdrawPlacementApplication(placementApplication.id, user, withdrawalReason)

      assertThat(result is CasResult.GeneralValidationError).isTrue()
      assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Request for Placement withdrawal is blocked")
    }
  }

  @Nested
  inner class WithdrawBooking {

    val cancelledAt = LocalDate.now()
    val userProvidedReason = UUID.randomUUID()
    val notes = "The Notes"
    val otherReason = "Other reason"

    val booking = BookingEntityFactory()
      .withApplication(application)
      .withDefaultPremises()
      .produce()

    @Test
    fun success() {
      val tree = WithdrawableTree(
        WithdrawableTreeNode(
          applicationId = application.id,
          WithdrawableEntityType.Booking,
          booking.id,
          WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true),
        ),
      )

      every { cas1WithdrawableTreeBuilder.treeForBooking(booking, user) } returns tree

      every {
        bookingService.createCas1Cancellation(any(), any(), any(), any(), any(), any())
      } returns CasResult.Success(CancellationEntityFactory().withDefaults().withBooking(booking).produce())

      val context = WithdrawalContext(WithdrawalTriggeredByUser(user), WithdrawableEntityType.Booking, booking.id)

      every {
        cas1WithdrawableTreeOperations.withdrawDescendantsOfRootNode(tree.rootNode, context)
      } returns Unit

      val result = cas1WithdrawableService.withdrawBooking(booking, user, cancelledAt, userProvidedReason, notes, otherReason)

      assertThat(result is CasResult.Success)

      verify {
        bookingService.createCas1Cancellation(
          booking,
          cancelledAt,
          userProvidedReason,
          notes,
          otherReason,
          context,
        )
      }

      verify {
        cas1WithdrawableTreeOperations.withdrawDescendantsOfRootNode(tree.rootNode, context)
      }
    }

    @Test
    fun `fails if user may not directly withdraw()`() {
      val tree = WithdrawableTree(
        WithdrawableTreeNode(
          applicationId = application.id,
          WithdrawableEntityType.Booking,
          booking.id,
          WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = false),
        ),
      )

      every { cas1WithdrawableTreeBuilder.treeForBooking(booking, user) } returns tree

      val result = cas1WithdrawableService.withdrawBooking(booking, user, cancelledAt, userProvidedReason, notes, otherReason)

      assertThat(result is CasResult.Unauthorised).isTrue()
    }

    @Test
    fun `fails if not withdrawable()`() {
      val tree = WithdrawableTree(
        WithdrawableTreeNode(
          applicationId = application.id,
          WithdrawableEntityType.Booking,
          booking.id,
          WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = true),
        ),
      )

      every { cas1WithdrawableTreeBuilder.treeForBooking(booking, user) } returns tree

      val result = cas1WithdrawableService.withdrawBooking(booking, user, cancelledAt, userProvidedReason, notes, otherReason)

      assertThat(result is CasResult.GeneralValidationError).isTrue()
      assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Placement is not in a withdrawable state")
    }

    @Test
    fun `fails if blocked()`() {
      val tree = WithdrawableTree(
        WithdrawableTreeNode(
          applicationId = application.id,
          WithdrawableEntityType.Booking,
          booking.id,
          WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true, blockingReason = BlockingReason.ArrivalRecordedInCas1),
        ),
      )

      every { cas1WithdrawableTreeBuilder.treeForBooking(booking, user) } returns tree

      val result = cas1WithdrawableService.withdrawBooking(booking, user, cancelledAt, userProvidedReason, notes, otherReason)

      assertThat(result is CasResult.GeneralValidationError).isTrue()
      assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Placement withdrawal is blocked")
    }
  }

  @Nested
  inner class WithdrawSpaceBooking {
    val cancelledAt = LocalDate.now()
    val userProvidedReason = UUID.randomUUID()
    val otherReason = "Other reason"
    val appealChangeRequestId = UUID.randomUUID()

    val spaceBooking = Cas1SpaceBookingEntityFactory()
      .withApplication(application)
      .produce()

    @Test
    fun success() {
      val tree = WithdrawableTree(
        WithdrawableTreeNode(
          applicationId = application.id,
          WithdrawableEntityType.SpaceBooking,
          spaceBooking.id,
          WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true),
        ),
      )

      every { cas1WithdrawableTreeBuilder.treeForSpaceBooking(spaceBooking, user) } returns tree

      every {
        cas1SpaceBookingService.withdraw(any(), any(), any(), any(), any(), any())
      } returns CasResult.Success(Unit)

      val context = WithdrawalContext(WithdrawalTriggeredByUser(user), WithdrawableEntityType.SpaceBooking, spaceBooking.id)

      every {
        cas1WithdrawableTreeOperations.withdrawDescendantsOfRootNode(tree.rootNode, context)
      } returns Unit

      val result = cas1WithdrawableService.withdrawSpaceBooking(spaceBooking, user, cancelledAt, userProvidedReason, otherReason, appealChangeRequestId)

      assertThat(result is CasResult.Success)

      verify {
        cas1SpaceBookingService.withdraw(spaceBooking, cancelledAt, userProvidedReason, otherReason, appealChangeRequestId, context)
      }

      verify {
        cas1WithdrawableTreeOperations.withdrawDescendantsOfRootNode(tree.rootNode, context)
      }
    }

    @Test
    fun `fails if user may not directly withdraw()`() {
      val tree = WithdrawableTree(
        WithdrawableTreeNode(
          applicationId = application.id,
          WithdrawableEntityType.SpaceBooking,
          spaceBooking.id,
          WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = false),
        ),
      )

      every { cas1WithdrawableTreeBuilder.treeForSpaceBooking(spaceBooking, user) } returns tree

      val result = cas1WithdrawableService.withdrawSpaceBooking(spaceBooking, user, cancelledAt, userProvidedReason, otherReason, appealChangeRequestId)

      assertThat(result is CasResult.Unauthorised).isTrue()
    }

    @Test
    fun `fails if not withdrawable()`() {
      val tree = WithdrawableTree(
        WithdrawableTreeNode(
          applicationId = application.id,
          WithdrawableEntityType.SpaceBooking,
          spaceBooking.id,
          WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = true),
        ),
      )

      every { cas1WithdrawableTreeBuilder.treeForSpaceBooking(spaceBooking, user) } returns tree

      val result = cas1WithdrawableService.withdrawSpaceBooking(spaceBooking, user, cancelledAt, userProvidedReason, otherReason, appealChangeRequestId)

      assertThat(result is CasResult.GeneralValidationError).isTrue()
      assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Space Booking is not in a withdrawable state")
    }

    @Test
    fun `fails if blocked()`() {
      val tree = WithdrawableTree(
        WithdrawableTreeNode(
          applicationId = application.id,
          WithdrawableEntityType.SpaceBooking,
          spaceBooking.id,
          WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true, blockingReason = BlockingReason.ArrivalRecordedInCas1),
        ),
      )

      every { cas1WithdrawableTreeBuilder.treeForSpaceBooking(spaceBooking, user) } returns tree

      val result = cas1WithdrawableService.withdrawSpaceBooking(spaceBooking, user, cancelledAt, userProvidedReason, otherReason, appealChangeRequestId)

      assertThat(result is CasResult.GeneralValidationError).isTrue()
      assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Space Booking withdrawal is blocked")
    }
  }

  @Nested
  inner class IsDirectlyWithdrawableForPlacementApplication {
    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .produce()

    val placementApplication = PlacementApplicationEntityFactory()
      .withDefaults()
      .withApplication(application)
      .produce()

    @Test
    fun `is withdrawable`() {
      every {
        cas1WithdrawableTreeBuilder.treeForPlacementApp(placementApplication, user)
      } returns
        WithdrawableTree(
          WithdrawableTreeNode(
            applicationId = application.id,
            entityType = WithdrawableEntityType.PlacementApplication,
            entityId = placementApplication.id,
            status = WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true),
            dates = emptyList(),
          ),
        )

      val result = cas1WithdrawableService.isDirectlyWithdrawable(placementApplication, user)

      assertThat(result).isTrue()
    }

    @Test
    fun `is not withdrawable`() {
      every {
        cas1WithdrawableTreeBuilder.treeForPlacementApp(placementApplication, user)
      } returns
        WithdrawableTree(
          WithdrawableTreeNode(
            applicationId = application.id,
            entityType = WithdrawableEntityType.PlacementApplication,
            entityId = placementApplication.id,
            status = WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = false),
            dates = emptyList(),
          ),
        )

      val result = cas1WithdrawableService.isDirectlyWithdrawable(placementApplication, user)

      assertThat(result).isFalse()
    }
  }

  @Nested
  inner class IsDirectlyWithdrawableForPlacementRequest {
    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .withSubmittedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS))
      .produce()

    val placementRequirements = PlacementRequirementsEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .produce()

    val placementRequest = PlacementRequestEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .withPlacementRequirements(placementRequirements)
      .produce()

    @Test
    fun `is withdrawable`() {
      every {
        cas1WithdrawableTreeBuilder.treeForPlacementReq(placementRequest, user)
      } returns
        WithdrawableTree(
          WithdrawableTreeNode(
            applicationId = application.id,
            entityType = WithdrawableEntityType.PlacementRequest,
            entityId = placementRequest.id,
            status = WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true),
            dates = emptyList(),
          ),
        )

      val result = cas1WithdrawableService.isDirectlyWithdrawable(placementRequest, user)

      assertThat(result).isTrue()
    }

    @Test
    fun `is not withdrawable`() {
      every {
        cas1WithdrawableTreeBuilder.treeForPlacementReq(placementRequest, user)
      } returns
        WithdrawableTree(
          WithdrawableTreeNode(
            applicationId = application.id,
            entityType = WithdrawableEntityType.PlacementApplication,
            entityId = placementRequest.id,
            status = WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = false),
            dates = emptyList(),
          ),
        )

      val result = cas1WithdrawableService.isDirectlyWithdrawable(placementRequest, user)

      assertThat(result).isFalse()
    }
  }
}
