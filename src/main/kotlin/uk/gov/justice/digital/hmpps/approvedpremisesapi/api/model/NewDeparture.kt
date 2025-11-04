package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param dateTime
 * @param reasonId
 * @param moveOnCategoryId
 * @param notes
 * @param destinationProviderId
 */
data class NewDeparture(

  val dateTime: java.time.Instant,

  val reasonId: java.util.UUID,

  val moveOnCategoryId: java.util.UUID,

  val notes: kotlin.String? = null,

  val destinationProviderId: java.util.UUID? = null,
)
