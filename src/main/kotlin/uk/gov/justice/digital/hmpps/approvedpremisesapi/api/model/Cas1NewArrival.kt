package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param arrivalDateTime
 */
data class Cas1NewArrival(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("arrivalDateTime", required = true) val arrivalDateTime: java.time.Instant,
)
