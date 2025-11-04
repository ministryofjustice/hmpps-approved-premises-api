package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param id
 * @param name
 * @param isActive
 * @param serviceScope
 */
data class CancellationReason(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @get:JsonProperty("isActive", required = true) val isActive: kotlin.Boolean,

  @get:JsonProperty("serviceScope", required = true) val serviceScope: kotlin.String,
)
