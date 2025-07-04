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
 * @param status
 * @param submittedAt
 * @param risks
 */
data class Cas3ApplicationSummary(

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

    @Schema(example = "null", description = "")
    @get:JsonProperty("submittedAt") val submittedAt: Instant? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("risks") val risks: PersonRisks? = null
)

