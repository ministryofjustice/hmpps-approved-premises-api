package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param type 
 * @param expectedDepartureDate 
 * @param notes 
 * @param keyWorkerStaffCode 
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(
      JsonSubTypes.Type(value = NewCas2Arrival::class, name = "CAS2"),
      JsonSubTypes.Type(value = NewCas3Arrival::class, name = "CAS3")
)

interface NewArrival{
                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val type: kotlin.String

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val expectedDepartureDate: java.time.LocalDate

                @get:Schema(example = "null", description = "")
        val notes: kotlin.String? 

                @get:Schema(example = "null", description = "")
        val keyWorkerStaffCode: kotlin.String? 


}

