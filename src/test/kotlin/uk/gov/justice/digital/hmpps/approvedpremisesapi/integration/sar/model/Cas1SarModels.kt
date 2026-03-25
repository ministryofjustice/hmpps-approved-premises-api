package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.sar.model

import com.fasterxml.jackson.annotation.JsonProperty


data class Cas1SarData(
  @JsonProperty("Applications") val applications: List<Cas1Application>? = null,
  @JsonProperty("ApplicationTimeline") val applicationTimeline: List<Map<String, Any>>? = null,
  @JsonProperty("Assessments") val assessments: List<Cas1Assessment>? = null,
  @JsonProperty("AssessmentClarificationNotes") val assessmentClarificationNotes: List<Cas1AssessmentClarificationNote>? = null,
  @JsonProperty("SpaceBookings") val spaceBookings: List<Cas1SpaceBooking>? = null,
  @JsonProperty("OfflineApplications") val offlineApplications: List<Cas1OfflineApplication>? = null,
  @JsonProperty("Appeals") val appeals: List<Cas1Appeal>? = null,
  @JsonProperty("PlacementApplications") val placementApplications: List<Cas1PlacementApplication>? = null,
  @JsonProperty("PlacementRequests") val placementRequests: List<Cas1PlacementRequest>? = null,
  @JsonProperty("PlacementRequirements") val placementRequirements: List<Cas1PlacementRequirements>? = null,
  @JsonProperty("PlacementRequirementCriteria") val placementRequirementCriteria: List<Cas1PlacementRequirementCriteria>? = null,
  @JsonProperty("BookingNotMades") val bookingNotMades: List<Cas1BookingNotMade>? = null,
  @JsonProperty("DomainEvents") val domainEvents: List<DomainEvent>? = null,
  @JsonProperty("DomainEventsMetadata") val domainEventsMetadata: List<DomainEventMetadata>? = null,
)

data class Cas1Application(
  val id: String? = null,
  @JsonProperty("offence_id") val offenceId: String? = null,
  @JsonProperty("application_user_name") val applicationUserName: String? = null,
  @JsonProperty("sentence_type") val sentenceType: String? = null,
  @JsonProperty("submitted_at") val submittedAt: String? = null,
  @JsonProperty("withdrawal_reason") val withdrawalReason: String? = null,
  @JsonProperty("ap_type") val apType: String? = null,
  @JsonProperty("created_at") val createdAt: String? = null,
  @JsonProperty("is_withdrawn") val isWithdrawn: Boolean? = null,
  @JsonProperty("inmate_in_out_status_on_submission") val inmateInOutStatusOnSubmission: String? = null,
  @JsonProperty("created_by_user") val createdByUser: String? = null,
  @JsonProperty("notice_type") val noticeType: String? = null,
  @JsonProperty("arrival_date") val arrivalDate: String? = null,
  @JsonProperty("target_location") val targetLocation: String? = null,
  @JsonProperty("conviction_id") val convictionId: String? = null,
  @JsonProperty("case_manager_name") val caseManagerName: String? = null,
  @JsonProperty("is_emergency_application") val isEmergencyApplication: Boolean? = null,
  @JsonProperty("release_type") val releaseType: String? = null,
  val name: String? = null,
  @JsonProperty("case_manager_is_not_applicant") val caseManagerIsNotApplicant: Boolean? = null,
  @JsonProperty("event_number") val eventNumber: String? = null,
  @JsonProperty("is_womens_application") val isWomensApplication: Boolean? = null,
  @JsonProperty("other_withdrawal_reason") val otherWithdrawalReason: String? = null,
  val status: String? = null,
  val data: Cas1ApplicationData? = null,
  val document: Map<String, Any>? = null,
)

data class Cas1ApplicationData(
  @JsonProperty("type-of-ap") val typeOfAp: TypeOfAp? = null,
  @JsonProperty("check-your-answers") val checkYourAnswers: CheckYourAnswers? = null,
  @JsonProperty("oasys-import") val oasysImport: OasysImport? = null,
  @JsonProperty("risk-management-features") val riskManagementFeatures: Map<String, Any>? = null,
  @JsonProperty("move-on") val moveOn: Map<String, Any>? = null,
  @JsonProperty("access-and-healthcare") val accessAndHealthcare: Map<String, Any>? = null,
  @JsonProperty("further-considerations") val furtherConsiderations: Map<String, Any>? = null,
  @JsonProperty("risk-to-self") val riskToSelf: Map<String, Any>? = null,
  @JsonProperty("supporting-information") val supportingInformation: Map<String, Any>? = null,
  @JsonProperty("offence-details") val offenceDetails: Map<String, Any>? = null,
  @JsonProperty("risk-management-plan") val riskManagementPlan: Map<String, Any>? = null,
  @JsonProperty("placement-location") val placementLocation: Map<String, Any>? = null,
)

data class TypeOfAp(val apType: ApTypeDetail? = null)
data class ApTypeDetail(val type: String? = null)
data class CheckYourAnswers(val review: ReviewDetail? = null)
data class ReviewDetail(val reviewed: Boolean? = null)

data class OasysImport(
  @JsonProperty("rosh-summary") val roshSummary: RoshSummary? = null,
  @JsonProperty("risk-management-plan") val riskManagementPlan: RiskManagementPlan? = null,
  @JsonProperty("offence-details") val offenceDetails: OffenceDetails? = null,
  @JsonProperty("supporting-information") val supportingInformation: SupportingInformation? = null,
)

data class RoshSummary(val roshSummaries: List<OasysSummary>? = null)
data class RiskManagementPlan(val riskManagementSummaries: List<OasysSummary>? = null)
data class OffenceDetails(val offenceDetailsSummaries: List<OasysSummary>? = null)
data class SupportingInformation(val supportingInformationSummaries: List<OasysSummary>? = null)

data class OasysSummary(
  val questionNumber: String? = null,
  val label: String? = null,
  val answer: String? = null,
)

data class Cas1Assessment(
  val id: String? = null,
  @JsonProperty("application_id") val applicationId: String? = null,
  @JsonProperty("assessment_user") val assessmentUser: String? = null,
  @JsonProperty("created_at") val createdAt: String? = null,
  @JsonProperty("allocated_at") val allocatedAt: String? = null,
  @JsonProperty("submitted_at") val submittedAt: String? = null,
  @JsonProperty("decision") val decision: String? = null,
  @JsonProperty("rejection_rationale") val rejectionRationale: String? = null,
  @JsonProperty("is_withdrawn") val isWithdrawn: Boolean? = null,
  @JsonProperty("service") val service: String? = null,
)

data class Cas1AssessmentClarificationNote(
  val id: String? = null,
  @JsonProperty("assessment_id") val assessmentId: String? = null,
  @JsonProperty("assessment_user") val assessmentUser: String? = null,
  @JsonProperty("created_at") val createdAt: String? = null,
  @JsonProperty("query") val query: String? = null,
  @JsonProperty("response") val response: String? = null,
  @JsonProperty("response_received_on") val responseReceivedOn: String? = null,
)

data class Cas1SpaceBooking(
  val id: String? = null,
  @JsonProperty("application_id") val applicationId: String? = null,
  @JsonProperty("offline_application_id") val offlineApplicationId: String? = null,
  @JsonProperty("premises_name") val premisesName: String? = null,
  @JsonProperty("ap_type") val apType: String? = null,
  @JsonProperty("tier") val tier: String? = null,
  @JsonProperty("arrival_date") val arrivalDate: String? = null,
  @JsonProperty("departure_date") val departureDate: String? = null,
  @JsonProperty("actual_arrival_date") val actualArrivalDate: String? = null,
  @JsonProperty("actual_arrival_time") val actualArrivalTime: String? = null,
  @JsonProperty("actual_departure_date") val actualDepartureDate: String? = null,
  @JsonProperty("actual_departure_time") val actualDepartureTime: String? = null,
  @JsonProperty("canonical_arrival_date") val canonicalArrivalDate: String? = null,
  @JsonProperty("canonical_departure_date") val canonicalDepartureDate: String? = null,
  @JsonProperty("crn") val crn: String? = null,
  @JsonProperty("key_worker_name") val keyWorkerName: String? = null,
  @JsonProperty("key_worker_staff_code") val keyWorkerStaffCode: String? = null,
  @JsonProperty("created_at") val createdAt: String? = null,
  @JsonProperty("expected_arrival_time") val expectedArrivalTime: String? = null,
  @JsonProperty("expected_departure_time") val expectedDepartureTime: String? = null,
  @JsonProperty("delius_event_number") val deliusEventNumber: String? = null,
  @JsonProperty("non_arrival_confirmed_at") val nonArrivalConfirmedAt: String? = null,
  @JsonProperty("non_arrival_reason") val nonArrivalReason: String? = null,
  @JsonProperty("non_arrival_notes") val nonArrivalNotes: String? = null,
  @JsonProperty("departure_reason") val departureReason: String? = null,
  @JsonProperty("departure_move_on_category") val departureMoveOnCategory: String? = null,
  @JsonProperty("departure_notes") val departureNotes: String? = null,
  @JsonProperty("cancellation_occurred_at") val cancellationOccurredAt: String? = null,
  @JsonProperty("cancellation_recorded_at") val cancellationRecordedAt: String? = null,
  @JsonProperty("cancellation_reason") val cancellationReason: String? = null,
  @JsonProperty("cancellation_reason_notes") val cancellationReasonNotes: String? = null,
)

data class Cas1OfflineApplication(
  val id: String? = null,
  @JsonProperty("created_at") val createdAt: String? = null,
  @JsonProperty("event_number") val eventNumber: String? = null,
)

data class Cas1Appeal(
  @JsonProperty("appeal_id") val appealId: String? = null,
  @JsonProperty("assessment_id") val assessmentId: String? = null,
  @JsonProperty("application_id") val applicationId: String? = null,
  @JsonProperty("appeal_date") val appealDate: String? = null,
  @JsonProperty("appeal_created_at") val appealCreatedAt: String? = null,
  @JsonProperty("created_by_user") val createdByUser: String? = null,
  @JsonProperty("decision") val decision: String? = null,
  @JsonProperty("appeal_detail") val appealDetail: String? = null,
  @JsonProperty("decision_detail") val decisionDetail: String? = null,
)

data class Cas1PlacementApplication(
  val id: String? = null,
  @JsonProperty("application_id") val applicationId: String? = null,
  @JsonProperty("created_by_user") val createdByUser: String? = null,
  @JsonProperty("created_at") val createdAt: String? = null,
  @JsonProperty("submitted_at") val submittedAt: String? = null,
  @JsonProperty("decision") val decision: String? = null,
  @JsonProperty("decision_made_at") val decisionMadeAt: String? = null,
  @JsonProperty("withdrawal_reason") val withdrawalReason: String? = null,
  @JsonProperty("placement_type") val placementType: String? = null,
  val data: Map<String, Any>? = null,
)

data class Cas1PlacementRequest(
  val id: String? = null,
  @JsonProperty("application_id") val applicationId: String? = null,
  @JsonProperty("assessment_id") val assessmentId: String? = null,
  @JsonProperty("placement_requirements_id") val placementRequirementsId: String? = null,
  @JsonProperty("booking_id") val bookingId: String? = null,
  @JsonProperty("space_booking_id") val spaceBookingId: String? = null,
  @JsonProperty("created_at") val createdAt: String? = null,
  @JsonProperty("expected_arrival") val expectedArrival: String? = null,
  @JsonProperty("duration") val duration: Int? = null,
  @JsonProperty("is_withdrawn") val isWithdrawn: Boolean? = null,
  @JsonProperty("withdrawal_reason") val withdrawalReason: String? = null,
)

data class Cas1PlacementRequirements(
  val id: String? = null,
  @JsonProperty("application_id") val applicationId: String? = null,
  @JsonProperty("assessment_id") val assessmentId: String? = null,
  @JsonProperty("created_at") val createdAt: String? = null,
  @JsonProperty("gender") val gender: String? = null,
  @JsonProperty("ap_type") val apType: String? = null,
  @JsonProperty("postcode") val postcode: String? = null,
  @JsonProperty("radius") val radius: Int? = null,
)

data class Cas1PlacementRequirementCriteria(
  @JsonProperty("placement_requirements_id") val placementRequirementsId: String? = null,
  @JsonProperty("criterion_id") val criterionId: String? = null,
  @JsonProperty("criterion_name") val criterionName: String? = null,
)

data class Cas1BookingNotMade(
  val id: String? = null,
  @JsonProperty("placement_request_id") val placementRequestId: String? = null,
  @JsonProperty("created_at") val createdAt: String? = null,
  @JsonProperty("notes") val notes: String? = null,
)
