package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2.ReferenceDataCas2Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2ApplicationStatusSeeding

@Service("Cas2ReferenceDataController")
class ReferenceDataController : ReferenceDataCas2Delegate {
  override fun referenceDataApplicationStatusGet(): ResponseEntity<List<Cas2ApplicationStatus>> {
    return ResponseEntity.ok(Cas2ApplicationStatusSeeding.statusList())
  }
}
