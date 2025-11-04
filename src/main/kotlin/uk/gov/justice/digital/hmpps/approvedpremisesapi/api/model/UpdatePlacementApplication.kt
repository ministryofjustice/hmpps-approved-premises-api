package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class UpdatePlacementApplication(

  @get:JsonProperty("data", required = true) val `data`: kotlin.collections.Map<kotlin.String, kotlin.Any>,
)
