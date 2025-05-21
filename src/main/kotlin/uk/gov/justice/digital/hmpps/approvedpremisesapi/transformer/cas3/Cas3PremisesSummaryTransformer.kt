package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3BedspaceSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesSummary

@Component
class Cas3PremisesSummaryTransformer {
  fun transformDomainToCas3PremisesSummary(domain: TemporaryAccommodationPremisesSummary, bedspaces: List<Cas3BedspaceSummary>): Cas3PremisesSummary = Cas3PremisesSummary(
    id = domain.id,
    name = domain.name,
    addressLine1 = domain.addressLine1,
    addressLine2 = domain.addressLine2,
    postcode = domain.postcode,
    status = domain.status,
    bedspaces = bedspaces,
    bedspaceCount = bedspaces.count { it.status == Cas3BedspaceSummary.Status.online },
    pdu = domain.pdu,
    localAuthorityAreaName = domain.localAuthorityAreaName,
  )

  fun transformDomainToCas3BedspaceSummary(domain: TemporaryAccommodationPremisesSummary): Cas3BedspaceSummary = Cas3BedspaceSummary(
    id = domain.bedspaceId!!,
    reference = domain.bedspaceReference!!,
    status = domain.bedspaceStatus!!,
  )
}
