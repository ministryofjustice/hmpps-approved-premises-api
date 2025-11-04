package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param spaceBookingId
 * @param type
 * @param requestJson Any object
 * @param reasonId
 */
data class Cas1NewChangeRequest(

  val spaceBookingId: java.util.UUID,

  val type: Cas1ChangeRequestType,

  val requestJson: kotlin.Any,

  val reasonId: java.util.UUID,
)
