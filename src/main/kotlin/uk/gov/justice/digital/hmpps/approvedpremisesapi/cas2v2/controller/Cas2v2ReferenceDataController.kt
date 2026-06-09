package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.model.Cas2PersistedApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcApplicationStatusTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service.Cas2v2PersistedApplicationStatusFinder

@Cas2v2Controller
class Cas2v2ReferenceDataController(
  private val statusTransformer: Cas2HdcApplicationStatusTransformer,
  private val statusFinder: Cas2v2PersistedApplicationStatusFinder,
) {
  @GetMapping("/reference-data/application-status")
  fun referenceDataApplicationStatusGet(): ResponseEntity<List<Cas2v2ApplicationStatus>> = ResponseEntity.ok(transformToApi(statusFinder.active()))

  private fun transformToApi(statusList: List<Cas2PersistedApplicationStatus>): List<Cas2v2ApplicationStatus> = statusList.map { status -> statusTransformer.transformV2ModelToApi(status) }
}
