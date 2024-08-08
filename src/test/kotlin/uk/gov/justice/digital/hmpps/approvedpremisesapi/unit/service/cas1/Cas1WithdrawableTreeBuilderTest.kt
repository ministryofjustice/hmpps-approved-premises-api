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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.BlockingReason.ArrivalRecordedInCas1
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.BlockingReason.ArrivalRecordedInDelius
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
    } returns WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true)

    val initialPlacementRequest = createPlacementRequest()
    setupWithdrawableState(initialPlacementRequest, WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = false))

    val initialPlacementRequestBooking = createBooking(adhoc = false)
    initialPlacementRequest.booking = initialPlacementRequestBooking
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
    val placementApp1PlacementRequest1Booking = createBooking(adhoc = false)
    placementApp1PlacementRequest1.booking = placementApp1PlacementRequest1Booking
    setupWithdrawableState(placementApp1PlacementRequest1Booking, WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = false))

    val placementApplication2 = createPlacementApplication()
    setupWithdrawableState(placementApplication2, WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true))

    every {
      placementApplicationService.getAllSubmittedNonReallocatedApplications(application.id)
    } returns listOf(placementApp1, placementApplication2)

    val adhocBooking1 = createBooking(adhoc = true)
    setupWithdrawableState(adhocBooking1, WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = false))

    every {
      bookingService.getAllAdhocOrUnknownForApplication(application)
    } returns listOf(adhocBooking1)

    val result = service.treeForApp(application, user)

    assertThat(result.render(includeIds = false).trim()).isEqualTo(
      """
Application(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:Y
---> PlacementRequest(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N
------> Booking(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:N
---> PlacementApplication(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N
------> PlacementRequest(), withdrawn:Y, withdrawable:N, mayDirectlyWithdraw:N
---------> Booking(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:N
---> PlacementApplication(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:Y
---> Booking(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N

Notes: []
      """
        .trim(),
    )
  }

  @Test
  fun `tree for app excludes placement request's bookings if bookings are adhoc or potentially adhoc`() {
    every {
      applicationService.getWithdrawableState(application, user)
    } returns WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true)

    val initialPlacementRequest = createPlacementRequest()
    setupWithdrawableState(initialPlacementRequest, WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = false))

    val initialPlacementRequestBooking = createBooking(adhoc = true)
    initialPlacementRequest.booking = initialPlacementRequestBooking
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
    val placementApp1PlacementRequest1Booking = createBooking(adhoc = false)
    placementApp1PlacementRequest1.booking = placementApp1PlacementRequest1Booking
    setupWithdrawableState(placementApp1PlacementRequest1Booking, WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = false))

    val placementApp2 = createPlacementApplication()
    setupWithdrawableState(placementApp2, WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = false))
    val placementApp2PlacementRequest1 = createPlacementRequest()
    placementApp2.placementRequests.add(placementApp2PlacementRequest1)
    setupWithdrawableState(placementApp2PlacementRequest1, WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = false))
    val placementApp2PlacementRequest1Booking = createBooking(adhoc = null)
    placementApp2PlacementRequest1.booking = placementApp2PlacementRequest1Booking
    setupWithdrawableState(placementApp2PlacementRequest1Booking, WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = false))

    every {
      placementApplicationService.getAllSubmittedNonReallocatedApplications(application.id)
    } returns listOf(placementApp1, placementApp2)

    every { bookingService.getAllAdhocOrUnknownForApplication(application) } returns emptyList()

    val result = service.treeForApp(application, user)

    assertThat(result.render(includeIds = false).trim()).isEqualTo(
      """
Application(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:Y
---> PlacementRequest(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N
---> PlacementApplication(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N
------> PlacementRequest(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N
---------> Booking(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:N
---> PlacementApplication(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N
------> PlacementRequest(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N

Notes: []
      """
        .trim(),
    )
  }

  @Test
  fun `tree for app blocks withdrawal if booking has arrival in CAS1`() {
    every {
      applicationService.getWithdrawableState(application, user)
    } returns WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true)

    val initialPlacementRequest = createPlacementRequest()
    setupWithdrawableState(initialPlacementRequest, WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = false))

    val initialPlacementRequestBooking = createBooking(adhoc = false)
    initialPlacementRequest.booking = initialPlacementRequestBooking
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
    val placementApp1PlacementRequest1Booking = createBooking(adhoc = false)
    placementApp1PlacementRequest1.booking = placementApp1PlacementRequest1Booking
    setupWithdrawableState(placementApp1PlacementRequest1Booking, WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = false, blockingReason = ArrivalRecordedInCas1))

    val placementApp2 = createPlacementApplication()
    setupWithdrawableState(placementApp2, WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = false))
    val placementApp2PlacementRequest1 = createPlacementRequest()
    placementApp2.placementRequests.add(placementApp2PlacementRequest1)
    setupWithdrawableState(placementApp2PlacementRequest1, WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = false))
    val placementApp2PlacementRequest1Booking = createBooking(adhoc = false)
    placementApp2PlacementRequest1.booking = placementApp2PlacementRequest1Booking
    setupWithdrawableState(placementApp2PlacementRequest1Booking, WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true))

    every {
      placementApplicationService.getAllSubmittedNonReallocatedApplications(application.id)
    } returns listOf(placementApp1, placementApp2)

    every { bookingService.getAllAdhocOrUnknownForApplication(application) } returns emptyList()

    val result = service.treeForApp(application, user)

    assertThat(result.render(includeIds = false).trim()).isEqualTo(
      """
Application(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:Y, BLOCKED
---> PlacementRequest(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N
------> Booking(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:N
---> PlacementApplication(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N, BLOCKED
------> PlacementRequest(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N, BLOCKED
---------> Booking(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:N, BLOCKING - ArrivalRecordedInCas1
---> PlacementApplication(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N
------> PlacementRequest(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N
---------> Booking(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:Y

Notes: [1 or more placements cannot be withdrawn as they have an arrival]
      """
        .trim(),
    )
  }

  @Test
  fun `tree for app blocks withdrawal if booking has arrival in Delius`() {
    every {
      applicationService.getWithdrawableState(application, user)
    } returns WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = true)

    every {
      placementRequestService.getPlacementRequestForInitialApplicationDates(application.id)
    } returns emptyList()

    val placementApp1 = createPlacementApplication()
    setupWithdrawableState(placementApp1, WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = false))
    val placementApp1PlacementRequest1 = createPlacementRequest()
    placementApp1.placementRequests.add(placementApp1PlacementRequest1)
    setupWithdrawableState(placementApp1PlacementRequest1, WithdrawableState(withdrawn = false, withdrawable = false, userMayDirectlyWithdraw = false))
    val placementApp1PlacementRequest1Booking = createBooking(adhoc = false)
    placementApp1PlacementRequest1.booking = placementApp1PlacementRequest1Booking
    setupWithdrawableState(placementApp1PlacementRequest1Booking, WithdrawableState(withdrawn = false, withdrawable = true, userMayDirectlyWithdraw = false, blockingReason = ArrivalRecordedInDelius))

    every {
      placementApplicationService.getAllSubmittedNonReallocatedApplications(application.id)
    } returns listOf(placementApp1)

    every { bookingService.getAllAdhocOrUnknownForApplication(application) } returns emptyList()

    val result = service.treeForApp(application, user)

    assertThat(result.render(includeIds = false).trim()).isEqualTo(
      """
Application(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:Y, BLOCKED
---> PlacementApplication(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N, BLOCKED
------> PlacementRequest(), withdrawn:N, withdrawable:N, mayDirectlyWithdraw:N, BLOCKED
---------> Booking(), withdrawn:N, withdrawable:Y, mayDirectlyWithdraw:N, BLOCKING - ArrivalRecordedInDelius

Notes: [1 or more placements cannot be withdrawn as they have an arrival recorded in Delius]
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

  private fun createBooking(adhoc: Boolean?) = BookingEntityFactory()
    .withDefaults()
    .withApplication(application)
    .withAdhoc(adhoc)
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
