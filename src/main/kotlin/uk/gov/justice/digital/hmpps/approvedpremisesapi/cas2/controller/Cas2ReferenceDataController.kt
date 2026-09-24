package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ApplicationStatusSeeding.activeStatuses
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcApplicationStatusTransformer

@Cas2Controller
class Cas2ReferenceDataController(
  private val statusTransformer: Cas2HdcApplicationStatusTransformer,
) {
  @GetMapping("/reference-data/application-status")
  fun referenceDataApplicationStatusGet(): ResponseEntity<List<Cas2ApplicationStatus>> = ResponseEntity.ok(transformToApi(activeStatuses()))

  private fun transformToApi(statusList: List<Cas2AssessmentStatus>): List<Cas2ApplicationStatus> = statusList.map { status -> statusTransformer.transformV2ModelToApi(status) }
}
