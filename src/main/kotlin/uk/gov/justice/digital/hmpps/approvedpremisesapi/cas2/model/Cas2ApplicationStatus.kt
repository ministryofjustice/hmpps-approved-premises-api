package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 *
 * @param id
 * @param name
 * @param label
 * @param description
 * @param statusDetails
 */
data class Cas2ApplicationStatus(

  @get:JsonProperty("id", required = true) val id: UUID,

  @Schema(example = "moreInfoRequested", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: String,

  @Schema(example = "More information requested", required = true, description = "")
  @get:JsonProperty("label", required = true) val label: String,

  @Schema(example = "More information about the application has been requested from the POM (Prison Offender Manager).", required = true, description = "")
  @get:JsonProperty("description", required = true) val description: String,

  @get:JsonProperty("statusDetails", required = true) val statusDetails: List<Cas2ApplicationStatusDetail>,
)
