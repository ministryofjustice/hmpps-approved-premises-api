package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonSummaryDiscriminator
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param crn 
 * @param personType 
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "personType", visible = true)
@JsonSubTypes(
      JsonSubTypes.Type(value = FullPersonSummary::class, name = "FullPersonSummary"),
      JsonSubTypes.Type(value = RestrictedPersonSummary::class, name = "RestrictedPersonSummary"),
      JsonSubTypes.Type(value = UnknownPersonSummary::class, name = "UnknownPersonSummary")
)

interface PersonSummary{
                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val crn: kotlin.String

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val personType: PersonSummaryDiscriminator


}

