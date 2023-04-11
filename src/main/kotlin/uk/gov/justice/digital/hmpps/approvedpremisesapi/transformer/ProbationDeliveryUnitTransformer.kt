package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationDeliveryUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity

@Component
class ProbationDeliveryUnitTransformer {
  fun transformJpaToApi(jpa: ProbationDeliveryUnitEntity) = ProbationDeliveryUnit(
    id = jpa.id,
    name = jpa.name,
  )
}
