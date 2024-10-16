package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model

import com.opencsv.bean.CsvBindByName
import java.time.LocalDate

data class FutureBookingsReportRow(
  @CsvBindByName(column = "bookingId")
  val bookingId: String,
  @CsvBindByName(column = "referralId")
  val referralId: String?,
  @CsvBindByName(column = "referralDate")
  val referralDate: LocalDate?,
  @CsvBindByName(column = "personName")
  val personName: String?,
  @CsvBindByName(column = "gender")
  val gender: String?,
  @CsvBindByName(column = "ethnicity")
  val ethnicity: String?,
  @CsvBindByName(column = "dateOfBirth")
  val dateOfBirth: LocalDate?,
  @CsvBindByName(column = "riskOfSeriousHarm")
  val riskOfSeriousHarm: String?,
  @CsvBindByName(column = "registeredSexOffender")
  val registeredSexOffender: String?,
  @CsvBindByName(column = "historyOfSexualOffence")
  val historyOfSexualOffence: String?,
  @CsvBindByName(column = "concerningSexualBehaviour")
  val concerningSexualBehaviour: String?,
  @CsvBindByName(column = "dutyToReferMade")
  val dutyToReferMade: String?,
  @CsvBindByName(column = "dateDutyToReferMade")
  val dateDutyToReferMade: LocalDate?,
  @CsvBindByName(column = "dutyToReferLocalAuthorityAreaName")
  val dutyToReferLocalAuthorityAreaName: String?,
  @CsvBindByName(column = "probationRegion")
  val probationRegion: String,
  @CsvBindByName(column = "pdu")
  val pdu: String?,
  @CsvBindByName(column = "localAuthority")
  val localAuthority: String?,
  @CsvBindByName(column = "addressLine1")
  val addressLine1: String,
  @CsvBindByName(column = "postCode")
  val postCode: String,
  @CsvBindByName(column = "crn")
  val crn: String,
  @CsvBindByName(column = "sourceOfReferral")
  val sourceOfReferral: String?,
  @CsvBindByName(column = "prisonAtReferral")
  val prisonAtReferral: String?,
  @CsvBindByName(column = "accommodationRequiredDate")
  val accommodationRequiredDate: LocalDate?,
  @CsvBindByName(column = "updatedAccommodationRequiredDate")
  val updatedAccommodationRequiredDate: LocalDate?,
  @CsvBindByName(column = "bookingStatus")
  val bookingStatus: String?,
)
