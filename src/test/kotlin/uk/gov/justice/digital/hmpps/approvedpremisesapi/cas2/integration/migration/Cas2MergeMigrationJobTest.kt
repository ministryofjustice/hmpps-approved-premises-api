package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.ExternalUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJobService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

const val NO_OF_CAS_2_V2_APPLICATIONS_TO_MIGRATE = 102
const val NO_OF_CAS_2_APPLICATIONS_TO_UPDATE = 103
const val TOTAL_NO_OF_APPLICATIONS = NO_OF_CAS_2_V2_APPLICATIONS_TO_MIGRATE + NO_OF_CAS_2_APPLICATIONS_TO_UPDATE
const val NO_OF_NOMIS_USERS_TO_MIGRATE = 110
const val NO_OF_EXTERNAL_USERS_TO_MIGRATE = 120
const val NO_OF_CAS2V2_USERS_TO_MIGRATE = 130
const val TOTAL_NO_OF_USERS_TO_MIGRATE = NO_OF_NOMIS_USERS_TO_MIGRATE + NO_OF_CAS2V2_USERS_TO_MIGRATE + NO_OF_EXTERNAL_USERS_TO_MIGRATE

class Cas2MergeMigrationJobTest : IntegrationTestBase() {

  @Autowired
  lateinit var migrationJobService: MigrationJobService
  lateinit var nomisUsers: List<NomisUserEntity>
  lateinit var externalUsers: List<ExternalUserEntity>
  lateinit var cas2v2Users: List<Cas2v2UserEntity>
  lateinit var cas2v2Applications: List<Cas2v2ApplicationEntity>
  lateinit var cas2Applications: List<Cas2ApplicationEntity>
  lateinit var cas2v2Assessments: List<Cas2v2AssessmentEntity>

  @BeforeEach
  fun setupData() {
    nomisUsers = generateSequence {
      nomisUserEntityFactory.produceAndPersist {
        withActiveCaseloadId(randomStringUpperCase(3))
      }
    }.take(NO_OF_NOMIS_USERS_TO_MIGRATE).toList()
    externalUsers = generateSequence {
      externalUserEntityFactory.produceAndPersist()
    }.take(NO_OF_EXTERNAL_USERS_TO_MIGRATE).toList()
    cas2v2Users = generateSequence {
      cas2v2UserEntityFactory.produceAndPersist()
    }.take(NO_OF_CAS2V2_USERS_TO_MIGRATE).toList()
    cas2v2Applications = generateSequence {
      cas2v2ApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(cas2v2Users.random())
      }
    }.take(NO_OF_CAS_2_V2_APPLICATIONS_TO_MIGRATE).toList()
    cas2v2Assessments = cas2v2Applications.map {
      val assessment = cas2v2AssessmentEntityFactory.produceAndPersist {
        withApplication(it)
      }
      it.assessment = assessment
      cas2v2ApplicationRepository.save(it)
      assessment
    }
    cas2Applications = generateSequence {
      cas2ApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(nomisUsers.random())
        withApplicationOrigin(ApplicationOrigin.homeDetentionCurfew)
      }
    }.take(NO_OF_CAS_2_APPLICATIONS_TO_UPDATE).toList()
  }

  @Test
  fun `should migrate all data required to cas2`() {
    migrationJobService.runMigrationJob(MigrationJobType.migrateDataToCas2Tables, 1)
    val migratedCas2Users = assertExpectedNumberOfCas2UsersWereMigrated()
    assertExpectedNumberOfNomisUsersWereMigrated()
    assertExpectedNumberOfExternalUsersWereMigrated()
    assertThatAllUsersDataWasMigratedSuccessfully(migratedCas2Users)

    val allCas2Applications = assertExpectedNumberOfCas2v2ApplicationsWereMigrated()
    assertThatAllApplicationsDataWasMigratedOrUpdatedSuccessfully(allCas2Applications)

    val allCas2Assessments = assertExpectedNumberOfCas2v2AssessmentsWereMigrated()
    assertThatCas2v2AssessmentsDataWasMigratedSuccessfully(allCas2Assessments)
  }

  @Test
  fun `running the migration job twice does not create duplicate rows`() {
    migrationJobService.runMigrationJob(MigrationJobType.migrateDataToCas2Tables, 1)
    val migratedCas2Users = assertExpectedNumberOfCas2UsersWereMigrated()
    assertExpectedNumberOfNomisUsersWereMigrated()
    assertExpectedNumberOfExternalUsersWereMigrated()
    assertThatAllUsersDataWasMigratedSuccessfully(migratedCas2Users)

    val allCas2Applications = assertExpectedNumberOfCas2v2ApplicationsWereMigrated()
    assertThatAllApplicationsDataWasMigratedOrUpdatedSuccessfully(allCas2Applications)

    val allCas2Assessments = assertExpectedNumberOfCas2v2AssessmentsWereMigrated()
    assertThatCas2v2AssessmentsDataWasMigratedSuccessfully(allCas2Assessments)

    migrationJobService.runMigrationJob(MigrationJobType.migrateDataToCas2Tables, 1)
    val migratedCas2Users2 = assertExpectedNumberOfCas2UsersWereMigrated()
    assertExpectedNumberOfNomisUsersWereMigrated()
    assertExpectedNumberOfExternalUsersWereMigrated()
    assertThatAllUsersDataWasMigratedSuccessfully(migratedCas2Users2)

    val allCas2Applications2 = assertExpectedNumberOfCas2v2ApplicationsWereMigrated()
    assertThatAllApplicationsDataWasMigratedOrUpdatedSuccessfully(allCas2Applications2)

    val allCas2Assessments2 = assertExpectedNumberOfCas2v2AssessmentsWereMigrated()
    assertThatCas2v2AssessmentsDataWasMigratedSuccessfully(allCas2Assessments2)
  }

  private fun assertExpectedNumberOfCas2v2ApplicationsWereMigrated(): List<Cas2ApplicationEntity> {
    val allApplications = cas2ApplicationRepository.findAll()
    assertThat(allApplications.size).isEqualTo(TOTAL_NO_OF_APPLICATIONS)
    return allApplications
  }

  private fun assertExpectedNumberOfCas2v2AssessmentsWereMigrated(): List<Cas2AssessmentEntity> {
    val allAssessments = cas2AssessmentRepository.findAll()
    assertThat(allAssessments.size).isEqualTo(NO_OF_CAS_2_V2_APPLICATIONS_TO_MIGRATE)
    return allAssessments
  }

  private fun assertExpectedNumberOfCas2UsersWereMigrated(): List<Cas2UserEntity> {
    val migratedUsers = cas2UserRepository.findAll()
    assertThat(migratedUsers.size).isEqualTo(TOTAL_NO_OF_USERS_TO_MIGRATE)
    return migratedUsers
  }

  private fun assertExpectedNumberOfNomisUsersWereMigrated(): List<NomisUserEntity> {
    val migratedUsers = nomisUserRepository.findAll()
    assertThat(migratedUsers.size).isEqualTo(NO_OF_NOMIS_USERS_TO_MIGRATE + NO_OF_CAS2V2_USERS_TO_MIGRATE)
    return migratedUsers
  }

  private fun assertExpectedNumberOfExternalUsersWereMigrated(): List<ExternalUserEntity> {
    val migratedUsers = externalUserRepository.findAll()
    assertThat(migratedUsers.size).isEqualTo(NO_OF_EXTERNAL_USERS_TO_MIGRATE + NO_OF_CAS2V2_USERS_TO_MIGRATE)
    return migratedUsers
  }

  private fun assertThatAllUsersDataWasMigratedSuccessfully(migratedUsers: List<Cas2UserEntity>) {
    migratedUsers.forEach { migratedUser ->
      val nomisUser = nomisUsers.firstOrNull { it.id == migratedUser.id }
      if (nomisUser != null) {
        assertThatNomisUsersMatch(
          cas2UserEntity = migratedUser,
          nomisUserEntity = nomisUser,
        )
      } else {
        val externalUser = externalUsers.firstOrNull { it.id == migratedUser.id }
        if (externalUser != null) {
          assertThatExternalUsersMatch(
            cas2UserEntity = migratedUser,
            externalUserEntity = externalUser,
          )
        } else {
          val cas2v2User = cas2v2Users.firstOrNull { it.id == migratedUser.id }!!
          assertThatCas2v2UsersMatch(
            cas2UserEntity = migratedUser,
            cas2v2UserEntity = cas2v2User,
          )
        }
      }
    }
  }

  private fun assertThatAllApplicationsDataWasMigratedOrUpdatedSuccessfully(allApplications: List<Cas2ApplicationEntity>) {
    allApplications.forEach { cas2Application ->
      val cas2v2Application = cas2v2Applications.firstOrNull { it.id == cas2Application.id }
      if (cas2v2Application != null) {
        assertThatCas2v2ApplicationsMatch(
          cas2ApplicationEntity = cas2Application,
          cas2v2ApplicationEntity = cas2v2Application,
        )
      } else {
        assertThat(cas2Application.applicationOrigin).isEqualTo(ApplicationOrigin.homeDetentionCurfew)
        assertThat(cas2Application.createdByCas2User).isNotNull
      }
    }
  }

  private fun assertThatCas2v2AssessmentsDataWasMigratedSuccessfully(allAssessments: List<Cas2AssessmentEntity>) {
    allAssessments.forEach { cas2Assessment ->
      val cas2v2Assessment = cas2v2Assessments.firstOrNull { it.id == cas2Assessment.id }!!
      assertThatCas2v2AssessmentsMatch(
        cas2AssessmentEntity = cas2Assessment,
        cas2v2AssessmentEntity = cas2v2Assessment,
      )
    }
  }

  private fun assertThatNomisUsersMatch(cas2UserEntity: Cas2UserEntity, nomisUserEntity: NomisUserEntity) {
    assertThat(cas2UserEntity.id).isEqualTo(nomisUserEntity.id)
    assertThat(cas2UserEntity.name).isEqualTo(nomisUserEntity.name)
    assertThat(cas2UserEntity.username).isEqualTo(nomisUserEntity.nomisUsername)
    assertThat(cas2UserEntity.email).isEqualTo(nomisUserEntity.email)
    assertThat(cas2UserEntity.userType).isEqualTo(Cas2UserType.NOMIS)
    assertThat(cas2UserEntity.nomisStaffId).isEqualTo(nomisUserEntity.nomisStaffId)
    assertThat(cas2UserEntity.activeNomisCaseloadId).isEqualTo(nomisUserEntity.activeCaseloadId)
    assertThat(cas2UserEntity.deliusStaffCode).isNull()
    assertThat(cas2UserEntity.deliusTeamCodes).isNull()
    assertThat(cas2UserEntity.isEnabled).isEqualTo(nomisUserEntity.isEnabled)
    assertThat(cas2UserEntity.isActive).isEqualTo(nomisUserEntity.isActive)
    assertThat(cas2UserEntity.externalType).isNull()
    assertThat(cas2UserEntity.nomisAccountType).isEqualTo(nomisUserEntity.accountType)
  }

  private fun assertThatExternalUsersMatch(cas2UserEntity: Cas2UserEntity, externalUserEntity: ExternalUserEntity) {
    assertThat(cas2UserEntity.id).isEqualTo(externalUserEntity.id)
    assertThat(cas2UserEntity.name).isEqualTo(externalUserEntity.name)
    assertThat(cas2UserEntity.username).isEqualTo(externalUserEntity.username)
    assertThat(cas2UserEntity.email).isEqualTo(externalUserEntity.email)
    assertThat(cas2UserEntity.userType).isEqualTo(Cas2UserType.EXTERNAL)
    assertThat(cas2UserEntity.nomisStaffId).isNull()
    assertThat(cas2UserEntity.activeNomisCaseloadId).isNull()
    assertThat(cas2UserEntity.deliusStaffCode).isNull()
    assertThat(cas2UserEntity.deliusTeamCodes).isNull()
    assertThat(cas2UserEntity.isEnabled).isEqualTo(externalUserEntity.isEnabled)
    assertThat(cas2UserEntity.isActive).isEqualTo(true)
    assertThat(cas2UserEntity.externalType).isEqualTo(externalUserEntity.origin)
    assertThat(cas2UserEntity.nomisAccountType).isNull()
  }

  private fun assertThatCas2v2UsersMatch(cas2UserEntity: Cas2UserEntity, cas2v2UserEntity: Cas2v2UserEntity) {
    assertThat(cas2UserEntity.id).isEqualTo(cas2v2UserEntity.id)
    assertThat(cas2UserEntity.name).isEqualTo(cas2v2UserEntity.name)
    assertThat(cas2UserEntity.username).isEqualTo(cas2v2UserEntity.username)
    assertThat(cas2UserEntity.email).isEqualTo(cas2v2UserEntity.email)
    assertThat(cas2UserEntity.userType).isEqualTo(getUserType(cas2v2UserEntity))
    assertThat(cas2UserEntity.nomisStaffId).isEqualTo(cas2v2UserEntity.nomisStaffId)
    assertThat(cas2UserEntity.activeNomisCaseloadId).isEqualTo(cas2v2UserEntity.activeNomisCaseloadId)
    assertThat(cas2UserEntity.deliusStaffCode).isEqualTo(cas2v2UserEntity.deliusStaffCode)
    assertThat(cas2UserEntity.deliusTeamCodes).isEqualTo(cas2v2UserEntity.deliusTeamCodes)
    assertThat(cas2UserEntity.isEnabled).isEqualTo(cas2v2UserEntity.isEnabled)
    assertThat(cas2UserEntity.isActive).isEqualTo(cas2v2UserEntity.isActive)
    if (cas2UserEntity.userType == Cas2UserType.NOMIS) {
      assertThat(cas2UserEntity.nomisAccountType).isEqualTo("GENERAL")
    } else {
      assertThat(cas2UserEntity.nomisAccountType).isNull()
    }
    if (cas2UserEntity.userType == Cas2UserType.EXTERNAL) {
      assertThat(cas2UserEntity.externalType).isEqualTo("NACRO")
    } else {
      assertThat(cas2UserEntity.externalType).isNull()
    }
  }

  private fun assertThatCas2v2ApplicationsMatch(cas2ApplicationEntity: Cas2ApplicationEntity, cas2v2ApplicationEntity: Cas2v2ApplicationEntity) {
    assertThat(cas2ApplicationEntity.id).isEqualTo(cas2v2ApplicationEntity.id)
    assertThat(cas2ApplicationEntity.createdByUser.id).isEqualTo(cas2v2ApplicationEntity.createdByUser.id)
    assertThat(cas2ApplicationEntity.crn).isEqualTo(cas2v2ApplicationEntity.crn)
    assertThat(cas2ApplicationEntity.data).isEqualTo(cas2v2ApplicationEntity.data)
    assertThat(cas2ApplicationEntity.createdAt).isEqualTo(cas2v2ApplicationEntity.createdAt)
    assertThat(cas2ApplicationEntity.submittedAt).isEqualTo(cas2v2ApplicationEntity.submittedAt)
    assertThat(cas2ApplicationEntity.document).isEqualTo(cas2v2ApplicationEntity.document)
    assertThat(cas2ApplicationEntity.nomsNumber).isEqualTo(cas2v2ApplicationEntity.nomsNumber)
    assertThat(cas2ApplicationEntity.referringPrisonCode).isEqualTo(cas2v2ApplicationEntity.referringPrisonCode)
    assertThat(cas2ApplicationEntity.preferredAreas).isEqualTo(cas2v2ApplicationEntity.preferredAreas)
    assertThat(cas2ApplicationEntity.hdcEligibilityDate).isEqualTo(cas2v2ApplicationEntity.hdcEligibilityDate)
    assertThat(cas2ApplicationEntity.conditionalReleaseDate).isEqualTo(cas2v2ApplicationEntity.conditionalReleaseDate)
    assertThat(cas2ApplicationEntity.telephoneNumber).isEqualTo(cas2v2ApplicationEntity.telephoneNumber)
    assertThat(cas2ApplicationEntity.abandonedAt).isEqualTo(cas2v2ApplicationEntity.abandonedAt)
    assertThat(cas2ApplicationEntity.applicationOrigin).isEqualTo(cas2v2ApplicationEntity.applicationOrigin)
    assertThat(cas2ApplicationEntity.bailHearingDate).isEqualTo(cas2v2ApplicationEntity.bailHearingDate)
    assertThat(cas2ApplicationEntity.createdByCas2User!!.id).isEqualTo(cas2v2ApplicationEntity.createdByUser.id)
  }

  private fun assertThatCas2v2AssessmentsMatch(cas2AssessmentEntity: Cas2AssessmentEntity, cas2v2AssessmentEntity: Cas2v2AssessmentEntity) {
    assertThat(cas2AssessmentEntity.id).isEqualTo(cas2v2AssessmentEntity.id)
    assertThat(cas2AssessmentEntity.application.id).isEqualTo(cas2v2AssessmentEntity.application.id)
    assertThat(cas2AssessmentEntity.nacroReferralId).isEqualTo(cas2v2AssessmentEntity.nacroReferralId)
    assertThat(cas2AssessmentEntity.assessorName).isEqualTo(cas2v2AssessmentEntity.assessorName)
    assertThat(cas2AssessmentEntity.createdAt).isEqualTo(cas2v2AssessmentEntity.createdAt)
  }

  private fun getUserType(user: Cas2v2UserEntity) = when (user.userType) {
    Cas2v2UserType.NOMIS -> Cas2UserType.NOMIS
    Cas2v2UserType.DELIUS -> Cas2UserType.DELIUS
    Cas2v2UserType.EXTERNAL -> Cas2UserType.EXTERNAL
  }
}
