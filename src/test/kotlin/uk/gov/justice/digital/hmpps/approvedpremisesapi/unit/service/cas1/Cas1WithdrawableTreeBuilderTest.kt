package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableTreeBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableState

class Cas1WithdrawableTreeBuilderTest {

  private val placementRequestService = mockk<PlacementRequestService>()
  private val bookingService = mockk<BookingService>()
  private val placementApplicationService = mockk<PlacementApplicationService>()
  private val applicationService = mockk<ApplicationService>()

  private val service = Cas1WithdrawableTreeBuilder(
    placementRequestService,
    bookingService,
    placementApplicationService,
    applicationService,
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
      applicationService.getWithdrawableState(application, user)
    } returns WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true)

    val initialPlacementRequest = createPlacementRequest()
    setupWithdrawableState(initialPlacementRequest, WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = false))

    val initialPlacementRequestBooking = createBooking()
    initialPlacementRequest.booking = initialPlacementRequestBooking
    setupWithdrawableState(initialPlacementRequestBooking, WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false))

    every {
      placementRequestService.getPlacementRequestForInitialApplicationDates(application.id)
    } returns listOf(
      initialPlacementRequest,
    )

    val placementApp1 = createPlacementApplication()
    setupWithdrawableState(placementApp1, WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = false))
    val placementApp1PlacementRequest1 = createPlacementRequest()
    placementApp1.placementRequests.add(placementApp1PlacementRequest1)
    setupWithdrawableState(placementApp1PlacementRequest1, WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = false, blockAncestorWithdrawals = true))
    val placementApp1PlacementRequest1Booking = createBooking()
    placementApp1PlacementRequest1.booking = placementApp1PlacementRequest1Booking
    setupWithdrawableState(placementApp1PlacementRequest1Booking, WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = false))

    val placementApplication2 = createPlacementApplication()
    setupWithdrawableState(placementApplication2, WithdrawableState(withdrawable = true, userMayDirectlyWithdraw = true))

    every {
      placementApplicationService.getAllPlacementApplicationEntitiesForApplicationId(application.id)
    } returns listOf(placementApp1, placementApplication2)

    val adhocBooking1 = createBooking()
    setupWithdrawableState(adhocBooking1, WithdrawableState(withdrawable = false, userMayDirectlyWithdraw = false, blockAncestorWithdrawals = false))

    every {
      bookingService.getAllAdhocOrUnknownForApplication(application)
    } returns listOf(adhocBooking1)

    val result = service.treeForApp(application, user)

    assertThat(result.render(0, includeIds = false).trim()).isEqualTo(
      """
Application(), withdrawable:Y, mayDirectlyWithdraw:Y, BLOCKED
---> PlacementRequest(), withdrawable:N, mayDirectlyWithdraw:N
------> Booking(), withdrawable:Y, mayDirectlyWithdraw:N
---> PlacementApplication(), withdrawable:N, mayDirectlyWithdraw:N, BLOCKED
------> PlacementRequest(), withdrawable:N, mayDirectlyWithdraw:N, BLOCKING
---------> Booking(), withdrawable:Y, mayDirectlyWithdraw:N
---> PlacementApplication(), withdrawable:Y, mayDirectlyWithdraw:Y
---> Booking(), withdrawable:N, mayDirectlyWithdraw:N
      """
        .trim(),
    )
  }

  private fun createPlacementRequest() = PlacementRequestEntityFactory()
    .withApplication(application)
    .withAssessment(assessment)
    .withPlacementRequirements(placementRequirements)
    .produce()

  private fun createPlacementApplication() = PlacementApplicationEntityFactory()
    .withApplication(application)
    .withCreatedByUser(user)
    .produce()

  private fun createBooking() = BookingEntityFactory()
    .withDefaults()
    .withApplication(application)
    .produce()

  private fun setupWithdrawableState(placementRequest: PlacementRequestEntity, state: WithdrawableState) {
    every {
      placementRequestService.getWithdrawableState(placementRequest, user)
    } returns state
  }

  private fun setupWithdrawableState(booking: BookingEntity, state: WithdrawableState) {
    every {
      bookingService.getWithdrawableState(booking, user)
    } returns state
  }

  private fun setupWithdrawableState(placementApplication: PlacementApplicationEntity, state: WithdrawableState) {
    every {
      placementApplicationService.getWithdrawableState(placementApplication, user)
    } returns state
  }
}
