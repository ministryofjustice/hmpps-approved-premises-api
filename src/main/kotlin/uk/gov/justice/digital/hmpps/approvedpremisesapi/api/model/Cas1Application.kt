package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1Application(

  val id: java.util.UUID,

  val person: Person,

  val createdAt: java.time.Instant,

  val createdByUserId: java.util.UUID,

  val createdByUserName: String?,

  val status: ApprovedPremisesApplicationStatus,

  val isWomensApplication: Boolean? = null,

  @Deprecated(message = "")
  val isPipeApplication: Boolean? = null,

  val isEmergencyApplication: Boolean? = null,

  @Deprecated(message = "")
  val isEsapApplication: Boolean? = null,

  val apType: ApType? = null,

  val arrivalDate: java.time.Instant? = null,

  val risks: PersonRisks? = null,

  val `data`: Any? = null,

  val document: Any? = null,

  val assessmentId: java.util.UUID? = null,

  val assessmentDecision: AssessmentDecision? = null,

  val assessmentDecisionDate: java.time.LocalDate? = null,

  val submittedAt: java.time.Instant? = null,

  val personStatusOnSubmission: PersonStatus? = null,

  val apArea: ApArea? = null,

  val cruManagementArea: Cas1CruManagementArea? = null,

  val applicantUserDetails: Cas1ApplicationUserDetails? = null,

  val caseManagerIsNotApplicant: Boolean? = null,

  val caseManagerUserDetails: Cas1ApplicationUserDetails? = null,

  val licenceExpiryDate: java.time.LocalDate? = null,
)
