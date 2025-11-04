package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Bed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import java.time.LocalDate
import java.util.UUID

/**
 *
 * @param id
 * @param person
 * @param arrivalDate
 * @param departureDate
 * @param bed
 */
data class FutureBooking(

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("person", required = true) val person: Person,

  @get:JsonProperty("arrivalDate", required = true) val arrivalDate: LocalDate,

  @get:JsonProperty("departureDate", required = true) val departureDate: LocalDate,

  @get:JsonProperty("bed") val bed: Bed? = null,
)
