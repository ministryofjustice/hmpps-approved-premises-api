package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2v2

import org.slf4j.LoggerFactory
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.stereotype.Component
import org.springframework.util.FileCopyUtils
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2v2ApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatusFinder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.insertHdcDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2.Cas2v2ApplicationsTransformer
import java.io.IOException
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Component
@SuppressWarnings("LongParameterList")
class Cas2v2ApplicationsSeedJob(
  private val repository: Cas2v2ApplicationRepository,
  private val cas2v2UserRepository: Cas2v2UserRepository,
  private val statusUpdateRepository: Cas2v2StatusUpdateRepository,
  private val assessmentRepository: Cas2v2AssessmentRepository,
  private val jsonSchemaService: JsonSchemaService,
  private val statusFinder: Cas2PersistedApplicationStatusFinder,
  private val cas2v2ApplicationsTransformer: Cas2v2ApplicationsTransformer,
) : SeedJob<Cas2v2ApplicationSeedCsvRow>(
  requiredHeaders = setOf("id", "nomsNumber", "crn", "state", "createdBy", "createdAt", "applicationOrigin", "bailHearingDate", "submittedAt", "statusUpdates", "location"),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas2v2ApplicationSeedCsvRow(
    id = UUID.fromString(columns["id"]!!.trim()),
    nomsNumber = columns["nomsNumber"]!!.trim(),
    crn = columns["crn"]!!.trim(),
    state = columns["state"]!!.trim(),
    createdBy = columns["createdBy"]!!.trim(),
    createdAt = OffsetDateTime.parse(columns["createdAt"]),
    applicationOrigin = cas2v2ApplicationsTransformer.applicationOriginFromText(columns["applicationOrigin"]!!.trim()),
    bailHearingDate = OffsetDateTime.parse(columns["bailHearingDate"]),
    submittedAt = parseDateIfNotNull(emptyToNull(columns["submittedAt"])),
    statusUpdates = columns["statusUpdates"]!!.trim(),
    location = columns["location"]!!.trim(),
    referringPrisonCode = columns["referringPrisonCode"]!!.trim(),
  )

  @SuppressWarnings("TooGenericExceptionThrown", "TooGenericExceptionCaught")
  override fun processRow(row: Cas2v2ApplicationSeedCsvRow) {
    log.info("Setting up Application id ${row.id}")
    if (repository.findById(row.id).isPresent()) {
      return log.info("Skipping ${row.id}: already seeded")
    }

    val applicant = cas2v2UserRepository.findByUsername(row.createdBy) ?: throw RuntimeException("Could not find applicant with cas2v2user ${row.createdBy}")

    try {
      createApplication(row, applicant)
    } catch (exception: Exception) {
      throw RuntimeException("Could not create application ${row.id}", exception)
    }
  }

  private fun createApplication(row: Cas2v2ApplicationSeedCsvRow, cas2v2UserEntity: Cas2v2UserEntity) {
    val application = repository.save(
      Cas2v2ApplicationEntity(
        id = row.id,
        crn = row.crn,
        nomsNumber = row.nomsNumber,
        createdAt = row.createdAt,
        createdByUser = cas2v2UserEntity,
        applicationOrigin = row.applicationOrigin,
        bailHearingDate = row.bailHearingDate.toLocalDate(),
        data = dataFor(state = row.state, nomsNumber = row.nomsNumber),
        document = documentFor(state = row.state, nomsNumber = row.nomsNumber),
        submittedAt = row.submittedAt,
        schemaVersion = jsonSchemaService.getNewestSchema(Cas2v2ApplicationJsonSchemaEntity::class.java),
        schemaUpToDate = true,
      ),
    )
    if (row.submittedAt != null) {
      applyFirstClassFields(application, row)

      createAssessment(application)
    }
    if (row.statusUpdates != "0") {
      repeat(row.statusUpdates.toInt()) { idx -> createStatusUpdate(idx, application) }
    }
  }

  private fun applyFirstClassFields(application: Cas2v2ApplicationEntity, row: Cas2v2ApplicationSeedCsvRow) {
    repository.saveAndFlush(
      application.apply {
        referringPrisonCode = row.referringPrisonCode
        preferredAreas = "Happisburgh | Norfolk"
        hdcEligibilityDate = LocalDate.now()
        conditionalReleaseDate = LocalDate.now().plusMonths(2)
        telephoneNumber = "0800 123 456"
      },
    )
  }

  private fun createStatusUpdate(idx: Int, application: Cas2v2ApplicationEntity) {
    log.info("Seeding status update $idx for application ${application.id}")
    val assessor = cas2v2UserRepository.findAll().random()
    val status = findStatusAtPosition(idx)
    statusUpdateRepository.save(
      Cas2v2StatusUpdateEntity(
        id = UUID.randomUUID(),
        application = application,
        assessment = application.assessment,
        assessor = assessor,
        description = status.description,
        label = status.label,
        statusId = status.id,
        createdAt = OffsetDateTime.now(),
      ),
    )
  }

  private fun createAssessment(application: Cas2v2ApplicationEntity) {
    val id = UUID.randomUUID()
    log.info("Seeding assessment $id for application ${application.id}")
    val assessment = assessmentRepository.save(
      Cas2v2AssessmentEntity(
        id = id,
        createdAt = OffsetDateTime.now(),
        application = application,
      ),
    )
    application.assessment = assessment
  }

  private fun findStatusAtPosition(idx: Int): Cas2PersistedApplicationStatus = statusFinder.active()[idx]

  private fun dataFor(state: String, nomsNumber: String): String {
    if (state != "NOT_STARTED") {
      return insertHdcDates(dataFixtureFor(nomsNumber))
    }
    return "{}"
  }

  private fun documentFor(state: String, nomsNumber: String): String {
    if (listOf("SUBMITTED", "IN_REVIEW").contains(state)) {
      return documentFixtureFor(nomsNumber)
    }
    return "{}"
  }

  private fun dataFixtureFor(nomsNumber: String): String = loadFixtureAsResource("data_$nomsNumber.json")

  private fun documentFixtureFor(nomsNumber: String): String = loadFixtureAsResource("document_$nomsNumber.json")

  private fun loadFixtureAsResource(filename: String): String {
    val path = "db/seed/local+dev+test/cas2v2_application_data/$filename"
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

data class Cas2v2ApplicationSeedCsvRow(
  val id: UUID,
  val nomsNumber: String,
  val crn: String,
  val applicationOrigin: ApplicationOrigin,
  val bailHearingDate: OffsetDateTime,
  // NOT_STARTED | IN-PROGRESS | SUBMITTED | IN_REVIEW
  val state: String,
  val createdBy: String,
  val createdAt: OffsetDateTime,
  val submittedAt: OffsetDateTime?,
  val statusUpdates: String,
  val location: String,
  val referringPrisonCode: String?,
)
