package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param startDate
 * @param endDate
 * @param reason
 * @param referenceNumber
 * @param notes
 */
data class UpdateCas1OutOfServiceBed(

  val startDate: java.time.LocalDate,

  val endDate: java.time.LocalDate,

  val reason: java.util.UUID,

  val referenceNumber: kotlin.String? = null,

  val notes: kotlin.String? = null,
)
