package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1Assessment(

  val id: java.util.UUID,

  val createdAt: java.time.Instant,

  val clarificationNotes: kotlin.collections.List<Cas1ClarificationNote>,

  val application: Cas1Application,

  val createdFromAppeal: kotlin.Boolean,

  val allocatedAt: java.time.Instant? = null,

  val submittedAt: java.time.Instant? = null,

  val decision: AssessmentDecision? = null,

  val rejectionRationale: kotlin.String? = null,

  val `data`: kotlin.Any? = null,

  val allocatedToStaffMember: ApprovedPremisesUser? = null,

  val status: Cas1AssessmentStatus? = null,

  val document: kotlin.Any? = null,
)
