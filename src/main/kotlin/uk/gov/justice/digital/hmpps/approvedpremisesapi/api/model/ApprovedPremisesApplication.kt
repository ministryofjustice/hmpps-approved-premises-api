package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class ApprovedPremisesApplication(

  @get:JsonProperty("createdByUserId", required = true) val createdByUserId: java.util.UUID,

  @get:JsonProperty("status", required = true) val status: ApprovedPremisesApplicationStatus,

  @get:JsonProperty("type", required = true) override val type: kotlin.String,

  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @get:JsonProperty("person", required = true) override val person: Person,

  @get:JsonProperty("createdAt", required = true) override val createdAt: java.time.Instant,

  @get:JsonProperty("isWomensApplication") val isWomensApplication: kotlin.Boolean? = null,

  @get:JsonProperty("isEmergencyApplication") val isEmergencyApplication: kotlin.Boolean? = null,

  @Schema(description = "Use apType")
  @Deprecated(message = "Use apType")
  @get:JsonProperty("isEsapApplication") val isEsapApplication: kotlin.Boolean? = null,

  @get:JsonProperty("apType") val apType: ApType? = null,

  @get:JsonProperty("arrivalDate") val arrivalDate: java.time.Instant? = null,

  @get:JsonProperty("risks") val risks: PersonRisks? = null,

  @get:JsonProperty("data") val `data`: kotlin.Any? = null,

  @get:JsonProperty("document") val document: kotlin.Any? = null,

  @get:JsonProperty("assessmentId") val assessmentId: java.util.UUID? = null,

  @get:JsonProperty("assessmentDecision") val assessmentDecision: AssessmentDecision? = null,

  @get:JsonProperty("assessmentDecisionDate") val assessmentDecisionDate: java.time.LocalDate? = null,

  @get:JsonProperty("submittedAt") val submittedAt: java.time.Instant? = null,

  @get:JsonProperty("personStatusOnSubmission") val personStatusOnSubmission: PersonStatus? = null,

  @get:JsonProperty("apArea") val apArea: ApArea? = null,

  @get:JsonProperty("cruManagementArea") val cruManagementArea: Cas1CruManagementArea? = null,

  @get:JsonProperty("applicantUserDetails") val applicantUserDetails: Cas1ApplicationUserDetails? = null,

  @Schema(description = "If true, caseManagerUserDetails will provide case manager details. Otherwise, applicantUserDetails can be used for case manager details")
  @get:JsonProperty("caseManagerIsNotApplicant") val caseManagerIsNotApplicant: kotlin.Boolean? = null,

  @get:JsonProperty("caseManagerUserDetails") val caseManagerUserDetails: Cas1ApplicationUserDetails? = null,

  @get:JsonProperty("licenceExpiryDate") val licenceExpiryDate: java.time.LocalDate? = null,
) : Application
