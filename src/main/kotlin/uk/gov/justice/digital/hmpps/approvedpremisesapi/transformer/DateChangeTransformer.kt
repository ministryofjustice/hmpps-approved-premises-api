package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DateChange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DateChangeEntity

@Component
class DateChangeTransformer {
  fun transformJpaToApi(jpa: DateChangeEntity) = DateChange(
    id = jpa.id,
    bookingId = jpa.booking.id,
    previousDepartureDate = jpa.previousDepartureDate,
    newDepartureDate = jpa.newDepartureDate,
    previousArrivalDate = jpa.previousArrivalDate,
    newArrivalDate = jpa.newArrivalDate,
    createdAt = jpa.changedAt.toInstant(),
  )
}
