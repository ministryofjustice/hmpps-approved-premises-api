package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Extension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExtensionEntity

@Component
class ExtensionTransformer {
  fun transformJpaToApi(jpa: ExtensionEntity) = Extension(
    id = jpa.id,
    bookingId = jpa.booking.id,
    previousDepartureDate = jpa.previousDepartureDate,
    newDepartureDate = jpa.newDepartureDate,
    notes = jpa.notes,
    createdAt = jpa.createdAt.toInstant(),
  )
}
