package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 */
class UpdateCas2Application(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) override val type: UpdateApplicationType,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("data", required = true) override val `data`: Map<String, Any>,
) : UpdateApplication
