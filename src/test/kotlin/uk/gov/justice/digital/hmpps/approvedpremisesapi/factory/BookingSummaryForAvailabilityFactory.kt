package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingSummaryForAvailability
import java.time.LocalDate

class BookingSummaryForAvailabilityFactory : Factory<BookingSummaryForAvailabilityImpl> {
  private var arrivalDate: Yielded<LocalDate> = { LocalDate.now() }
  private var departureDate: Yielded<LocalDate> = { LocalDate.now() }
  private var arrived: Yielded<Boolean> = { false }
  private var isNotArrived: Yielded<Boolean> = { false }
  private var cancelled: Yielded<Boolean> = { false }

  fun withArrivalDate(arrivalDate: LocalDate) = apply {
    this.arrivalDate = { arrivalDate }
  }
  fun withDepartureDate(departureDate: LocalDate) = apply {
    this.departureDate = { departureDate }
  }
  fun withArrived(arrived: Boolean) = apply {
    this.arrived = { arrived }
  }
  fun withIsNotArrived(isNotArrived: Boolean) = apply {
    this.isNotArrived = { isNotArrived }
  }
  fun withCancelled(cancelled: Boolean) = apply {
    this.cancelled = { cancelled }
  }

  override fun produce(): BookingSummaryForAvailabilityImpl = BookingSummaryForAvailabilityImpl(
    arrivalDateVar = this.arrivalDate(),
    departureDateVar = this.departureDate(),
    arrivedVar = this.arrived(),
    isNotArrivedVar = this.isNotArrived(),
    cancelledVar = this.cancelled(),
  )
}

class BookingSummaryForAvailabilityImpl(
  private var arrivalDateVar: LocalDate,
  private var departureDateVar: LocalDate,
  private var arrivedVar: Boolean,
  private var isNotArrivedVar: Boolean,
  private var cancelledVar: Boolean,
) : BookingSummaryForAvailability {
  override fun getArrivalDate(): LocalDate = this.arrivalDateVar
  override fun getDepartureDate(): LocalDate = this.departureDateVar
  override fun getArrived(): Boolean = this.arrivedVar
  override fun getIsNotArrived(): Boolean = this.isNotArrivedVar
  override fun getCancelled(): Boolean = this.cancelledVar
}
