package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import java.time.LocalDate

class Cas3UpdateBedspaceCreatedDateJobTest : MigrationJobTestBase() {

  private fun createBedWithStartDate(premises: TemporaryAccommodationPremisesEntity, startDate: LocalDate): BedEntity = bedEntityFactory.produceAndPersist {
    withYieldedRoom {
      roomEntityFactory.produceAndPersist {
        withYieldedPremises { premises }
      }
    }
    withStartDate(startDate)
  }

  @Test
  fun `all bedspaces will populated with createdDate as startDate value`() {
    givenAUser { user, jwt ->

      val probationRegion = givenAProbationRegion()

      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { probationRegion }
        withProbationDeliveryUnit(
          probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(probationRegion)
          },
        )
      }

      val bed1 = createBedWithStartDate(premises, LocalDate.of(2023, 6, 15))

      val bed2 = createBedWithStartDate(premises, LocalDate.of(2023, 8, 20))

      val bed3 = createBedWithStartDate(premises, LocalDate.of(2023, 10, 10))

      migrationJobService.runMigrationJob(MigrationJobType.updateCas3BedspaceCreatedAt, 10)

      val savedBeds = bedRepository.findAll()
      assertThat(savedBeds).hasSize(3)

      val updatedBed1 = savedBeds.find { it.id == bed1.id }!!
      val updatedBed2 = savedBeds.find { it.id == bed2.id }!!
      val updatedBed3 = savedBeds.find { it.id == bed3.id }!!

      assertThat(updatedBed1.createdDate).isEqualTo(bed1.startDate)
      assertThat(updatedBed2.createdDate).isEqualTo(bed2.startDate)
      assertThat(updatedBed3.createdDate).isEqualTo(bed3.startDate)
    }
  }

  @Test
  fun `job handles pagination correctly with multiple pages`() {
    givenAUser { user, jwt ->
      val probationRegion = givenAProbationRegion()

      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { probationRegion }
        withProbationDeliveryUnit(
          probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(probationRegion)
          },
        )
      }

      val beds = (1..5).map { index ->
        val bed = createBedWithStartDate(premises, LocalDate.of(2023, 6, index))
        bed
      }

      migrationJobService.runMigrationJob(MigrationJobType.updateCas3BedspaceCreatedAt, 2)

      val savedBeds = bedRepository.findAll()
      assertThat(savedBeds).hasSize(5)

      savedBeds.forEach { savedBed ->
        val originalBed = beds.find { it.id == savedBed.id }!!
        assertThat(savedBed.createdDate).isEqualTo(originalBed.startDate)
      }
    }
  }
}
