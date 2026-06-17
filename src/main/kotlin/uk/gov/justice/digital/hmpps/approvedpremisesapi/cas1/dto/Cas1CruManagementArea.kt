package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class Cas1CruManagementArea(

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("name", required = true) val name: String,
)
