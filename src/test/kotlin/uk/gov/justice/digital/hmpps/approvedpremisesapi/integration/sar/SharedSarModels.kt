package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.sar

import com.fasterxml.jackson.annotation.JsonProperty

// --- Shared Models ---

data class DomainEvent(
  val id: String? = null,
  @JsonProperty("application_id") val applicationId: String? = null,
  @JsonProperty("assessment_id") val assessmentId: String? = null,
  @JsonProperty("booking_id") val bookingId: String? = null,
  @JsonProperty("crn") val crn: String? = null,
  @JsonProperty("type") val type: String? = null,
  @JsonProperty("occurred_at") val occurredAt: String? = null,
  @JsonProperty("created_at") val createdAt: String? = null,
  val data: Any? = null,
  val service: String? = null,
)

data class DomainEventMetadata(
  @JsonProperty("domain_event_id") val domainEventId: String? = null,
  @JsonProperty("metadata_key") val metadataKey: String? = null,
  @JsonProperty("metadata_value") val metadataValue: String? = null,
)
