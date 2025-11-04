package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param id
 * @param bookingId
 * @param previousDepartureDate
 * @param newDepartureDate
 * @param createdAt
 * @param notes
 */
data class Extension(

  val id: java.util.UUID,

  val bookingId: java.util.UUID,

  val previousDepartureDate: java.time.LocalDate,

  val newDepartureDate: java.time.LocalDate,

  val createdAt: java.time.Instant,

  val notes: kotlin.String? = null,
)
