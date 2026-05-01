package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Appeal(

  val id: java.util.UUID,

  val appealDate: java.time.LocalDate,

  val appealDetail: kotlin.String,

  val decision: AppealDecision,

  val decisionDetail: kotlin.String,

  val createdAt: java.time.Instant,

  val applicationId: java.util.UUID,

  val createdByUser: User,

  val assessmentId: java.util.UUID? = null,
)
