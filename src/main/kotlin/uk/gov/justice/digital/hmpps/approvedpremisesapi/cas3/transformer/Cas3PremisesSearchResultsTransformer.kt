package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesSummaryResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspacePremisesSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesSearchResults
import java.util.UUID

@Component
class Cas3PremisesSearchResultsTransformer {
  fun transformDomainToCas3PremisesSearchResults(results: Map<UUID, List<Cas3PremisesSummaryResult>>) = Cas3PremisesSearchResults(
    results = results.map { premises ->
      transformDomainToCas3PremisesSearchResult(
        premises.value.first(),
        premises.value.filter { it.bedspaceId != null }.map(::transformDomainToCas3BedspacePremisesSearchResult),
      )
    },
    totalPremises = results.size,
    totalOnlineBedspaces = results.values.sumOf { premises ->
      premises.count { it.bedspaceStatus == Cas3BedspaceStatus.online }
    },
    totalUpcomingBedspaces = results.values.sumOf { premises ->
      premises.count { it.bedspaceStatus == Cas3BedspaceStatus.upcoming }
    },
  )

  private fun transformDomainToCas3PremisesSearchResult(domain: Cas3PremisesSummaryResult, bedspaces: List<Cas3BedspacePremisesSearchResult>) = Cas3PremisesSearchResult(
    id = domain.id,
    reference = domain.name,
    addressLine1 = domain.addressLine1,
    addressLine2 = domain.addressLine2,
    postcode = domain.postcode,
    town = domain.town,
    bedspaces = bedspaces,
    pdu = domain.pdu,
    localAuthorityAreaName = domain.localAuthorityAreaName,
    totalArchivedBedspaces = bedspaces.count { it.status == Cas3BedspaceStatus.archived },
  )

  private fun transformDomainToCas3BedspacePremisesSearchResult(domain: Cas3PremisesSummaryResult) = Cas3BedspacePremisesSearchResult(
    id = domain.bedspaceId!!,
    reference = domain.bedspaceReference!!,
    status = domain.bedspaceStatus!!,
  )
}
