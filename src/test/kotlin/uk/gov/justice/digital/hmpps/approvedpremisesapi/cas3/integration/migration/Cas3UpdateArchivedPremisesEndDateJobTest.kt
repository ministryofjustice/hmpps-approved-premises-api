package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import java.time.LocalDate

class Cas3UpdateArchivedPremisesEndDateJobTest : MigrationJobTestBase() {

  @Autowired
  private lateinit var premisesRepository: PremisesRepository

  private fun createPremisesWithoutEndDate(user: UserEntity, status: PropertyStatus = PropertyStatus.archived): TemporaryAccommodationPremisesEntity {
    val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
      withProbationRegion(user.probationRegion)
    }

    return temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion { user.probationRegion }
      withProbationDeliveryUnit(probationDeliveryUnit)
      withStatus(status)
      withEndDate(null)
    }
  }

  private fun createPremisesWithEndDate(user: UserEntity, endDate: LocalDate, status: PropertyStatus = PropertyStatus.archived): TemporaryAccommodationPremisesEntity {
    val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
      withProbationRegion(user.probationRegion)
    }

    return temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion { user.probationRegion }
      withProbationDeliveryUnit(probationDeliveryUnit)
      withStatus(status)
      withEndDate(endDate)
    }
  }

  @Test
  fun `updates premises end date from the latest bedspace end date`() {
    givenAUser { user, _ ->
      val premises = createPremisesWithoutEndDate(user)

      val earlierEndDate = LocalDate.now().plusDays(5)
      val laterEndDate = LocalDate.now().plusDays(10)

      bedEntityFactory.produceAndPersist {
        withRoom(
          roomEntityFactory.produceAndPersist {
            withPremises(premises)
          },
        )
        withEndDate(earlierEndDate)
      }

      bedEntityFactory.produceAndPersist {
        withRoom(
          roomEntityFactory.produceAndPersist {
            withPremises(premises)
          },
        )
        withEndDate(laterEndDate)
      }

      assertThat(premises.endDate).isNull()

      migrationJobService.runMigrationJob(MigrationJobType.updateCas3ArchivedPremisesEndDate)

      val updatedPremises = premisesRepository.findById(premises.id).get() as TemporaryAccommodationPremisesEntity

      assertThat(updatedPremises.endDate).isEqualTo(laterEndDate)
    }
  }

  @Test
  fun `does not update premises that already have end dates`() {
    givenAUser { user, _ ->
      val existingEndDate = LocalDate.now().plusDays(3)
      val premises = createPremisesWithEndDate(user, existingEndDate)

      val laterEndDate = LocalDate.now().plusDays(10)
      bedEntityFactory.produceAndPersist {
        withRoom(
          roomEntityFactory.produceAndPersist {
            withPremises(premises)
          },
        )
        withEndDate(laterEndDate)
      }

      migrationJobService.runMigrationJob(MigrationJobType.updateCas3ArchivedPremisesEndDate)

      val updatedPremises = premisesRepository.findById(premises.id).get() as TemporaryAccommodationPremisesEntity

      assertThat(updatedPremises.endDate).isEqualTo(existingEndDate)
    }
  }

  @Test
  fun `does not update premises when no bedspaces exist`() {
    givenAUser { user, _ ->
      val premises = createPremisesWithoutEndDate(user)

      assertThat(premises.endDate).isNull()

      migrationJobService.runMigrationJob(MigrationJobType.updateCas3ArchivedPremisesEndDate)

      val updatedPremises = premisesRepository.findById(premises.id).get() as TemporaryAccommodationPremisesEntity

      assertThat(updatedPremises.endDate).isNull()
    }
  }

  @Test
  fun `updates archived premises but not active premises`() {
    givenAUser { user, _ ->
      val archivedPremises = createPremisesWithoutEndDate(user, PropertyStatus.archived)
      val bedEndDate = LocalDate.now().plusDays(10)

      bedEntityFactory.produceAndPersist {
        withRoom(
          roomEntityFactory.produceAndPersist {
            withPremises(archivedPremises)
          },
        )
        withEndDate(bedEndDate)
      }

      val activePremises = createPremisesWithoutEndDate(user, PropertyStatus.active)

      bedEntityFactory.produceAndPersist {
        withRoom(
          roomEntityFactory.produceAndPersist {
            withPremises(activePremises)
          },
        )
        withEndDate(bedEndDate)
      }

      assertThat(archivedPremises.endDate).isNull()
      assertThat(activePremises.endDate).isNull()
      assertThat(archivedPremises.status).isEqualTo(PropertyStatus.archived)
      assertThat(activePremises.status).isEqualTo(PropertyStatus.active)

      migrationJobService.runMigrationJob(MigrationJobType.updateCas3ArchivedPremisesEndDate)

      val updatedArchivedPremises = premisesRepository.findById(archivedPremises.id).get() as TemporaryAccommodationPremisesEntity
      val updatedActivePremises = premisesRepository.findById(activePremises.id).get() as TemporaryAccommodationPremisesEntity

      assertThat(updatedArchivedPremises.endDate).isEqualTo(bedEndDate)
      assertThat(updatedActivePremises.endDate).isNull()
    }
  }
}
