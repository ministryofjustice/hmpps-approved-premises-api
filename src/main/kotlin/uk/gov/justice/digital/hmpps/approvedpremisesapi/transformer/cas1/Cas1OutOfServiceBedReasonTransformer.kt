package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedReasonReferenceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonEntityReferenceType

@Component
class Cas1OutOfServiceBedReasonTransformer {
  fun transformJpaToApi(jpa: Cas1OutOfServiceBedReasonEntity) = Cas1OutOfServiceBedReason(
    id = jpa.id,
    name = jpa.name,
    isActive = jpa.isActive,
    referenceType = when (jpa.referenceType) {
      Cas1OutOfServiceBedReasonEntityReferenceType.CRN -> Cas1OutOfServiceBedReasonReferenceType.CRN
      Cas1OutOfServiceBedReasonEntityReferenceType.WORK_ORDER -> Cas1OutOfServiceBedReasonReferenceType.WORK_ORDER
    },
  )
}
