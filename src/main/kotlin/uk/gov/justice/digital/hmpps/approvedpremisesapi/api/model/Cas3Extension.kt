package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 *
 * @param id
 * @param bookingId
 * @param previousDepartureDate
 * @param newDepartureDate
 * @param createdAt
 * @param notes
 */
data class Cas3Extension(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("bookingId", required = true) val bookingId: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("previousDepartureDate", required = true) val previousDepartureDate: LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("newDepartureDate", required = true) val newDepartureDate: LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

    @Schema(example = "null", description = "")
    @get:JsonProperty("notes") val notes: String? = null
)

