package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.seed

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import java.util.UUID

@Component
class Cas3AssignApplicationToPduSeedJob(
  private val assessmentRepository: AssessmentRepository,
  private val applicationRepository: ApplicationRepository,
  private val probationDeliveryUnitRepository: ProbationDeliveryUnitRepository,
) : SeedJob<Cas3AssignApplicationToPduSeedCsvRow>(
  requiredHeaders = setOf(
    "assessment_id",
    "pdu_name",
  ),
  runInTransaction = false,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas3AssignApplicationToPduSeedCsvRow(
    assessmentId = UUID.fromString(columns["assessment_id"]!!.trim()),
    pduName = columns["pdu_name"]!!.trim(),
  )

  override fun processRow(row: Cas3AssignApplicationToPduSeedCsvRow) {
    assignApplicationToPdu(row)
  }

  private fun assignApplicationToPdu(row: Cas3AssignApplicationToPduSeedCsvRow) {
    val assessment =
      assessmentRepository.findByIdOrNull(row.assessmentId) ?: error("Assessment with id ${row.assessmentId} not found")

    if (assessment is TemporaryAccommodationAssessmentEntity) {
      val application = applicationRepository.findByIdOrNull(assessment.application.id) ?: error("Application with id ${assessment.application.id} not found")

      if (application is TemporaryAccommodationApplicationEntity) {
        val pdu = probationDeliveryUnitRepository.findByName(row.pduName)
          ?: error("Probation Delivery Unit with name ${row.pduName} not found")

        application.probationDeliveryUnit = pdu
        applicationRepository.save(application)

        log.info("Application with id ${application.id} has been successfully assigned to pdu ${pdu.name}")
      } else {
        error("Application with id ${assessment.application.id} is not a temporary accommodation application")
      }
    } else {
      error("Assessment with id ${row.assessmentId} is not a temporary accommodation assessment")
    }
  }
}

data class Cas3AssignApplicationToPduSeedCsvRow(
  val assessmentId: UUID,
  val pduName: String,
)
