package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplicationType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 */
class UpdateCas2Application(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) override val type: UpdateApplicationType,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("data", required = true) override val `data`: kotlin.collections.Map<kotlin.String, kotlin.Any>
) : UpdateApplication{

}

