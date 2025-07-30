package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3v2BookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class TestCas3BookingSearchResult : Cas3v2BookingSearchResult {
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

  private var bookingStartDate: LocalDate = LocalDate.now().randomDateBefore(14)
  private var bookingEndDate: LocalDate = LocalDate.now().randomDateAfter(14)
  private var bookingCreatedAt: OffsetDateTime = OffsetDateTime.now().minusDays(30).randomDateTimeBefore(14)
  private var premisesId: UUID = UUID.randomUUID()
  private var premisesName: String = randomStringMultiCaseWithNumbers(6)
  private var premisesAddressLine1: String = randomStringMultiCaseWithNumbers(6)
  private var premisesAddressLine2: String? = null
  private var premisesTown: String? = null
  private var premisesPostcode: String = randomStringMultiCaseWithNumbers(6)
  private var bedspaceId: UUID = UUID.randomUUID()
  private var bedspaceReference: String = randomStringMultiCaseWithNumbers(6)

  fun withPersonName(personName: String) = apply {
    this.personName = personName
  }

  fun withPersonCrn(personCrn: String) = apply {
    this.personCrn = personCrn
  }

  fun withBookingStatus(bookingStatus: Cas3BookingStatus) = apply {
    this.bookingStatus = bookingStatus.value
  }

  fun withBookingCreatedAt(bookingCreatedAt: OffsetDateTime) = apply {
    this.bookingCreatedAt = bookingCreatedAt
  }

  override fun getPersonName(): String? = this.personName

  override fun getPersonCrn(): String = this.personCrn

  override fun getBookingStatus(): String = this.bookingStatus

  override fun getBookingId(): UUID = this.bookingId

  override fun getBookingStartDate(): LocalDate = this.bookingStartDate

  override fun getBookingEndDate(): LocalDate = this.bookingEndDate

  override fun getBookingCreatedAt(): Instant = this.bookingCreatedAt.toInstant()

  override fun getPremisesId(): UUID = this.premisesId

  override fun getPremisesName(): String = this.premisesName

  override fun getPremisesAddressLine1(): String = this.premisesAddressLine1

  override fun getPremisesAddressLine2(): String? = this.premisesAddressLine2

  override fun getPremisesTown(): String? = this.premisesTown

  override fun getPremisesPostcode(): String = this.premisesPostcode

  override fun getBedspaceId(): UUID = this.bedspaceId

  override fun getBedspaceReference(): String = this.bedspaceReference
}
