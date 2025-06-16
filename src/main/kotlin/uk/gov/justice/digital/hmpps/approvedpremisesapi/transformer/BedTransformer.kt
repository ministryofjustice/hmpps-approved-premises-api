package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Bed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3BedspacesEntity

@Component
class BedTransformer {
  fun transformJpaToApi(jpa: BedEntity) = Bed(
    id = jpa.id,
    name = jpa.name,
    code = jpa.code,
    bedEndDate = jpa.endDate,
  )

  fun transformJpaToApi(jpa: Cas3BedspacesEntity) = Bed(
    id = jpa.id,
    name = jpa.reference,
    code = null,
    bedEndDate = jpa.endDate,
  )
}
