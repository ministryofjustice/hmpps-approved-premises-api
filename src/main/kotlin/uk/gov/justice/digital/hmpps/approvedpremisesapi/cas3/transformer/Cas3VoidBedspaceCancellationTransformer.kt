package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceCancellationEntity

@Component
class Cas3VoidBedspaceCancellationTransformer {
  fun transformJpaToApi(jpa: Cas3VoidBedspaceCancellationEntity) = LostBedCancellation(
    id = jpa.id,
    createdAt = jpa.createdAt.toInstant(),
    notes = jpa.notes,
  )
}
