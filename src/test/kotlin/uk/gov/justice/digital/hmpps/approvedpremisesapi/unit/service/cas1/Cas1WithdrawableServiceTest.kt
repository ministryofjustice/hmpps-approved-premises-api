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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableTreeBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableTreeOperations
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableDatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableTreeNode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalContext
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas1WithdrawableServiceTest {
  private val applicationService = mockk<ApplicationService>()
  private val placementRequestService = mockk<PlacementRequestService>()
  private val placementApplicationService = mockk<PlacementApplicationService>()
  private val bookingService = mockk<BookingService>()
  private val cas1WithdrawableTreeBuilder = mockk<Cas1WithdrawableTreeBuilder>()
  private val cas1WithdrawableTreeOperations = mockk<Cas1WithdrawableTreeOperations>()

  private val withdrawableService = WithdrawableService(
    applicationService,
    placementRequestService,
    placementApplicationService,
    bookingService,
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
  fun `allWithdrawables correctly maps for given tree node`() {
    val appId = UUID.randomUUID()

    every {
      cas1WithdrawableTreeBuilder.treeForApp(application, user)
    } returns
      WithdrawableTreeNode(
        applicationId = application.id,
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
      cas1WithdrawableTreeBuilder.treeForApp(application, user)
    } returns
      WithdrawableTreeNode(
        applicationId = application.id,
        entityType = WithdrawableEntityType.Application,
        entityId = appWithdrawableId,
        status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
        children = listOf(
          WithdrawableTreeNode(
            applicationId = application.id,
            entityType = WithdrawableEntityType.PlacementRequest,
            entityId = placementRequest1WithdrawableId,
            status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
            children = listOf(
              WithdrawableTreeNode(
                applicationId = application.id,
                entityType = WithdrawableEntityType.Booking,
                entityId = placementWithdrawableId,
                status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
              ),
            ),
          ),
          WithdrawableTreeNode(
            applicationId = application.id,
            entityType = WithdrawableEntityType.PlacementRequest,
            entityId = placementRequestWithdrawableButNotPermittedId,
            status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false),
          ),
          WithdrawableTreeNode(
            applicationId = application.id,
            entityType = WithdrawableEntityType.PlacementApplication,
            entityId = placementApplication1WithdrawableId,
            status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
            children = listOf(
              WithdrawableTreeNode(
                applicationId = application.id,
                entityType = WithdrawableEntityType.PlacementRequest,
                entityId = placementRequest2WithdrawableId,
                status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
              ),
            ),
          ),
          WithdrawableTreeNode(
            applicationId = application.id,
            entityType = WithdrawableEntityType.PlacementApplication,
            entityId = placementApplication2NotWithdrawableId,
            status = WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = true),
          ),
          WithdrawableTreeNode(
            applicationId = application.id,
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
  fun `allWithdrawables doesn't return entities that are blocking, or ancestors of blocking`() {
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
      WithdrawableTreeNode(
        applicationId = application.id,
        entityType = WithdrawableEntityType.Application,
        entityId = appWithdrawableButBlockedId,
        status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
        children = listOf(
          WithdrawableTreeNode(
            applicationId = application.id,
            entityType = WithdrawableEntityType.PlacementRequest,
            entityId = placementRequest1WithdrawableButBlockedId,
            status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
            children = listOf(
              WithdrawableTreeNode(
                applicationId = application.id,
                entityType = WithdrawableEntityType.Booking,
                entityId = placementWithdrawableButBlockingId,
                status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true, blockAncestorWithdrawals = true),
              ),
            ),
          ),
          WithdrawableTreeNode(
            applicationId = application.id,
            entityType = WithdrawableEntityType.PlacementRequest,
            entityId = placementRequestWithdrawableButNotPermittedId,
            status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false),
          ),
          WithdrawableTreeNode(
            applicationId = application.id,
            entityType = WithdrawableEntityType.PlacementApplication,
            entityId = placementApplication1WithdrawableId,
            status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
            children = listOf(
              WithdrawableTreeNode(
                applicationId = application.id,
                entityType = WithdrawableEntityType.PlacementRequest,
                entityId = placementRequest2WithdrawableId,
                status = WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true, blockAncestorWithdrawals = false),
              ),
            ),
          ),
          WithdrawableTreeNode(
            applicationId = application.id,
            entityType = WithdrawableEntityType.PlacementApplication,
            entityId = placementApplication2NotWithdrawableId,
            status = WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = true),
          ),
          WithdrawableTreeNode(
            applicationId = application.id,
            entityType = WithdrawableEntityType.Booking,
            entityId = placementNotWithdrawableId,
            status = WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = true),
          ),
        ),
      )

    val result = withdrawableService.allWithdrawables(application, user)

    assertThat(result).hasSize(2)
    assertThat(result).anyMatch { it.id == placementRequest2WithdrawableId }
    assertThat(result).anyMatch { it.id == placementApplication1WithdrawableId }
  }

  @Nested
  inner class WithdrawApplication {

    private val withdrawalReason = "the reason"
    private val withdrawalOtherReason = "the other reason"

    @Test
    fun success() {
      every { applicationService.getApplication(application.id) } returns application

      val tree = WithdrawableTreeNode(
        applicationId = application.id,
        WithdrawableEntityType.Application,
        application.id,
        WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
      )

      every { cas1WithdrawableTreeBuilder.treeForApp(application, user) } returns tree

      every {
        applicationService.withdrawApprovedPremisesApplication(any(), any(), any(), any())
      } returns CasResult.Success(Unit)

      every {
        cas1WithdrawableTreeOperations.withdrawDescendantsOfRootNode(
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
        cas1WithdrawableTreeOperations.withdrawDescendantsOfRootNode(
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
        applicationId = application.id,
        WithdrawableEntityType.Application,
        application.id,
        WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false),
      )

      every { cas1WithdrawableTreeBuilder.treeForApp(application, user) } returns tree

      val result = withdrawableService.withdrawApplication(application.id, user, withdrawalReason, withdrawalOtherReason)

      assertThat(result is CasResult.Unauthorised).isTrue()
    }

    @Test
    fun `fails if not withdrawable()`() {
      every { applicationService.getApplication(application.id) } returns application

      val tree = WithdrawableTreeNode(
        applicationId = application.id,
        WithdrawableEntityType.Application,
        application.id,
        WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = true),
      )

      every { cas1WithdrawableTreeBuilder.treeForApp(application, user) } returns tree

      val result = withdrawableService.withdrawApplication(application.id, user, withdrawalReason, withdrawalOtherReason)

      assertThat(result is CasResult.GeneralValidationError).isTrue()
      assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Application is not in a withdrawable state")
    }

    @Test
    fun `fails if blocked()`() {
      every { applicationService.getApplication(application.id) } returns application

      val tree = WithdrawableTreeNode(
        applicationId = application.id,
        WithdrawableEntityType.Application,
        application.id,
        WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
        children = listOf(
          WithdrawableTreeNode(
            applicationId = application.id,
            WithdrawableEntityType.Booking,
            UUID.randomUUID(),
            WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true, blockAncestorWithdrawals = true),
          ),
        ),
      )

      every { cas1WithdrawableTreeBuilder.treeForApp(application, user) } returns tree

      val result = withdrawableService.withdrawApplication(application.id, user, withdrawalReason, withdrawalOtherReason)

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

      val tree = WithdrawableTreeNode(
        applicationId = application.id,
        WithdrawableEntityType.PlacementRequest,
        placementRequest.id,
        WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
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

      val context = WithdrawalContext(user, WithdrawableEntityType.PlacementRequest, placementRequest.id)

      every {
        cas1WithdrawableTreeOperations.withdrawDescendantsOfRootNode(tree, context)
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
        cas1WithdrawableTreeOperations.withdrawDescendantsOfRootNode(tree, context)
      }
    }

    @Test
    fun `fails if user may not directly withdraw()`() {
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest

      val tree = WithdrawableTreeNode(
        applicationId = application.id,
        WithdrawableEntityType.PlacementRequest,
        placementRequest.id,
        WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false),
      )

      every { cas1WithdrawableTreeBuilder.treeForPlacementReq(placementRequest, user) } returns tree

      val result = withdrawableService.withdrawPlacementRequest(placementRequest.id, user, withdrawalReason)

      assertThat(result is CasResult.Unauthorised).isTrue()
    }

    @Test
    fun `fails if not withdrawable()`() {
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest

      val tree = WithdrawableTreeNode(
        applicationId = application.id,
        WithdrawableEntityType.PlacementRequest,
        placementRequest.id,
        WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = true),
      )

      every { cas1WithdrawableTreeBuilder.treeForPlacementReq(placementRequest, user) } returns tree

      val result = withdrawableService.withdrawPlacementRequest(placementRequest.id, user, withdrawalReason)

      assertThat(result is CasResult.GeneralValidationError).isTrue()
      assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Request for Placement is not in a withdrawable state")
    }

    @Test
    fun `fails if blocked()`() {
      every { placementRequestService.getPlacementRequestOrNull(placementRequest.id) } returns placementRequest

      val tree = WithdrawableTreeNode(
        applicationId = application.id,
        WithdrawableEntityType.PlacementRequest,
        placementRequest.id,
        WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
        children = listOf(
          WithdrawableTreeNode(
            applicationId = application.id,
            WithdrawableEntityType.Booking,
            UUID.randomUUID(),
            WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true, blockAncestorWithdrawals = true),
          ),
        ),
      )

      every { cas1WithdrawableTreeBuilder.treeForPlacementReq(placementRequest, user) } returns tree

      val result = withdrawableService.withdrawPlacementRequest(placementRequest.id, user, withdrawalReason)

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
      every { placementApplicationService.getApplicationOrNull(placementApplication.id) } returns placementApplication

      val tree = WithdrawableTreeNode(
        applicationId = application.id,
        WithdrawableEntityType.PlacementApplication,
        placementApplication.id,
        WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
      )

      every { cas1WithdrawableTreeBuilder.treeForPlacementApp(placementApplication, user) } returns tree

      every {
        placementApplicationService.withdrawPlacementApplication(any(), any(), any())
      } returns CasResult.Success(placementApplication)

      val context = WithdrawalContext(user, WithdrawableEntityType.PlacementApplication, placementApplication.id)

      every {
        cas1WithdrawableTreeOperations.withdrawDescendantsOfRootNode(tree, context)
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
        cas1WithdrawableTreeOperations.withdrawDescendantsOfRootNode(tree, context)
      }
    }

    @Test
    fun `fails if user may not directly withdraw()`() {
      every { placementApplicationService.getApplicationOrNull(placementApplication.id) } returns placementApplication

      val tree = WithdrawableTreeNode(
        applicationId = application.id,
        WithdrawableEntityType.PlacementApplication,
        placementApplication.id,
        WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false),
      )

      every { cas1WithdrawableTreeBuilder.treeForPlacementApp(placementApplication, user) } returns tree

      val result = withdrawableService.withdrawPlacementApplication(placementApplication.id, user, withdrawalReason)

      assertThat(result is CasResult.Unauthorised).isTrue()
    }

    @Test
    fun `fails if not withdrawable()`() {
      every { placementApplicationService.getApplicationOrNull(placementApplication.id) } returns placementApplication

      val tree = WithdrawableTreeNode(
        applicationId = application.id,
        WithdrawableEntityType.PlacementApplication,
        placementApplication.id,
        WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = true),
      )

      every { cas1WithdrawableTreeBuilder.treeForPlacementApp(placementApplication, user) } returns tree

      val result = withdrawableService.withdrawPlacementApplication(placementApplication.id, user, withdrawalReason)

      assertThat(result is CasResult.GeneralValidationError).isTrue()
      assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Request for Placement is not in a withdrawable state")
    }

    @Test
    fun `fails if blocked()`() {
      every { placementApplicationService.getApplicationOrNull(placementApplication.id) } returns placementApplication

      val tree = WithdrawableTreeNode(
        applicationId = application.id,
        WithdrawableEntityType.PlacementApplication,
        placementApplication.id,
        WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
        children = listOf(
          WithdrawableTreeNode(
            applicationId = application.id,
            WithdrawableEntityType.PlacementApplication,
            placementApplication.id,
            WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true, blockAncestorWithdrawals = true),
          ),
        ),
      )

      every { cas1WithdrawableTreeBuilder.treeForPlacementApp(placementApplication, user) } returns tree

      val result = withdrawableService.withdrawPlacementApplication(placementApplication.id, user, withdrawalReason)

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
      val tree = WithdrawableTreeNode(
        applicationId = application.id,
        WithdrawableEntityType.Booking,
        booking.id,
        WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true),
      )

      every { cas1WithdrawableTreeBuilder.treeForBooking(booking, user) } returns tree

      every {
        bookingService.createCas1Cancellation(any(), any(), any(), any(), any(), any())
      } returns CasResult.Success(CancellationEntityFactory().withDefaults().withBooking(booking).produce())

      val context = WithdrawalContext(user, WithdrawableEntityType.Booking, booking.id)

      every {
        cas1WithdrawableTreeOperations.withdrawDescendantsOfRootNode(tree, context)
      } returns Unit

      val result = withdrawableService.withdrawBooking(booking, user, cancelledAt, userProvidedReason, notes, otherReason)

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
        cas1WithdrawableTreeOperations.withdrawDescendantsOfRootNode(tree, context)
      }
    }

    @Test
    fun `fails if user may not directly withdraw()`() {
      val tree = WithdrawableTreeNode(
        applicationId = application.id,
        WithdrawableEntityType.Booking,
        booking.id,
        WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false),
      )

      every { cas1WithdrawableTreeBuilder.treeForBooking(booking, user) } returns tree

      val result = withdrawableService.withdrawBooking(booking, user, cancelledAt, userProvidedReason, notes, otherReason)

      assertThat(result is CasResult.Unauthorised).isTrue()
    }

    @Test
    fun `fails if not withdrawable()`() {
      val tree = WithdrawableTreeNode(
        applicationId = application.id,
        WithdrawableEntityType.Booking,
        booking.id,
        WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = true),
      )

      every { cas1WithdrawableTreeBuilder.treeForBooking(booking, user) } returns tree

      val result = withdrawableService.withdrawBooking(booking, user, cancelledAt, userProvidedReason, notes, otherReason)

      assertThat(result is CasResult.GeneralValidationError).isTrue()
      assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Placement is not in a withdrawable state")
    }

    @Test
    fun `fails if blocked()`() {
      val tree = WithdrawableTreeNode(
        applicationId = application.id,
        WithdrawableEntityType.Booking,
        booking.id,
        WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true, blockAncestorWithdrawals = true),
      )

      every { cas1WithdrawableTreeBuilder.treeForBooking(booking, user) } returns tree

      val result = withdrawableService.withdrawBooking(booking, user, cancelledAt, userProvidedReason, notes, otherReason)

      assertThat(result is CasResult.GeneralValidationError).isTrue()
      assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Placement withdrawal is blocked")
    }
  }
}
