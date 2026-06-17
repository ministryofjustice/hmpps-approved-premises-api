package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class Cas1NonArrival(

  @get:JsonProperty("reason", required = true) val reason: UUID,

  @get:JsonProperty("notes") val notes: String? = null,
)
