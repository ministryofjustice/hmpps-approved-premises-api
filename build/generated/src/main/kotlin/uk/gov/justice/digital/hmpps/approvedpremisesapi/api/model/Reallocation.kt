package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param user 
 * @param taskType 
 */
data class Reallocation(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("user", required = true) val user: ApprovedPremisesUser,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("taskType", required = true) val taskType: TaskType
) {

}

