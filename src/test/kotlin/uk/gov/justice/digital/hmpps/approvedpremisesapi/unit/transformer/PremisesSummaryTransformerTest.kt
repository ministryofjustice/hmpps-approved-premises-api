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
      status = PropertyStatus.ACTIVE,
      bedCount = 1,
      localAuthorityAreaName = "Rochford",
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
        status = PropertyStatus.ACTIVE,
        bedCount = 1,
        service = "CAS3",
        localAuthorityAreaName = "Rochford",
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
      status = PropertyStatus.ACTIVE,
      bedCount = 1,
      apCode = "APCODE",
      regionName = "Some region",
      apAreaName = "Some AP Area name",
    )

    val result = premisesSummaryTransformer.transformDomainToApi(domainPremisesSummary)

    assertThat(result).isEqualTo(
      ApprovedPremisesSummary(
        id = uuid,
        name = "bob",
        addressLine1 = "address",
        addressLine2 = "address line 2",
        postcode = "123ABC",
        status = PropertyStatus.ACTIVE,
        bedCount = 1,
        apCode = "APCODE",
        service = "CAS1",
        probationRegion = "Some region",
        apArea = "Some AP Area name",
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
      status = PropertyStatus.ACTIVE,
      bedCount = 1,
      localAuthorityAreaName = null,
    )

    val result = premisesSummaryTransformer.transformDomainToApi(domainPremisesSummary)

    assertThat(result).isEqualTo(
      TemporaryAccommodationPremisesSummary(
        id = uuid,
        name = "bob",
        addressLine1 = "address",
        addressLine2 = null,
        postcode = "123ABC",
        pdu = "North east",
        status = PropertyStatus.ACTIVE,
        bedCount = 1,
        service = "CAS3",
        localAuthorityAreaName = null,
      ),
    )
  }
}
