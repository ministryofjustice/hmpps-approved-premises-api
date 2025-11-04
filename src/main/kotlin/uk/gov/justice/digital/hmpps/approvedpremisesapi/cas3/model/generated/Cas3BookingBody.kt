package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Bedspace
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 *
 * @param id
 * @param person
 * @param arrivalDate
 * @param originalArrivalDate
 * @param departureDate
 * @param originalDepartureDate
 * @param createdAt
 * @param bedspace
 */
data class Cas3BookingBody(

  val id: UUID,

  val person: Person,

  val arrivalDate: LocalDate,

  val originalArrivalDate: LocalDate,

  val departureDate: LocalDate,

  val originalDepartureDate: LocalDate,

  val createdAt: Instant,

  val bedspace: Cas3Bedspace,
)
