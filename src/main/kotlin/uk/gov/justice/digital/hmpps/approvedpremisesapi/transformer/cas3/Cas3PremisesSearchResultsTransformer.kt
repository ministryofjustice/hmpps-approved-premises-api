package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BedspacePremisesSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3PremisesSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3PremisesSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesSummary
import java.util.UUID

@Component
class Cas3PremisesSearchResultsTransformer {
  fun transformDomainToCas3PremisesSearchResults(results: Map<UUID, List<TemporaryAccommodationPremisesSummary>>) = Cas3PremisesSearchResults(
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

  private fun transformDomainToCas3PremisesSearchResult(domain: TemporaryAccommodationPremisesSummary, bedspaces: List<Cas3BedspacePremisesSearchResult>) = Cas3PremisesSearchResult(
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

  private fun transformDomainToCas3BedspacePremisesSearchResult(domain: TemporaryAccommodationPremisesSummary) = Cas3BedspacePremisesSearchResult(
    id = domain.bedspaceId!!,
    reference = domain.bedspaceReference!!,
    status = domain.bedspaceStatus!!,
  )
}
