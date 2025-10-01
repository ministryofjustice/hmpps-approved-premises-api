package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.seed

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.StatusUpdateService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import java.util.UUID

@Component
class Cas2UpdateAssessmentStatusSeedJob(
  private val assessmentRepository: Cas2AssessmentRepository,
  private val applicationRepository: Cas2ApplicationRepository,
  private val cas2UserRepository: Cas2UserRepository,
  private val cas2UpdateService: StatusUpdateService,
) : SeedJob<Cas2AssessmentUpdateStatusSeedRow>(
  requiredHeaders =
  setOf(
    "assessmentId",
    "applicationId",
    "assessorUsername",
    "newStatus",
    "newStatusDetails",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas2AssessmentUpdateStatusSeedRow(
    assessmentId = UUID.fromString(columns["assessmentId"]!!.trim()),
    applicationId = UUID.fromString(columns["applicationId"]!!.trim()),
    assessorUsername = columns["assessorUsername"]!!.trim(),
    newStatus = columns["newStatus"]!!.trim(),
    newStatusDetails = columns["newStatusDetails"]?.split("||") ?: emptyList(),
  )

  override fun processRow(row: Cas2AssessmentUpdateStatusSeedRow) {
    log.info(
      "Processing assessment cancellation for assessment ${row.assessmentId} " +
        "for application ${row.applicationId}",
    )

    val assessment =
      assessmentRepository.findByIdOrNull(row.assessmentId)
        ?: throw SeedException(
          "Assessment with id ${row.assessmentId} not found",
        )

    val application = applicationRepository.findByIdOrNull(row.applicationId)
      ?: throw SeedException(
        "Application with id ${row.applicationId} not found",
      )

    val assessor =
      cas2UserRepository.findByUsernameAndUserType(row.assessorUsername, Cas2UserType.EXTERNAL)
        ?: throw SeedException(
          "Assessor with username ${row.assessorUsername} not found",
        )

    if (assessment.application.id != application.id) {
      throw SeedException(
        "Application with id ${row.applicationId} not found on assessment ${row.assessmentId}",
      )
    }

    if (assessor.name != assessment.assessorName) {
      throw SeedException(
        "Assessor name ${assessor.name} does not match the assessor name on assesment ${row.assessmentId}",
      )
    }

    cas2UpdateService.createForAssessment(
      assessmentId = assessment.id,
      statusUpdate = Cas2AssessmentStatusUpdate(
        newStatus = row.newStatus,
        newStatusDetails = row.newStatusDetails,
      ),
      assessor = assessor,
    )
  }
}

data class Cas2AssessmentUpdateStatusSeedRow(
  val assessmentId: UUID,
  val applicationId: UUID,
  val assessorUsername: String,
  val newStatus: String,
  val newStatusDetails: List<String>,
)
