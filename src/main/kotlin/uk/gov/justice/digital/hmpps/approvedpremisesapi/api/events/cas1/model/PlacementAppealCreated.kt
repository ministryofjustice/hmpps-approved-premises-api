package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class PlacementAppealCreated(
  @Schema(example = "14c80733-4b6d-4f35-b724-66955aac320c", required = true)
  @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

  @Schema(example = "null", required = true)
  @get:JsonProperty("premises", required = true) val premises: Premises,

  @Schema(example = "Mon Jan 30 00:00:00 GMT 2023", required = true)
  @get:JsonProperty("arrivalOn", required = true) val arrivalOn: java.time.LocalDate,

  @Schema(example = "Sun Apr 30 01:00:00 BST 2023", required = true)
  @get:JsonProperty("departureOn", required = true) val departureOn: java.time.LocalDate,

  @get:JsonProperty("requestedBy") val requestedBy: StaffMember,

  @get:JsonProperty("appealReason") val appealReason: Cas1DomainEventCodedId,
) : Cas1DomainEventPayload