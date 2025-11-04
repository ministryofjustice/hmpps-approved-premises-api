package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class ApplicationTimelineNote(

  val note: kotlin.String,

  val id: java.util.UUID? = null,

  val createdByUser: User? = null,

  val createdAt: java.time.Instant? = null,
)
