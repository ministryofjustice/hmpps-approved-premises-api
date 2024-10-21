package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model

import java.time.LocalDate

data class FutureBookingsReportRow(
  val bookingId: String,
  val referralId: String?,
  val referralDate: LocalDate?,
  val personName: String?,
  val gender: String?,
  val ethnicity: String?,
  val dateOfBirth: LocalDate?,
  val riskOfSeriousHarm: String?,
  val registeredSexOffender: String?,
  val historyOfSexualOffence: String?,
  val concerningSexualBehaviour: String?,
  val dutyToReferMade: String?,
  val dateDutyToReferMade: LocalDate?,
  val dutyToReferLocalAuthorityAreaName: String?,
  val probationRegion: String,
  val pdu: String?,
  val localAuthority: String?,
  val addressLine1: String,
  val postCode: String,
  val crn: String,
  val sourceOfReferral: String?,
  val prisonAtReferral: String?,
  val startDate: LocalDate,
  val accommodationRequiredDate: LocalDate?,
  val updatedAccommodationRequiredDate: LocalDate?,
  val bookingStatus: String?,
)
