package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class PrisonCaseNote(

  val id: kotlin.String,

  val sensitive: kotlin.Boolean,

  val createdAt: java.time.Instant,

  val occurredAt: java.time.Instant,

  val authorName: kotlin.String,

  val type: kotlin.String,

  val subType: kotlin.String,

  val note: kotlin.String,
)
