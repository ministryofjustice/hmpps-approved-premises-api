package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationTimeline
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param person 
 * @param applications 
 */
data class PersonalTimeline(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("person", required = true) val person: Person,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("applications", required = true) val applications: kotlin.collections.List<ApplicationTimeline>
) {

}

