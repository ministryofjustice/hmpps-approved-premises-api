package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param crn
 * @param convictionId
 * @param deliusEventNumber
 * @param offenceId
 */
data class Cas3NewApplication(

  val crn: String,

  @Schema(example = "1502724704", description = "")
  val convictionId: Long? = null,

  @Schema(example = "7", description = "")
  val deliusEventNumber: String? = null,

  @Schema(example = "M1502750438", description = "")
  val offenceId: String? = null,
)
