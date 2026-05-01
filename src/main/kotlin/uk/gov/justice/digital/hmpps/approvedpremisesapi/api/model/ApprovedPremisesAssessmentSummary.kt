package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class ApprovedPremisesAssessmentSummary(

  val status: ApprovedPremisesAssessmentStatus,

  val dueAt: java.time.Instant,

  override val type: kotlin.String,

  override val id: java.util.UUID,

  override val applicationId: java.util.UUID,

  override val createdAt: java.time.Instant,

  override val person: Person,

  override val arrivalDate: java.time.Instant? = null,

  override val dateOfInfoRequest: java.time.Instant? = null,

  override val decision: AssessmentDecision? = null,

  override val risks: PersonRisks? = null,
) : AssessmentSummary
