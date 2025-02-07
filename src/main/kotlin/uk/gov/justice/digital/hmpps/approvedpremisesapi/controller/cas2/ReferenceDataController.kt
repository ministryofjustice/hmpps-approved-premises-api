package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2.ReferenceDataCas2Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatusFinder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.ApplicationStatusTransformer

@Service("Cas2ReferenceDataController")
class ReferenceDataController(
  private val statusTransformer: ApplicationStatusTransformer,
  private val statusFinder: Cas2PersistedApplicationStatusFinder,
) : ReferenceDataCas2Delegate {
  override fun referenceDataApplicationStatusGet(): ResponseEntity<List<Cas2ApplicationStatus>> {
    return ResponseEntity.ok(transformToApi(statusFinder.active()))
  }

  private fun transformToApi(statusList: List<Cas2PersistedApplicationStatus>): List<Cas2ApplicationStatus> {
    return statusList.map { status -> statusTransformer.transformModelToApi(status) }
  }
}
