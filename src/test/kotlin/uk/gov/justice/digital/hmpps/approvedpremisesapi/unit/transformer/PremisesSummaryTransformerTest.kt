package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesSummaryTransformer
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesSummary as DomainApprovedPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesSummary as DomainTemporaryAccommodationPremisesSummary

class PremisesSummaryTransformerTest {
  private val premisesSummaryTransformer = PremisesSummaryTransformer()

  @Test
  fun `transformDomainToApi transforms the DomainTemporaryAccommodationPremisesSummary into a TemporaryAccommodationPremisesSummary`() {
    val uuid = UUID.randomUUID()
    val domainPremisesSummary = DomainTemporaryAccommodationPremisesSummary(
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
      ),
    )
  }

  @Test
  fun `transformDomainToApi transforms the ApprovedPremisesSummary into an ApprovedPremisesSummary`() {
    val uuid = UUID.randomUUID()
    val domainPremisesSummary = DomainApprovedPremisesSummary(
      id = uuid,
      name = "bob",
      addressLine1 = "address",
      addressLine2 = "address line 2",
      postcode = "123ABC",
      status = PropertyStatus.active,
      bedCount = 1,
      apCode = "APCODE",
    )

    val result = premisesSummaryTransformer.transformDomainToApi(domainPremisesSummary)

    assertThat(result).isEqualTo(
      ApprovedPremisesSummary(
        id = uuid,
        name = "bob",
        addressLine1 = "address",
        addressLine2 = "address line 2",
        postcode = "123ABC",
        status = PropertyStatus.active,
        bedCount = 1,
        apCode = "APCODE",
      ),
    )
  }
}
