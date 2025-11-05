package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Cas1OutOfServiceBed(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @get:JsonProperty("startDate", required = true) val startDate: java.time.LocalDate,

  @field:Schema(example = "null", required = true, description = "This date is inclusive. The bed will be unavailable for the whole of the day")
  @get:JsonProperty("endDate", required = true) val endDate: java.time.LocalDate,

  @get:JsonProperty("bed", required = true) val bed: NamedId,

  @get:JsonProperty("room", required = true) val room: NamedId,

  @get:JsonProperty("premises", required = true) val premises: NamedId,

  @get:JsonProperty("apArea", required = true) val apArea: NamedId,

  @get:JsonProperty("reason", required = true) val reason: Cas1OutOfServiceBedReason,

  @get:JsonProperty("daysLostCount", required = true) val daysLostCount: kotlin.Int,

  @get:JsonProperty("temporality", required = true) val temporality: Temporality,

  @get:JsonProperty("status", required = true) val status: Cas1OutOfServiceBedStatus,

  @get:JsonProperty("revisionHistory", required = true) val revisionHistory: kotlin.collections.List<Cas1OutOfServiceBedRevision>,

  @get:JsonProperty("referenceNumber") val referenceNumber: kotlin.String? = null,

  @get:JsonProperty("notes") val notes: kotlin.String? = null,

  @get:JsonProperty("cancellation") val cancellation: Cas1OutOfServiceBedCancellation? = null,
)
