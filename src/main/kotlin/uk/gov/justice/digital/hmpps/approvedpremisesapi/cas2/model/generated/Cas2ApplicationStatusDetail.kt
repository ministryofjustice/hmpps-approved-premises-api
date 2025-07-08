package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 *
 * @param id
 * @param name
 * @param label
 */
data class Cas2ApplicationStatusDetail(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: UUID,

  @Schema(example = "changeOfCircumstances", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: String,

  @Schema(example = "Change of Circumstances", required = true, description = "")
  @get:JsonProperty("label", required = true) val label: String,
)
