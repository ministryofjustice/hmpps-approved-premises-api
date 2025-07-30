package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3ExtensionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Extension

@Component
class Cas3ExtensionTransformer {
  fun transformJpaToApi(jpa: Cas3ExtensionEntity) = Cas3Extension(
    id = jpa.id,
    bookingId = jpa.booking.id,
    previousDepartureDate = jpa.previousDepartureDate,
    newDepartureDate = jpa.newDepartureDate,
    notes = jpa.notes,
    createdAt = jpa.createdAt.toInstant(),
  )
}
