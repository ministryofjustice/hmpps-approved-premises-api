package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier

import java.time.LocalDateTime
import java.util.UUID

data class Tier(
  val tierScore: String,
  val calculationId: UUID,
  val calculationDate: LocalDateTime,
)
