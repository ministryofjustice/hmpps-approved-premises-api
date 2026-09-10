package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class WithdrawableDatePeriodDto(
  val startDate: java.time.LocalDate,
  val endDate: java.time.LocalDate?,
)
