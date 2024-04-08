package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas3

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3.ReferenceDataCas3Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3DutyToReferRejectionReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas3DutyToReferRejectionReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3DutyToReferRejectionReasonTransformer

@Service
class Cas3ReferenceDataController(
  private val cas3DutyToReferRejectionReasonRepository: Cas3DutyToReferRejectionReasonRepository,
  private val cas3DutyToReferRejectionReasonTransformer: Cas3DutyToReferRejectionReasonTransformer,
) : ReferenceDataCas3Delegate {
  override fun referenceDataDutyToReferRejectionReasonsGet(): ResponseEntity<List<Cas3DutyToReferRejectionReason>> {
    return ResponseEntity.ok(
      cas3DutyToReferRejectionReasonRepository.findByIsActiveTrue()
        .map(cas3DutyToReferRejectionReasonTransformer::transformJpaToApi),
    )
  }
}
