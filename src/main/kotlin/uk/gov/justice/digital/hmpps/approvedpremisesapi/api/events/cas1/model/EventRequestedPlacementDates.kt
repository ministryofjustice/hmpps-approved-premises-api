package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.time.LocalDate

data class EventRequestedPlacementDates(
  val startDate: LocalDate,
  val endDate: LocalDate?,
)
