package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity

@Component
class ApAreaTransformer {
  fun transformJpaToApi(jpa: ApAreaEntity) = ApArea(id = jpa.id, name = jpa.name, identifier = jpa.identifier)
}
