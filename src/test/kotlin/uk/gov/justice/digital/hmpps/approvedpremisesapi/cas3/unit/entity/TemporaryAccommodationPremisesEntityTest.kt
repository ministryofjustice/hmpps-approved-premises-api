package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus

class TemporaryAccommodationPremisesEntityTest {
  @Nested
  inner class PropertyStatusToCas3PremisesStatus {

    @Test
    fun `returns ARCHIVED when premises has a PropertyStatus of archived`() {
      val premises = TemporaryAccommodationPremisesEntityFactory().withDefaults().withStatus(PropertyStatus.archived).produce()
      assertThat(premises.status).isEqualTo(PropertyStatus.archived)
      assertThat(premises.cas3PremisesStatus).isEqualTo(Cas3PremisesStatus.archived)
    }

    @Test
    fun `returns ONLINE when premises has a PropertyStatus of active`() {
      val premises = TemporaryAccommodationPremisesEntityFactory().withDefaults().withStatus(PropertyStatus.active).produce()
      assertThat(premises.status).isEqualTo(PropertyStatus.active)
      assertThat(premises.cas3PremisesStatus).isEqualTo(Cas3PremisesStatus.online)
    }
  }
}
