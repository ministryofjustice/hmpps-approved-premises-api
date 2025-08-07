package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.bankholidaysapi

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class UKBankHolidays(
  @JsonProperty("england-and-wales")
  val englandAndWales: CountryBankHolidays,
  @JsonProperty("scotland")
  val scotland: CountryBankHolidays,
  @JsonProperty("northern-ireland")
  val northernIreland: CountryBankHolidays,
)

data class CountryBankHolidays(
  val division: String,
  val events: List<BankHolidayEvent>,
)

data class BankHolidayEvent(
  val title: String,
  @JsonFormat(pattern = "yyyy-MM-dd")
  val date: LocalDate,
  val notes: String,
  val bunting: Boolean,
)
