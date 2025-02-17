package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DestinationProvider
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DestinationProviderEntity

@Component
class DestinationProviderTransformer {
  fun transformJpaToApi(jpa: DestinationProviderEntity) = DestinationProvider(
    id = jpa.id,
    name = jpa.name,
    isActive = jpa.isActive,
  )
}
