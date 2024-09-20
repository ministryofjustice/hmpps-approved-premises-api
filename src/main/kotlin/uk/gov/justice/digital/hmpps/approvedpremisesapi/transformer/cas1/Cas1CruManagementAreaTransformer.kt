package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity

@Component
class Cas1CruManagementAreaTransformer {
  fun transformJpaToApi(jpa: Cas1CruManagementAreaEntity) = Cas1CruManagementArea(
    id = jpa.id,
    name = jpa.name,
  )
}
