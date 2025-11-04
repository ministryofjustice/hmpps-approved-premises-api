package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param person
 * @param createdAt
 * @param createdByUserId
 * @param status
 * @param isWomensApplication
 * @param isPipeApplication Use apType
 * @param isEmergencyApplication
 * @param isEsapApplication Use apType
 * @param apType
 * @param arrivalDate
 * @param risks
 * @param &#x60;data&#x60; Any object
 * @param document Any object
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
 * @param licenceExpiryDate
 */
data class Cas1Application(

  val id: java.util.UUID,

  val person: Person,

  val createdAt: java.time.Instant,

  val createdByUserId: java.util.UUID,

  val status: ApprovedPremisesApplicationStatus,

  val isWomensApplication: kotlin.Boolean? = null,

  @Schema(example = "null", description = "Use apType")
  @Deprecated(message = "")
  val isPipeApplication: kotlin.Boolean? = null,

  val isEmergencyApplication: kotlin.Boolean? = null,

  @Schema(example = "null", description = "Use apType")
  @Deprecated(message = "")
  val isEsapApplication: kotlin.Boolean? = null,

  val apType: ApType? = null,

  val arrivalDate: java.time.Instant? = null,

  val risks: PersonRisks? = null,

  val `data`: kotlin.Any? = null,

  val document: kotlin.Any? = null,

  val assessmentId: java.util.UUID? = null,

  val assessmentDecision: AssessmentDecision? = null,

  val assessmentDecisionDate: java.time.LocalDate? = null,

  val submittedAt: java.time.Instant? = null,

  val personStatusOnSubmission: PersonStatus? = null,

  val apArea: ApArea? = null,

  val cruManagementArea: Cas1CruManagementArea? = null,

  val applicantUserDetails: Cas1ApplicationUserDetails? = null,

  @Schema(example = "null", description = "If true, caseManagerUserDetails will provide case manager details. Otherwise, applicantUserDetails can be used for case manager details")
  val caseManagerIsNotApplicant: kotlin.Boolean? = null,

  val caseManagerUserDetails: Cas1ApplicationUserDetails? = null,

  val licenceExpiryDate: java.time.LocalDate? = null,
)
