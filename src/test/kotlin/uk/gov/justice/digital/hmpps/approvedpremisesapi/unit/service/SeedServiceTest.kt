package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationContext
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.SeedConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SeedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.LogEntry
import java.util.function.Consumer

class SeedServiceTest {
  private val seedConfig = SeedConfig()
  private val mockApplicationContext = mockk<ApplicationContext>()
  private val mockTransactionTemplate = mockk<TransactionTemplate>()
  private val mockSeedLogger = mockk<SeedLogger>()
  private val logEntries = mutableListOf<LogEntry>()

  private val seedService = SeedService(seedConfig, mockApplicationContext, mockTransactionTemplate, mockSeedLogger)

  @BeforeEach
  fun setUp() {
    every { mockSeedLogger.info(any()) } answers {
      logEntries += LogEntry(it.invocation.args[0] as String, "info", null)
    }
    every { mockSeedLogger.warn(any()) } answers {
      logEntries += LogEntry(it.invocation.args[0] as String, "warn", null)
    }
    every { mockSeedLogger.error(any()) } answers {
      logEntries += LogEntry(it.invocation.args[0] as String, "error", null)
    }
    every { mockSeedLogger.error(any(), any()) } answers {
      logEntries += LogEntry(it.invocation.args[0] as String, "error", it.invocation.args[1] as Throwable)
    }
  }

  @Test
  fun `autoSeed does nothing if automatic seeding is not enabled`() {
    seedConfig.auto.enabled = false

    seedService.autoSeed()

    verify { listOf(mockApplicationContext, mockTransactionTemplate, mockSeedLogger) wasNot called }
  }

  @Test
  fun `autoSeed logs a warning if a file prefix corresponds to a location that does not exist`() {
    seedConfig.auto.enabled = true
    seedConfig.auto.filePrefixes = listOf("classpath:does/not/exist")

    seedService.autoSeed()

    assertThat(logEntries).anyMatch {
      it.level == "warn" &&
        it.message == "class path resource [does/not/exist/] cannot be resolved to URL because it does not exist"
    }
  }

  @Test
  fun `autoSeed logs a warning if it encounters a CSV file with an unknown seed job type`() {
    seedConfig.auto.enabled = true
    seedConfig.auto.filePrefixes = listOf("classpath:db/seed/unknown-job-type")

    seedService.autoSeed()

    assertThat(logEntries).anyMatch {
      it.level == "warn" &&
        it.message.contains("/db/seed/unknown-job-type/unknown_seed_file.csv does not have a known job type; skipping.")
    }
  }

  @Test
  fun `autoSeed runs the job for a CSV file with the name of the job type`() {
    seedConfig.auto.enabled = true
    seedConfig.auto.filePrefixes = listOf("classpath:db/seed/known-job-type")

    val mockPremisesRepository = mockk<PremisesRepository>()
    val mockProbationRegionRepository = mockk<ProbationRegionRepository>()
    val mockLocalAuthorityAreaRepository = mockk<LocalAuthorityAreaRepository>()
    val mockCharacteristicRepository = mockk<CharacteristicRepository>()

    every { mockApplicationContext.getBean(PremisesRepository::class.java) } returns mockPremisesRepository
    every { mockApplicationContext.getBean(ProbationRegionRepository::class.java) } returns mockProbationRegionRepository
    every { mockApplicationContext.getBean(LocalAuthorityAreaRepository::class.java) } returns mockLocalAuthorityAreaRepository
    every { mockApplicationContext.getBean(CharacteristicRepository::class.java) } returns mockCharacteristicRepository

    every { mockTransactionTemplate.executeWithoutResult(any()) } answers {
      (it.invocation.args[0] as Consumer<TransactionStatus?>).accept(null)
    }

    every { mockPremisesRepository.findByApCode(any(), ApprovedPremisesEntity::class.java) } returns null
    every { mockProbationRegionRepository.findByName(any()) } returns
      ProbationRegionEntityFactory()
        .withApArea(ApAreaEntityFactory().produce())
        .produce()
    every { mockLocalAuthorityAreaRepository.findByName(any()) } returns
      LocalAuthorityEntityFactory()
        .produce()
    every { mockCharacteristicRepository.findByPropertyNameAndScopes(any(), any(), any()) } returns
      CharacteristicEntityFactory()
        .produce()

    seedService.autoSeed()

    verify { mockPremisesRepository.save(any()) }
  }
}
