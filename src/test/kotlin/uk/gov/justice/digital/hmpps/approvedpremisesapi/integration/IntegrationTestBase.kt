package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApAreaTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.LocalAuthorityAreaTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.PremisesTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ProbationRegionTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.JwtAuthHelper

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  private lateinit var flyway: Flyway

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @Autowired
  lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  lateinit var probationRegionRepository: ProbationRegionTestRepository

  @Autowired
  lateinit var apAreaRepository: ApAreaTestRepository

  @Autowired
  lateinit var localAuthorityAreaRepository: LocalAuthorityAreaTestRepository

  @Autowired
  lateinit var premisesRepository: PremisesTestRepository

  lateinit var probationRegionEntityFactory: ProbationRegionEntityFactory
  lateinit var apAreaEntityFactory: ApAreaEntityFactory
  lateinit var localAuthorityEntityFactory: LocalAuthorityEntityFactory
  lateinit var premisesEntityFactory: PremisesEntityFactory

  @BeforeEach
  fun beforeEach() {
    flyway.clean()
    flyway.migrate()
  }

  @BeforeEach
  fun setupFactories() {
    probationRegionEntityFactory = ProbationRegionEntityFactory(probationRegionRepository)
    apAreaEntityFactory = ApAreaEntityFactory(apAreaRepository)
    localAuthorityEntityFactory = LocalAuthorityEntityFactory(localAuthorityAreaRepository)
    premisesEntityFactory = PremisesEntityFactory(premisesRepository)
  }
}
