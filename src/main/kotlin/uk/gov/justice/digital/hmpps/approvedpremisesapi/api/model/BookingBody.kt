package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param id
 * @param person
 * @param arrivalDate
 * @param originalArrivalDate
 * @param departureDate
 * @param originalDepartureDate
 * @param createdAt
 * @param serviceName
 * @param keyWorker KeyWorker is a legacy field only used by CAS1. It is not longer being captured or populated
 * @param bed
 */
data class BookingBody(

  val id: java.util.UUID,

  val person: Person,

  val arrivalDate: java.time.LocalDate,

  val originalArrivalDate: java.time.LocalDate,

  val departureDate: java.time.LocalDate,

  val originalDepartureDate: java.time.LocalDate,

  val createdAt: java.time.Instant,

  val serviceName: ServiceName,

  val bed: Bed? = null,
)
