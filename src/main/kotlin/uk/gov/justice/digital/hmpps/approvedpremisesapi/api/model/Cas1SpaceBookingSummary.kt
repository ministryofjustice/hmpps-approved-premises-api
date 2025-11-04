package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class Cas1SpaceBookingSummary(

  val id: java.util.UUID,

  val person: PersonSummary,

  val premises: NamedId,

  @Schema(example = "null", required = true, description = "actual arrival date or, if not known, the expected arrival date")
  val canonicalArrivalDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "actual departure date or, if not known, the expected departure date")
  val canonicalDepartureDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "expected arrival date")
  val expectedArrivalDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "expected departure date")
  val expectedDepartureDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "Room and premise characteristics")
  val characteristics: kotlin.collections.List<Cas1SpaceCharacteristic> = arrayListOf(),

  val isCancelled: kotlin.Boolean,

  val openChangeRequestTypes: kotlin.collections.List<Cas1ChangeRequestType>,

  @Schema(example = "null", description = "actual arrival date if known")
  val actualArrivalDate: java.time.LocalDate? = null,

  @Schema(example = "null", description = "actual departure date if known")
  val actualDepartureDate: java.time.LocalDate? = null,

  val isNonArrival: kotlin.Boolean? = null,

  @Schema(example = "null", description = "Risk rating tier level of corresponding application")
  val tier: kotlin.String? = null,

  val keyWorkerAllocation: Cas1KeyWorkerAllocation? = null,

  val deliusEventNumber: kotlin.String? = null,

  @Schema(example = "null", description = "Use 'openChangeRequestTypes'")
  @Deprecated(message = "")
  val plannedTransferRequested: kotlin.Boolean? = null,

  @Schema(example = "null", description = "Use 'openChangeRequestTypes'")
  @Deprecated(message = "")
  val appealRequested: kotlin.Boolean? = null,

  @Schema(example = "null", description = "Booking creation datetime")
  val createdAt: java.time.Instant? = null,
)
