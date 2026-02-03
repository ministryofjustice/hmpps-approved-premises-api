package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.BookingDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.ProfileInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

class BookingDetailsFactory : Factory<BookingDetails> {
  private var offenderNo: Yielded<String> = { randomStringUpperCase(8) }
  private var bookingId: Yielded<Long?> = { 12345L }
  private var profileInformation: Yielded<List<ProfileInformation>?> = { null }

  fun withOffenderNo(offenderNo: String) = apply {
    this.offenderNo = { offenderNo }
  }

  fun withBookingId(bookingId: Long?) = apply {
    this.bookingId = { bookingId }
  }

  fun withProfileInformation(profileInformation: List<ProfileInformation>?) = apply {
    this.profileInformation = { profileInformation }
  }

  override fun produce(): BookingDetails = BookingDetails(
    offenderNo = this.offenderNo(),
    bookingId = this.bookingId(),
    profileInformation = this.profileInformation(),
  )
}
