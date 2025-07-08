package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ExternalUser
import java.time.Instant
import java.util.UUID

/**
 *
 * @param id
 * @param name
 * @param label
 * @param description
 * @param updatedBy
 * @param updatedAt
 * @param statusUpdateDetails
 */
data class Cas2StatusUpdate(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: UUID,

  @Schema(example = "moreInfoRequested", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: String,

  @Schema(example = "More information requested", required = true, description = "")
  @get:JsonProperty("label", required = true) val label: String,

  @Schema(example = "More information about the application has been requested from the POM (Prison Offender Manager).", required = true, description = "")
  @get:JsonProperty("description", required = true) val description: String,

  @Schema(example = "null", description = "")
  @get:JsonProperty("updatedBy") val updatedBy: ExternalUser? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("updatedAt") val updatedAt: Instant? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("statusUpdateDetails") val statusUpdateDetails: List<Cas2StatusUpdateDetail>? = null,
)
