package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CancellationReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonEntity

@Component
class CancellationReasonTransformer {
  fun transformJpaToApi(jpa: CancellationReasonEntity) = CancellationReason(
    id = jpa.id,
    name = jpa.name,
    isActive = jpa.isActive,
    serviceScope = jpa.serviceScope,
  )
}
