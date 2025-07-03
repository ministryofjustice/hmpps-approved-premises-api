package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Extension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3ExtensionEntity

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
