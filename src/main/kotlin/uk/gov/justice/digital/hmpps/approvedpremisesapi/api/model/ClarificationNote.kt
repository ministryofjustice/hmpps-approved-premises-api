package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param id
 * @param createdAt
 * @param createdByStaffMemberId
 * @param query
 * @param responseReceivedOn
 * @param response
 */
data class ClarificationNote(

  val id: java.util.UUID,

  val createdAt: java.time.Instant,

  val createdByStaffMemberId: java.util.UUID,

  val query: kotlin.String,

  val responseReceivedOn: java.time.LocalDate? = null,

  val response: kotlin.String? = null,
)
