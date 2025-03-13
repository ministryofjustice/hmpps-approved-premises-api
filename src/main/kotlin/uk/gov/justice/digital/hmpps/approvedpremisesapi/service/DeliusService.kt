package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity

@Service
class DeliusService(
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
) {
  fun getReferralDetails(booking: BookingEntity) = when (val clientResult = apDeliusContextApiClient.getReferralDetails(booking.crn, booking.id.toString())) {
    is ClientResult.Success -> clientResult.body
    is ClientResult.Failure.StatusCode -> when (clientResult.status) {
      HttpStatus.NOT_FOUND -> null
      else -> clientResult.throwException()
    }
    is ClientResult.Failure -> clientResult.throwException()
  }

  fun referralHasArrival(booking: BookingEntity) = (getReferralDetails(booking)?.arrivedAt != null)
}
