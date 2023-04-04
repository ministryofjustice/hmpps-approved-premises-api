package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedCancellationEntity

@Component
class LostBedCancellationTransformer {
  fun transformJpaToApi(jpa: LostBedCancellationEntity) = LostBedCancellation(
    id = jpa.id,
    createdAt = jpa.createdAt.toInstant(),
    notes = jpa.notes,
  )
}
