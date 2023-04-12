package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class BookingSearchResultFactory : Factory<BookingSearchResult> {
  private var personName: Yielded<String>? = null
  private var personCrn: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var bookingId: Yielded<UUID> = { UUID.randomUUID() }
  private var bookingStatus: Yielded<String> = {
    randomOf(
      listOf(
        "arrived",
        "awaiting-arrival",
        "not-arrived",
        "departed",
        "cancelled",
        "provisional",
        "confirmed",
      )
    )
  }
  private var bookingStartDate: Yielded<LocalDate> = { LocalDate.now().randomDateBefore() }
  private var bookingEndDate: Yielded<LocalDate> = { LocalDate.now().randomDateAfter() }
  private var bookingCreatedAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().minusDays(30).randomDateTimeBefore() }
  private var premisesId: Yielded<UUID> = { UUID.randomUUID() }
  private var premisesName: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var premisesAddressLine1: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var premisesAddressLine2: Yielded<String>? = null
  private var premisesTown: Yielded<String>? = null
  private var premisesPostcode: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var roomId: Yielded<UUID> = { UUID.randomUUID() }
  private var roomName: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var bedId: Yielded<UUID> = { UUID.randomUUID() }
  private var bedName: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }

  fun withPersonName(personName: String) = apply {
    this.personName = { personName }
  }

  fun withBookingStatus(bookingStatus: BookingStatus) = apply {
    this.bookingStatus = { bookingStatus.value }
  }

  fun withBookingCreatedAt(bookingCreatedAt: OffsetDateTime) = apply {
    this.bookingCreatedAt = { bookingCreatedAt }
  }

  override fun produce() = BookingSearchResult(
    personName = this.personName?.invoke(),
    personCrn = this.personCrn(),
    bookingId = this.bookingId(),
    bookingStatus = this.bookingStatus(),
    bookingStartDate = this.bookingStartDate(),
    bookingEndDate = this.bookingEndDate(),
    bookingCreatedAt = this.bookingCreatedAt(),
    premisesId = this.premisesId(),
    premisesName = this.premisesName(),
    premisesAddressLine1 = this.premisesAddressLine1(),
    premisesAddressLine2 = this.premisesAddressLine2?.invoke(),
    premisesTown = this.premisesTown?.invoke(),
    premisesPostcode = this.premisesPostcode(),
    roomId = this.roomId(),
    roomName = this.roomName(),
    bedId = this.bedId(),
    bedName = this.bedName(),
  )
}
