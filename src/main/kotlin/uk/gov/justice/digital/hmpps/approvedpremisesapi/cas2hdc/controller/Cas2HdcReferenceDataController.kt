package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.reference.Cas2PersistedApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.reference.Cas2PersistedApplicationStatusFinder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcApplicationStatusTransformer

@Cas2HdcController
class Cas2HdcReferenceDataController(
  private val statusTransformer: Cas2HdcApplicationStatusTransformer,
  private val statusFinder: Cas2PersistedApplicationStatusFinder,
) {
  @GetMapping("/reference-data/application-status")
  fun referenceDataApplicationStatusGet(): ResponseEntity<List<Cas2ApplicationStatus>> = ResponseEntity.ok(transformToApi(statusFinder.active()))

  private fun transformToApi(statusList: List<Cas2PersistedApplicationStatus>): List<Cas2ApplicationStatus> = statusList.map { status -> statusTransformer.transformModelToApi(status) }
}
