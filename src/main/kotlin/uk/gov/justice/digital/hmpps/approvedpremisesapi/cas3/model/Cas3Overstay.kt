package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Cas3Overstay(
  val id: UUID,
  val bookingId: UUID,
  val previousDepartureDate: LocalDate,
  val newDepartureDate: LocalDate,
  val isAuthorised: Boolean,
  val createdAt: Instant,
  val reason: String? = null,
)
