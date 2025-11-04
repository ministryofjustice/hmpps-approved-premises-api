package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

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

  val id: UUID,

  @Schema(example = "moreInfoRequested", required = true, description = "")
  val name: String,

  @Schema(example = "More information requested", required = true, description = "")
  val label: String,

  @Schema(example = "More information about the application has been requested from the POM (Prison Offender Manager).", required = true, description = "")
  val description: String,

  val updatedBy: ExternalUser? = null,

  val updatedAt: Instant? = null,

  val statusUpdateDetails: List<Cas2StatusUpdateDetail>? = null,
)
