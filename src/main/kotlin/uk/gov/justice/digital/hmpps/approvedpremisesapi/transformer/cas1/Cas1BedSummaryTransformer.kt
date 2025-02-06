package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1PremisesBedSummary as JpaBedSummary

@Component
class Cas1BedSummaryTransformer {
  fun transformJpaToApi(summary: JpaBedSummary) = Cas1PremisesBedSummary(
    id = summary.getId(),
    roomName = summary.getRoomName(),
    bedName = summary.getBedName(),
  )

  fun transformEntityToApi(bed: BedEntity) = Cas1PremisesBedSummary(
    id = bed.id,
    roomName = bed.room.name,
    bedName = bed.name,
  )
}
