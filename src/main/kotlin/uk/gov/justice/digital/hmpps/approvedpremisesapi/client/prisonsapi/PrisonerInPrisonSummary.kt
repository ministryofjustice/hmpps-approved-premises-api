package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi

import java.time.LocalDateTime

data class PrisonerInPrisonSummary(
  val prisonPeriod: List<PrisonPeriod>?,
)

data class PrisonPeriod(
  val entryDate: LocalDateTime,
  val releaseDate: LocalDateTime? = null,
  val movementDates: List<SignificantMovements>,
)

data class SignificantMovements(
  val dateInToPrison: LocalDateTime? = null,
  val dateOutOfPrison: LocalDateTime? = null,
)
