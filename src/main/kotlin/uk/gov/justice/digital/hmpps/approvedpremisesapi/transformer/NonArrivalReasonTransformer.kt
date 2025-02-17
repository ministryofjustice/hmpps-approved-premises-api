package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NonArrivalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonEntity

@Component
class NonArrivalReasonTransformer {
  fun transformJpaToApi(jpa: NonArrivalReasonEntity) = NonArrivalReason(
    id = jpa.id,
    name = jpa.name,
    isActive = jpa.isActive,
  )
}
