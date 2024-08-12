package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.sql.Timestamp
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class TestBookingSearchResult : BookingSearchResult {
  private var personName: String? = null
  private var personCrn: String = randomStringMultiCaseWithNumbers(6)
  private var bookingId: UUID = UUID.randomUUID()
  private var bookingStatus: String =
    randomOf(
      listOf(
        "arrived",
        "awaiting-arrival",
        "not-arrived",
        "departed",
        "cancelled",
        "provisional",
        "confirmed",
      ),
    )

  private var bookingStartDate: LocalDate = LocalDate.now().randomDateBefore()
  private var bookingEndDate: LocalDate = LocalDate.now().randomDateAfter()
  private var bookingCreatedAt: OffsetDateTime = OffsetDateTime.now().minusDays(30).randomDateTimeBefore(14)
  private var premisesId: UUID = UUID.randomUUID()
  private var premisesName: String = randomStringMultiCaseWithNumbers(6)
  private var premisesAddressLine1: String = randomStringMultiCaseWithNumbers(6)
  private var premisesAddressLine2: String? = null
  private var premisesTown: String? = null
  private var premisesPostcode: String = randomStringMultiCaseWithNumbers(6)
  private var roomId: UUID = UUID.randomUUID()
  private var roomName: String = randomStringMultiCaseWithNumbers(6)
  private var bedId: UUID = UUID.randomUUID()
  private var bedName: String = randomStringMultiCaseWithNumbers(6)

  fun withPersonName(personName: String) = apply {
    this.personName = personName
  }

  fun withPersonCrn(personCrn: String) = apply {
    this.personCrn = personCrn
  }

  fun withBookingStatus(bookingStatus: BookingStatus) = apply {
    this.bookingStatus = bookingStatus.value
  }

  fun withBookingCreatedAt(bookingCreatedAt: OffsetDateTime) = apply {
    this.bookingCreatedAt = bookingCreatedAt
  }

  override fun getPersonName(): String? {
    return this.personName
  }

  override fun getPersonCrn(): String {
    return this.personCrn
  }

  override fun getBookingStatus(): String {
    return this.bookingStatus
  }

  override fun getBookingId(): UUID {
    return this.bookingId
  }

  override fun getBookingStartDate(): LocalDate {
    return this.bookingStartDate
  }

  override fun getBookingEndDate(): LocalDate {
    return this.bookingEndDate
  }

  override fun getBookingCreatedAt(): Timestamp {
    return Timestamp.from(this.bookingCreatedAt.toInstant())
  }

  override fun getPremisesId(): UUID {
    return this.premisesId
  }

  override fun getPremisesName(): String {
    return this.premisesName
  }

  override fun getPremisesAddressLine1(): String {
    return this.premisesAddressLine1
  }

  override fun getPremisesAddressLine2(): String? {
    return this.premisesAddressLine2
  }

  override fun getPremisesTown(): String? {
    return this.premisesTown
  }

  override fun getPremisesPostcode(): String {
    return this.premisesPostcode
  }

  override fun getRoomId(): UUID {
    return this.roomId
  }

  override fun getRoomName(): String {
    return this.roomName
  }

  override fun getBedId(): UUID {
    return this.bedId
  }

  override fun getBedName(): String {
    return this.bedName
  }
}
