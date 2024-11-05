package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ReferralDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DeliusService
import java.time.ZonedDateTime

class DeliusServiceTest {
  private val apDeliusContextApiClient = mockk<ApDeliusContextApiClient>()

  private val service = DeliusService(apDeliusContextApiClient)

  @Nested
  inner class ReferralHasArrival {

    val booking = BookingEntityFactory()
      .withDefaults()
      .produce()

    @Test
    fun `referralHasArrival returns true if has arrival`() {
      every { apDeliusContextApiClient.getReferralDetails(booking.crn, booking.id.toString()) } returns
        ClientResult.Success(
          HttpStatus.OK,
          ReferralDetail(
            arrivedAt = ZonedDateTime.now(),
            departedAt = ZonedDateTime.now().plusDays(1),
          ),
        )

      val result = service.referralHasArrival(booking)

      assertThat(result).isTrue
    }

    @Test
    fun `referralHasArrival returns false if no arrival`() {
      every { apDeliusContextApiClient.getReferralDetails(booking.crn, booking.id.toString()) } returns
        ClientResult.Success(
          HttpStatus.OK,
          ReferralDetail(
            arrivedAt = null,
            departedAt = null,
          ),
        )

      val result = service.referralHasArrival(booking)

      assertThat(result).isFalse
    }

    @Test
    fun `referralHasArrival returns false if 404 returned`() {
      every { apDeliusContextApiClient.getReferralDetails(booking.crn, booking.id.toString()) } returns
        ClientResult.Failure.StatusCode(
          method = HttpMethod.GET,
          path = "/",
          status = HttpStatus.NOT_FOUND,
          body = "the body",
        )

      val result = service.referralHasArrival(booking)

      assertThat(result).isFalse
    }

    @Test
    fun `referralHasArrival throw exception if failure other than 404 returned`() {
      every { apDeliusContextApiClient.getReferralDetails(booking.crn, booking.id.toString()) } returns
        ClientResult.Failure.StatusCode(
          method = HttpMethod.GET,
          path = "/",
          status = HttpStatus.BAD_REQUEST,
          body = "the body",
        )

      assertThatThrownBy {
        service.referralHasArrival(booking)
      }.hasMessage("Unable to complete GET request to /: 400 BAD_REQUEST")
    }
  }
}
