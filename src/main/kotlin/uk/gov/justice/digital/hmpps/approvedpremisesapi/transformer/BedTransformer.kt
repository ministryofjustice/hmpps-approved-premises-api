package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Bed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity

@Component
class BedTransformer {
  fun transformJpaToApi(jpa: BedEntity) = Bed(
    id = jpa.id,
    name = jpa.name,
    code = jpa.code,
    bedEndDate = jpa.endDate,
  )
}
