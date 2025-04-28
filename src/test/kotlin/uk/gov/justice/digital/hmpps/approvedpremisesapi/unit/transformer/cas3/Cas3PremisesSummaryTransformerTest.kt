package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3BedspaceSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.FlatTemporaryAccommodationPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3PremisesSummaryTransformer
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesSummary as DomainTemporaryAccommodationPremisesSummary

class Cas3PremisesSummaryTransformerTest {
  private val cas3PremisesSummaryTransformer = Cas3PremisesSummaryTransformer()

  @Test
  fun `transformDomainToApi transforms the DomainTemporaryAccommodationPremisesSummary into a TemporaryAccommodationPremisesSummary`() {
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
  fun `transformFlatDomainToCas3PremisesSummary transforms the FlatTemporaryAccommodationPremisesSummary and bedspaces into a Cas3PremisesSummary`() {
    val uuid = UUID.randomUUID()
    val domainPremisesSummary = FlatTemporaryAccommodationPremisesSummaryData(
      id = uuid,
      name = "bob",
      addressLine1 = "address",
      addressLine2 = "address line 2",
      postcode = "123ABC",
      pdu = "North east",
      status = PropertyStatus.active,
      localAuthorityAreaName = "Rochford",
      bedspacesReference = "TEST1",
      bedspacesId = UUID.randomUUID(),
      bedspacesStatus = Cas3BedspaceSummary.Status.online,
    )

    val bedspaces = listOf(
      Cas3BedspaceSummary(
        domainPremisesSummary.bedspacesId!!,
        domainPremisesSummary.bedspacesReference!!,
        domainPremisesSummary.bedspacesStatus!!,
      ),
      Cas3BedspaceSummary(
        UUID.randomUUID(),
        "TEST2",
        Cas3BedspaceSummary.Status.archived,
      ),
    )

    val result = cas3PremisesSummaryTransformer.transformFlatDomainToCas3PremisesSummary(domainPremisesSummary, bedspaces)

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
        bedspaces = bedspaces,
        localAuthorityAreaName = "Rochford",
      ),
    )
  }

  @Test
  fun `transformDomainToApi transforms the DomainTemporaryAccommodationPremisesSummary into a TemporaryAccommodationPremisesSummary without optional elements`() {
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

  @Test
  fun `transformFlatDomainToCas3PremisesSummary transforms the FlatTemporaryAccommodationPremisesSummary into a Cas3PremisesSummary without optional elements`() {
    val uuid = UUID.randomUUID()
    val domainPremisesSummary = FlatTemporaryAccommodationPremisesSummaryData(
      id = uuid,
      name = "bob",
      addressLine1 = "address",
      addressLine2 = null,
      postcode = "123ABC",
      pdu = "North east",
      status = PropertyStatus.active,
      localAuthorityAreaName = null,
      bedspacesReference = "TEST1",
      bedspacesId = UUID.randomUUID(),
      bedspacesStatus = Cas3BedspaceSummary.Status.online,
    )

    val bedspaces = listOf(
      Cas3BedspaceSummary(
        domainPremisesSummary.bedspacesId!!,
        domainPremisesSummary.bedspacesReference!!,
        domainPremisesSummary.bedspacesStatus!!,
      ),
      Cas3BedspaceSummary(
        UUID.randomUUID(),
        "TEST2",
        Cas3BedspaceSummary.Status.archived,
      ),
    )

    val result = cas3PremisesSummaryTransformer.transformFlatDomainToCas3PremisesSummary(domainPremisesSummary, bedspaces)

    assertThat(result).isEqualTo(
      Cas3PremisesSummary(
        id = uuid,
        name = "bob",
        addressLine1 = "address",
        postcode = "123ABC",
        pdu = "North east",
        status = PropertyStatus.active,
        bedspaceCount = 1,
        bedspaces = bedspaces,
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

  @SuppressWarnings("LongParameterList")
  class FlatTemporaryAccommodationPremisesSummaryData(
    override val id: UUID,
    override val name: String,
    override val addressLine1: String,
    override val addressLine2: String?,
    override val postcode: String,
    override val pdu: String,
    override val status: PropertyStatus,
    override val bedspacesId: UUID?,
    override val bedspacesReference: String?,
    override val bedspacesStatus: Cas3BedspaceSummary.Status?,
    override val localAuthorityAreaName: String?,
  ) : FlatTemporaryAccommodationPremisesSummary
}
