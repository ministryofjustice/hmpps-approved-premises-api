package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NonArrivalReason

/**
 *
 * @param id
 * @param bookingId
 * @param date
 * @param reason
 * @param createdAt
 * @param notes
 */
data class Cas3NonArrival(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("bookingId", required = true) val bookingId: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("date", required = true) val date: LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("reason", required = true) val reason: NonArrivalReason,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

    @Schema(example = "null", description = "")
    @get:JsonProperty("notes") val notes: String? = null
)

