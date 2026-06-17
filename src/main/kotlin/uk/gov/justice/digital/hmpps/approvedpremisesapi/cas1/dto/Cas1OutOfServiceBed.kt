package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Temporality
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Cas1OutOfServiceBed(

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @get:JsonProperty("startDate", required = true) val startDate: LocalDate,

  @Schema(example = "null", required = true, description = "This date is inclusive. The bed will be unavailable for the whole of the day")
  @get:JsonProperty("endDate", required = true) val endDate: LocalDate,

  @get:JsonProperty("bed", required = true) val bed: NamedId,

  @get:JsonProperty("room", required = true) val room: NamedId,

  @get:JsonProperty("premises", required = true) val premises: NamedId,

  @get:JsonProperty("apArea", required = true) val apArea: NamedId,

  @get:JsonProperty("reason", required = true) val reason: Cas1OutOfServiceBedReason,

  @get:JsonProperty("daysLostCount", required = true) val daysLostCount: Int,

  @get:JsonProperty("temporality", required = true) val temporality: Temporality,

  @get:JsonProperty("status", required = true) val status: Cas1OutOfServiceBedStatus,

  @get:JsonProperty("revisionHistory", required = true) val revisionHistory: List<Cas1OutOfServiceBedRevision>,

  @get:JsonProperty("referenceNumber") val referenceNumber: String? = null,

  @get:JsonProperty("notes") val notes: String? = null,

  @get:JsonProperty("cancellation") val cancellation: Cas1OutOfServiceBedCancellation? = null,
)
