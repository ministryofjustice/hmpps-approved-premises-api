package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesBasicSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApAreaTransformer

@Service
class Cas1PremisesTransformer(
  val apAreaTransformer: ApAreaTransformer,
) {
  fun toPremiseSummary(premisesSummaryInfo: Cas1PremisesService.Cas1PremisesSummaryInfo): Cas1PremisesSummary {
    val entity = premisesSummaryInfo.entity
    return Cas1PremisesSummary(
      id = entity.id,
      name = entity.name,
      apCode = entity.apCode,
      postcode = entity.postcode,
      bedCount = premisesSummaryInfo.bedCount,
      availableBeds = premisesSummaryInfo.availableBeds,
      outOfServiceBeds = premisesSummaryInfo.outOfServiceBeds,
      apArea = apAreaTransformer.transformJpaToApi(entity.probationRegion.apArea!!),
    )
  }

  fun toPremiseBasicSummary(entity: PremisesEntity): Cas1PremisesBasicSummary {
    val apArea = entity.probationRegion.apArea!!
    return Cas1PremisesBasicSummary(
      id = entity.id,
      name = entity.name,
      apArea = NamedId(apArea.id, apArea.name),
    )
  }
}
