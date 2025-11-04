package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1AssessmentSummary(

  val id: java.util.UUID,

  val applicationId: java.util.UUID,

  val createdAt: java.time.Instant,

  val person: Person,

  val status: Cas1AssessmentStatus,

  val dueAt: java.time.Instant,

  val arrivalDate: java.time.Instant? = null,

  val dateOfInfoRequest: java.time.Instant? = null,

  val decision: AssessmentDecision? = null,

  val risks: PersonRisks? = null,
)
