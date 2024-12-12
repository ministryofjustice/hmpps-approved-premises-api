package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param crn 
 * @param type 
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(
      JsonSubTypes.Type(value = FullPerson::class, name = "FullPerson"),
      JsonSubTypes.Type(value = RestrictedPerson::class, name = "RestrictedPerson"),
      JsonSubTypes.Type(value = UnknownPerson::class, name = "UnknownPerson")
)

interface Person{
                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val crn: kotlin.String

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val type: PersonType


}

