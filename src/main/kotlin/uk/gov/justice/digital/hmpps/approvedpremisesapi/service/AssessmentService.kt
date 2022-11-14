package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AssessmentService(
  private val userRepository: UserRepository,
  private val assessmentRepository: AssessmentRepository,
  private val assessmentClarificationNoteRepository: AssessmentClarificationNoteRepository,
  private val jsonSchemaService: JsonSchemaService
) {
  fun getVisibleAssessmentsForUser(user: UserEntity): List<AssessmentEntity> {
    // TODO: Potentially needs LAO enforcing too: https://trello.com/c/alNxpm9e/856-investigate-whether-assessors-will-have-access-to-limited-access-offenders

    val latestSchema = jsonSchemaService.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java)

    val assessments = if (user.hasRole(UserRole.WORKFLOW_MANAGER)) {
      assessmentRepository.findAll()
    } else {
      assessmentRepository.findAllByAllocatedToUser_Id(user.id)
    }

    assessments.forEach {
      it.schemaUpToDate = it.schemaVersion.id == latestSchema.id
    }

    return assessments
  }

  fun getAssessmentForUser(user: UserEntity, assessmentId: UUID): AuthorisableActionResult<AssessmentEntity> {
    // TODO: Potentially needs LAO enforcing too: https://trello.com/c/alNxpm9e/856-investigate-whether-assessors-will-have-access-to-limited-access-offenders

    val latestSchema = jsonSchemaService.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java)

    val assessment = assessmentRepository.findByIdOrNull(assessmentId)
      ?: return AuthorisableActionResult.NotFound()

    if (!user.hasRole(UserRole.WORKFLOW_MANAGER) && assessment.allocatedToUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    assessment.schemaUpToDate = assessment.schemaVersion.id == latestSchema.id

    return AuthorisableActionResult.Success(assessment)
  }

  fun createAssessment(application: ApplicationEntity): AssessmentEntity {
    val requiredQualifications = getRequiredQualifications(application)

    // Might want to handle this more robustly in future if it emerges this is more common than initially thought
    val allocatedUser = getUserForAllocation(requiredQualifications)
      ?: throw RuntimeException("No Users with all of required qualifications (${requiredQualifications.joinToString(", ")}) could be found")

    val dateTimeNow = OffsetDateTime.now()

    return assessmentRepository.save(
      AssessmentEntity(
        id = UUID.randomUUID(), application = application,
        data = null, document = null, schemaVersion = jsonSchemaService.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java),
        allocatedToUser = allocatedUser,
        allocatedAt = dateTimeNow,
        createdAt = dateTimeNow,
        submittedAt = null,
        decision = null,
        schemaUpToDate = true,
        clarificationNotes = mutableListOf()
      )
    )
  }

  fun addAssessmentClarificationNote(user: UserEntity, assessmentId: UUID, text: String): AuthorisableActionResult<AssessmentClarificationNoteEntity> {
    val assessmentResult = getAssessmentForUser(user, assessmentId)
    val assessment = when (assessmentResult) {
      is AuthorisableActionResult.Success -> assessmentResult.entity
      is AuthorisableActionResult.Unauthorised -> return AuthorisableActionResult.Unauthorised()
      is AuthorisableActionResult.NotFound -> return AuthorisableActionResult.NotFound()
    }

    val clarificationNoteEntity = assessmentClarificationNoteRepository.save(
      AssessmentClarificationNoteEntity(
        id = UUID.randomUUID(),
        assessment = assessment,
        createdByUser = user,
        createdAt = OffsetDateTime.now(),
        text = text
      )
    )

    return AuthorisableActionResult.Success(clarificationNoteEntity)
  }

  private fun getUserForAllocation(qualifications: List<UserQualification>): UserEntity? = userRepository.findQualifiedAssessorWithLeastPendingAllocations(qualifications.map(UserQualification::toString), qualifications.size.toLong())
  private fun getRequiredQualifications(application: ApplicationEntity): List<UserQualification> {
    val requiredQualifications = mutableListOf<UserQualification>()

    if (application.isPipeApplication == true) {
      requiredQualifications += UserQualification.PIPE
    }

    if (application.isWomensApplication == true) {
      requiredQualifications += UserQualification.WOMENS
    }

    return requiredQualifications
  }
}
