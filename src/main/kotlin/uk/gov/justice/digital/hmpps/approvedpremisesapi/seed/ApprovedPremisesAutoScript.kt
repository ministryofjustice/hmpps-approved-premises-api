package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.stereotype.Component
import org.springframework.util.FileCopyUtils
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmitted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedSubmittedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Ldu
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Region
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Team
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.SeedConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.JsonSchemaService
import java.io.IOException
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
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
  private val domainEventService: DomainEventService,
  private val apAreaRepository: ApAreaRepository,
  private val assessmentRepository: AssessmentRepository,
  private val assessmentClarificationNoteRepository: AssessmentClarificationNoteRepository,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: String,
) {

  fun script() {
    seedLogger.info("Auto-Scripting for Approved Premises")
    scriptApplications()
  }

  private fun scriptApplications() {
    seedLogger.info("Auto-Scripting Approved Premises applications")
    userRepository
      .findAll()
      .filter { listOf("BERNARD.BEAKS", "JIMSNOWLDAP").contains(it.deliusUsername) }
      .forEach { user ->
        listOf("IN_PROGRESS", "SUBMITTED", "INFO_REQUIRED").forEach { state ->
          createApplicationFor(applicant = user, state = state)
        }
      }
  }

  private fun createApplicationFor(applicant: UserEntity, state: String) {
    seedLogger.info("Auto-scripting Approved Premises application for ${applicant.deliusUsername}, in state $state")
    val createdAt = randomDateTime()

    val application = applicationRepository.save(
      ApprovedPremisesApplicationEntity(
        id = UUID.randomUUID(),
        crn = "X320741",
        nomsNumber = seedConfig.autoScript.noms,
        createdAt = createdAt,
        createdByUser = applicant,
        data = applicationDataFor(state = state, crn = "X320741"),
        document = applicationDocumentFor(state = state, crn = "X320741"),
        submittedAt = null,
        schemaVersion = jsonSchemaService.getNewestSchema(ApprovedPremisesApplicationJsonSchemaEntity::class.java),
        schemaUpToDate = true,
        apArea = null,
        arrivalDate = createdAt.plusDays(80),
        assessments = mutableListOf(),
        convictionId = 2500295345,
        offenceId = "M2500295343",
        eventNumber = "2",
        inmateInOutStatusOnSubmission = null,
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
        sentenceType = null,
        situation = null,
        status = ApprovedPremisesApplicationStatus.STARTED,
        targetLocation = null,
        teamCodes = mutableListOf(),
      ),
    )

    if (listOf("SUBMITTED", "INFO_REQUIRED").contains(state)) {
      val submittedApplication = submitApplication(application)
      val assessment = createApprovedPremisesAssessment(submittedApplication)

      if (state == "INFO_REQUIRED") {
        requireInfo(assessment)
      }
    }
  }

  private fun requireInfo(assessment: AssessmentEntity) {
    seedLogger.info("Auto-scripting clarification note for AP assessment ${assessment.id}")

    assessmentClarificationNoteRepository.save(
      AssessmentClarificationNoteEntity(
        id = UUID.randomUUID(),
        assessment = assessment,
        createdByUser = assessment.allocatedToUser!!,
        createdAt = assessment.createdAt.plusDays(randomInt(0, 7).toLong()),
        query = "Please provide more details",
        response = null,
        responseReceivedOn = null,
      ),
    )
  }

  private fun submitApplication(
    application: ApprovedPremisesApplicationEntity,
  ):
    ApprovedPremisesApplicationEntity {
    seedLogger.info("Auto-scripting submission of AP application ${application.id}")
    val appWithPromotedProperties = applyFirstClassProperties(application)
    createApplicationSubmittedEvent(appWithPromotedProperties)

    return appWithPromotedProperties
  }

  private fun createApplicationSubmittedEvent(application: ApprovedPremisesApplicationEntity) {
    val eventId = UUID.randomUUID()
    seedLogger.info("Auto-scripting AP application.submitted domain event for ${application.id}")
    domainEventService.saveApplicationSubmittedDomainEvent(
      DomainEvent(
        id = eventId,
        applicationId = application.id,
        crn = application.crn,
        occurredAt = application.submittedAt!!.toInstant(),
        data = ApplicationSubmittedEnvelope(
          id = eventId,
          timestamp = application.submittedAt!!.toInstant(),
          eventType = "approved-premises.application.submitted",
          eventDetails = applicationSubmittedDomainEventDetails(application),
        ),
      ),
    )
  }

  private fun applicationSubmittedDomainEventDetails(application: ApprovedPremisesApplicationEntity): ApplicationSubmitted {
    return ApplicationSubmitted(
      applicationId = application.id,
      applicationUrl = applicationUrlTemplate
        .replace("#id", application.id.toString()),
      personReference = PersonReference(
        crn = application.crn,
        noms = application.nomsNumber ?: "Unknown NOMS Number",
      ),
      deliusEventNumber = application.eventNumber,
      mappa = "CAT M2/LEVEL M2",
      offenceId = application.offenceId,
      releaseType = application.releaseType.toString(),
      age = 37,
      gender = ApplicationSubmitted.Gender.male,
      targetLocation = "LS2",
      submittedAt = application.submittedAt!!.toInstant(),
      submittedBy = staffDetails(),
      sentenceLengthInMonths = null,
    )
  }

  private fun staffDetails(): ApplicationSubmittedSubmittedBy {
    return ApplicationSubmittedSubmittedBy(
      staffMember = StaffMember(
        staffCode = "SH00007",
        staffIdentifier = 17,
        forenames = "JIM",
        surname = "SNOW",
        username = "JimSnowLdap",
      ),
      probationArea = ProbationArea(
        code = "GCS",
        name = "Gloucestershire",
      ),
      team = Team(
        code = "N07UAT",
        name = "Unallocated Team(N07)",
      ),
      ldu = Ldu(
        code = "N07UAT",
        name = "Unallocated Level 3(N07)",
      ),
      region = Region(
        code = "GCS",
        name = "Gloucestershire",
      ),
    )
  }

  private fun createApprovedPremisesAssessment(
    application: ApprovedPremisesApplicationEntity,
  ): AssessmentEntity {
    seedLogger.info("Auto-scripting assessment for AP application ${application.id}")

    val assessor = application.createdByUser
    return assessmentRepository.saveAndFlush(
      ApprovedPremisesAssessmentEntity(
        id = UUID.randomUUID(),
        application = application,
        data = null,
        document = null,
        schemaVersion = jsonSchemaService.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java),
        allocatedToUser = assessor,
        allocatedAt = application.submittedAt,
        reallocatedAt = null,
        createdAt = application.submittedAt!!.toInstant().atOffset(ZoneOffset.ofHoursMinutes(0, 0)),
        submittedAt = null,
        decision = null,
        schemaUpToDate = true,
        rejectionRationale = null,
        clarificationNotes = mutableListOf(),
        referralHistoryNotes = mutableListOf(),
        isWithdrawn = false,
      ),
    )
  }
  private fun applyFirstClassProperties(application: ApprovedPremisesApplicationEntity): ApprovedPremisesApplicationEntity {
    val submittedAt = application.createdAt.plusDays(
      randomInt(
        EARLIEST_SUBMISSION,
        LATEST_SUBMISSION,
      ).toLong(),
    )

    return applicationRepository.saveAndFlush(
      application.apply {
        this.submittedAt = submittedAt
        targetLocation = "LS2"
        sentenceType = "extendedDeterminate"
        situation = null
        inmateInOutStatusOnSubmission = "IN"
        apArea = apAreaRepository.findByName("North East")
        status = ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT
      },
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

  private fun applicationDataFor(state: String, crn: String): String {
    if (state != "NOT_STARTED") {
      return dataFixtureFor(questionnaire = "application", crn = crn)
    }
    return "{}"
  }

  private fun applicationDocumentFor(state: String, crn: String): String {
    if (listOf("SUBMITTED", "INFO_REQUIRED").contains(state)) {
      return documentFixtureFor(questionnaire = "application", crn = crn)
    }
    return "{}"
  }

  private fun dataFixtureFor(questionnaire: String, crn: String): String {
    return loadFixtureAsResource("${questionnaire}_data_$crn.json")
  }

  private fun documentFixtureFor(questionnaire: String, crn: String): String {
    return loadFixtureAsResource("${questionnaire}_document_$crn.json")
  }

  private fun loadFixtureAsResource(filename: String): String {
    val path = "db/seed/local+dev+test/approved_premises/$filename"
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
