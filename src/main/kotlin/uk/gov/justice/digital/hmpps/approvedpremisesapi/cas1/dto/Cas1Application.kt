package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Cas1Application(

  val id: UUID,

  val person: Person,

  val createdAt: Instant,

  val createdByUserId: UUID,

  val createdByUserName: String?,

  val status: ApprovedPremisesApplicationStatus,

  val isWomensApplication: Boolean? = null,

  @Deprecated(message = "")
  val isPipeApplication: Boolean? = null,

  val isEmergencyApplication: Boolean? = null,

  @Deprecated(message = "")
  val isEsapApplication: Boolean? = null,

  val apType: ApType? = null,

  val arrivalDate: Instant? = null,

  @Schema(description = "Contains ROSH Risks, Tier, Risk Flags and MAPPA captured when the application was created")
  val risks: PersonRisks? = null,

  val `data`: Any? = null,

  val document: Any? = null,

  val assessmentId: UUID? = null,

  val assessmentDecision: AssessmentDecision? = null,

  val assessmentDecisionDate: LocalDate? = null,

  val submittedAt: Instant? = null,

  val personStatusOnSubmission: PersonStatus? = null,

  val apArea: ApArea? = null,

  val cruManagementArea: Cas1CruManagementArea? = null,

  val applicantUserDetails: Cas1ApplicationUserDetails? = null,

  val caseManagerIsNotApplicant: Boolean? = null,

  val caseManagerUserDetails: Cas1ApplicationUserDetails? = null,

  val licenceExpiryDate: LocalDate? = null,

  val releaseType: ReleaseTypeOption? = null,

  val sentenceType: SentenceTypeOption? = null,
)
