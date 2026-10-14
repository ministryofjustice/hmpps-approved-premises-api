package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

data class Cas1WithdrawableDatePeriodDto(
  val startDate: java.time.LocalDate,
  val endDate: java.time.LocalDate?,
)
