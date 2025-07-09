package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param type 
 * @param translatedDocument Any object
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(
      JsonSubTypes.Type(value = SubmitApprovedPremisesApplication::class, name = "CAS1"),
      JsonSubTypes.Type(value = SubmitCas2Application::class, name = "CAS2"),
      JsonSubTypes.Type(value = SubmitTemporaryAccommodationApplication::class, name = "CAS3")
)

interface SubmitApplication{
                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
                val type: kotlin.String
                @get:Schema(example = "null", description = "Any object")
                val translatedDocument: kotlin.Any? 

}

