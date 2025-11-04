package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 * Notes added to an application
 * @param note
 * @param id
 * @param createdByUser
 * @param createdAt
 */
data class ApplicationTimelineNote(

  val note: kotlin.String,

  val id: java.util.UUID? = null,

  val createdByUser: User? = null,

  val createdAt: java.time.Instant? = null,
)
