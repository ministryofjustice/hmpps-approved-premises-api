package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.BlockingReason.ArrivalRecordedInCas1
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.BlockingReason.ArrivalRecordedInDelius
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableTreeBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableState

class Cas1WithdrawableTreeBuilderTest {

  private val placementRequestService = mockk<Cas1PlacementRequestService>()
  private val cas1PlacementApplicationService = mockk<Cas1PlacementApplicationService>()
  private val cas1ApplicationService = mockk<Cas1ApplicationService>()
  private val cas1SpaceBookingService = mockk<Cas1SpaceBookingService>()

  private val service = Cas1WithdrawableTreeBuilder(
    placementRequestService,
    cas1PlacementApplicationService,
    cas1ApplicationService,
    cas1SpaceBookingService,
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

  val approvedPremises = ApprovedPremisesEntityFactory()
    .withProbationRegion(probationRegion)
    .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
    .produce()

  @Test
  fun `tree for app returns all potential elements`() {
    every {
      cas1ApplicationService.getWithdrawableState(application, user)
    } returns WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true)

    val initialPlacementRequest = createPlacementRequest()
    setupWithdrawableState(initialPlacementRequest, WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = false))

    val initialPlacementRequestBooking = createSpaceBooking()
    initialPlacementRequest.spaceBookings = mutableListOf(initialPlacementRequestBooking)
    setupWithdrawableState(initialPlacementRequestBooking, WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = false))

    every {
      placementRequestService.getPlacementRequestForInitialApplicationDates(application.id)
    } returns listOf(
      initialPlacementRequest,
    )

    val placementApp1 = createPlacementApplication()
    setupWithdrawableState(placementApp1, WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = false))
    val placementApp1PlacementRequest1 = createPlacementRequest()
    placementApp1.placementRequests.add(placementApp1PlacementRequest1)
    setupWithdrawableState(placementApp1PlacementRequest1, WithdrawableState(withdrawn = true, withdrawable = false, userMayDirectlyWithdraw = false))
    val placementApp1PlacementRequest1SpaceBooking1 = createSpaceBooking()
    placementApp1PlacementRequest1.spaceBookings.add(placementApp1PlacementRequest1SpaceBooking1)
    setupWithdrawableState(placementApp1PlacementRequest1SpaceBooking1, WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = false))
    val placementApp1PlacementRequest1SpaceBooking2 = createSpaceBooking()
    placementApp1PlacementRequest1.spaceBookings.add(placementApp1PlacementRequest1SpaceBooking2)
    setupWithdrawableState(placementApp1PlacementRequest1SpaceBooking2, WithdrawableState(withdrawn = true, withdrawable = false, userMayDirectlyWithdraw = false))

    val placementApplication2 = createPlacementApplication()
    setupWithdrawableState(placementApplication2, WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true))

    every {
      cas1PlacementApplicationService.getAllSubmittedNonReallocatedApplications(application.id)
    } returns listOf(placementApp1, placementApplication2)

    val result = service.treeForApp(application, user)

    assertThat(result.render(includeIds = false).trim()).isEqualTo(
      """
Application(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:Y
---> PlacementRequest(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N
------> SpaceBooking(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:N
---> PlacementApplication(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N
------> PlacementRequest(), withdrawn:Y, withdrawable:N, mayDirectlyWithdraw:N
---------> SpaceBooking(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:N
---------> SpaceBooking(), withdrawn:Y, withdrawable:N, mayDirectlyWithdraw:N
---> PlacementApplication(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:Y

Notes: []
      """
        .trim(),
    )
  }

  @Test
  fun `tree for app blocks withdrawal if booking has arrival in CAS1`() {
    every {
      cas1ApplicationService.getWithdrawableState(application, user)
    } returns WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true)

    val initialPlacementRequest = createPlacementRequest()
    setupWithdrawableState(initialPlacementRequest, WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = false))

    val initialPlacementRequestBooking = createSpaceBooking()
    initialPlacementRequest.spaceBookings = mutableListOf(initialPlacementRequestBooking)
    setupWithdrawableState(initialPlacementRequestBooking, WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = false))

    every {
      placementRequestService.getPlacementRequestForInitialApplicationDates(application.id)
    } returns listOf(
      initialPlacementRequest,
    )

    val placementApp1 = createPlacementApplication()
    setupWithdrawableState(placementApp1, WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = false))
    val placementApp1PlacementRequest1 = createPlacementRequest()
    placementApp1.placementRequests.add(placementApp1PlacementRequest1)
    setupWithdrawableState(placementApp1PlacementRequest1, WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = false))
    val placementApp1PlacementRequest1Booking = createSpaceBooking()
    placementApp1PlacementRequest1.spaceBookings = mutableListOf(placementApp1PlacementRequest1Booking)
    setupWithdrawableState(placementApp1PlacementRequest1Booking, WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = false, blockingReason = ArrivalRecordedInCas1))

    val placementApp2 = createPlacementApplication(automatic = true)
    setupWithdrawableState(placementApp2, WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = false))
    val placementApp2PlacementRequest1 = createPlacementRequest()
    placementApp2.placementRequests.add(placementApp2PlacementRequest1)
    setupWithdrawableState(placementApp2PlacementRequest1, WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = false))
    val placementApp2PlacementRequest1Booking = createSpaceBooking()
    placementApp2PlacementRequest1.spaceBookings = mutableListOf(placementApp2PlacementRequest1Booking)
    setupWithdrawableState(placementApp2PlacementRequest1Booking, WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true))

    every {
      cas1PlacementApplicationService.getAllSubmittedNonReallocatedApplications(application.id)
    } returns listOf(placementApp1, placementApp2)

    val result = service.treeForApp(application, user)

    assertThat(result.render(includeIds = false).trim()).isEqualTo(
      """
Application(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:Y, BLOCKED
---> PlacementRequest(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N
------> SpaceBooking(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:N
---> PlacementApplication(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N, BLOCKED
------> PlacementRequest(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N, BLOCKED
---------> SpaceBooking(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:N, BLOCKING - ArrivalRecordedInCas1
---> PlacementApplication() automatic, withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N
------> PlacementRequest(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N
---------> SpaceBooking(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:Y

Notes: [1 or more placements cannot be withdrawn as they have an arrival]
      """
        .trim(),
    )
  }

  @Test
  fun `tree for app blocks withdrawal if booking has arrival in Delius`() {
    every {
      cas1ApplicationService.getWithdrawableState(application, user)
    } returns WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true)

    every {
      placementRequestService.getPlacementRequestForInitialApplicationDates(application.id)
    } returns emptyList()

    val placementApp1 = createPlacementApplication()
    setupWithdrawableState(placementApp1, WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = false))
    val placementApp1PlacementRequest1 = createPlacementRequest()
    placementApp1.placementRequests.add(placementApp1PlacementRequest1)
    setupWithdrawableState(placementApp1PlacementRequest1, WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = false))
    val placementApp1PlacementRequest1Booking = createSpaceBooking()
    placementApp1PlacementRequest1.spaceBookings = mutableListOf(placementApp1PlacementRequest1Booking)
    setupWithdrawableState(placementApp1PlacementRequest1Booking, WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = false, blockingReason = ArrivalRecordedInDelius))

    every {
      cas1PlacementApplicationService.getAllSubmittedNonReallocatedApplications(application.id)
    } returns listOf(placementApp1)

    val result = service.treeForApp(application, user)

    assertThat(result.render(includeIds = false).trim()).isEqualTo(
      """
Application(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:Y, BLOCKED
---> PlacementApplication(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N, BLOCKED
------> PlacementRequest(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N, BLOCKED
---------> SpaceBooking(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:N, BLOCKING - ArrivalRecordedInDelius

Notes: [1 or more placements cannot be withdrawn as they have an arrival recorded in Delius]
      """
        .trim(),
    )
  }

  @Test
  fun `tree for app blocks withdrawal if space booking has arrival in CAS1`() {
    every {
      cas1ApplicationService.getWithdrawableState(application, user)
    } returns WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true)

    val initialPlacementRequest = createPlacementRequest()
    setupWithdrawableState(initialPlacementRequest, WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = false))

    val initialPlacementRequestSpaceBooking = createSpaceBooking()
    initialPlacementRequest.spaceBookings.add(initialPlacementRequestSpaceBooking)
    setupWithdrawableState(initialPlacementRequestSpaceBooking, WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = false))

    every {
      placementRequestService.getPlacementRequestForInitialApplicationDates(application.id)
    } returns listOf(
      initialPlacementRequest,
    )

    val placementApp1 = createPlacementApplication()
    setupWithdrawableState(placementApp1, WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = false))
    val placementApp1PlacementRequest1 = createPlacementRequest()
    placementApp1.placementRequests.add(placementApp1PlacementRequest1)
    setupWithdrawableState(placementApp1PlacementRequest1, WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = false))
    val placementApp1PlacementRequest1SpaceBooking = createSpaceBooking()
    placementApp1PlacementRequest1.spaceBookings.add(placementApp1PlacementRequest1SpaceBooking)
    setupWithdrawableState(
      placementApp1PlacementRequest1SpaceBooking,
      WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = false, blockingReason = ArrivalRecordedInCas1),
    )

    every {
      cas1PlacementApplicationService.getAllSubmittedNonReallocatedApplications(application.id)
    } returns listOf(placementApp1)

    val result = service.treeForApp(application, user)

    assertThat(result.render(includeIds = false).trim()).isEqualTo(
      """
Application(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:Y, BLOCKED
---> PlacementRequest(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N
------> SpaceBooking(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:N
---> PlacementApplication(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N, BLOCKED
------> PlacementRequest(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N, BLOCKED
---------> SpaceBooking(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:N, BLOCKING - ArrivalRecordedInCas1

Notes: [1 or more placements cannot be withdrawn as they have an arrival]
      """
        .trim(),
    )
  }

  private fun createPlacementRequest() = PlacementRequestEntityFactory()
    .withApplication(application)
    .withAssessment(assessment)
    .withPlacementRequirements(placementRequirements)
    .produce()

  private fun createPlacementApplication(automatic: Boolean = false) = PlacementApplicationEntityFactory()
    .withApplication(application)
    .withCreatedByUser(user)
    .withAutomatic(automatic)
    .produce()

  private fun createSpaceBooking() = Cas1SpaceBookingEntityFactory()
    .withApplication(application)
    .produce()

  private fun setupWithdrawableState(placementRequest: PlacementRequestEntity, state: WithdrawableState) {
    every {
      placementRequestService.getWithdrawableState(placementRequest, user)
    } returns state
  }

  private fun setupWithdrawableState(spaceBooking: Cas1SpaceBookingEntity, state: WithdrawableState) {
    every {
      cas1SpaceBookingService.getWithdrawableState(spaceBooking, user)
    } returns state
  }

  private fun setupWithdrawableState(placementApplication: PlacementApplicationEntity, state: WithdrawableState) {
    every {
      cas1PlacementApplicationService.getWithdrawableState(placementApplication, user)
    } returns state
  }
}
