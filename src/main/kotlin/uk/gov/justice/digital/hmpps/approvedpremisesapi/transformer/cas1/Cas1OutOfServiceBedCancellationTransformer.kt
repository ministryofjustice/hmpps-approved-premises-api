package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedCancellationEntity

@Component
class Cas1OutOfServiceBedCancellationTransformer {
  fun transformJpaToApi(jpa: Cas1OutOfServiceBedCancellationEntity) = Cas1OutOfServiceBedCancellation(
    id = jpa.id,
    createdAt = jpa.createdAt.toInstant(),
    notes = jpa.notes,
  )
}
