package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class PlacementAppealRejected(
  @Schema(required = true)
  @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

  @Schema(required = true)
  @get:JsonProperty("premises", required = true) val premises: Premises,

  @Schema(required = true)
  @get:JsonProperty("arrivalOn", required = true) val arrivalOn: java.time.LocalDate,

  @Schema(required = true)
  @get:JsonProperty("departureOn", required = true) val departureOn: java.time.LocalDate,

  @get:JsonProperty("rejectedBy") val rejectedBy: StaffMember,

  @get:JsonProperty("rejectionReason") val rejectionReason: Cas1DomainEventCodedId,
) : Cas1DomainEventPayload