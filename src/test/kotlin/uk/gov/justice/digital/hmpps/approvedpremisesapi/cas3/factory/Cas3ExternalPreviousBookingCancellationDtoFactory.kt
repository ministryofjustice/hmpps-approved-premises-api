package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ExternalPreviousBookingCancellationDto
import java.time.LocalDate

class Cas3ExternalPreviousBookingCancellationDtoFactory : Factory<Cas3ExternalPreviousBookingCancellationDto> {
  private var cancellationDate: Yielded<LocalDate> = { LocalDate.now() }
  private var cancellationReason: Yielded<String?> = { null }

  fun withCancellationDate(cancellationDate: LocalDate) = apply {
    this.cancellationDate = { cancellationDate }
  }

  fun withCancellationReason(cancellationReason: String?) = apply {
    this.cancellationReason = { cancellationReason }
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  override fun produce(): Cas3ExternalPreviousBookingCancellationDto = Cas3ExternalPreviousBookingCancellationDto(
    cancellationDate = this.cancellationDate(),
    cancellationReason = this.cancellationReason(),
  )
}
