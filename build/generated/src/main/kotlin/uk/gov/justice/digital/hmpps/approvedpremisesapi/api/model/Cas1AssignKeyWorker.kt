package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param staffCode 
 */
data class Cas1AssignKeyWorker(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("staffCode", required = true) val staffCode: kotlin.String
) {

}

