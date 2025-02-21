package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MoveOnCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity

@Component
class MoveOnCategoryTransformer {
  fun transformJpaToApi(jpa: MoveOnCategoryEntity) = MoveOnCategory(
    id = jpa.id,
    name = jpa.name,
    isActive = jpa.isActive,
    serviceScope = jpa.serviceScope,
  )
}
