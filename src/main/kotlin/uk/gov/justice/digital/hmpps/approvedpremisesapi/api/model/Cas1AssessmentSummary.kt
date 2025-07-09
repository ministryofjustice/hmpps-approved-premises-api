package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id 
 * @param applicationId 
 * @param createdAt 
 * @param person 
 * @param status 
 * @param dueAt 
 * @param arrivalDate 
 * @param dateOfInfoRequest 
 * @param decision 
 * @param risks 
 */
data class Cas1AssessmentSummary(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("person", required = true) val person: Person,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("status", required = true) val status: Cas1AssessmentStatus,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("dueAt", required = true) val dueAt: java.time.Instant,

    @Schema(example = "null", description = "")
    @get:JsonProperty("arrivalDate") val arrivalDate: java.time.Instant? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("dateOfInfoRequest") val dateOfInfoRequest: java.time.Instant? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("decision") val decision: AssessmentDecision? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("risks") val risks: PersonRisks? = null
    ) {

}

