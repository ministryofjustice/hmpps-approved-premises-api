package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.migration

import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationAssignmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateDetailRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.ExternalUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.ExternalUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2StatusUpdateDetailRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationInBatchesJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import java.time.OffsetDateTime
import java.util.UUID

const val BATCH_SIZE = 100
const val FIXED_CREATED_BY_NOMIS_USER_ID_FOR_CAS2_V2_USER = "93c4a5f0-ce8f-4576-b920-c8e0f938b896"
const val FIXED_CREATED_BY_EXTERNAL_USER_ID_FOR_CAS2_V2_USER = "bce27384-95b3-4ff7-9478-3e0a9e8f7c3f"

@SuppressWarnings("TooManyFunctions")
@Component
class Cas2MergeMigrationJob(
  private val cas2UserRepository: Cas2UserRepository,
  private val cas2v2UserRepository: Cas2v2UserRepository,
  private val nomisUserRepository: NomisUserRepository,
  private val externalUserRepository: ExternalUserRepository,
  private val cas2ApplicationRepository: Cas2ApplicationRepository,
  private val cas2v2ApplicationRepository: Cas2v2ApplicationRepository,
  private val cas2ApplicationNoteRepository: Cas2ApplicationNoteRepository,
  private val cas2v2ApplicationNoteRepository: Cas2v2ApplicationNoteRepository,
  private val cas2AssessmentRepository: Cas2AssessmentRepository,
  private val cas2v2AssessmentRepository: Cas2v2AssessmentRepository,
  private val cas2StatusUpdateRepository: Cas2StatusUpdateRepository,
  private val cas2v2StatusUpdateRepository: Cas2v2StatusUpdateRepository,
  private val cas2StatusUpdateDetailRepository: Cas2StatusUpdateDetailRepository,
  private val cas2ApplicationAssignmentRepository: Cas2ApplicationAssignmentRepository,
  private val cas2v2StatusUpdateDetailRepository: Cas2v2StatusUpdateDetailRepository,
  private val migrationLogger: MigrationLogger,
  transactionTemplate: TransactionTemplate,
) : MigrationInBatchesJob(migrationLogger, transactionTemplate) {
  override val shouldRunInTransaction: Boolean = java.lang.Boolean.FALSE

  override fun process(pageSize: Int) {
    migrationLogger.info("Add dummy data to nomis_users and external_users tables")
    nomisUserRepository.save(generateDummyCas2v2NomisUser())
    externalUserRepository.save(generateDummyCas2v2ExternalUser())
    migrationLogger.info("Starting cas2 merge migration process...")
    migrateCas2Users()
    migrateAndUpdateCas2Applications()
    migrateCas2Assessments()
    migrateAndUpdateCas2ApplicationNotes()
    migrateAndUpdateCas2StatusUpdates()
    migrateCas2StatusUpdateDetails()
    updateCas2ApplicationAssignments()
    migrationLogger.info("Finished cas2 merge migration process...")
  }

  private fun migrateCas2Users() {
    migrationLogger.info("Starting cas2 user migration process...")
    val nomisIds = nomisUserRepository.findNomisUserIds().filter { it.toString() != FIXED_CREATED_BY_NOMIS_USER_ID_FOR_CAS2_V2_USER }
    migrationLogger.info("Nomis users to migrate: ${nomisIds.size}.")
    super.processInBatches(nomisIds, batchSize = BATCH_SIZE) { batchIds ->
      migrationLogger.info("Migrate with batch size of ${batchIds.size} in process")
      cas2UserRepository.saveAllAndFlush(generateNomisUser(batchIds))
    }
    val externalIds = externalUserRepository.findExternalUserIds().filter { it.toString() != FIXED_CREATED_BY_EXTERNAL_USER_ID_FOR_CAS2_V2_USER }
    migrationLogger.info("External users to migrate: ${externalIds.size}.")
    super.processInBatches(externalIds, batchSize = BATCH_SIZE) { batchIds ->
      migrationLogger.info("Migrate with batch size of ${batchIds.size} in process")
      cas2UserRepository.saveAllAndFlush(generateExternalUser(batchIds))
    }
    val cas2Ids = cas2v2UserRepository.findCas2v2UserIds()
    migrationLogger.info("Cas2 users to migrate: ${cas2Ids.size}.")
    super.processInBatches(cas2Ids, batchSize = BATCH_SIZE) { batchIds ->
      migrationLogger.info("Migrate with batch size of ${batchIds.size} in process")
      cas2UserRepository.saveAllAndFlush(generateCas2v2User(batchIds))
    }
    migrationLogger.info("Finished cas2 user migration process...")
  }

  private fun migrateAndUpdateCas2Applications() {
    migrationLogger.info("Starting cas2 application migration process...")
    val cas2ApplicationIds = cas2ApplicationRepository.findApplicationIds()
    migrationLogger.info("Cas2 applications to update: ${cas2ApplicationIds.size}.")
    super.processInBatches(cas2ApplicationIds, batchSize = BATCH_SIZE) { batchIds ->
      migrationLogger.info("Update with batch size of ${batchIds.size} in process")
      cas2ApplicationRepository.saveAllAndFlush(generateCas2Application(batchIds))
    }
    val cas2v2ApplicationIds = cas2v2ApplicationRepository.findApplicationIds()
    migrationLogger.info("Cas2v2 applications to migrate: ${cas2v2ApplicationIds.size}.")
    super.processInBatches(cas2v2ApplicationIds, batchSize = BATCH_SIZE) { batchIds ->
      migrationLogger.info("Migrate with batch size of ${batchIds.size} in process")
      cas2ApplicationRepository.saveAllAndFlush(generateCas2v2Application(batchIds))
    }
    migrationLogger.info("Finished cas2 application migration process...")
  }

  private fun migrateAndUpdateCas2ApplicationNotes() {
    migrationLogger.info("Starting cas2 application note migration process...")
    val cas2EntityIds = cas2ApplicationNoteRepository.findApplicationNoteIds()
    migrationLogger.info("Cas2 applications notes to update: ${cas2EntityIds.size}.")
    super.processInBatches(cas2EntityIds, batchSize = BATCH_SIZE) { batchIds ->
      migrationLogger.info("Update with batch size of ${batchIds.size} in process")
      cas2ApplicationNoteRepository.saveAllAndFlush(generateCas2ApplicationNote(batchIds))
    }
    val cas2v2EntityIds = cas2v2ApplicationNoteRepository.findApplicationNoteIds()
    migrationLogger.info("Cas2v2 application notes to migrate: ${cas2v2EntityIds.size}.")
    super.processInBatches(cas2v2EntityIds, batchSize = BATCH_SIZE) { batchIds ->
      migrationLogger.info("Migrate with batch size of ${batchIds.size} in process")
      cas2ApplicationNoteRepository.saveAllAndFlush(generateCas2v2ApplicationNote(batchIds))
    }
    migrationLogger.info("Finished cas2 application note migration process...")
  }

  private fun migrateAndUpdateCas2StatusUpdates() {
    migrationLogger.info("Starting cas2 status update migration process...")
    val cas2EntityIds = cas2StatusUpdateRepository.findStatusUpdateIds()
    migrationLogger.info("Cas2 status update to update: ${cas2EntityIds.size}.")
    super.processInBatches(cas2EntityIds, batchSize = BATCH_SIZE) { batchIds ->
      migrationLogger.info("Update with batch size of ${batchIds.size} in process")
      cas2StatusUpdateRepository.saveAllAndFlush(generateCas2StatusUpdate(batchIds))
    }
    val cas2v2EntityIds = cas2v2StatusUpdateRepository.findStatusUpdateIds()
    migrationLogger.info("Cas2v2 status update to migrate: ${cas2v2EntityIds.size}.")
    super.processInBatches(cas2v2EntityIds, batchSize = BATCH_SIZE) { batchIds ->
      migrationLogger.info("Migrate with batch size of ${batchIds.size} in process")
      cas2StatusUpdateRepository.saveAllAndFlush(generateCas2v2StatusUpdate(batchIds))
    }
    migrationLogger.info("Finished cas2 statusUpdate note migration process...")
  }

  private fun updateCas2ApplicationAssignments() {
    migrationLogger.info("Starting cas2 application assignment migration process...")
    val cas2EntityIds = cas2ApplicationAssignmentRepository.findApplicationAssignmentIds()
    migrationLogger.info("Cas2 application assignment to update: ${cas2EntityIds.size}.")
    super.processInBatches(cas2EntityIds, batchSize = BATCH_SIZE) { batchIds ->
      migrationLogger.info("Update with batch size of ${batchIds.size} in process")
      cas2ApplicationAssignmentRepository.saveAllAndFlush(generateCas2ApplicationAssignment(batchIds))
    }
    migrationLogger.info("Finished cas2 applicationAssignment note migration process...")
  }

  private fun migrateCas2Assessments() {
    migrationLogger.info("Starting cas2 assessment migration process...")
    val entityIds = cas2v2AssessmentRepository.findAssessmentIds()
    migrationLogger.info("Cas2 assessments to migrate: ${entityIds.size}.")
    super.processInBatches(entityIds, batchSize = BATCH_SIZE) { batchIds ->
      migrationLogger.info("Migrate with batch size of ${batchIds.size} in process")
      cas2AssessmentRepository.saveAllAndFlush(generateCas2v2Assessment(batchIds))
    }
    migrationLogger.info("Finished cas2 assessment migration process...")
  }

  private fun migrateCas2StatusUpdateDetails() {
    migrationLogger.info("Starting cas2 statusUpdateDetail migration process...")
    val entityIds = cas2v2StatusUpdateDetailRepository.findStatusUpdateDetailIds()
    migrationLogger.info("Cas2 statusUpdateDetails to migrate: ${entityIds.size}.")
    super.processInBatches(entityIds, batchSize = BATCH_SIZE) { batchIds ->
      migrationLogger.info("Migrate with batch size of ${batchIds.size} in process")
      cas2StatusUpdateDetailRepository.saveAllAndFlush(generateCas2v2StatusUpdateDetail(batchIds))
    }
    migrationLogger.info("Finished cas2 statusUpdateDetail migration process...")
  }

  private fun generateCas2StatusUpdate(statusUpdateIds: List<UUID>) = cas2StatusUpdateRepository.findAllById(statusUpdateIds).map {
    Cas2StatusUpdateEntity(
      id = it.id,
      statusId = it.statusId,
      application = it.application,
      assessor = it.assessor,
      description = it.description,
      label = it.label,
      createdAt = it.createdAt,
      assessment = it.assessment,
      cas2UserAssessor = cas2UserRepository.findById(it.assessor.id).get(),
    )
  }

  private fun generateCas2ApplicationAssignment(applicationAssignmentIds: List<UUID>) = cas2ApplicationAssignmentRepository.findAllById(applicationAssignmentIds).map {
    Cas2ApplicationAssignmentEntity(
      id = it.id,
      application = it.application,
      prisonCode = it.prisonCode,
      createdAt = it.createdAt,
      allocatedPomUser = it.allocatedPomUser,
      allocatedPomCas2User = if (it.allocatedPomUser != null) {
        cas2UserRepository.findById(it.allocatedPomUser!!.id).get()
      } else {
        null
      },
    )
  }

  private fun generateCas2v2StatusUpdate(statusUpdateIds: List<UUID>) = cas2v2StatusUpdateRepository.findAllById(statusUpdateIds).map {
    val application = cas2ApplicationRepository.findById(it.application.id).get()
    Cas2StatusUpdateEntity(
      id = it.id,
      statusId = it.statusId,
      application = application,
      assessor = externalUserRepository.findById(UUID.fromString(FIXED_CREATED_BY_EXTERNAL_USER_ID_FOR_CAS2_V2_USER)).get(),
      description = it.description,
      label = it.label,
      createdAt = it.createdAt,
      assessment = application.assessment,
      cas2UserAssessor = cas2UserRepository.findById(it.assessor.id).get(),
    )
  }

  private fun generateCas2Application(applicationIds: List<UUID>) = cas2ApplicationRepository.findAllById(applicationIds).map {
    Cas2ApplicationEntity(
      id = it.id,
      crn = it.crn,
      createdByUser = it.createdByUser,
      createdByCas2User = cas2UserRepository.findById(it.createdByUser.id).get(),
      data = it.data,
      document = it.document,
      createdAt = it.createdAt,
      submittedAt = it.submittedAt,
      abandonedAt = it.abandonedAt,
      statusUpdates = it.statusUpdates,
      nomsNumber = it.nomsNumber,
      telephoneNumber = it.telephoneNumber,
      notes = it.notes,
      assessment = it.assessment,
      applicationAssignments = it.applicationAssignments,
      referringPrisonCode = it.referringPrisonCode,
      hdcEligibilityDate = it.hdcEligibilityDate,
      conditionalReleaseDate = it.conditionalReleaseDate,
      preferredAreas = it.preferredAreas,
      applicationOrigin = it.applicationOrigin,
      bailHearingDate = it.bailHearingDate,
      serviceOrigin = Cas2ServiceOrigin.HDC,
    )
  }

  private fun generateCas2v2StatusUpdateDetail(statusUpdateDetailIds: List<UUID>) = cas2v2StatusUpdateDetailRepository.findAllById(statusUpdateDetailIds).map {
    Cas2StatusUpdateDetailEntity(
      id = it.id,
      createdAt = it.createdAt!!,
      statusDetailId = it.statusDetailId,
      label = it.label,
      statusUpdate = cas2StatusUpdateRepository.findById(it.statusUpdate.id).get(),
    )
  }

  private fun generateCas2v2Assessment(assessmentIds: List<UUID>) = cas2v2AssessmentRepository.findAllById(assessmentIds).map {
    Cas2AssessmentEntity(
      id = it.id,
      createdAt = it.createdAt,
      application = cas2ApplicationRepository.findById(it.application.id).get(),
      nacroReferralId = it.nacroReferralId,
      assessorName = it.assessorName,
      serviceOrigin = Cas2ServiceOrigin.BAIL,
    )
  }

  private fun generateCas2v2Application(applicationIds: List<UUID>) = cas2v2ApplicationRepository.findAllById(applicationIds).map {
    Cas2ApplicationEntity(
      id = it.id,
      crn = it.crn,
      createdByUser = nomisUserRepository.findById(UUID.fromString(FIXED_CREATED_BY_NOMIS_USER_ID_FOR_CAS2_V2_USER)).get(),
      createdByCas2User = cas2UserRepository.findById(it.createdByUser.id).get(),
      data = it.data,
      document = it.document,
      createdAt = it.createdAt,
      submittedAt = it.submittedAt,
      abandonedAt = it.abandonedAt,
      nomsNumber = it.nomsNumber,
      telephoneNumber = it.telephoneNumber,
      referringPrisonCode = it.referringPrisonCode,
      hdcEligibilityDate = it.hdcEligibilityDate,
      conditionalReleaseDate = it.conditionalReleaseDate,
      preferredAreas = it.preferredAreas,
      applicationOrigin = it.applicationOrigin,
      bailHearingDate = it.bailHearingDate,
      serviceOrigin = Cas2ServiceOrigin.BAIL,
    )
  }

  private fun generateCas2v2ApplicationNote(applicationNoteIds: List<UUID>) = cas2v2ApplicationNoteRepository.findAllById(applicationNoteIds).map {
    val nomisUser = nomisUserRepository.findById(UUID.fromString(FIXED_CREATED_BY_NOMIS_USER_ID_FOR_CAS2_V2_USER)).get()

    val note = Cas2ApplicationNoteEntity(
      id = it.id,
      application = cas2ApplicationRepository.findById(it.application.id).get(),
      createdByUser = nomisUser,
      createdByCas2User = cas2UserRepository.findById(it.createdByUser.id).get(),
      body = it.body,
      createdAt = it.createdAt,
      assessment = if (it.assessment != null) {
        cas2AssessmentRepository.findById(it.assessment!!.id).get()
      } else {
        null
      },
    )
    note
  }

  private fun generateCas2ApplicationNote(applicationNoteIds: List<UUID>) = cas2ApplicationNoteRepository.findAllById(applicationNoteIds).map {
    Cas2ApplicationNoteEntity(
      id = it.id,
      application = it.application,
      createdByUser = it.getUser(),
      createdByCas2User = cas2UserRepository.findById(it.getUserId()).get(),
      body = it.body,
      createdAt = it.createdAt,
      assessment = it.assessment,
    )
  }

  private fun generateNomisUser(nomisIds: List<UUID>) = nomisUserRepository.findAllById(nomisIds).map {
    Cas2UserEntity(
      id = it.id,
      name = it.name,
      email = it.email,
      createdAt = it.createdAt,
      updatedAt = OffsetDateTime.now(),
      isActive = it.isActive,
      isEnabled = it.isEnabled,
      nomisStaffId = it.nomisStaffId,
      nomisAccountType = it.accountType,
      activeNomisCaseloadId = it.activeCaseloadId,
      deliusStaffCode = null,
      deliusTeamCodes = null,
      externalType = null,
      userType = Cas2UserType.NOMIS,
      username = it.nomisUsername,
      serviceOrigin = Cas2ServiceOrigin.HDC,
    )
  }

  private fun generateExternalUser(externalIds: List<UUID>) = externalUserRepository.findAllById(externalIds).map {
    Cas2UserEntity(
      id = it.id,
      name = it.name,
      email = it.email,
      createdAt = it.createdAt,
      updatedAt = OffsetDateTime.now(),
      isActive = true,
      isEnabled = it.isEnabled,
      nomisStaffId = null,
      nomisAccountType = null,
      activeNomisCaseloadId = null,
      deliusStaffCode = null,
      deliusTeamCodes = null,
      externalType = it.origin,
      userType = Cas2UserType.EXTERNAL,
      username = it.username,
      serviceOrigin = Cas2ServiceOrigin.HDC,
    )
  }

  private fun generateCas2v2User(cas2v2Ids: List<UUID>) = cas2v2UserRepository.findAllById(cas2v2Ids).map {
    val userType = getUserType(it)
    Cas2UserEntity(
      id = it.id,
      name = it.name,
      email = it.email,
      createdAt = it.createdAt!!,
      updatedAt = OffsetDateTime.now(),
      isActive = it.isActive,
      isEnabled = it.isEnabled,
      nomisStaffId = it.nomisStaffId,
      nomisAccountType = if (userType == Cas2UserType.NOMIS) "GENERAL" else null,
      activeNomisCaseloadId = it.activeNomisCaseloadId,
      deliusStaffCode = it.deliusStaffCode,
      deliusTeamCodes = it.deliusTeamCodes,
      externalType = if (userType == Cas2UserType.EXTERNAL) "NACRO" else null,
      userType = getUserType(it),
      username = it.username,
      serviceOrigin = Cas2ServiceOrigin.BAIL,
    )
  }

  private fun generateDummyCas2v2NomisUser() = NomisUserEntity(
    id = UUID.fromString(FIXED_CREATED_BY_NOMIS_USER_ID_FOR_CAS2_V2_USER),
    name = "DUMMY_USER",
    email = "DUMMY_USER",
    createdAt = OffsetDateTime.now(),
    isActive = true,
    isEnabled = true,
    nomisStaffId = 0,
    accountType = "GENERAL",
    activeCaseloadId = null,
    nomisUsername = "DUMMY_USER_$FIXED_CREATED_BY_NOMIS_USER_ID_FOR_CAS2_V2_USER",
  )

  private fun generateDummyCas2v2ExternalUser() = ExternalUserEntity(
    id = UUID.fromString(FIXED_CREATED_BY_EXTERNAL_USER_ID_FOR_CAS2_V2_USER),
    name = "DUMMY_USER",
    email = "DUMMY_USER",
    createdAt = OffsetDateTime.now(),
    isEnabled = true,
    username = "DUMMY_USER_$FIXED_CREATED_BY_EXTERNAL_USER_ID_FOR_CAS2_V2_USER",
    origin = "NACRO",
  )

  private fun getUserType(user: Cas2v2UserEntity) = when (user.userType) {
    Cas2v2UserType.NOMIS -> Cas2UserType.NOMIS
    Cas2v2UserType.DELIUS -> Cas2UserType.DELIUS
    Cas2v2UserType.EXTERNAL -> Cas2UserType.EXTERNAL
  }
}
