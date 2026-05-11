package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.sar

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.sar.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.sar.DomainEventMetadata

// --- CAS3 (Temporary Accommodation) ---

data class Cas3SarData(
  @JsonProperty("Applications") val applications: List<Cas3Application>? = null,
  @JsonProperty("Assessments") val assessments: List<Cas3Assessment>? = null,
  @JsonProperty("AssessmentReferralHistoryNotes") val assessmentReferralHistoryNotes: List<Cas3AssessmentReferralHistoryNote>? = null,
  @JsonProperty("Bookings") val bookings: List<Cas3Booking>? = null,
  @JsonProperty("BookingExtensions") val bookingExtensions: List<Cas3BookingExtension>? = null,
  @JsonProperty("Cancellations") val cancellations: List<Cas3Cancellation>? = null,
  @JsonProperty("DomainEvents") val domainEvents: List<DomainEvent>? = null,
  @JsonProperty("DomainEventsMetadata") val domainEventsMetadata: List<DomainEventMetadata>? = null,
)

data class Cas3Application(
  val id: String? = null,
  @JsonProperty("crn") val crn: String? = null,
  @JsonProperty("created_at") val createdAt: String? = null,
  @JsonProperty("submitted_at") val submittedAt: String? = null,
  val data: Cas3ApplicationData? = null,
  val document: Map<String, Any>? = null,
)

data class Cas3ApplicationData(
  @JsonProperty("safeguarding-and-support") val safeguardingAndSupport: Map<String, Any>? = null,
  @JsonProperty("placement-location") val placementLocation: Map<String, Any>? = null,
  @JsonProperty("licence-conditions") val licenceConditions: Map<String, Any>? = null,
  @JsonProperty("offence-and-behaviour-summary") val offenceAndBehaviourSummary: Map<String, Any>? = null,
  @JsonProperty("accommodation-referral-details") val accommodationReferralDetails: Map<String, Any>? = null,
)

data class Cas3Assessment(
  val id: String? = null,
  @JsonProperty("application_id") val applicationId: String? = null,
  @JsonProperty("allocated_at") val allocatedAt: String? = null,
  @JsonProperty("created_at") val createdAt: String? = null,
  @JsonProperty("submitted_at") val submittedAt: String? = null,
  @JsonProperty("decision") val decision: String? = null,
  @JsonProperty("rejection_rationale") val rejectionRationale: String? = null,
)

data class Cas3AssessmentReferralHistoryNote(
  val id: String? = null,
  @JsonProperty("assessment_id") val assessmentId: String? = null,
  @JsonProperty("created_at") val createdAt: String? = null,
  @JsonProperty("message") val message: String? = null,
  @JsonProperty("created_by_user") val createdByUser: String? = null,
)

data class Cas3Booking(
  val id: String? = null,
  @JsonProperty("premises_name") val premisesName: String? = null,
  @JsonProperty("arrival_date") val arrivalDate: String? = null,
  @JsonProperty("departure_date") val departureDate: String? = null,
  @JsonProperty("created_at") val createdAt: String? = null,
)

data class Cas3BookingExtension(
  val id: String? = null,
  @JsonProperty("booking_id") val bookingId: String? = null,
  @JsonProperty("new_departure_date") val newDepartureDate: String? = null,
  @JsonProperty("previous_departure_date") val previousDepartureDate: String? = null,
  @JsonProperty("created_at") val createdAt: String? = null,
  @JsonProperty("notes") val notes: String? = null,
)

data class Cas3Cancellation(
  val id: String? = null,
  @JsonProperty("booking_id") val bookingId: String? = null,
  @JsonProperty("date") val date: String? = null,
  @JsonProperty("reason") val reason: String? = null,
  @JsonProperty("notes") val notes: String? = null,
  @JsonProperty("created_at") val createdAt: String? = null,
)
