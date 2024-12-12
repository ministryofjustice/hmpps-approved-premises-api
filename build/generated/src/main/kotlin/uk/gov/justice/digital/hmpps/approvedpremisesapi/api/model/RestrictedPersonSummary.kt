package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonSummaryDiscriminator
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 */
class RestrictedPersonSummary(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("crn", required = true) override val crn: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("personType", required = true) override val personType: PersonSummaryDiscriminator
) : PersonSummary{

}

