package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param userId
 */
data class NewReallocation(

  @Schema(example = "null", description = "")
  @get:JsonProperty("userId") val userId: java.util.UUID? = null,
)
