package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.util.UUID

data class EventBookingSummary(
  val id: UUID,
  val premises: Premises,
  val arrivalDate: java.time.LocalDate,
  val departureDate: java.time.LocalDate,
)
