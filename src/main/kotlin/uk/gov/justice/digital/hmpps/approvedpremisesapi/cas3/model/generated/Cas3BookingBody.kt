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

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("person", required = true) val person: Person,

  @get:JsonProperty("arrivalDate", required = true) val arrivalDate: LocalDate,

  @get:JsonProperty("originalArrivalDate", required = true) val originalArrivalDate: LocalDate,

  @get:JsonProperty("departureDate", required = true) val departureDate: LocalDate,

  @get:JsonProperty("originalDepartureDate", required = true) val originalDepartureDate: LocalDate,

  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @get:JsonProperty("bedspace", required = true) val bedspace: Cas3Bedspace,
)
