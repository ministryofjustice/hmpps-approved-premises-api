package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AssessmentService(
  private val userRepository: UserRepository,
  private val assessmentRepository: AssessmentRepository,
  private val jsonSchemaService: JsonSchemaService
) {
  fun createAssessment(application: ApplicationEntity): AssessmentEntity {
    val requiredQualifications = getRequiredQualifications(application)

    // Might want to handle this more robustly in future if it emerges this is more common than initially thought
    val allocatedUser = getUserForAllocation(requiredQualifications)
      ?: throw RuntimeException("No Users with all of required qualifications (${requiredQualifications.joinToString(", ")}) could be found")

    val dateTimeNow = OffsetDateTime.now()

    return assessmentRepository.save(
      AssessmentEntity(
        id = UUID.randomUUID(), application = application,
        data = null, document = null, schemaVersion = jsonSchemaService.getNewestSchema(JsonSchemaType.ASSESSMENT),
        allocatedToUser = allocatedUser,
        allocatedAt = dateTimeNow,
        createdAt = dateTimeNow,
        submittedAt = null, schemaUpToDate = true
      )
    )
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
