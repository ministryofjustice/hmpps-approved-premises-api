package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param &#x60;data&#x60;
 */
data class UpdatePlacementApplication(

  @get:JsonProperty("data", required = true) val `data`: kotlin.collections.Map<kotlin.String, kotlin.Any>,
)
