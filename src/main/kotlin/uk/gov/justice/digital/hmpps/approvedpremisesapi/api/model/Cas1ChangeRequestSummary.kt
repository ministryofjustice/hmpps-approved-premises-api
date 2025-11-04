package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param id
 * @param person
 * @param type
 * @param createdAt
 * @param expectedArrivalDate
 * @param placementRequestId
 * @param tier
 * @param actualArrivalDate
 */
data class Cas1ChangeRequestSummary(

  val id: java.util.UUID,

  val person: PersonSummary,

  val type: Cas1ChangeRequestType,

  val createdAt: java.time.Instant,

  val expectedArrivalDate: java.time.LocalDate,

  val placementRequestId: java.util.UUID,

  val tier: kotlin.String? = null,

  val actualArrivalDate: java.time.LocalDate? = null,
)
