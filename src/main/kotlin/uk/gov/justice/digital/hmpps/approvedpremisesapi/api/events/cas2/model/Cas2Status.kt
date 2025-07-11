package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * A status which can be applied to a submitted application by an assessor
 * @param name
 * @param label
 * @param description
 * @param statusDetails
 */
data class Cas2Status(

  @Schema(example = "moreInfoRequested", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @Schema(example = "More information requested", required = true, description = "")
  @get:JsonProperty("label", required = true) val label: kotlin.String,

  @Schema(example = "More information about the application has been requested from the POM (Prison Offender Manager).", required = true, description = "")
  @get:JsonProperty("description", required = true) val description: kotlin.String,

  @Schema(example = "null", description = "")
  @get:JsonProperty("statusDetails") val statusDetails: kotlin.collections.List<Cas2StatusDetail>? = null,
)
