package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

/**
 * 
 * @param createdByUserId 
 * @param status 
 * @param risks 
 */
data class TemporaryAccommodationApplicationSummary(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdByUserId", required = true) val createdByUserId: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("status", required = true) val status: ApplicationStatus,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) override val type: String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) override val id: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("person", required = true) override val person: Person,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) override val createdAt: Instant,

    @Schema(example = "null", description = "")
    @get:JsonProperty("risks") val risks: PersonRisks? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("submittedAt") override val submittedAt: Instant? = null
    ) : ApplicationSummary{

}

