package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param person
 * @param canonicalArrivalDate actual arrival date or, if not known, the expected arrival date
 * @param canonicalDepartureDate actual departure date or, if not known, the expected departure date
 * @param tier Risk rating tier level of corresponding application
 * @param keyWorkerAllocation
 * @param status
 */
data class Cas1SpaceBookingSummary(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("person", required = true) val person: PersonSummary,

  @Schema(example = "null", required = true, description = "actual arrival date or, if not known, the expected arrival date")
  @get:JsonProperty("canonicalArrivalDate", required = true) val canonicalArrivalDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "actual departure date or, if not known, the expected departure date")
  @get:JsonProperty("canonicalDepartureDate", required = true) val canonicalDepartureDate: java.time.LocalDate,

  @Schema(example = "null", description = "Risk rating tier level of corresponding application")
  @get:JsonProperty("tier") val tier: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("keyWorkerAllocation") val keyWorkerAllocation: Cas1KeyWorkerAllocation? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("status") val status: Cas1SpaceBookingSummaryStatus? = null,
)
