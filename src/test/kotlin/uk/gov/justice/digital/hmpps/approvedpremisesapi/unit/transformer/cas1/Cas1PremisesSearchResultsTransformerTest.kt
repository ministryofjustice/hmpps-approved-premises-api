package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.CandidatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PremisesSearchResultsTransformer
import java.math.BigDecimal
import java.util.UUID

class Cas1PremisesSearchResultsTransformerTest {
  private val transformer = Cas1PremisesSearchResultsTransformer()

  @Test
  fun `transformDomainToApi transforms correctly`() {
    val candidatePremise1Id = UUID.randomUUID()
    val apAreaId1 = UUID.randomUUID()

    val candidatePremise2Id = UUID.randomUUID()
    val apAreaId2 = UUID.randomUUID()

    val searchResults = listOf(
      CandidatePremises(
        premisesId = candidatePremise1Id,
        distanceInMiles = 1.0f,
        apType = ApprovedPremisesType.NORMAL,
        fullAddress = null,
        name = "Some AP 1",
        addressLine1 = "1 The Street",
        addressLine2 = null,
        town = "Townsbury",
        postcode = "TB1 2AB",
        apAreaId = apAreaId1,
        apAreaName = "Some AP Area 1",
        characteristics = emptyList(),
      ),
      CandidatePremises(
        premisesId = candidatePremise2Id,
        distanceInMiles = 2.0f,
        apType = ApprovedPremisesType.ESAP,
        fullAddress = "The full address, not quite the same, somewhere",
        name = "Some Other AP",
        addressLine1 = "2 The Street",
        addressLine2 = "Additional Bit",
        town = "Townton",
        postcode = "TB1 2AC",
        apAreaId = apAreaId2,
        apAreaName = "Some AP Area 2",
        characteristics = emptyList(),
      ),
    )

    val actual = transformer.transformDomainToApi(searchResults)
    assertThat(actual.resultsCount).isEqualTo(2)

    val premises1 = actual.results[0].premises
    assertThat(premises1.id).isEqualTo(candidatePremise1Id)
    assertThat(premises1.apType).isEqualTo(ApType.normal)
    assertThat(premises1.name).isEqualTo("Some AP 1")
    assertThat(premises1.fullAddress).isEqualTo("1 The Street, Townsbury")
    assertThat(premises1.postcode).isEqualTo("TB1 2AB")
    assertThat(premises1.apArea.id).isEqualTo(apAreaId1)
    assertThat(premises1.apArea.name).isEqualTo("Some AP Area 1")
    assertThat(premises1.characteristics).isEmpty()
    assertThat(actual.results[0].distanceInMiles).isEqualTo(BigDecimal.valueOf(1.0))

    val premises2 = actual.results[1].premises
    assertThat(premises2.id).isEqualTo(candidatePremise2Id)
    assertThat(premises2.apType).isEqualTo(ApType.esap)
    assertThat(premises2.name).isEqualTo("Some Other AP")
    assertThat(premises2.fullAddress).isEqualTo("The full address, not quite the same, somewhere")
    assertThat(premises2.postcode).isEqualTo("TB1 2AC")
    assertThat(premises2.apArea.id).isEqualTo(apAreaId2)
    assertThat(premises2.apArea.name).isEqualTo("Some AP Area 2")
    assertThat(premises2.characteristics).isEmpty()
    assertThat(actual.results[1].distanceInMiles).isEqualTo(BigDecimal.valueOf(2.0))
  }
}
