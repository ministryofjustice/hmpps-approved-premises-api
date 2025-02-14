package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FutureBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BedTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer

@Component
class Cas3FutureBookingTransformer(
  private val personTransformer: PersonTransformer,
  private val bedTransformer: BedTransformer,
) {
  fun transformJpaToApi(jpa: BookingEntity, personSummaryInfo: PersonSummaryInfoResult): FutureBooking = FutureBooking(
    id = jpa.id,
    person = personTransformer.transformSummaryToPersonApi(personSummaryInfo),
    arrivalDate = jpa.arrivalDate,
    departureDate = jpa.departureDate,
    bed = jpa.bed?.let { bedTransformer.transformJpaToApi(it) },
  )
}
