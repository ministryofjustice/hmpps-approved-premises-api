package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1PremisesDaySummary(
  val forDate: java.time.LocalDate,
  val previousDate: java.time.LocalDate,
  val nextDate: java.time.LocalDate,
  val spaceBookingSummaries: List<Cas1SpaceBookingSummary>,
  val outOfServiceBeds: List<Cas1OutOfServiceBedSummary>,
)
