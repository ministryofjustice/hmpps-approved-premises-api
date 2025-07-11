package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param dateTime
 * @param reasonId
 * @param moveOnCategoryId
 * @param notes
 * @param destinationProviderId
 */
data class NewDeparture(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("dateTime", required = true) val dateTime: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("reasonId", required = true) val reasonId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("moveOnCategoryId", required = true) val moveOnCategoryId: java.util.UUID,

  @Schema(example = "null", description = "")
  @get:JsonProperty("notes") val notes: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("destinationProviderId") val destinationProviderId: java.util.UUID? = null,
)
