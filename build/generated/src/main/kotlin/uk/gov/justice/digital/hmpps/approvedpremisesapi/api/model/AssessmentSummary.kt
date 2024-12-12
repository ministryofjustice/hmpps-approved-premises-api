package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param type 
 * @param id 
 * @param applicationId 
 * @param createdAt 
 * @param person 
 * @param arrivalDate 
 * @param dateOfInfoRequest 
 * @param decision 
 * @param risks 
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(
      JsonSubTypes.Type(value = ApprovedPremisesAssessmentSummary::class, name = "CAS1"),
      JsonSubTypes.Type(value = TemporaryAccommodationAssessmentSummary::class, name = "CAS3")
)

interface AssessmentSummary{
                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val type: kotlin.String

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val id: java.util.UUID

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val applicationId: java.util.UUID

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val createdAt: java.time.Instant

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val person: Person

                @get:Schema(example = "null", description = "")
        val arrivalDate: java.time.Instant? 

                @get:Schema(example = "null", description = "")
        val dateOfInfoRequest: java.time.Instant? 

                @get:Schema(example = "null", description = "")
        val decision: AssessmentDecision? 

                @get:Schema(example = "null", description = "")
        val risks: PersonRisks? 


}

