package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3DutyToReferRejectionReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas3DutyToReferRejectionReasonEntity

@Component
class Cas3DutyToReferRejectionReasonTransformer {
  fun transformJpaToApi(jpa: Cas3DutyToReferRejectionReasonEntity) = Cas3DutyToReferRejectionReason(
    id = jpa.id,
    name = jpa.name,
    isActive = jpa.isActive,
  )
}
