package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
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
  fun `all bedspaces with null createdAt are updated from startDate`() {
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
      bed1.createdAt = null
      bedRepository.save(bed1)

      val bed2 = createBedWithStartDate(premises, LocalDate.of(2023, 8, 20))
      bed2.createdAt = null
      bedRepository.save(bed2)

      val bed3 = createBedWithStartDate(premises, LocalDate.of(2023, 10, 10))
      bed3.createdAt = null
      bedRepository.save(bed3)

      migrationJobService.runMigrationJob(MigrationJobType.updateCas3BedspaceCreatedAt, 10)

      val savedBeds = bedRepository.findAll()
      assertThat(savedBeds).hasSize(3)

      val updatedBed1 = savedBeds.find { it.id == bed1.id }!!
      val updatedBed2 = savedBeds.find { it.id == bed2.id }!!
      val updatedBed3 = savedBeds.find { it.id == bed3.id }!!

      assertThat(updatedBed1.createdAt).isEqualTo(bed1.startDate!!.toLocalDateTime())
      assertThat(updatedBed2.createdAt).isEqualTo(bed2.startDate!!.toLocalDateTime())
      assertThat(updatedBed3.createdAt).isEqualTo(bed3.startDate!!.toLocalDateTime())
    }
  }

  @Test
  fun `bedspaces with existing createdAt values are not updated`() {
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

      val existingCreatedAt = LocalDate.of(2023, 1, 1).toLocalDateTime()

      val bedWithCreatedAt = createBedWithStartDate(premises, LocalDate.of(2023, 6, 15))
      bedWithCreatedAt.createdAt = existingCreatedAt
      bedRepository.save(bedWithCreatedAt)

      val bedWithoutCreatedAt = createBedWithStartDate(premises, LocalDate.of(2023, 8, 20))
      bedWithoutCreatedAt.createdAt = null
      bedRepository.save(bedWithoutCreatedAt)

      migrationJobService.runMigrationJob(MigrationJobType.updateCas3BedspaceCreatedAt, 10)

      val savedBeds = bedRepository.findAll()
      assertThat(savedBeds).hasSize(2)

      val updatedBedWithCreatedAt = savedBeds.find { it.id == bedWithCreatedAt.id }!!
      val updatedBedWithoutCreatedAt = savedBeds.find { it.id == bedWithoutCreatedAt.id }!!

      assertThat(updatedBedWithCreatedAt.createdAt).isEqualTo(existingCreatedAt)
      assertThat(updatedBedWithoutCreatedAt.createdAt).isEqualTo(bedWithoutCreatedAt.startDate!!.toLocalDateTime())
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
        bed.createdAt = null
        bedRepository.save(bed)
        bed
      }

      migrationJobService.runMigrationJob(MigrationJobType.updateCas3BedspaceCreatedAt, 2)

      val savedBeds = bedRepository.findAll()
      assertThat(savedBeds).hasSize(5)

      savedBeds.forEach { savedBed ->
        val originalBed = beds.find { it.id == savedBed.id }!!
        assertThat(savedBed.createdAt).isEqualTo(originalBed.startDate!!.toLocalDateTime())
      }
    }
  }

  @Test
  fun `bedspaces with null startDate are not updated`() {
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

      val bedWithNullStartDate = bedEntityFactory.produceAndPersist {
        withYieldedRoom {
          roomEntityFactory.produceAndPersist {
            withYieldedPremises { premises }
          }
        }
        withStartDate(null)
      }
      bedWithNullStartDate.createdAt = null
      bedRepository.save(bedWithNullStartDate)

      val bedWithValidStartDate = createBedWithStartDate(premises, LocalDate.of(2023, 8, 20))
      bedWithValidStartDate.createdAt = null
      bedRepository.save(bedWithValidStartDate)

      migrationJobService.runMigrationJob(MigrationJobType.updateCas3BedspaceCreatedAt, 10)

      val savedBeds = bedRepository.findAll()
      assertThat(savedBeds).hasSize(2)

      val updatedBedWithNullStartDate = savedBeds.find { it.id == bedWithNullStartDate.id }!!
      val updatedBedWithValidStartDate = savedBeds.find { it.id == bedWithValidStartDate.id }!!

      assertThat(updatedBedWithNullStartDate.createdAt).isNull()
      assertThat(updatedBedWithValidStartDate.createdAt).isEqualTo(bedWithValidStartDate.startDate!!.toLocalDateTime())
    }
  }
}
