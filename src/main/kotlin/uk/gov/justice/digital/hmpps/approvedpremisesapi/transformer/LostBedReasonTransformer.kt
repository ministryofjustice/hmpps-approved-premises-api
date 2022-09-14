package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedReasonEntity

@Component
class LostBedReasonTransformer() {
  fun transformJpaToApi(jpa: LostBedReasonEntity) = LostBedReason(
    id = jpa.id,
    name = jpa.name,
    isActive = jpa.isActive
  )
}
