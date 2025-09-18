package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJobService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

const val NO_OF_USERS_TO_MIGRATE = 110

class Cas2UserNomisMigrationJobTest : IntegrationTestBase() {

  @Autowired
  lateinit var migrationJobService: MigrationJobService
  lateinit var nomisUsers: List<NomisUserEntity>

  @BeforeEach
  fun setupDataRequiredForDataMigrationCas2UsersTable() {
    nomisUsers = generateSequence {
      nomisUserEntityFactory.produceAndPersist {
        withActiveCaseloadId(randomStringUpperCase(3))
      }
    }.take(NO_OF_USERS_TO_MIGRATE).toList()
  }

  @Test
  fun `should migrate all data required to new cas2 user table and run the job twice`() {
    migrationJobService.runMigrationJob(MigrationJobType.migrateNomisUsersToCas2UsersTable, 1)
    val migratedUsers = assertExpectedNumberOfUsersWereMigrated()
    assertThatAllNomisUsersDataWasMigratedSuccessfully(migratedUsers)
  }

  @Test
  fun `running the migration job twice does not create duplicate rows`() {
    migrationJobService.runMigrationJob(MigrationJobType.migrateNomisUsersToCas2UsersTable, 1)
    var migratedUsers = assertExpectedNumberOfUsersWereMigrated()
    assertThatAllNomisUsersDataWasMigratedSuccessfully(migratedUsers)

    migrationJobService.runMigrationJob(MigrationJobType.migrateNomisUsersToCas2UsersTable, 1)
    migratedUsers = assertExpectedNumberOfUsersWereMigrated()
    assertThatAllNomisUsersDataWasMigratedSuccessfully(migratedUsers)
  }

  private fun assertExpectedNumberOfUsersWereMigrated(): List<Cas2UserEntity> {
    val migratedPremises = cas2UserRepository.findAll()
    assertThat(migratedPremises.size).isEqualTo(NO_OF_USERS_TO_MIGRATE)
    return migratedPremises
  }

  private fun assertThatAllNomisUsersDataWasMigratedSuccessfully(migratedPremises: List<Cas2UserEntity>) {
    migratedPremises.forEach { migratedPremise ->
      val nomisUser = nomisUsers.firstOrNull { it.id == migratedPremise.id }!!
      assertThatUsersMatch(
        cas2UserEntity = migratedPremise,
        nomsiUserEntity = nomisUser,
      )
    }
  }

  private fun assertThatUsersMatch(cas2UserEntity: Cas2UserEntity, nomsiUserEntity: NomisUserEntity) {
    assertThat(cas2UserEntity.id).isEqualTo(nomsiUserEntity.id)
    assertThat(cas2UserEntity.name).isEqualTo(nomsiUserEntity.name)
    assertThat(cas2UserEntity.username).isEqualTo(nomsiUserEntity.nomisUsername)
    assertThat(cas2UserEntity.email).isEqualTo(nomsiUserEntity.email)
    assertThat(cas2UserEntity.userType).isEqualTo(Cas2UserType.NOMIS)
    assertThat(cas2UserEntity.nomisStaffId).isEqualTo(nomsiUserEntity.nomisStaffId)
    assertThat(cas2UserEntity.activeNomisCaseloadId).isEqualTo(nomsiUserEntity.activeCaseloadId)
    assertThat(cas2UserEntity.deliusStaffCode).isNull()
    assertThat(cas2UserEntity.deliusTeamCodes).isNull()
    assertThat(cas2UserEntity.isEnabled).isEqualTo(nomsiUserEntity.isEnabled)
    assertThat(cas2UserEntity.isActive).isEqualTo(nomsiUserEntity.isActive)
    assertThat(cas2UserEntity.externalType).isNull()
    assertThat(cas2UserEntity.nomisAccountType).isEqualTo(nomsiUserEntity.accountType)
  }
}
