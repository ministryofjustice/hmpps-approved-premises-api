package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspaceCancellationEntity

@Component
class Cas3VoidBedspaceCancellationTransformer {
  fun transformJpaToApi(jpa: Cas3VoidBedspaceCancellationEntity) = LostBedCancellation(
    id = jpa.id,
    createdAt = jpa.createdAt.toInstant(),
    notes = jpa.notes,
  )
}
