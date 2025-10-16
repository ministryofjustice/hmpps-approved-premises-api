package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity

@Component
class ProbationRegionTransformer {
  fun transformJpaToApi(jpa: ProbationRegionEntity) = ProbationRegion(id = jpa.id, name = jpa.name, hptEmail = jpa.hptEmail)
}
