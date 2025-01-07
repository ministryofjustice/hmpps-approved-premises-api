package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3LostBedCancellationEntity

@Component
class Cas3LostBedCancellationTransformer {
  fun transformJpaToApi(jpa: Cas3LostBedCancellationEntity) = LostBedCancellation(
    id = jpa.id,
    createdAt = jpa.createdAt.toInstant(),
    notes = jpa.notes,
  )
}
