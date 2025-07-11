package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param alertId
 * @param dateCreated
 * @param comment
 * @param description
 * @param dateExpires
 * @param alertTypeDescription
 */
data class PersonAcctAlert(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("alertId", required = true) val alertId: kotlin.Long,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("dateCreated", required = true) val dateCreated: java.time.LocalDate,

  @Schema(example = "null", description = "")
  @get:JsonProperty("comment") val comment: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("description") val description: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("dateExpires") val dateExpires: java.time.LocalDate? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("alertTypeDescription") val alertTypeDescription: kotlin.String? = null,
)
