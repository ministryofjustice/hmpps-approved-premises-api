package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas3

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3.ReferenceDataCas3Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3DutyToReferOutcome
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas3DutyToReferOutcomeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3DutyToReferOutcomeTransformer

@Service
class Cas3ReferenceDataController(
  private val cas3DutyToReferOutcomeRepository: Cas3DutyToReferOutcomeRepository,
  private val cas3DutyToReferOutcomeTransformer: Cas3DutyToReferOutcomeTransformer,
) : ReferenceDataCas3Delegate {
  override fun referenceDataDutyToReferOutcomesGet(): ResponseEntity<List<Cas3DutyToReferOutcome>> {
    return ResponseEntity.ok(
      cas3DutyToReferOutcomeRepository.findByIsActiveTrue()
        .map(cas3DutyToReferOutcomeTransformer::transformJpaToApi),
    )
  }
}
