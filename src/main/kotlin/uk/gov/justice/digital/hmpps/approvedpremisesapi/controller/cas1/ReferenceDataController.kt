package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.ReferenceDataCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedReasonTransformer

@Service("Cas1ReferenceDataController")
class ReferenceDataController(
  private val reasonTransformer: Cas1OutOfServiceBedReasonTransformer,
  private val repository: Cas1OutOfServiceBedReasonRepository,
) : ReferenceDataCas1Delegate {

  override fun referenceDataOutOfServiceBedReasonsGet(): ResponseEntity<List<Cas1OutOfServiceBedReason>> {
    return ResponseEntity.ok(transformedOutOfServiceBedReasons())
  }

  private fun transformedOutOfServiceBedReasons(): List<Cas1OutOfServiceBedReason> {
    return repository.findActive().map { reason -> reasonTransformer.transformJpaToApi(reason) }
  }
}
