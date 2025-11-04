package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class Cas1OutOfServiceBed(

  val id: java.util.UUID,

  val createdAt: java.time.Instant,

  val startDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "This date is inclusive. The bed will be unavailable for the whole of the day")
  val endDate: java.time.LocalDate,

  val bed: NamedId,

  val room: NamedId,

  val premises: NamedId,

  val apArea: NamedId,

  val reason: Cas1OutOfServiceBedReason,

  val daysLostCount: kotlin.Int,

  val temporality: Temporality,

  val status: Cas1OutOfServiceBedStatus,

  val revisionHistory: kotlin.collections.List<Cas1OutOfServiceBedRevision>,

  val referenceNumber: kotlin.String? = null,

  val notes: kotlin.String? = null,

  val cancellation: Cas1OutOfServiceBedCancellation? = null,
)
