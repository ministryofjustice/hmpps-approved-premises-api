package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WithdrawableService

class WithdrawableServiceTest {
  private val mockPlacementRequestService = mockk<PlacementRequestService>()
  private val mockBookingService = mockk<BookingService>()
  private val mockPlacementApplicationService = mockk<PlacementApplicationService>()

  private val withdrawableService = WithdrawableService(
    mockPlacementRequestService,
    mockBookingService,
    mockPlacementApplicationService,
  )

  @Test
  fun `it returns all withdrawables as a Withdrawables object`() {
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
      .produceMany()
      .take(2)
      .toList()

    val bookings = BookingEntityFactory()
      .withPremises(premises)
      .produceMany()
      .take(3)
      .toList()

    every { mockPlacementRequestService.getWithdrawablePlacementRequests(application) } returns placementRequests
    every { mockPlacementApplicationService.getWithdrawablePlacementApplications(application) } returns placementApplications
    every { mockBookingService.getCancelleableBookings(application) } returns bookings

    val result = withdrawableService.allWithdrawables(application)

    assertThat(result.bookings).isEqualTo(bookings)
    assertThat(result.placementRequests).isEqualTo(placementRequests)
    assertThat(result.placementApplications).isEqualTo(placementApplications)
  }
}
