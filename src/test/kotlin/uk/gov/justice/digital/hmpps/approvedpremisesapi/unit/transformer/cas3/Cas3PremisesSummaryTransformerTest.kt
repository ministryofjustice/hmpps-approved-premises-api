package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3PremisesSummaryTransformer
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesSummary as DomainTemporaryAccommodationPremisesSummary

class Cas3PremisesSummaryTransformerTest {
  private val cas3PremisesSummaryTransformer = Cas3PremisesSummaryTransformer()

  @Test
  fun `transformDomainToCas3PremisesSummary transforms the DomainTemporaryAccommodationPremisesSummary into a Cas3PremisesSummary`() {
    val uuid = UUID.randomUUID()
    val domainPremisesSummary = TemporaryAccommodationPremisesSummaryData(
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
        bedspaceCount = 1,
        localAuthorityAreaName = "Rochford",
      ),
    )
  }

  @Test
  fun `transformDomainToCas3PremisesSummary transforms the DomainTemporaryAccommodationPremisesSummary into a Cas3PremisesSummary without optional elements`() {
    val uuid = UUID.randomUUID()
    val domainPremisesSummary = TemporaryAccommodationPremisesSummaryData(
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
        bedspaceCount = 1,
        localAuthorityAreaName = null,
      ),
    )
  }

  @SuppressWarnings("LongParameterList")
  class TemporaryAccommodationPremisesSummaryData(
    override val id: UUID,
    override val name: String,
    override val addressLine1: String,
    override val addressLine2: String?,
    override val postcode: String,
    override val pdu: String,
    override val status: PropertyStatus,
    override val bedCount: Int,
    override val localAuthorityAreaName: String?,
  ) : DomainTemporaryAccommodationPremisesSummary
}
