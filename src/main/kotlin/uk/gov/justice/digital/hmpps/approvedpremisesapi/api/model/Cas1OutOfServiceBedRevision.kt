package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param id
 * @param updatedAt
 * @param revisionType
 * @param updatedBy
 * @param startDate
 * @param endDate
 * @param reason
 * @param referenceNumber
 * @param notes
 */
data class Cas1OutOfServiceBedRevision(

  val id: java.util.UUID,

  val updatedAt: java.time.Instant,

  val revisionType: kotlin.collections.List<Cas1OutOfServiceBedRevisionType>,

  val updatedBy: User? = null,

  val startDate: java.time.LocalDate? = null,

  val endDate: java.time.LocalDate? = null,

  val reason: Cas1OutOfServiceBedReason? = null,

  val referenceNumber: kotlin.String? = null,

  val notes: kotlin.String? = null,
)
