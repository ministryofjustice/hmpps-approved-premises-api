package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3PremisesSummaryTransformer
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesSummary as DomainTemporaryAccommodationPremisesSummary

class Cas3PremisesSummaryTransformerTest {
  private val cas3PremisesSummaryTransformer = Cas3PremisesSummaryTransformer()

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
      localAuthorityAreaName = "Rochford",
    )

    val result = cas3PremisesSummaryTransformer.transformDomainToApi(domainPremisesSummary)

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
        service = "CAS3",
        localAuthorityAreaName = "Rochford",
      ),
    )
  }

  @Test
  fun `transformDomainToApi transforms the DomainTemporaryAccommodationPremisesSummary into a Cas3PremisesSummary`() {
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
      localAuthorityAreaName = "Rochford",
    )

    val result = cas3PremisesSummaryTransformer.transformDomainToCas3PremisesSummary(domainPremisesSummary)

    assertThat(result).isEqualTo(
      Cas3PremisesSummary(
        id = uuid,
        name = "bob",
        addressLine1 = "address",
        addressLine2 = "address line 2",
        postcode = "123ABC",
        pdu = "North east",
        status = PropertyStatus.active,
        bedCount = 1,
        localAuthorityAreaName = "Rochford",
      ),
    )
  }

  @Test
  fun `transformDomainToApi transforms the DomainTemporaryAccommodationPremisesSummary into a TemporaryAccommodationPremisesSummary without optional elements`() {
    val uuid = UUID.randomUUID()
    val domainPremisesSummary = DomainTemporaryAccommodationPremisesSummary(
      id = uuid,
      name = "bob",
      addressLine1 = "address",
      addressLine2 = null,
      postcode = "123ABC",
      pdu = "North east",
      status = PropertyStatus.active,
      bedCount = 1,
      localAuthorityAreaName = null,
    )

    val result = cas3PremisesSummaryTransformer.transformDomainToApi(domainPremisesSummary)

    assertThat(result).isEqualTo(
      TemporaryAccommodationPremisesSummary(
        id = uuid,
        name = "bob",
        addressLine1 = "address",
        addressLine2 = null,
        postcode = "123ABC",
        pdu = "North east",
        status = PropertyStatus.active,
        bedCount = 1,
        service = "CAS3",
        localAuthorityAreaName = null,
      ),
    )
  }

  @Test
  fun `transformDomainToApi transforms the DomainTemporaryAccommodationPremisesSummary into a Cas3PremisesSummary without optional elements`() {
    val uuid = UUID.randomUUID()
    val domainPremisesSummary = DomainTemporaryAccommodationPremisesSummary(
      id = uuid,
      name = "bob",
      addressLine1 = "address",
      addressLine2 = null,
      postcode = "123ABC",
      pdu = "North east",
      status = PropertyStatus.active,
      bedCount = 1,
      localAuthorityAreaName = null,
    )

    val result = cas3PremisesSummaryTransformer.transformDomainToCas3PremisesSummary(domainPremisesSummary)

    assertThat(result).isEqualTo(
      Cas3PremisesSummary(
        id = uuid,
        name = "bob",
        addressLine1 = "address",
        addressLine2 = null,
        postcode = "123ABC",
        pdu = "North east",
        status = PropertyStatus.active,
        bedCount = 1,
        localAuthorityAreaName = null,
      ),
    )
  }
}
