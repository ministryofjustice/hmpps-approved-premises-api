package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 */
class UnknownPerson(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("crn", required = true) override val crn: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) override val type: PersonType
) : Person{

}

