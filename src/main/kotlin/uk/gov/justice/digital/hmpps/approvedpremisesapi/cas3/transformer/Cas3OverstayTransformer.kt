package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3OverstayEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Overstay

@Component
class Cas3OverstayTransformer {
  fun transformJpaToApi(jpa: Cas3OverstayEntity) = Cas3Overstay(
    id = jpa.id,
    bookingId = jpa.booking.id,
    previousDepartureDate = jpa.previousDepartureDate,
    newDepartureDate = jpa.newDepartureDate,
    isAuthorised = jpa.isAuthorised,
    reason = jpa.reason,
    createdAt = jpa.createdAt.toInstant(),
  )
}
