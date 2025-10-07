package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3PremisesEntityFactory

class Cas3PremisesEntityTest {
  @Nested
  inner class BedspaceTotals {

    val premises = Cas3PremisesEntityFactory().withDefaults().produce()

    @Test
    fun `returns correct totals when premises has mixed bedspaces`() {
      premises.bedspaces = mutableListOf(
        Cas3BedspaceEntityFactory().onlineBedspace(premises),
        Cas3BedspaceEntityFactory().onlineBedspace(premises),
        Cas3BedspaceEntityFactory().archivedBedspace(premises),
        Cas3BedspaceEntityFactory().archivedBedspace(premises),
        Cas3BedspaceEntityFactory().upcomingBedspace(premises),
        Cas3BedspaceEntityFactory().upcomingBedspace(premises),
      )
      assertThat(premises.countOnlineBedspaces()).isEqualTo(2)
      assertThat(premises.countUpcomingBedspaces()).isEqualTo(2)
      assertThat(premises.countUpcomingBedspaces()).isEqualTo(2)
    }

    @Test
    fun `returns all zeros when premises has no bedspaces`() {
      premises.bedspaces = mutableListOf()
      assertThat(premises.countOnlineBedspaces()).isEqualTo(0)
      assertThat(premises.countUpcomingBedspaces()).isEqualTo(0)
      assertThat(premises.countUpcomingBedspaces()).isEqualTo(0)
    }
  }
}
