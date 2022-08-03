package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.LocalAuthorityArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity

@Component
class LocalAuthorityAreaTransformer {
  fun transformJpaToApi(jpa: LocalAuthorityAreaEntity) = LocalAuthorityArea(id = jpa.id, identifier = jpa.identifier, name = jpa.name)
}
