package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PremisesSummary as ApiPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesSummary as DomainTemporaryAccommodationPremisesSummary

@Component
class Cas3PremisesSummaryTransformer {
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

  fun transformDomainToCas3PremisesSummary(domain: DomainTemporaryAccommodationPremisesSummary): Cas3PremisesSummary = Cas3PremisesSummary(
    id = domain.id,
    name = domain.name,
    addressLine1 = domain.addressLine1,
    addressLine2 = domain.addressLine2,
    postcode = domain.postcode,
    status = domain.status,
    bedCount = domain.bedCount,
    pdu = domain.pdu,
    localAuthorityAreaName = domain.localAuthorityAreaName,
  )
}
