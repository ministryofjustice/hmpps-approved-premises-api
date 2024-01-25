package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import org.springframework.core.io.DefaultResourceLoader
import org.springframework.stereotype.Component
import org.springframework.util.FileCopyUtils
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.SeedConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.JsonSchemaService
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
const val MINUTES_PER_DAY = 60 * 24

@Component
class ApprovedPremisesAutoScript(
  private val seedLogger: SeedLogger,
  private val seedConfig: SeedConfig,
  private val userRepository: UserRepository,
  private val applicationRepository: ApplicationRepository,
  private val jsonSchemaService: JsonSchemaService,
  private val apAreaRepository: ApAreaRepository,
) {

  fun script() {
    seedLogger.info("Auto-Scripting for Approved Premises")
    scriptApplications()
  }

  private fun scriptApplications() {
    seedLogger.info("Auto-Scripting Approved Premises applications")
    userRepository.findAll().forEach { user ->
      listOf("IN_PROGRESS", "SUBMITTED", "INFO_REQUIRED").forEach { state ->
        createApplicationFor(applicant = user, state = state)
      }
    }
  }

  private fun createApplicationFor(applicant: UserEntity, state: String) {
    seedLogger.info("Auto-scripting application for ${applicant.deliusUsername}, in state $state")
    val createdAt = randomDateTime()
    val submittedAt = if (state == "IN_PROGRESS") {
      null
    } else {
      createdAt.plusDays(
        randomInt(
          EARLIEST_SUBMISSION,
          LATEST_SUBMISSION,
        ).toLong(),
      )
    }
    val application = applicationRepository.save(
      ApprovedPremisesApplicationEntity(
        id = UUID.randomUUID(),
        crn = "X320741",
        nomsNumber = seedConfig.autoScript.noms,
        createdAt = createdAt,
        createdByUser = applicant,
        data = dataFor(state = state, crn = "A1234AI"),
        document = documentFor(state = state, crn = "A1234AI"),
        submittedAt = submittedAt,
        schemaVersion = jsonSchemaService.getNewestSchema(ApprovedPremisesApplicationJsonSchemaEntity::class.java),
        schemaUpToDate = true,
        apArea = apAreaRepository.findByName("North East"),
        arrivalDate = createdAt.plusDays(80),
        assessments = mutableListOf(),
        convictionId = 2500295345,
        offenceId = "M2500295343",
        eventNumber = "2",
        inmateInOutStatusOnSubmission = "IN",
        isEmergencyApplication = false,
        isEsapApplication = false,
        isInapplicable = false,
        isPipeApplication = false,
        isWithdrawn = false,
        isWomensApplication = false,
        name = "AADLAND BERTRAND",
        withdrawalReason = null,
        otherWithdrawalReason = null,
        placementRequests = mutableListOf(),
        releaseType = "licence",
        riskRatings = riskRatings(),
        sentenceType = "extendedDeterminate",
        situation = null,
        status = ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT,
        targetLocation = "LS2",
        teamCodes = mutableListOf(),
      ),
    )
  }

  private fun riskRatings(): PersonRisks {
    return PersonRisks(
      roshRisks = RiskWithStatus(
        status = RiskStatus.Retrieved,
        value = RoshRisks(
          overallRisk = "High",
          riskToChildren = "Medium",
          riskToPublic = "High",
          riskToKnownAdult = "High",
          riskToStaff = "Low",
          lastUpdated = LocalDate.parse("2022-11-02"),
        ),
      ),
      tier = RiskWithStatus(
        status = RiskStatus.Retrieved,
        value = RiskTier(
          level = "D2",
          lastUpdated = LocalDate.parse("2022-09-05"),
        ),
      ),
      flags = RiskWithStatus(
        status = RiskStatus.Retrieved,
        value = listOf("Risk to Known Adult"),
      ),
      mappa = RiskWithStatus(
        status = RiskStatus.Retrieved,
        value = Mappa(
          level = "CAT M2/LEVEL M2",
          lastUpdated = LocalDate.parse("2021-02-01"),
        ),
      ),
    )
  }

  private fun randomDateTime(minDays: Int = LATEST_CREATION, maxDays: Int = EARLIEST_CREATION): OffsetDateTime {
    return OffsetDateTime.now()
      .minusMinutes(randomInt(MINUTES_PER_DAY * minDays, MINUTES_PER_DAY * maxDays).toLong())
      .truncatedTo(ChronoUnit.SECONDS)
  }

  private fun randomInt(min: Int, max: Int) = Random.nextInt(min, max)

  private fun dataFor(state: String, crn: String): String {
    if (state != "NOT_STARTED") {
      return dataFixtureFor(crn)
    }
    return "{}"
  }

  private fun documentFor(state: String, crn: String): String {
    if (listOf("SUBMITTED", "INFO_REQUIRED").contains(state)) {
      return documentFixtureFor(crn)
    }
    return "{}"
  }

  private fun dataFixtureFor(crn: String): String {
    return loadFixtureAsResource("data_$crn.json")
  }

  private fun documentFixtureFor(crn: String): String {
    return loadFixtureAsResource("document_$crn.json")
  }

  private fun loadFixtureAsResource(filename: String): String {
    val path = "db/seed/local+dev+test/approved_premises_application_data/$filename"
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
