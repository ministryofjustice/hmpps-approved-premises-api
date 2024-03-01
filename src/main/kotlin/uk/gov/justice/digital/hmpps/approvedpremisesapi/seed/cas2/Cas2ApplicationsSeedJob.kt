package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2

import org.slf4j.LoggerFactory
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.util.FileCopyUtils
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatusFinder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.JsonSchemaService
import java.io.IOException
import java.io.InputStreamReader
import java.time.OffsetDateTime
import java.util.UUID

@SuppressWarnings("LongParameterList")
class Cas2ApplicationsSeedJob(
  fileName: String,
  private val repository: Cas2ApplicationRepository,
  private val userRepository: NomisUserRepository,
  private val externalUserRepository: ExternalUserRepository,
  private val statusUpdateRepository: Cas2StatusUpdateRepository,
  private val assessmentRepository: Cas2AssessmentRepository,
  private val jsonSchemaService: JsonSchemaService,
  private val statusFinder: Cas2PersistedApplicationStatusFinder,
) : SeedJob<Cas2ApplicationSeedCsvRow>(
  id = UUID.randomUUID(),
  fileName = fileName,
  requiredHeaders = setOf("id", "nomsNumber", "crn", "state", "createdBy", "createdAt", "submittedAt", "statusUpdates", "location"),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas2ApplicationSeedCsvRow(
    id = UUID.fromString(columns["id"]!!.trim()),
    nomsNumber = columns["nomsNumber"]!!.trim(),
    crn = columns["crn"]!!.trim(),
    state = columns["state"]!!.trim(),
    createdBy = columns["createdBy"]!!.trim(),
    createdAt = OffsetDateTime.parse(columns["createdAt"]),
    submittedAt = parseDateIfNotNull(emptyToNull(columns["submittedAt"])),
    statusUpdates = columns["statusUpdates"]!!.trim(),
    location = columns["location"]!!.trim(),
  )

  override fun processRow(row: Cas2ApplicationSeedCsvRow) {
    log.info("Setting up Application id ${row.id}")
    if (repository.findById(row.id).isPresent()) {
      return log.info("Skipping ${row.id}: already seeded")
    }

    val applicant = userRepository.findByNomisUsername(row.createdBy) ?: throw RuntimeException("Could not find applicant with nomisUsername ${row.createdBy}")

    try {
      createApplication(row, applicant)
    } catch (exception: Exception) {
      throw RuntimeException("Could not create application ${row.id}", exception)
    }
  }

  private fun createApplication(row: Cas2ApplicationSeedCsvRow, applicant: NomisUserEntity) {
    val application = repository.save(
      Cas2ApplicationEntity(
        id = row.id,
        crn = row.crn,
        nomsNumber = row.nomsNumber,
        createdAt = row.createdAt,
        createdByUser = applicant,
        data = dataFor(state = row.state, nomsNumber = row.nomsNumber),
        document = documentFor(state = row.state, nomsNumber = row.nomsNumber),
        submittedAt = row.submittedAt,
        schemaVersion = jsonSchemaService.getNewestSchema(Cas2ApplicationJsonSchemaEntity::class.java),
        schemaUpToDate = true,
      ),
    )
    if (row.statusUpdates != "0") {
      repeat(row.statusUpdates.toInt()) { idx -> createStatusUpdate(idx, application) }
    }
    if (row.submittedAt != null) {
      createAssessment(application)
    }
  }

  private fun createStatusUpdate(idx: Int, application: Cas2ApplicationEntity) {
    log.info("Seeding status update $idx for application ${application.id}")
    val assessor = externalUserRepository.findAll().random()
    val status = findStatusAtPosition(idx)
    statusUpdateRepository.save(
      Cas2StatusUpdateEntity(
        id = UUID.randomUUID(),
        application = application,
        assessor = assessor,
        description = status.description,
        label = status.label,
        statusId = status.id,
        createdAt = OffsetDateTime.now(),
      ),
    )
  }

  private fun createAssessment(application: Cas2ApplicationEntity) {
    val id = UUID.randomUUID()
    log.info("Seeding assessment $id for application ${application.id}")
    assessmentRepository.save(
      Cas2AssessmentEntity(
        id = id,
        createdAt = OffsetDateTime.now(),
        application = application,
      ),
    )
  }

  private fun findStatusAtPosition(idx: Int): Cas2PersistedApplicationStatus {
    return statusFinder.active()[idx]
  }

  private fun dataFor(state: String, nomsNumber: String): String {
    if (state != "NOT_STARTED") {
      return dataFixtureFor(nomsNumber)
    }
    return "{}"
  }

  private fun documentFor(state: String, nomsNumber: String): String {
    if (listOf("SUBMITTED", "IN_REVIEW").contains(state)) {
      return documentFixtureFor(nomsNumber)
    }
    return "{}"
  }

  private fun dataFixtureFor(nomsNumber: String): String {
    return loadFixtureAsResource("data_$nomsNumber.json")
  }

  private fun documentFixtureFor(nomsNumber: String): String {
    return loadFixtureAsResource("document_$nomsNumber.json")
  }

  private fun loadFixtureAsResource(filename: String): String {
    val path = "db/seed/local+dev+test/cas2_application_data/$filename"
    val loader = DefaultResourceLoader()
    return try {
      val resource = loader.getResource(path)
      val reader = InputStreamReader(resource.inputStream, "UTF-8")
      FileCopyUtils.copyToString(reader)
    } catch (e: IOException) {
      log.warn("FAILED to load seed fixture: " + e.message!!)
      "{}"
    }
  }

  private fun emptyToNull(value: String?) = value?.ifBlank { null }
  private fun parseDateIfNotNull(date: String?) = date?.let { OffsetDateTime.parse(it) }
}

data class Cas2ApplicationSeedCsvRow(
  val id: UUID,
  val nomsNumber: String,
  val crn: String,
  val state: String, // NOT_STARTED | IN-PROGRESS | SUBMITTED | IN_REVIEW
  val createdBy: String,
  val createdAt: OffsetDateTime,
  val submittedAt: OffsetDateTime?,
  val statusUpdates: String,
  val location: String,
)
