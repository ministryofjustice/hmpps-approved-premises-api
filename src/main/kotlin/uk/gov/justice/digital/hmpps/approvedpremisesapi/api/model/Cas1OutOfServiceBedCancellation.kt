package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param id
 * @param createdAt
 * @param notes
 */
data class Cas1OutOfServiceBedCancellation(

  val id: java.util.UUID,

  val createdAt: java.time.Instant,

  val notes: kotlin.String? = null,
)
