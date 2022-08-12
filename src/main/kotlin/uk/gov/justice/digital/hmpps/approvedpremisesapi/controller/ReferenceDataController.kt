package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.ReferenceDataApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.DepartureReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DepartureReasonTransformer

@Service
class ReferenceDataController(
  private val departureReasonRepository: DepartureReasonRepository,
  private val departureReasonTransformer: DepartureReasonTransformer
) : ReferenceDataApiDelegate {
  override fun referenceDataDepartureReasonsGet(): ResponseEntity<List<DepartureReason>> {
    val reasons = departureReasonRepository.findAll()

    return ResponseEntity.ok(reasons.map(departureReasonTransformer::transformJpaToApi))
  }
}
