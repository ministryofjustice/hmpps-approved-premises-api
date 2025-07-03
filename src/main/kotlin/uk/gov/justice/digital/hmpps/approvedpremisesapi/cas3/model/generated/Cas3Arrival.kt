package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 *
 * @param expectedDepartureDate
 * @param arrivalDate
 * @param arrivalTime
 * @param bookingId
 * @param createdAt
 * @param notes
 */
data class Cas3Arrival(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("expectedDepartureDate", required = true) val expectedDepartureDate: LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("arrivalDate", required = true) val arrivalDate: LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("arrivalTime", required = true) val arrivalTime: String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("bookingId", required = true) val bookingId: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

    @Schema(example = "null", description = "")
    @get:JsonProperty("notes") val notes: String? = null
)

