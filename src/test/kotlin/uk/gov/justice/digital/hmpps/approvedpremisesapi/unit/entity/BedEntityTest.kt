package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.entity

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import java.time.LocalDate

class BedEntityTest {
  @Nested
  inner class CasBedspaceStatus {
    @Test
    fun `getCas3BedspaceStatus returns the correct status of the bedspace`() {
      val upcomingBedEntity = createPremisesWithOneBed(LocalDate.now().plusDays(1).randomDateAfter(180), null)
      assert(upcomingBedEntity.getCas3BedspaceStatus() == Cas3BedspaceStatus.upcoming)

      val archivedBedEntity = createPremisesWithOneBed(LocalDate.now().randomDateBefore(120), LocalDate.now().randomDateBefore(90))
      assert(archivedBedEntity.getCas3BedspaceStatus() == Cas3BedspaceStatus.archived)

      val onlineBedEntityWithoutEndDate = createPremisesWithOneBed(LocalDate.now().randomDateBefore(120), null)
      assert(onlineBedEntityWithoutEndDate.getCas3BedspaceStatus() == Cas3BedspaceStatus.online)

      val onlineBedEntityWithEndDate = createPremisesWithOneBed(LocalDate.now().randomDateBefore(180), LocalDate.now().randomDateAfter(180))
      assert(onlineBedEntityWithEndDate.getCas3BedspaceStatus() == Cas3BedspaceStatus.online)
    }

    private fun createPremisesWithOneBed(bedspaceStartDate: LocalDate?, bedspaceEndDate: LocalDate?): BedEntity {
      val probationRegion = ProbationRegionEntityFactory().produce()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      return BedEntityFactory()
        .withRoom(room)
        .withStartDate(bedspaceStartDate)
        .withEndDate(bedspaceEndDate)
        .produce()
    }
  }
}
