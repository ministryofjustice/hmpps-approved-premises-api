package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesSummaryResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesSummaryV2
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesSummary

@Component
class Cas3PremisesSummaryTransformer {
  fun transformDomainToCas3PremisesSummary(domain: TemporaryAccommodationPremisesSummary, bedspaceCount: Int): Cas3PremisesSummary = Cas3PremisesSummary(
    id = domain.id,
    name = domain.name,
    addressLine1 = domain.addressLine1,
    addressLine2 = domain.addressLine2,
    postcode = domain.postcode,
    status = domain.status,
    bedspaceCount = bedspaceCount,
    pdu = domain.pdu,
    town = domain.town,
    localAuthorityAreaName = domain.localAuthorityAreaName,
  )

  fun toCas3PremisesSummary(domain: Cas3PremisesSummaryResult, bedspaceCount: Int): Cas3PremisesSummaryV2 = Cas3PremisesSummaryV2(
    id = domain.id,
    name = domain.name,
    addressLine1 = domain.addressLine1,
    addressLine2 = domain.addressLine2,
    postcode = domain.postcode,
    status = domain.status,
    bedspaceCount = bedspaceCount,
    pdu = domain.pdu,
    town = domain.town,
    localAuthorityAreaName = domain.localAuthorityAreaName,
  )
}
