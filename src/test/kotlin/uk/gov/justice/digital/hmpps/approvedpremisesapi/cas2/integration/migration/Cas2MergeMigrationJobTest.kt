package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.ExternalUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.migration.FIXED_CREATED_BY_EXTERNAL_USER_ID_FOR_CAS2_V2_USER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.migration.FIXED_CREATED_BY_NOMIS_USER_ID_FOR_CAS2_V2_USER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.integration.Cas2v2IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2StatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJobService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.OffsetDateTime

const val NO_OF_CAS_2_V2_APPLICATIONS_TO_MIGRATE = 102
const val NO_OF_CAS_2_APPLICATIONS_TO_UPDATE = 103
const val NO_OF_CAS_2_STATUS_UPDATES_TO_UPDATE = 108
const val TOTAL_NO_OF_APPLICATIONS = NO_OF_CAS_2_V2_APPLICATIONS_TO_MIGRATE + NO_OF_CAS_2_APPLICATIONS_TO_UPDATE
const val NO_OF_NOMIS_USERS_TO_MIGRATE = 110
const val NO_OF_EXTERNAL_USERS_TO_MIGRATE = 120
const val NO_OF_CAS2V2_USERS_TO_MIGRATE = 130
const val TOTAL_NO_OF_USERS_TO_MIGRATE = NO_OF_NOMIS_USERS_TO_MIGRATE + NO_OF_CAS2V2_USERS_TO_MIGRATE + NO_OF_EXTERNAL_USERS_TO_MIGRATE

class Cas2MergeMigrationJobTest : Cas2v2IntegrationTestBase() {

  @Autowired
  lateinit var migrationJobService: MigrationJobService
  lateinit var nomisUsers: List<NomisUserEntity>
  lateinit var externalUsers: List<ExternalUserEntity>
  lateinit var cas2v2Users: List<Cas2v2UserEntity>
  lateinit var cas2v2Applications: List<Cas2v2ApplicationEntity>
  lateinit var cas2Applications: List<Cas2ApplicationEntity>
  lateinit var cas2ApplicationAssignments: List<Cas2ApplicationAssignmentEntity>
  lateinit var cas2v2Assessments: List<Cas2v2AssessmentEntity>
  lateinit var cas2v2ApplicationNotes: List<Cas2v2ApplicationNoteEntity>
  lateinit var cas2ApplicationNotes: List<Cas2ApplicationNoteEntity>
  lateinit var cas2v2StatusUpdates: List<Cas2v2StatusUpdateEntity>
  lateinit var cas2StatusUpdates: List<Cas2StatusUpdateEntity>
  lateinit var cas2v2StatusUpdateDetails: List<Cas2v2StatusUpdateDetailEntity>

  @BeforeEach
  fun setupData() {
    nomisUsers = generateSequence {
      nomisUserEntityFactory.produceAndPersist {
        withActiveCaseloadId(randomStringUpperCase(3))
        withCreatedAt(OffsetDateTime.parse("2021-03-04T10:15:30+01:00"))
      }
    }.take(NO_OF_NOMIS_USERS_TO_MIGRATE).toList()
    externalUsers = generateSequence {
      externalUserEntityFactory.produceAndPersist {
        withCreatedAt(OffsetDateTime.parse("2021-03-04T10:15:30+01:00"))
      }
    }.take(NO_OF_EXTERNAL_USERS_TO_MIGRATE).toList()
    cas2v2Users = generateSequence {
      cas2v2UserEntityFactory.produceAndPersist {
        withUserType(Cas2v2UserType.entries.random())
        withCreatedAt(OffsetDateTime.parse("2021-03-04T10:15:30+01:00"))
        withActiveNomisCaseloadId(randomStringUpperCase(3))
        withDeliusStaffCode(randomStringUpperCase(3))
        withDeliusTeamCodes(listOf(randomStringUpperCase(3)))
      }
    }.take(NO_OF_CAS2V2_USERS_TO_MIGRATE).toList()
    cas2v2Applications = generateSequence {
      cas2v2ApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(cas2v2Users.random())
      }
    }.take(NO_OF_CAS_2_V2_APPLICATIONS_TO_MIGRATE).toList()
    cas2v2Assessments = cas2v2Applications.map {
      val assessment = cas2v2AssessmentEntityFactory.produceAndPersist {
        withApplication(it)
        withCreatedAt(OffsetDateTime.parse("2021-03-04T10:15:30+01:00"))
      }
      it.assessment = assessment
      cas2v2ApplicationRepository.save(it)
      assessment
    }
    cas2v2ApplicationNotes = cas2v2Applications.map {
      val applicationNote = cas2v2NoteEntityFactory.produceAndPersist {
        withApplication(it)
        withAssessment(it.assessment)
        withCreatedByUser(it.createdByUser)
      }
      cas2v2ApplicationRepository.save(it)
      applicationNote
    }
    cas2v2StatusUpdates = cas2v2Applications.map {
      val statusUpdate = cas2v2StatusUpdateEntityFactory.produceAndPersist {
        withApplication(it)
        withAssessment(it.assessment!!)
        withCreatedAt(OffsetDateTime.parse("2021-09-19T13:00:13.530161Z"))
        withAssessor(cas2v2Users.filter { it.userType == Cas2v2UserType.EXTERNAL }.random())
        withDescription(randomStringUpperCase(8))
      }
      cas2v2ApplicationRepository.save(it)
      statusUpdate
    }
    cas2v2StatusUpdateDetails = cas2v2StatusUpdates.map {
      val statusUpdateDetail = cas2v2StatusUpdateDetailEntityFactory.produceAndPersist {
        withCreatedAt(OffsetDateTime.parse("2022-09-19T13:00:13.530161Z"))
        withStatusUpdate(it)
        withLabel(randomStringUpperCase(4))
      }
      cas2v2StatusUpdateRepository.save(it)
      statusUpdateDetail
    }
    cas2Applications = generateSequence {
      cas2ApplicationEntityFactory.produceAndPersist {
        withCreatedByNomisUser(nomisUsers.random())
        withReferringPrisonCode(randomStringUpperCase(3))
        withApplicationOrigin(ApplicationOrigin.homeDetentionCurfew)
      }
    }.take(NO_OF_CAS_2_APPLICATIONS_TO_UPDATE).toList()
    cas2ApplicationAssignments = cas2Applications.map {
      it.createApplicationAssignment(it.referringPrisonCode!!, it.createdByUser)
      cas2ApplicationRepository.save(it)
      it.applicationAssignments.first()
    }.take(NO_OF_CAS_2_APPLICATIONS_TO_UPDATE).toList()
    cas2StatusUpdates = generateSequence {
      cas2StatusUpdateEntityFactory.produceAndPersist {
        withApplication(cas2Applications.random())
        withAssessor(externalUsers.random())
      }
    }.take(NO_OF_CAS_2_STATUS_UPDATES_TO_UPDATE).toList()
    cas2ApplicationNotes = cas2Applications.map {
      val applicationNote = cas2NoteEntityFactory.produceAndPersist {
        withApplication(it)
        withAssessment(it.assessment)
        withCreatedByUser(it.createdByUser)
      }
      cas2ApplicationRepository.save(it)
      applicationNote
    }
  }

  @Test
  fun `should migrate all data required to cas2`() {
    migrateAll()
  }

  @Test
  fun `running the migration job twice does not create duplicate rows`() {
    migrateAll()
    migrateAll()
  }

  private fun migrateAll() {
    migrationJobService.runMigrationJob(MigrationJobType.migrateDataToCas2Tables, 1)
    val migratedCas2Users = assertExpectedNumberOfCas2UsersWereMigrated()
    assertExpectedNumberOfNomisUsersWereMigrated()
    assertExpectedNumberOfExternalUsersWereMigrated()
    assertThatAllUsersDataWasMigratedSuccessfully(migratedCas2Users)

    val allCas2Applications = assertExpectedNumberOfCas2v2ApplicationsWereMigrated()
    assertThatAllApplicationsDataWasMigratedOrUpdatedSuccessfully(allCas2Applications)

    val allCas2Assessments = assertExpectedNumberOfCas2v2AssessmentsWereMigrated()
    assertThatCas2v2AssessmentsDataWasMigratedSuccessfully(allCas2Assessments)

    val allCas2ApplicationNotes = assertExpectedNumberOfCas2v2ApplicationNotesWereMigrated()
    assertThatAllApplicationNotesDataWasMigratedOrUpdatedSuccessfully(allCas2ApplicationNotes)

    val allCas2StatusUpdates = assertExpectedNumberOfCas2v2StatusUpdatesWereMigrated()
    assertThatAllStatusUpdatesDataWasMigratedOrUpdatedSuccessfully(allCas2StatusUpdates)

    val allCas2StatusUpdateDetails = assertExpectedNumberOfCas2v2StatusUpdateDetailsWereMigrated()
    assertThatCas2v2StatusUpdateDetailsDataWasMigratedSuccessfully(allCas2StatusUpdateDetails)

    val allCas2ApplicationAssignments = assertExpectedNumberOfCas2ApplicationAssignmentsWereUpdated()
    assertThatAllApplicationAssignmentsDataWasMigratedOrUpdatedSuccessfully(allCas2ApplicationAssignments)
  }

  private fun assertExpectedNumberOfCas2v2ApplicationsWereMigrated(): List<Cas2ApplicationEntity> {
    val allApplications = cas2ApplicationRepository.findAll()
    assertThat(allApplications.size).isEqualTo(TOTAL_NO_OF_APPLICATIONS)
    return allApplications
  }

  private fun assertExpectedNumberOfCas2v2StatusUpdatesWereMigrated(): List<Cas2StatusUpdateEntity> {
    val allStatusUpdates = cas2StatusUpdateRepository.findAll()
    assertThat(allStatusUpdates.size).isEqualTo(NO_OF_CAS_2_STATUS_UPDATES_TO_UPDATE + NO_OF_CAS_2_V2_APPLICATIONS_TO_MIGRATE)
    return allStatusUpdates
  }

  private fun assertExpectedNumberOfCas2ApplicationAssignmentsWereUpdated(): List<Cas2ApplicationAssignmentEntity> {
    val allApplicationAssignments = cas2ApplicationAssignmentRepository.findAll()
    assertThat(allApplicationAssignments.size).isEqualTo(NO_OF_CAS_2_APPLICATIONS_TO_UPDATE)
    return allApplicationAssignments
  }

  private fun assertExpectedNumberOfCas2v2ApplicationNotesWereMigrated(): List<Cas2ApplicationNoteEntity> {
    val allApplicationNotes = cas2NoteRepository.findAll()
    assertThat(allApplicationNotes.size).isEqualTo(NO_OF_CAS_2_APPLICATIONS_TO_UPDATE + NO_OF_CAS_2_V2_APPLICATIONS_TO_MIGRATE)
    return allApplicationNotes
  }

  private fun assertExpectedNumberOfCas2v2AssessmentsWereMigrated(): List<Cas2AssessmentEntity> {
    val allAssessments = cas2AssessmentRepository.findAll()
    assertThat(allAssessments.size).isEqualTo(NO_OF_CAS_2_V2_APPLICATIONS_TO_MIGRATE)
    return allAssessments
  }

  private fun assertExpectedNumberOfCas2v2StatusUpdateDetailsWereMigrated(): List<Cas2StatusUpdateDetailEntity> {
    val allStatusUpdateDetails = cas2StatusUpdateDetailRepository.findAll()
    assertThat(allStatusUpdateDetails.size).isEqualTo(NO_OF_CAS_2_V2_APPLICATIONS_TO_MIGRATE)
    return allStatusUpdateDetails
  }

  private fun assertExpectedNumberOfCas2UsersWereMigrated(): List<Cas2UserEntity> {
    val migratedUsers = cas2UserRepository.findAll()
    assertThat(migratedUsers.size).isEqualTo(TOTAL_NO_OF_USERS_TO_MIGRATE)
    return migratedUsers
  }

  private fun assertExpectedNumberOfNomisUsersWereMigrated(): List<NomisUserEntity> {
    val migratedUsers = nomisUserRepository.findAll()
    assertThat(migratedUsers.size).isEqualTo(NO_OF_NOMIS_USERS_TO_MIGRATE + 1)
    return migratedUsers
  }

  private fun assertExpectedNumberOfExternalUsersWereMigrated(): List<ExternalUserEntity> {
    val migratedUsers = externalUserRepository.findAll()
    assertThat(migratedUsers.size).isEqualTo(NO_OF_EXTERNAL_USERS_TO_MIGRATE + 1)
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
        assertThat(cas2Application.createdByUser!!.id).isEqualTo(cas2Application.createdByUser.id)
        assertThat(cas2Application.serviceOrigin).isEqualTo(Cas2ServiceOrigin.HDC)
      }
    }
  }

  private fun assertThatAllStatusUpdatesDataWasMigratedOrUpdatedSuccessfully(allStatusUpdates: List<Cas2StatusUpdateEntity>) {
    allStatusUpdates.forEach { cas2StatusUpdate ->
      val cas2v2StatusUpdate = cas2v2StatusUpdates.firstOrNull { it.id == cas2StatusUpdate.id }
      if (cas2v2StatusUpdate != null) {
        assertThatCas2v2StatusUpdatesMatch(
          cas2StatusUpdateEntity = cas2StatusUpdate,
          cas2v2StatusUpdateEntity = cas2v2StatusUpdate,
        )
      } else {
        assertThat(cas2StatusUpdate.cas2UserAssessor!!.id).isEqualTo(cas2StatusUpdate.cas2UserAssessor!!.id)
      }
    }
  }

  private fun assertThatAllApplicationAssignmentsDataWasMigratedOrUpdatedSuccessfully(allApplicationAssignments: List<Cas2ApplicationAssignmentEntity>) {
    allApplicationAssignments.forEach { cas2ApplicationAssignment ->
      assertThat(cas2ApplicationAssignment.allocatedPomNomisUser?.id).isEqualTo(cas2ApplicationAssignment.allocatedPomUser?.id)
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

  private fun assertThatCas2v2StatusUpdateDetailsDataWasMigratedSuccessfully(allStatusUpdateDetails: List<Cas2StatusUpdateDetailEntity>) {
    allStatusUpdateDetails.forEach { cas2StatusUpdateDetail ->
      val cas2v2StatusUpdateDetail = cas2v2StatusUpdateDetails.firstOrNull { it.id == cas2StatusUpdateDetail.id }!!
      assertThatCas2v2StatusUpdateDetailsMatch(
        cas2StatusUpdateDetailEntity = cas2StatusUpdateDetail,
        cas2v2StatusUpdateDetailEntity = cas2v2StatusUpdateDetail,
      )
    }
  }

  private fun assertThatAllApplicationNotesDataWasMigratedOrUpdatedSuccessfully(allApplicationNotes: List<Cas2ApplicationNoteEntity>) {
    allApplicationNotes.forEach { cas2ApplicationNote ->
      val cas2v2ApplicationNote = cas2v2ApplicationNotes.firstOrNull { it.id == cas2ApplicationNote.id }

      if (cas2v2ApplicationNote != null) {
        assertThatCas2v2ApplicationNotesMatch(
          cas2ApplicationNoteEntity = cas2ApplicationNote,
          cas2v2ApplicationNoteEntity = cas2v2ApplicationNote,
        )
      } else {
        assertThat(cas2ApplicationNote.createdByCas2User!!.id).isEqualTo(cas2ApplicationNote.getUserId())
      }
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
    assertThat(cas2UserEntity.createdAt).isEqualTo(nomisUserEntity.createdAt)
    assertThat(cas2UserEntity.serviceOrigin).isEqualTo(Cas2ServiceOrigin.HDC)
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
    assertThat(cas2UserEntity.createdAt).isEqualTo(externalUserEntity.createdAt)
    assertThat(cas2UserEntity.serviceOrigin).isEqualTo(Cas2ServiceOrigin.HDC)
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
    assertThat(cas2UserEntity.createdAt).isEqualTo(cas2v2UserEntity.createdAt)
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
    assertThat(cas2UserEntity.serviceOrigin).isEqualTo(Cas2ServiceOrigin.BAIL)
  }

  private fun assertThatCas2v2ApplicationsMatch(cas2ApplicationEntity: Cas2ApplicationEntity, cas2v2ApplicationEntity: Cas2v2ApplicationEntity) {
    assertThat(cas2ApplicationEntity.id).isEqualTo(cas2v2ApplicationEntity.id)
    assertThat(cas2ApplicationEntity.createdByNomisUser!!.id.toString()).isEqualTo(FIXED_CREATED_BY_NOMIS_USER_ID_FOR_CAS2_V2_USER)
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
    assertThat(cas2ApplicationEntity.createdByUser!!.id).isEqualTo(cas2v2ApplicationEntity.createdByUser.id)
    assertThat(cas2ApplicationEntity.serviceOrigin).isEqualTo(Cas2ServiceOrigin.BAIL)
  }

  private fun assertThatCas2v2StatusUpdatesMatch(cas2StatusUpdateEntity: Cas2StatusUpdateEntity, cas2v2StatusUpdateEntity: Cas2v2StatusUpdateEntity) {
    assertThat(cas2StatusUpdateEntity.id).isEqualTo(cas2v2StatusUpdateEntity.id)
    assertThat(cas2StatusUpdateEntity.statusId).isEqualTo(cas2v2StatusUpdateEntity.statusId)
    assertThat(cas2StatusUpdateEntity.application.id).isEqualTo(cas2v2StatusUpdateEntity.application.id)
    assertThat(cas2StatusUpdateEntity.cas2UserAssessor?.id).isEqualTo(cas2v2StatusUpdateEntity.assessor.id)
    assertThat(cas2StatusUpdateEntity.assessor.id.toString()).isEqualTo(FIXED_CREATED_BY_EXTERNAL_USER_ID_FOR_CAS2_V2_USER)
    assertThat(cas2StatusUpdateEntity.description).isEqualTo(cas2v2StatusUpdateEntity.description)
    assertThat(cas2StatusUpdateEntity.label).isEqualTo(cas2v2StatusUpdateEntity.label)
    assertThat(cas2StatusUpdateEntity.createdAt).isEqualTo(cas2v2StatusUpdateEntity.createdAt)
    assertThat(cas2StatusUpdateEntity.assessment!!.id).isEqualTo(cas2v2StatusUpdateEntity.assessment!!.id)
  }

  private fun assertThatCas2v2AssessmentsMatch(cas2AssessmentEntity: Cas2AssessmentEntity, cas2v2AssessmentEntity: Cas2v2AssessmentEntity) {
    assertThat(cas2AssessmentEntity.id).isEqualTo(cas2v2AssessmentEntity.id)
    assertThat(cas2AssessmentEntity.application.id).isEqualTo(cas2v2AssessmentEntity.application.id)
    assertThat(cas2AssessmentEntity.nacroReferralId).isEqualTo(cas2v2AssessmentEntity.nacroReferralId)
    assertThat(cas2AssessmentEntity.assessorName).isEqualTo(cas2v2AssessmentEntity.assessorName)
    assertThat(cas2AssessmentEntity.createdAt).isEqualTo(cas2v2AssessmentEntity.createdAt)
    assertThat(cas2AssessmentEntity.serviceOrigin).isEqualTo(Cas2ServiceOrigin.BAIL)
  }

  private fun assertThatCas2v2StatusUpdateDetailsMatch(cas2StatusUpdateDetailEntity: Cas2StatusUpdateDetailEntity, cas2v2StatusUpdateDetailEntity: Cas2v2StatusUpdateDetailEntity) {
    assertThat(cas2StatusUpdateDetailEntity.id).isEqualTo(cas2v2StatusUpdateDetailEntity.id)
    assertThat(cas2StatusUpdateDetailEntity.createdAt).isEqualTo(cas2v2StatusUpdateDetailEntity.createdAt)
    assertThat(cas2StatusUpdateDetailEntity.statusDetailId).isEqualTo(cas2v2StatusUpdateDetailEntity.statusDetailId)
    assertThat(cas2StatusUpdateDetailEntity.statusUpdate.id).isEqualTo(cas2v2StatusUpdateDetailEntity.statusUpdate.id)
    assertThat(cas2StatusUpdateDetailEntity.label).isEqualTo(cas2v2StatusUpdateDetailEntity.label)
  }

  private fun assertThatCas2v2ApplicationNotesMatch(cas2ApplicationNoteEntity: Cas2ApplicationNoteEntity, cas2v2ApplicationNoteEntity: Cas2v2ApplicationNoteEntity) {
    assertThat(cas2ApplicationNoteEntity.id).isEqualTo(cas2v2ApplicationNoteEntity.id)
    assertThat(cas2ApplicationNoteEntity.application.id).isEqualTo(cas2v2ApplicationNoteEntity.application.id)
    assertThat(cas2ApplicationNoteEntity.createdByCas2User?.id).isEqualTo(cas2v2ApplicationNoteEntity.createdByUser.id)
    assertThat(cas2ApplicationNoteEntity.body).isEqualTo(cas2v2ApplicationNoteEntity.body)
    assertThat(cas2ApplicationNoteEntity.createdAt).isEqualTo(cas2v2ApplicationNoteEntity.createdAt)
    assertThat(cas2ApplicationNoteEntity.assessment?.id).isEqualTo(cas2v2ApplicationNoteEntity.assessment?.id)
  }

  private fun getUserType(user: Cas2v2UserEntity) = when (user.userType) {
    Cas2v2UserType.NOMIS -> Cas2UserType.NOMIS
    Cas2v2UserType.DELIUS -> Cas2UserType.DELIUS
    Cas2v2UserType.EXTERNAL -> Cas2UserType.EXTERNAL
  }
}
