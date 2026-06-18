package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import java.time.LocalDate

data class Cas1PremisesDaySummary(
  val forDate: LocalDate,
  val previousDate: LocalDate,
  val nextDate: LocalDate,
  val spaceBookingSummaries: List<Cas1SpaceBookingSummary>,
  val outOfServiceBeds: List<Cas1OutOfServiceBedSummary>,
)
