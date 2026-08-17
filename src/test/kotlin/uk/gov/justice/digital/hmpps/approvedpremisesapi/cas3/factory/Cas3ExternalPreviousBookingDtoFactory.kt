package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ExternalPreviousBookingCancellationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ExternalPreviousBookingDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus

class Cas3ExternalPreviousBookingDtoFactory : Factory<Cas3ExternalPreviousBookingDto> {
  private var bookingStatus: Yielded<Cas3BookingStatus?> = { null }
  private var cancellation: Yielded<Cas3ExternalPreviousBookingCancellationDto?> = { null }

  fun withBookingStatus(bookingStatus: Cas3BookingStatus?) = apply {
    this.bookingStatus = { bookingStatus }
  }

  fun withCancellation(cancellation: Cas3ExternalPreviousBookingCancellationDto?) = apply {
    this.cancellation = { cancellation }
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  override fun produce(): Cas3ExternalPreviousBookingDto = Cas3ExternalPreviousBookingDto(
    bookingStatus = this.bookingStatus(),
    cancellation = this.cancellation(),
  )
}
