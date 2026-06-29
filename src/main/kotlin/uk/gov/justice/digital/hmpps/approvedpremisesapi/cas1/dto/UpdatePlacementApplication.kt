package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class UpdatePlacementApplication(

  @get:JsonProperty("data", required = true) val `data`: Map<String, Any>,
)
