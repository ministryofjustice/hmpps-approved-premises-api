package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class ApprovedPremisesApplication(

  val createdByUserId: java.util.UUID,

  val status: ApprovedPremisesApplicationStatus,

  val isWomensApplication: kotlin.Boolean? = null,

  val isEmergencyApplication: kotlin.Boolean? = null,

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

  @Schema(description = "If true, caseManagerUserDetails will provide case manager details. Otherwise, applicantUserDetails can be used for case manager details")
  val caseManagerIsNotApplicant: kotlin.Boolean? = null,

  val caseManagerUserDetails: Cas1ApplicationUserDetails? = null,

  val licenceExpiryDate: java.time.LocalDate? = null,

  val releaseType: ReleaseTypeOption? = null,

  val sentenceType: SentenceTypeOption? = null,

  override val type: kotlin.String,

  override val id: java.util.UUID,

  override val person: Person,

  override val createdAt: java.time.Instant,
) : Application
