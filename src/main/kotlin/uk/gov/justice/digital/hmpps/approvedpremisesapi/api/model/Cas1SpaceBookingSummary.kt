package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Cas1SpaceBookingSummary(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("person", required = true) val person: PersonSummary,

  @get:JsonProperty("premises", required = true) val premises: NamedId,

  @Schema(example = "null", required = true, description = "actual arrival date or, if not known, the expected arrival date")
  @get:JsonProperty("canonicalArrivalDate", required = true) val canonicalArrivalDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "actual departure date or, if not known, the expected departure date")
  @get:JsonProperty("canonicalDepartureDate", required = true) val canonicalDepartureDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "expected arrival date")
  @get:JsonProperty("expectedArrivalDate", required = true) val expectedArrivalDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "expected departure date")
  @get:JsonProperty("expectedDepartureDate", required = true) val expectedDepartureDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "Room and premise characteristics")
  @get:JsonProperty("characteristics", required = true) val characteristics: kotlin.collections.List<Cas1SpaceCharacteristic> = arrayListOf(),

  @get:JsonProperty("isCancelled", required = true) val isCancelled: kotlin.Boolean,

  @get:JsonProperty("openChangeRequestTypes", required = true) val openChangeRequestTypes: kotlin.collections.List<Cas1ChangeRequestType>,

  @Schema(example = "null", description = "actual arrival date if known")
  @get:JsonProperty("actualArrivalDate") val actualArrivalDate: java.time.LocalDate? = null,

  @Schema(example = "null", description = "actual departure date if known")
  @get:JsonProperty("actualDepartureDate") val actualDepartureDate: java.time.LocalDate? = null,

  @get:JsonProperty("isNonArrival") val isNonArrival: kotlin.Boolean? = null,

  @Schema(example = "null", description = "Risk rating tier level of corresponding application")
  @get:JsonProperty("tier") val tier: kotlin.String? = null,

  @get:JsonProperty("keyWorkerAllocation") val keyWorkerAllocation: Cas1KeyWorkerAllocation? = null,

  @get:JsonProperty("deliusEventNumber") val deliusEventNumber: kotlin.String? = null,

  @Schema(example = "null", description = "Use 'openChangeRequestTypes'")
  @Deprecated(message = "")
  @get:JsonProperty("plannedTransferRequested") val plannedTransferRequested: kotlin.Boolean? = null,

  @Schema(example = "null", description = "Use 'openChangeRequestTypes'")
  @Deprecated(message = "")
  @get:JsonProperty("appealRequested") val appealRequested: kotlin.Boolean? = null,

  @Schema(example = "null", description = "Booking creation datetime")
  @get:JsonProperty("createdAt") val createdAt: java.time.Instant? = null,
)
