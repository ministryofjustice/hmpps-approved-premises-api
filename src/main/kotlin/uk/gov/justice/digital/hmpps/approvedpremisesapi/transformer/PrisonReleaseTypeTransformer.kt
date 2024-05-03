package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PrisonReleaseType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PrisonReleaseTypeEntity

@Component
class PrisonReleaseTypeTransformer {
  fun transformJpaToApi(jpa: PrisonReleaseTypeEntity) = PrisonReleaseType(
    id = jpa.id,
    name = jpa.name,
    isActive = jpa.isActive,
    serviceScope = jpa.serviceScope,
  )
}
