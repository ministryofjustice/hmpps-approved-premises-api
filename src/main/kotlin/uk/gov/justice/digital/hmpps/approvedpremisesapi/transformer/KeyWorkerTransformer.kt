package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.KeyWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.KeyWorkerEntity

@Component
class KeyWorkerTransformer() {
  fun transformJpaToApi(jpa: KeyWorkerEntity) = KeyWorker(id = jpa.id, name = jpa.name, isActive = jpa.isActive)
}
