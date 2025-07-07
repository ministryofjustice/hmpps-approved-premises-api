package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks

/**
 *
 * @param id
 * @param person
 * @param createdAt
 * @param createdByUserId
 * @param schemaVersion
 * @param outdatedSchema
 * @param status
 * @param offenceId
 * @param &#x60;data&#x60; Any object
 * @param document Any object
 * @param risks
 * @param submittedAt
 * @param arrivalDate
 * @param assessmentId
 */
data class Cas3Application(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("person", required = true) val person: Person,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdByUserId", required = true) val createdByUserId: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("status", required = true) val status: ApplicationStatus,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("offenceId", required = true) val offenceId: String,

    @Schema(example = "null", description = "Any object")
    @get:JsonProperty("data") val `data`: Any? = null,

    @Schema(example = "null", description = "Any object")
    @get:JsonProperty("document") val document: Any? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("risks") val risks: PersonRisks? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("submittedAt") val submittedAt: Instant? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("arrivalDate") val arrivalDate: Instant? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("assessmentId") val assessmentId: UUID? = null
)

