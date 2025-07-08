package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplicationType

/**
 * 
 */
class UpdateCas2Application(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) override val type: UpdateApplicationType,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("data", required = true) override val `data`: Map<String, Any>
    ) : UpdateApplication {

}

