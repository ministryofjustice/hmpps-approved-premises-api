package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspacePremisesSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3PremisesSearchResultsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.util.UUID

class Cas3PremisesSearchResultsTransformerTest {
  private val cas3PremisesSearchResultsTransformer = Cas3PremisesSearchResultsTransformer()

  @Test
  fun `transformDomainToCas3PremisesSearchResults transforms the TemporaryAccommodationPremisesSummary into a Cas3PremisesSearchResults`() {
    val premisesId = UUID.randomUUID()
    val bedspaceOne = createPremisesSummaryData(premisesId, Cas3BedspaceStatus.online, true)
    val bedspaceTwo = createPremisesSummaryData(premisesId, Cas3BedspaceStatus.archived, true)
    val bedspaceThree = createPremisesSummaryData(premisesId, Cas3BedspaceStatus.online, true)
    val bedspaceFour = createPremisesSummaryData(premisesId, Cas3BedspaceStatus.upcoming, true)
    val domainPremisesSummary = mapOf(premisesId to listOf(bedspaceOne, bedspaceTwo, bedspaceThree, bedspaceFour))

    val result = cas3PremisesSearchResultsTransformer.transformDomainToCas3PremisesSearchResults(domainPremisesSummary)

    assertThat(result).isEqualTo(
      Cas3PremisesSearchResults(
        totalPremises = 1,
        totalOnlineBedspaces = 2,
        totalUpcomingBedspaces = 1,
        results = listOf(
          Cas3PremisesSearchResult(
            id = premisesId,
            reference = bedspaceOne.name,
            addressLine1 = bedspaceOne.addressLine1,
            addressLine2 = bedspaceOne.addressLine2,
            postcode = bedspaceOne.postcode,
            town = bedspaceOne.town,
            pdu = bedspaceOne.pdu,
            localAuthorityAreaName = bedspaceOne.localAuthorityAreaName,
            bedspaces = listOf(
              createCas3BedspacePremisesSearchResult(bedspaceOne),
              createCas3BedspacePremisesSearchResult(bedspaceTwo),
              createCas3BedspacePremisesSearchResult(bedspaceThree),
              createCas3BedspacePremisesSearchResult(bedspaceFour),
            ),
            totalArchivedBedspaces = 1,
          ),
        ),
      ),
    )
  }

  @Test
  fun `transformDomainToCas3PremisesSearchResults transforms the TemporaryAccommodationPremisesSummary into a Cas3PremisesSearchResults without optional elements`() {
    val premisesId = UUID.randomUUID()
    val bedspaceOne = createPremisesSummaryData(premisesId, Cas3BedspaceStatus.online, false)
    val domainPremisesSummary = mapOf(premisesId to listOf(bedspaceOne))

    val result = cas3PremisesSearchResultsTransformer.transformDomainToCas3PremisesSearchResults(domainPremisesSummary)

    assertThat(result).isEqualTo(
      Cas3PremisesSearchResults(
        totalPremises = 1,
        totalOnlineBedspaces = 0,
        totalUpcomingBedspaces = 0,
        results = listOf(
          Cas3PremisesSearchResult(
            id = premisesId,
            reference = bedspaceOne.name,
            addressLine1 = bedspaceOne.addressLine1,
            addressLine2 = null,
            postcode = bedspaceOne.postcode,
            town = null,
            pdu = bedspaceOne.pdu,
            localAuthorityAreaName = null,
            bedspaces = emptyList(),
            totalArchivedBedspaces = 0,
          ),
        ),
      ),
    )
  }

  private fun createPremisesSummaryData(
    premisesId: UUID,
    bedspaceStatus: Cas3BedspaceStatus,
    withOptionalOptions: Boolean,
  ) = when {
    withOptionalOptions -> {
      TemporaryAccommodationPremisesSummaryData(
        id = premisesId,
        name = "premises test",
        addressLine1 = "address",
        addressLine2 = "address line 2",
        postcode = "123ABC",
        town = "Wednesbury",
        pdu = "North east",
        localAuthorityAreaName = "Rochford",
        bedspaceReference = randomStringMultiCaseWithNumbers(10),
        bedspaceId = UUID.randomUUID(),
        bedspaceStatus = bedspaceStatus,
      )
    }

    else -> {
      TemporaryAccommodationPremisesSummaryData(
        id = premisesId,
        name = "premises test",
        addressLine1 = "address",
        addressLine2 = null,
        postcode = "123ABC",
        town = null,
        pdu = "North east",
        localAuthorityAreaName = null,
        bedspaceReference = null,
        bedspaceId = null,
        bedspaceStatus = null,
      )
    }
  }

  private fun createCas3BedspacePremisesSearchResult(premisesSummaryData: TemporaryAccommodationPremisesSummaryData) = Cas3BedspacePremisesSearchResult(
    id = premisesSummaryData.bedspaceId!!,
    reference = premisesSummaryData.bedspaceReference!!,
    status = premisesSummaryData.bedspaceStatus!!,
  )

  @SuppressWarnings("LongParameterList")
  class TemporaryAccommodationPremisesSummaryData(
    override val id: UUID,
    override val name: String,
    override val addressLine1: String,
    override val addressLine2: String?,
    override val postcode: String,
    override val pdu: String,
    override val town: String?,
    override val bedspaceId: UUID?,
    override val bedspaceReference: String?,
    override val bedspaceStatus: Cas3BedspaceStatus?,
    override val localAuthorityAreaName: String?,
  ) : TemporaryAccommodationPremisesSummary
}
