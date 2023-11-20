package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class BookingSearchResultDto(
  var personName: String?,
  val personCrn: String,
  val bookingId: UUID,
  val bookingStatus: String,
  val bookingStartDate: LocalDate,
  val bookingEndDate: LocalDate,
  val bookingCreatedAt: OffsetDateTime,
  val premisesId: UUID,
  val premisesName: String,
  val premisesAddressLine1: String,
  val premisesAddressLine2: String?,
  val premisesTown: String?,
  val premisesPostcode: String,
  val roomId: UUID,
  val roomName: String,
  val bedId: UUID,
  val bedName: String,
)
