package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesSummaryTransformer
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesSummary as DomainPremisesSummary

class PremisesSummaryTransformerTest {
  private val premisesSummaryTransformer = PremisesSummaryTransformer()

  @Test
  fun `transformDomainToApi transforms the DomainPremisesSummary into a TemporaryAccommodationPremisesSummary`() {
    val uuid = UUID.randomUUID()
    val domainPremisesSummary = DomainPremisesSummary(
      id = uuid,
      name = "bob",
      addressLine1 = "address",
      addressLine2 = "address line 2",
      postcode = "123ABC",
      pdu = "North east",
      status = PropertyStatus.active,
      bedCount = 1,
    )

    val result = premisesSummaryTransformer.transformDomainToApi(domainPremisesSummary)

    assertThat(result).isEqualTo(
      TemporaryAccommodationPremisesSummary(
        id = uuid,
        name = "bob",
        addressLine1 = "address",
        addressLine2 = "address line 2",
        postcode = "123ABC",
        pdu = "North east",
        status = PropertyStatus.active,
        bedCount = 1,
      )
    )
  }
}
