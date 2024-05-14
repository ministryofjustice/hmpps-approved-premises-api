package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.SeedConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatusFinder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2.Cas2AutoScript
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.insertHdcDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.StatusUpdateService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.LogEntry
import java.time.OffsetDateTime
import java.util.UUID

class Cas2AutoScriptTest {
  private val mockSeedLogger = mockk<SeedLogger>()
  private val logEntries = mutableListOf<LogEntry>()

  private val mockSeedConfig = mockk<SeedConfig>()

  private val mockNomisUserRepository = mockk<NomisUserRepository>()
  private val mockNomisUserEntity = mockk<NomisUserEntity>()

  private val mockApplicationRepository = mockk<Cas2ApplicationRepository>()
  private val mockApplicationEntity = mockk<Cas2ApplicationEntity>(relaxed = true)

  private val mockExternalUserRepository = mockk<ExternalUserRepository>()
  private val mockExternalUserEntity = mockk<ExternalUserEntity>()

  private val mockStatusUpdateRepository = mockk<Cas2StatusUpdateRepository>()
  private val mockStatusUpdateEntity = mockk<Cas2StatusUpdateEntity>()

  private val mockAssessmentRepository = mockk<Cas2AssessmentRepository>()
  private val mockAssessmentEntity = mockk<Cas2AssessmentEntity>()

  private val mockJsonSchemaService = mockk<JsonSchemaService>()
  private val mockJsonSchemaEntity = mockk<JsonSchemaEntity>()

  private val mockApplicationService = mockk<ApplicationService>()
  private val mockStatusUpdateService = mockk<StatusUpdateService>()
  private val statusFinder = Cas2PersistedApplicationStatusFinder()

  private val autoScript = Cas2AutoScript(
    mockSeedLogger,
    mockSeedConfig,
    mockNomisUserRepository,
    mockApplicationRepository,
    mockExternalUserRepository,
    mockStatusUpdateRepository,
    mockAssessmentRepository,
    mockJsonSchemaService,
    mockApplicationService,
    mockStatusUpdateService,
    statusFinder,
  )

  @BeforeEach
  fun setUp() {
    every { mockSeedLogger.info(any()) } answers {
      logEntries += LogEntry(it.invocation.args[0] as String, "info", null)
    }

    every { mockSeedConfig.autoScript.noms } answers { "NOMS123" }
    every { mockSeedConfig.autoScript.prisonCode } answers { "PRI" }
    every { mockNomisUserRepository.findAll() } answers { listOf(mockNomisUserEntity) }
    every { mockNomisUserEntity.nomisUsername } answers { "SMITHJ_GEN" }

    every { mockExternalUserRepository.findAll() } answers { listOf(mockExternalUserEntity) }
    every {
      mockJsonSchemaService.getNewestSchema(
        Cas2ApplicationJsonSchemaEntity::class.java,
      )
    } answers { mockJsonSchemaEntity }

    every { mockApplicationRepository.save(any()) } answers { mockApplicationEntity }
    every { mockApplicationRepository.saveAndFlush(any()) } answers { mockApplicationEntity }
    every { mockApplicationEntity.id } answers {
      UUID.fromString("6c8d4bbb-72e6-47fe-9cde-ca2eefc5274b")
    }
    every { mockApplicationEntity.submittedAt } answers { OffsetDateTime.parse("2022-09-21T12:45:00+01:00") }

    every { mockStatusUpdateRepository.save(any()) } answers { mockStatusUpdateEntity }
    every { mockStatusUpdateEntity.createdAt = (any()) } answers { mockStatusUpdateEntity }

    every { mockAssessmentRepository.save(any()) } answers { mockAssessmentEntity }

    every { mockApplicationService.createCas2ApplicationSubmittedEvent(any()) } answers { }
    every { mockStatusUpdateService.createStatusUpdatedDomainEvent(any()) } answers { }
    mockkStatic(::insertHdcDates)
  }

  @Test
  fun `creates 3 applications for each Nomis User`() {
    every { insertHdcDates(any<String>()) } returns "{}"
    autoScript.script()

    verify(exactly = 3) { mockApplicationRepository.save(any()) }
    verify(exactly = 3) { insertHdcDates(any()) }
  }

  @Test
  fun `uses the NOMS number and Prison Code supplied in AutoScriptConfig`() {
    autoScript.script()

    verify(exactly = 5) { mockSeedConfig.autoScript }
  }

  @Test
  fun `creates at least 1 status update`() {
    autoScript.script()

    verify(atLeast = 1) { mockStatusUpdateRepository.save(any()) }
  }

  @Test
  fun `creates at application-submitted domain event`() {
    autoScript.script()

    verify(atLeast = 1) { mockApplicationService.createCas2ApplicationSubmittedEvent(any()) }
  }

  @Test
  fun `creates at application-status-updated domain event`() {
    autoScript.script()

    verify(atLeast = 1) { mockStatusUpdateService.createStatusUpdatedDomainEvent(any()) }
  }

  @Test
  fun `creates at least 1 assessment for an application`() {
    autoScript.script()

    verify(atLeast = 1) { mockAssessmentRepository.save(any()) }
  }
}
