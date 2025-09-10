package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenATemporaryAccommodationPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import java.time.LocalDate
import java.time.ZoneOffset

class Cas3UpdatePremisesCreatedDateJobTest : MigrationJobTestBase() {

  @Test
  fun `all premises with null createdAt are updated from startDate`() {
    givenAUser { user, jwt ->

      val probationRegion = givenAProbationRegion()

      val premises1 = givenATemporaryAccommodationPremises(
        region = probationRegion,
        startDate = LocalDate.of(2023, 6, 15),
      ) { premises ->
        premises.createdAt = null
        temporaryAccommodationPremisesRepository.save(premises)
      }

      val premises2 = givenATemporaryAccommodationPremises(
        region = probationRegion,
        startDate = LocalDate.of(2023, 8, 20),
      ) { premises ->
        premises.createdAt = null
        temporaryAccommodationPremisesRepository.save(premises)
      }

      val premises3 = givenATemporaryAccommodationPremises(
        region = probationRegion,
        startDate = LocalDate.of(2023, 10, 10),
      ) { premises ->
        premises.createdAt = null
        temporaryAccommodationPremisesRepository.save(premises)
      }

      migrationJobService.runMigrationJob(MigrationJobType.updateCas3PremisesCreatedAt, 10)

      val savedPremises = temporaryAccommodationPremisesRepository.findAll()
      assertThat(savedPremises).hasSize(3)

      val updatedPremises1 = savedPremises.find { it.id == premises1.id }!!
      val updatedPremises2 = savedPremises.find { it.id == premises2.id }!!
      val updatedPremises3 = savedPremises.find { it.id == premises3.id }!!

      assertThat(updatedPremises1.createdAt).isEqualTo(premises1.startDate.atStartOfDay().atOffset(ZoneOffset.UTC))
      assertThat(updatedPremises2.createdAt).isEqualTo(premises2.startDate.atStartOfDay().atOffset(ZoneOffset.UTC))
      assertThat(updatedPremises3.createdAt).isEqualTo(premises3.startDate.atStartOfDay().atOffset(ZoneOffset.UTC))
    }
  }

  @Test
  fun `premises with existing createdAt values are not updated`() {
    givenAUser { user, jwt ->
      val probationRegion = givenAProbationRegion()

      val existingCreatedAt = LocalDate.of(2023, 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC)

      val premisesWithCreatedAt = givenATemporaryAccommodationPremises(
        region = probationRegion,
        startDate = LocalDate.of(2023, 6, 15),
      ) { premises ->
        premises.createdAt = existingCreatedAt
        temporaryAccommodationPremisesRepository.save(premises)
      }

      val premisesWithoutCreatedAt = givenATemporaryAccommodationPremises(
        region = probationRegion,
        startDate = LocalDate.of(2023, 8, 20),
      ) { premises ->
        premises.createdAt = null
        temporaryAccommodationPremisesRepository.save(premises)
      }

      migrationJobService.runMigrationJob(MigrationJobType.updateCas3PremisesCreatedAt, 10)

      val savedPremises = temporaryAccommodationPremisesRepository.findAll()
      assertThat(savedPremises).hasSize(2)

      val updatedPremisesWithCreatedAt = savedPremises.find { it.id == premisesWithCreatedAt.id }!!
      val updatedPremisesWithoutCreatedAt = savedPremises.find { it.id == premisesWithoutCreatedAt.id }!!

      assertThat(updatedPremisesWithCreatedAt.createdAt).isEqualTo(existingCreatedAt)

      assertThat(updatedPremisesWithoutCreatedAt.createdAt).isEqualTo(premisesWithoutCreatedAt.startDate.atStartOfDay().atOffset(ZoneOffset.UTC))
    }
  }

  @Test
  fun `job handles pagination correctly with multiple pages`() {
    givenAUser { user, jwt ->
      val probationRegion = givenAProbationRegion()

      val premises = (1..5).map { index ->
        givenATemporaryAccommodationPremises(
          region = probationRegion,
          startDate = LocalDate.of(2023, 6, index),
        ) { premise ->
          premise.createdAt = null
          temporaryAccommodationPremisesRepository.save(premise)
        }
      }

      migrationJobService.runMigrationJob(MigrationJobType.updateCas3PremisesCreatedAt, 2)

      val savedPremises = temporaryAccommodationPremisesRepository.findAll()
      assertThat(savedPremises).hasSize(5)

      savedPremises.forEach { savedPremise ->
        val originalPremises = premises.find { it.id == savedPremise.id }!!
        assertThat(savedPremise.createdAt).isEqualTo(originalPremises.startDate.atStartOfDay().atOffset(ZoneOffset.UTC))
      }
    }
  }
}
