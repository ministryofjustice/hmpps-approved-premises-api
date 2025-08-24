package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.seed

import org.slf4j.LoggerFactory
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.stereotype.Component
import org.springframework.util.FileCopyUtils
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.reference.Cas2PersistedApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.reference.Cas2PersistedApplicationStatusFinder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service.Cas2v2ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service.Cas2v2StatusUpdateService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.transformCas2UserEntityToNomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.SeedConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.insertHdcDates
import java.io.IOException
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.random.Random

const val EARLIEST_CREATION = 45
const val LATEST_CREATION = 15
const val EARLIEST_SUBMISSION = 1
const val LATEST_SUBMISSION = 5
const val FEWEST_UPDATES = 1
const val MOST_UPDATES = 6
const val MINUTES_PER_DAY = 60 * 24

@Component
class Cas2v2StartupScript(
  private val seedLogger: SeedLogger,
  private val seedConfig: SeedConfig,
  private val cas2UserRepository: Cas2UserRepository,
  private val cas2applicationRepository: Cas2ApplicationRepository,
  private val cas2statusUpdateRepository: Cas2StatusUpdateRepository,
  private val cas2assessmentRepository: Cas2AssessmentRepository,
  private val cas2v2applicationService: Cas2v2ApplicationService,
  private val cas2v2statusUpdateService: Cas2v2StatusUpdateService,
  private val statusFinder: Cas2PersistedApplicationStatusFinder,
) {
  fun script() {
    seedLogger.info("Running Start up Script for CAS2v2")
    scriptApplications()
  }

  private val log = LoggerFactory.getLogger(this::class.java)

  private fun scriptApplications() {
    seedLogger.info("Auto-Scripting CAS2v2 applications")
    cas2UserRepository.findAll().forEach { user ->
      listOf("IN_PROGRESS", "SUBMITTED", "IN_REVIEW").forEach { state ->
        listOf(ApplicationOrigin.homeDetentionCurfew, ApplicationOrigin.prisonBail, ApplicationOrigin.courtBail).forEach { applicationOrigin ->
          createApplicationFor(cas2UserEntity = user, state = state, applicationOrigin = applicationOrigin)
        }
      }
    }
  }

  private fun createApplicationFor(cas2UserEntity: Cas2UserEntity, state: String, applicationOrigin: ApplicationOrigin) {
    seedLogger.info("Auto-scripting application for ${cas2UserEntity.username}, in state $state")
    val createdAt = randomDateTime()
    val submittedAt = if (state == "IN_PROGRESS") null else createdAt.plusDays(randomInt(EARLIEST_SUBMISSION, LATEST_SUBMISSION).toLong())
    val application = cas2applicationRepository.save(
      Cas2ApplicationEntity(
        id = UUID.randomUUID(),
        crn = "X320742",
        nomsNumber = seedConfig.onStartup.script.noms,
        createdAt = createdAt,
        createdByUser = transformCas2UserEntityToNomisUserEntity(cas2UserEntity),
        createdByCas2User = cas2UserEntity,
        data = dataFor(state = state, nomsNumber = "DO16821"),
        document = documentFor(state = state, nomsNumber = "DO16821"),
        submittedAt = submittedAt,
        applicationOrigin = applicationOrigin,
      ),
    )

    if (listOf("SUBMITTED", "IN_REVIEW").contains(state)) {
      val appWithPromotedProperties = applyFirstClassProperties(application)
      cas2v2applicationService.createCas2ApplicationSubmittedEvent(appWithPromotedProperties)
      createAssessment(application)
    }

    if (state == "IN_REVIEW") {
      val quantity = randomInt(FEWEST_UPDATES, MOST_UPDATES)
      seedLogger.info("Auto-scripting $quantity status updates for application ${application.id}")
      repeat(quantity) { idx -> createStatusUpdate(idx, application) }
    }
  }

  private fun applyFirstClassProperties(application: Cas2ApplicationEntity): Cas2ApplicationEntity = cas2applicationRepository.saveAndFlush(
    application.apply {
      referringPrisonCode = seedConfig.onStartup.script.prisonCode
      preferredAreas = "Happisburgh | Norfolk"
      hdcEligibilityDate = LocalDate.now()
      conditionalReleaseDate = LocalDate.now().plusMonths(2)
      telephoneNumber = "0800 123 456"
    },
  )

  private fun createStatusUpdate(idx: Int, application: Cas2ApplicationEntity) {
    seedLogger.info("Auto-scripting status update $idx for application ${application.id}")
    val assessor = cas2UserRepository.findAll().random()
    log.info(assessor.toString())
    val status = findStatusAtPosition(idx)
    val update = cas2statusUpdateRepository.save(
      Cas2StatusUpdateEntity(
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
    update.apply { this.createdAt = application.submittedAt!!.plusDays(idx + 1.toLong()) }
    cas2statusUpdateRepository.save(update)
    cas2v2statusUpdateService.createStatusUpdatedDomainEvent(update)
  }

  private fun findStatusAtPosition(idx: Int): Cas2PersistedApplicationStatus = statusFinder.active()[idx]

  private fun createAssessment(application: Cas2ApplicationEntity) {
    val id = UUID.randomUUID()
    seedLogger.info("Auto-scripting assessment $id for application ${application.id}")
    val assessment = cas2assessmentRepository.save(
      Cas2AssessmentEntity(
        id = id,
        createdAt = OffsetDateTime.now(),
        application = application,
      ),
    )
    application.assessment = assessment
  }

  private fun randomDateTime(minDays: Int = LATEST_CREATION, maxDays: Int = EARLIEST_CREATION): OffsetDateTime = OffsetDateTime.now()
    .minusMinutes(randomInt(MINUTES_PER_DAY * minDays, MINUTES_PER_DAY * maxDays).toLong())
    .truncatedTo(ChronoUnit.SECONDS)

  private fun randomInt(min: Int, max: Int) = Random.nextInt(min, max)

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
    val path = "db/seed/dev+test/cas2v2_application_data/$filename"
    val loader = DefaultResourceLoader()
    return try {
      val resource = loader.getResource(path)
      val reader = InputStreamReader(resource.inputStream, "UTF-8")
      FileCopyUtils.copyToString(reader)
    } catch (e: IOException) {
      seedLogger.warn("FAILED to load seed fixture: " + e.message!!)
      "{}"
    }
  }
}
