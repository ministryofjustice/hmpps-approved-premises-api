package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param createdByUserId
 * @param schemaVersion
 * @param outdatedSchema
 * @param status
 * @param isWomensApplication
 * @param isPipeApplication Use apType
 * @param isEmergencyApplication
 * @param isEsapApplication Use apType
 * @param apType
 * @param arrivalDate
 * @param risks
 * @param &#x60;data&#x60; Any object that conforms to the current JSON schema for an application
 * @param document Any object that conforms to the current JSON schema for an application
 * @param assessmentId
 * @param assessmentDecision
 * @param assessmentDecisionDate
 * @param submittedAt
 * @param personStatusOnSubmission
 * @param apArea
 * @param cruManagementArea
 * @param applicantUserDetails
 * @param caseManagerIsNotApplicant If true, caseManagerUserDetails will provide case manager details. Otherwise, applicantUserDetails can be used for case manager details
 * @param caseManagerUserDetails
 * @param genderForAp
 */
data class ApprovedPremisesApplication(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdByUserId", required = true) val createdByUserId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("schemaVersion", required = true) val schemaVersion: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("outdatedSchema", required = true) val outdatedSchema: kotlin.Boolean,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("status", required = true) val status: ApprovedPremisesApplicationStatus,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) override val type: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("person", required = true) override val person: Person,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdAt", required = true) override val createdAt: java.time.Instant,

  @Schema(example = "null", description = "")
  @get:JsonProperty("isWomensApplication") val isWomensApplication: kotlin.Boolean? = null,

  @Schema(example = "null", description = "Use apType")
  @Deprecated(message = "")
  @get:JsonProperty("isPipeApplication") val isPipeApplication: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("isEmergencyApplication") val isEmergencyApplication: kotlin.Boolean? = null,

  @Schema(example = "null", description = "Use apType")
  @Deprecated(message = "")
  @get:JsonProperty("isEsapApplication") val isEsapApplication: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("apType") val apType: ApType? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("arrivalDate") val arrivalDate: java.time.Instant? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("risks") val risks: PersonRisks? = null,

  @Schema(example = "null", description = "Any object that conforms to the current JSON schema for an application")
  @get:JsonProperty("data") val `data`: kotlin.Any? = null,

  @Schema(example = "null", description = "Any object that conforms to the current JSON schema for an application")
  @get:JsonProperty("document") val document: kotlin.Any? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("assessmentId") val assessmentId: java.util.UUID? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("assessmentDecision") val assessmentDecision: AssessmentDecision? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("assessmentDecisionDate") val assessmentDecisionDate: java.time.LocalDate? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("submittedAt") val submittedAt: java.time.Instant? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("personStatusOnSubmission") val personStatusOnSubmission: PersonStatus? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("apArea") val apArea: ApArea? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("cruManagementArea") val cruManagementArea: Cas1CruManagementArea? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("applicantUserDetails") val applicantUserDetails: Cas1ApplicationUserDetails? = null,

  @Schema(example = "null", description = "If true, caseManagerUserDetails will provide case manager details. Otherwise, applicantUserDetails can be used for case manager details")
  @get:JsonProperty("caseManagerIsNotApplicant") val caseManagerIsNotApplicant: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("caseManagerUserDetails") val caseManagerUserDetails: Cas1ApplicationUserDetails? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("genderForAp") val genderForAp: GenderForAp? = null,
) : Application
