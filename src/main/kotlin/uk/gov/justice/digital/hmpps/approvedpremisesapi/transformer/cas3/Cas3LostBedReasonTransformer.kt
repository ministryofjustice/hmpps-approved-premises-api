package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3LostBedReasonEntity

@Component
class Cas3LostBedReasonTransformer {
  fun transformJpaToApi(jpa: Cas3LostBedReasonEntity) = LostBedReason(
    id = jpa.id,
    name = jpa.name,
    isActive = jpa.isActive,
    serviceScope = jpa.serviceScope,
  )
}
