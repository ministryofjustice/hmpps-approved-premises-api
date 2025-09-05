package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.Cas3IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJobService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import java.time.LocalDate

const val NO_OF_PREMISES_TO_MIGRATE = 110

class Cas3MigrateNewBedspaceModelDataJobTest : Cas3IntegrationTestBase() {

  @Autowired
  lateinit var migrationJobService: MigrationJobService
  lateinit var temporaryAccommodationPremises: List<TemporaryAccommodationPremisesEntity>

  @BeforeEach
  fun setupDataRequiredForDataMigrationToBedspaceModelTables() {
    val cas3PremisesCharacteristics = characteristicRepository.findAllByServiceAndModelScope(
      modelScope = "premises",
      serviceScope = ServiceName.temporaryAccommodation.value,
    )
    temporaryAccommodationPremises = generateSequence {
      temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        val premisesCharacteristicsCopy = cas3PremisesCharacteristics.toMutableList()
        val probationRegion = givenAProbationRegion()
        withProbationRegion(probationRegion)
        withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
        withProbationDeliveryUnit(
          probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(probationRegion)
          },
        )
        withCharacteristics(
          mutableListOf(
            pickRandomCharacteristicAndRemoveFromList(premisesCharacteristicsCopy),
            pickRandomCharacteristicAndRemoveFromList(premisesCharacteristicsCopy),
            pickRandomCharacteristicAndRemoveFromList(premisesCharacteristicsCopy),
            pickRandomCharacteristicAndRemoveFromList(premisesCharacteristicsCopy),
          ),
        )
        withStartDate(LocalDate.now().minusDays(100))
        withEndDate(LocalDate.now().plusDays(180))
      }
    }.take(NO_OF_PREMISES_TO_MIGRATE).toList()

    createRoomsWithSingleBedInPremises(
      temporaryAccommodationPremises,
      endDate = LocalDate.now().randomDateAfter(30),
      numOfRoomsPerPremise = randomInt(1, 5),
    )
  }

  @Test
  fun `should migrate all data required to new cas3 bedspace model tables`() {
    migrationJobService.runMigrationJob(MigrationJobType.updateCas3BedspaceModelData, 1)
    val migratedPremises = assertExpectedNumberOfPremisesWereMigrated()
    assertThatAllCas3PremisesDataAndCas3BedspacesDataWasMigratedSuccessfully(migratedPremises)
  }

  @Test
  fun `running the migration job twice does not create duplicate rows`() {
    migrationJobService.runMigrationJob(MigrationJobType.updateCas3BedspaceModelData, 1)
    var migratedPremises = assertExpectedNumberOfPremisesWereMigrated()
    assertThatAllCas3PremisesDataAndCas3BedspacesDataWasMigratedSuccessfully(migratedPremises)

    migrationJobService.runMigrationJob(MigrationJobType.updateCas3BedspaceModelData, 1)
    migratedPremises = assertExpectedNumberOfPremisesWereMigrated()
    assertThatAllCas3PremisesDataAndCas3BedspacesDataWasMigratedSuccessfully(migratedPremises)
  }

  private fun assertExpectedNumberOfPremisesWereMigrated(): List<Cas3PremisesEntity> {
    val migratedPremises = cas3PremisesRepository.findAll()
    assertThat(migratedPremises.size).isEqualTo(NO_OF_PREMISES_TO_MIGRATE)
    return migratedPremises
  }

  private fun assertThatAllCas3PremisesDataAndCas3BedspacesDataWasMigratedSuccessfully(migratedPremises: List<Cas3PremisesEntity>) {
    migratedPremises.forEach { migratedPremise ->
      val tap = temporaryAccommodationPremises.firstOrNull { it.id == migratedPremise.id }!!
      assertThatPremisesMatch(
        cas3PremisesEntity = migratedPremise,
        temporaryAccommodationPremisesEntity = tap,
      )
      assertThatPremisesCharacteristicsMatch(
        cas3PremisesEntity = migratedPremise,
        temporaryAccommodationPremisesEntity = tap,
      )
      assertThatBedspacesMatchRoomsAndBeds(
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
    assertThat(cas3PremisesEntity.turnaroundWorkingDays).isEqualTo(temporaryAccommodationPremisesEntity.turnaroundWorkingDays)
    assertThat(cas3PremisesEntity.startDate).isEqualTo(temporaryAccommodationPremisesEntity.startDate)
    assertThat(cas3PremisesEntity.endDate).isEqualTo(temporaryAccommodationPremisesEntity.endDate)
  }

  private fun assertThatBedspacesMatchRoomsAndBeds(cas3PremisesEntity: Cas3PremisesEntity, temporaryAccommodationPremisesEntity: TemporaryAccommodationPremisesEntity) {
    assertThat(cas3PremisesEntity.bedspaces.size).isEqualTo(temporaryAccommodationPremisesEntity.rooms.size)
    cas3PremisesEntity.bedspaces.forEach { bedspace ->
      val expectedRoomToMatch = temporaryAccommodationPremises
        .first { it.id == bedspace.premises.id }.rooms
        .first { room -> room.beds.first().id == bedspace.id }
      val expectedBedToMatch = expectedRoomToMatch.beds.first()

      assertThat(bedspace.reference).isEqualTo(expectedRoomToMatch.name)
      assertThat(bedspace.notes).isEqualTo(expectedRoomToMatch.notes)

      assertThat(bedspace.endDate).isEqualTo(expectedBedToMatch.endDate)
      assertThat(bedspace.startDate).isEqualTo(expectedBedToMatch.startDate)
      assertThat(bedspace.createdAt).isEqualTo(expectedBedToMatch.createdAt)
      assertThatBedspaceCharacteristicsMatch(bedspace, expectedRoomToMatch)
    }
  }

  private fun assertThatPremisesCharacteristicsMatch(cas3PremisesEntity: Cas3PremisesEntity, temporaryAccommodationPremisesEntity: TemporaryAccommodationPremisesEntity) {
    assertThat(cas3PremisesEntity.characteristics.size).isEqualTo(temporaryAccommodationPremisesEntity.characteristics.size)
    cas3PremisesEntity.characteristics.forEach { migratedCharacteristic ->
      val sourcePremisesCharacteristic = temporaryAccommodationPremisesEntity.characteristics.first { it.id == migratedCharacteristic.id }
      assertThat(migratedCharacteristic.description).isEqualTo(sourcePremisesCharacteristic.name)
      assertThat(migratedCharacteristic.name).isEqualTo(sourcePremisesCharacteristic.propertyName)
      assertThat(migratedCharacteristic.isActive).isEqualTo(sourcePremisesCharacteristic.isActive)
    }
  }

  private fun assertThatBedspaceCharacteristicsMatch(cas3Bedspace: Cas3BedspacesEntity, expectedRoomToMatch: RoomEntity) {
    assertThat(cas3Bedspace.characteristics.size).isEqualTo(expectedRoomToMatch.characteristics.size)
    cas3Bedspace.characteristics.forEach { migratedCharacteristic ->
      val sourcePremisesCharacterists = expectedRoomToMatch.characteristics.first { it.id == migratedCharacteristic.id }
      assertThat(migratedCharacteristic.description).isEqualTo(sourcePremisesCharacterists.name)
      assertThat(migratedCharacteristic.name).isEqualTo(sourcePremisesCharacterists.propertyName)
      assertThat(migratedCharacteristic.isActive).isEqualTo(sourcePremisesCharacterists.isActive)
    }
  }
}
