package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesBasicSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesBasicSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApAreaTransformer

@Service
class Cas1PremisesTransformer(
  val apAreaTransformer: ApAreaTransformer,
) {
  fun toPremises(premisesSummaryInfo: Cas1PremisesService.Cas1PremisesInfo): Cas1Premises {
    val entity = premisesSummaryInfo.entity
    return Cas1Premises(
      id = entity.id,
      name = entity.name,
      apCode = entity.apCode,
      fullAddress = entity.resolveFullAddress(),
      postcode = entity.postcode,
      bedCount = premisesSummaryInfo.bedCount,
      availableBeds = premisesSummaryInfo.availableBeds,
      outOfServiceBeds = premisesSummaryInfo.outOfServiceBeds,
      apArea = apAreaTransformer.transformJpaToApi(entity.probationRegion.apArea!!),
      supportsSpaceBookings = entity.supportsSpaceBookings,
      managerDetails = entity.managerDetails,
      overbookingSummary = premisesSummaryInfo.overbookingSummary,
    )
  }

  fun toPremiseBasicSummary(entity: ApprovedPremisesBasicSummary): Cas1PremisesBasicSummary {
    return Cas1PremisesBasicSummary(
      id = entity.id,
      name = entity.name,
      apCode = entity.apCode,
      apArea = NamedId(entity.apAreaId, entity.apAreaName),
      bedCount = entity.bedCount,
      supportsSpaceBookings = entity.supportsSpaceBookings,
      fullAddress = ApprovedPremisesEntity.resolveFullAddress(
        fullAddress = entity.fullAddress,
        addressLine1 = entity.addressLine1,
        addressLine2 = entity.addressLine2,
        town = entity.town,
      ),
      postcode = entity.postcode,
    )
  }
}
