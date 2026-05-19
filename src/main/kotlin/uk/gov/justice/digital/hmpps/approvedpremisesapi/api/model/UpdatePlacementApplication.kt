package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class UpdatePlacementApplication(

  @get:JsonProperty("data", required = true) val `data`: Map<String, Any>,
)
