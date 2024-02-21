package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model

import java.time.LocalDate

data class TransitionalAccommodationReferralReportRow(
  val referralId: String?,
  val referralDate: LocalDate?,
  val personName: String?,
  val pncNumber: String?,
  val crn: String,
  val gender: String?,
  val ethnicity: String?,
  val dateOfBirth: LocalDate?,
  val sexOffender: Boolean?,
  val needForAccessibleProperty: Boolean?,
  val riskOfSeriousHarm: String?,
  val historyOfArsonOffence: Boolean?,
  val dutyToReferMade: Boolean?,
  val dateDutyToReferMade: LocalDate?,
  val dutyToReferLocalAuthorityAreaName: String?,
  val probationRegion: String,
  val referralSubmittedDate: LocalDate?,
  val referralRejected: Boolean?,
  val rejectionReason: String?,
  val rejectionDate: LocalDate?,
  val sourceOfReferral: String?,
  val prisonAtReferral: String?,
  val prisonReleaseDate: LocalDate?,
  val accommodationRequiredDate: LocalDate?,
  val bookingOffered: Boolean?,
)
