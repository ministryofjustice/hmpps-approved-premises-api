package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ApplicationStatusSeeding.activeStatuses
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcApplicationStatusTransformer

@Cas2HdcController
class Cas2HdcReferenceDataController(
  private val statusTransformer: Cas2HdcApplicationStatusTransformer,
) {
  @GetMapping("/reference-data/application-status")
  fun referenceDataApplicationStatusGet(): ResponseEntity<List<Cas2HdcApplicationStatus>> = ResponseEntity.ok(transformToApi(activeStatuses()))

  private fun transformToApi(statusList: List<Cas2AssessmentStatus>): List<Cas2HdcApplicationStatus> = statusList.map { status -> statusTransformer.transformModelToApi(status) }
}
