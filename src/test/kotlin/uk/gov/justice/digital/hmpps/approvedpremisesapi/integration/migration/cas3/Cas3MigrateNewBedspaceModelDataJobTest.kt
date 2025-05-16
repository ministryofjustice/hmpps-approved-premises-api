package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJobService

const val NO_OF_PREMISES_TO_MIGRATE = 110

class Cas3MigrateNewBedspaceModelDataJobTest : IntegrationTestBase() {

  @Autowired
  lateinit var migrationJobService: MigrationJobService
  lateinit var temporaryAccommodationPremises: List<TemporaryAccommodationPremisesEntity>

  @BeforeEach
  fun setupDataRequiredForDataMigrationToBedspaceModelTables() {
    temporaryAccommodationPremises = generateSequence {
      temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        val probationRegion = givenAProbationRegion()
        withProbationRegion(probationRegion)
        withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
        withProbationDeliveryUnit(
          probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(probationRegion)
          },
        )
      }
    }.take(NO_OF_PREMISES_TO_MIGRATE).toList()
  }

  @Test
  fun `should migrate all data required to new cas3 bedspace model tables and running the job twice`() {
    migrationJobService.runMigrationJob(MigrationJobType.cas3BedspaceModelData, 1)
    val migratedPremises = assertExpectedNumberOfPremisesWereMigrated()
    assertThatAllCas3PremisesDataWasMigratedSuccessfully(migratedPremises)
  }

  @Test
  fun `running the migration job twice does not create duplicate rows`() {
    migrationJobService.runMigrationJob(MigrationJobType.cas3BedspaceModelData, 1)
    var migratedPremises = assertExpectedNumberOfPremisesWereMigrated()
    assertThatAllCas3PremisesDataWasMigratedSuccessfully(migratedPremises)

    migrationJobService.runMigrationJob(MigrationJobType.cas3BedspaceModelData, 1)
    migratedPremises = assertExpectedNumberOfPremisesWereMigrated()
    assertThatAllCas3PremisesDataWasMigratedSuccessfully(migratedPremises)
  }

  private fun assertExpectedNumberOfPremisesWereMigrated(): List<Cas3PremisesEntity> {
    val migratedPremises = cas3PremisesRepository.findAll()
    assertThat(migratedPremises.size).isEqualTo(NO_OF_PREMISES_TO_MIGRATE)
    return migratedPremises
  }

  private fun assertThatAllCas3PremisesDataWasMigratedSuccessfully(migratedPremises: List<Cas3PremisesEntity>) {
    migratedPremises.forEach { migratedPremise ->
      val tap = temporaryAccommodationPremises.firstOrNull { it.id == migratedPremise.id }!!
      assertThatPremisesMatch(
        cas3PremisesEntity = migratedPremise,
        temporaryAccommodationPremisesEntity = tap,
      )
    }
  }

  private fun assertThatPremisesMatch(cas3PremisesEntity: Cas3PremisesEntity, temporaryAccommodationPremisesEntity: TemporaryAccommodationPremisesEntity) {
    assertThat(cas3PremisesEntity.name).isEqualTo(temporaryAccommodationPremisesEntity.name)
    assertThat(cas3PremisesEntity.postcode).isEqualTo(temporaryAccommodationPremisesEntity.postcode)
    assertThat(cas3PremisesEntity.addressLine1).isEqualTo(temporaryAccommodationPremisesEntity.addressLine1)
    assertThat(cas3PremisesEntity.addressLine2).isEqualTo(temporaryAccommodationPremisesEntity.addressLine2)
    assertThat(cas3PremisesEntity.town).isEqualTo(temporaryAccommodationPremisesEntity.town)
    assertThat(cas3PremisesEntity.probationDeliveryUnit).isEqualTo(temporaryAccommodationPremisesEntity.probationDeliveryUnit)
    assertThat(cas3PremisesEntity.localAuthorityArea?.id).isEqualTo(temporaryAccommodationPremisesEntity.localAuthorityArea?.id)
    assertThat(cas3PremisesEntity.status).isEqualTo(temporaryAccommodationPremisesEntity.status)
    assertThat(cas3PremisesEntity.notes).isEqualTo(temporaryAccommodationPremisesEntity.notes)
  }
}
