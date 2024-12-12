package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplicationType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param type 
 * @param &#x60;data&#x60; 
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(
      JsonSubTypes.Type(value = UpdateApprovedPremisesApplication::class, name = "CAS1"),
      JsonSubTypes.Type(value = UpdateCas2Application::class, name = "CAS2"),
      JsonSubTypes.Type(value = UpdateTemporaryAccommodationApplication::class, name = "CAS3")
)

interface UpdateApplication{
                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val type: UpdateApplicationType

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val `data`: kotlin.collections.Map<kotlin.String, kotlin.Any>


}

