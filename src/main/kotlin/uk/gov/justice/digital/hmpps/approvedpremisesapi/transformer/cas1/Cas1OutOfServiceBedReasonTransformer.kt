package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonEntity

@Component
class Cas1OutOfServiceBedReasonTransformer {
  fun transformJpaToApi(jpa: Cas1OutOfServiceBedReasonEntity) = Cas1OutOfServiceBedReason(
    id = jpa.id,
    name = jpa.name,
    isActive = jpa.isActive,
  )
}
