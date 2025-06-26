package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import java.time.LocalDate
import java.util.UUID

data class Cas3NewBooking(
  val crn: String,
  val arrivalDate: LocalDate,
  val departureDate: LocalDate,
  val serviceName: ServiceName,
  val bedspaceId: UUID? = null,
  val enableTurnarounds: Boolean? = null,
  val assessmentId: UUID? = null,
  val eventNumber: String? = null,
)
