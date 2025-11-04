package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class ApprovedPremisesAssessment(

  val application: ApprovedPremisesApplication,

  val createdFromAppeal: kotlin.Boolean,

  override val service: kotlin.String,

  override val id: java.util.UUID,

  override val createdAt: java.time.Instant,

  override val clarificationNotes: kotlin.collections.List<ClarificationNote>,

  val allocatedToStaffMember: ApprovedPremisesUser? = null,

  val status: ApprovedPremisesAssessmentStatus? = null,

  val document: kotlin.Any? = null,

  override val allocatedAt: java.time.Instant? = null,

  override val submittedAt: java.time.Instant? = null,

  override val decision: AssessmentDecision? = null,

  override val rejectionRationale: kotlin.String? = null,

  override val `data`: kotlin.Any? = null,
) : Assessment
