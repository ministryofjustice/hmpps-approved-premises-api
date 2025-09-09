package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import java.time.LocalDate

class BedEntityTest {
  @Nested
  inner class CasBedspaceStatus {
    @Test
    fun `returns upcoming when startDate is in the future`() {
      val upcomingBedEntity = createPremisesWithOneBed(
        LocalDate.now().plusDays(1).randomDateAfter(180),
        null,
      )

      assertThat(upcomingBedEntity.getCas3BedspaceStatus())
        .isEqualTo(Cas3BedspaceStatus.upcoming)
    }

    @Test
    fun `returns archived when endDate is in the past`() {
      val archivedBedEntity = createPremisesWithOneBed(
        LocalDate.now().randomDateBefore(120),
        LocalDate.now().randomDateBefore(90),
      )

      assertThat(archivedBedEntity.getCas3BedspaceStatus())
        .isEqualTo(Cas3BedspaceStatus.archived)
    }

    @Test
    fun `returns online when startDate is in the past and no endDate`() {
      val onlineBedEntityWithoutEndDate = createPremisesWithOneBed(
        LocalDate.now().randomDateBefore(120),
        null,
      )

      assertThat(onlineBedEntityWithoutEndDate.getCas3BedspaceStatus())
        .isEqualTo(Cas3BedspaceStatus.online)
    }

    @Test
    fun `returns online when startDate is in the past and endDate is in the future`() {
      val onlineBedEntityWithEndDate = createPremisesWithOneBed(
        LocalDate.now().randomDateBefore(180),
        LocalDate.now().randomDateAfter(180),
      )

      assertThat(onlineBedEntityWithEndDate.getCas3BedspaceStatus())
        .isEqualTo(Cas3BedspaceStatus.online)
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
