package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDateTime
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
data class Tier(
  val tierScore: String,
  val calculationId: UUID,
  val calculationDate: LocalDateTime,
  val changeReason: String?,
  val provisional: Boolean? = null,
)
