package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.time.LocalDate

class BookingServiceTest {
  private val mockOffenderDetailService = mockk<OffenderDetailService>()

  private val mockBookingRepository = mockk<BookingRepository>()
  private val mockUserService = mockk<UserService>()
  private val mockUserAccessService = mockk<UserAccessService>()

  fun createBookingService(): BookingService = BookingService(
    offenderDetailService = mockOffenderDetailService,
    bookingRepository = mockBookingRepository,
    userService = mockUserService,
    userAccessService = mockUserAccessService,
  )

  private val bookingService = createBookingService()

  private val user = UserEntityFactory()
    .withUnitTestControlProbationRegion()
    .produce()

  @Nested
  inner class GetBooking {
    val premises = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    private val bookingEntity = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-08-25"))
      .withPremises(premises)
      .produce()

    private val personInfo = PersonInfoResult.Success.Full(
      crn = bookingEntity.crn,
      offenderDetailSummary = OffenderDetailsSummaryFactory().produce(),
      inmateDetail = InmateDetailFactory().produce(),
    )

    @Test
    fun `returns a booking`() {
      every { mockBookingRepository.findByIdOrNull(bookingEntity.id) } returns bookingEntity
      every { mockUserService.getUserForRequest() } returns user
      every { mockUserAccessService.userCanViewBooking(user, bookingEntity) } returns true
      every {
        mockOffenderDetailService.getPersonInfoResult(
          bookingEntity.crn,
          user.deliusUsername,
          user.hasQualification(UserQualification.LAO),
        )
      } returns personInfo

      val result = bookingService.getBooking(bookingEntity.id)

      assertThat(result is AuthorisableActionResult.Success).isTrue()
      result as AuthorisableActionResult.Success

      assertThat(result.entity).isEqualTo(BookingService.BookingAndPersons(bookingEntity, personInfo))
    }

    @Test
    fun `returns NotFound if the booking cannot be found`() {
      every { mockBookingRepository.findByIdOrNull(bookingEntity.id) } returns null

      val result = bookingService.getBooking(bookingEntity.id)

      assertThat(result is AuthorisableActionResult.NotFound).isTrue()
      result as AuthorisableActionResult.NotFound

      assertThat(result.id).isEqualTo(bookingEntity.id.toString())
      assertThat(result.entityType).isEqualTo("Booking")
    }

    @Test
    fun `returns Unauthorised if the user cannot view the booking`() {
      every { mockBookingRepository.findByIdOrNull(bookingEntity.id) } returns bookingEntity
      every { mockUserService.getUserForRequest() } returns user
      every { mockUserAccessService.userCanViewBooking(user, bookingEntity) } returns false

      val result = bookingService.getBooking(bookingEntity.id)

      assertThat(result is AuthorisableActionResult.Unauthorised).isTrue()
    }
  }
}
