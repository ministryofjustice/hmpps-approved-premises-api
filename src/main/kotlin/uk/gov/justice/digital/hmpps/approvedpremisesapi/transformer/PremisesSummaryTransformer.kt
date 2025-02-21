package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PremisesSummary as ApiPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesSummary as DomainApprovedPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesSummary as DomainTemporaryAccommodationPremisesSummary

@Component
class PremisesSummaryTransformer {
  fun transformDomainToApi(domain: DomainTemporaryAccommodationPremisesSummary): ApiPremisesSummary = TemporaryAccommodationPremisesSummary(
    id = domain.id,
    name = domain.name,
    addressLine1 = domain.addressLine1,
    addressLine2 = domain.addressLine2,
    postcode = domain.postcode,
    status = domain.status,
    bedCount = domain.bedCount,
    pdu = domain.pdu,
    service = "CAS3",
    localAuthorityAreaName = domain.localAuthorityAreaName,
  )

  fun transformDomainToApi(domain: DomainApprovedPremisesSummary): ApiPremisesSummary = ApprovedPremisesSummary(
    id = domain.id,
    name = domain.name,
    addressLine1 = domain.addressLine1,
    addressLine2 = domain.addressLine2,
    postcode = domain.postcode,
    status = domain.status,
    bedCount = domain.bedCount,
    apCode = domain.apCode,
    service = "CAS1",
    probationRegion = domain.regionName,
    apArea = domain.apAreaName,
  )
}
