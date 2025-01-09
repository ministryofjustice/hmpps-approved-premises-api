package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory

class ApprovedPremisesEntityTest {

  @Nested
  inner class ResolveFullAddress {

    @Test
    fun `uses full address on entity, if defined`() {
      val approvedPremises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withFullAddress("The, full, address")
        .withAddressLine1("line1")
        .withAddressLine2("line2")
        .withTown("town")
        .produce()

      assertThat(approvedPremises.resolveFullAddress()).isEqualTo("The, full, address")
    }

    @Test
    fun `uses address parts, if no full address defined, all parts provided`() {
      val approvedPremises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withFullAddress(null)
        .withAddressLine1("line1")
        .withAddressLine2("line2")
        .withTown("town")
        .produce()

      assertThat(approvedPremises.resolveFullAddress()).isEqualTo("line1, line2, town")
    }

    @Test
    fun `uses address parts, if no full address defined, mandatory parts only`() {
      val approvedPremises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withFullAddress(null)
        .withAddressLine1("line1")
        .withAddressLine2(null)
        .withTown(null)
        .produce()

      assertThat(approvedPremises.resolveFullAddress()).isEqualTo("line1")
    }
  }
}
