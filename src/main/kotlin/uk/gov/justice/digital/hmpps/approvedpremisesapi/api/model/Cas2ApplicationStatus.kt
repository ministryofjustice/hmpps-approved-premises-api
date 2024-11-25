package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param name
 * @param label
 * @param description
 * @param statusDetails
 */
data class Cas2ApplicationStatus(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "moreInfoRequested", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @Schema(example = "More information requested", required = true, description = "")
  @get:JsonProperty("label", required = true) val label: kotlin.String,

  @Schema(example = "More information about the application has been requested from the POM (Prison Offender Manager).", required = true, description = "")
  @get:JsonProperty("description", required = true) val description: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("statusDetails", required = true) val statusDetails: kotlin.collections.List<Cas2ApplicationStatusDetail>,
)
