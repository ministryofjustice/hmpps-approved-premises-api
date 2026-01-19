package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3VoidBedspaceReason

@Component
class Cas3VoidBedspaceReasonTransformer {
  fun transformJpaToApi(jpa: Cas3VoidBedspaceReasonEntity) = LostBedReason(
    id = jpa.id,
    name = jpa.name,
    isActive = jpa.isActive,
    serviceScope = ServiceName.temporaryAccommodation.value,
  )

  fun toCas3VoidBedspaceReason(entity: Cas3VoidBedspaceReasonEntity) = Cas3VoidBedspaceReason(
    id = entity.id,
    name = entity.name,
    isActive = entity.isActive,
    description = null,
  )
}
