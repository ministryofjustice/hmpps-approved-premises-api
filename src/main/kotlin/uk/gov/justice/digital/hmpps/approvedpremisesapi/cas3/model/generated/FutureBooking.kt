package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

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

  val id: UUID,

  val person: Person,

  val arrivalDate: LocalDate,

  val departureDate: LocalDate,

  val bed: Bed? = null,
)
