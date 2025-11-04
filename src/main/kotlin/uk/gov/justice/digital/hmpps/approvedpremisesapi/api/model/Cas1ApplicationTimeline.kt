package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param id
 * @param createdAt
 * @param isOfflineApplication
 * @param timelineEvents
 * @param status
 * @param createdBy
 */
data class Cas1ApplicationTimeline(

  val id: java.util.UUID,

  val createdAt: java.time.Instant,

  val isOfflineApplication: kotlin.Boolean,

  val timelineEvents: kotlin.collections.List<Cas1TimelineEvent>,

  val status: Cas1ApplicationStatus? = null,

  val createdBy: User? = null,
)
