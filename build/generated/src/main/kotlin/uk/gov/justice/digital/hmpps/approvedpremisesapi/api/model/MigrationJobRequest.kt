package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param jobType 
 */
data class MigrationJobRequest(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("jobType", required = true) val jobType: MigrationJobType
) {

}

