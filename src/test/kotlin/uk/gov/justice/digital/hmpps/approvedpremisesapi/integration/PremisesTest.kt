package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.LocalAuthorityArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApAreaTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.LocalAuthorityAreaTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.PremisesTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ProbationRegionTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.JwtAuthHelper
import java.time.Duration
import java.util.UUID

class PremisesTest : IntegrationTestBase() {
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

  private lateinit var probationRegionEntityFactory: ProbationRegionEntityFactory
  private lateinit var apAreaEntityFactory: ApAreaEntityFactory
  private lateinit var localAuthorityEntityFactory: LocalAuthorityEntityFactory
  private lateinit var premisesEntityFactory: PremisesEntityFactory

  @BeforeEach
  fun setupFactories() {
    probationRegionEntityFactory = ProbationRegionEntityFactory(probationRegionRepository)
    apAreaEntityFactory = ApAreaEntityFactory(apAreaRepository)
    localAuthorityEntityFactory = LocalAuthorityEntityFactory(localAuthorityAreaRepository)
    premisesEntityFactory = PremisesEntityFactory(premisesRepository)
  }

  @Test
  fun `Get all Premises returns OK with correct body`() {
    val premises = premisesEntityFactory
      .withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
      .withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      .withYieldedProbationRegion { probationRegionEntityFactory.produceAndPersist() }
      .produceAndPersistMultiple(10)

    val expectedJson = objectMapper.writeValueAsString(premises.map(::premisesEntityToExpectedApiResponse))

    val jwt = jwtAuthHelper.createJwt(
      subject = "some-api",
      expiryTime = Duration.ofMinutes(2)
    )

    webTestClient.get()
      .uri("/premises")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Premises by ID returns OK with correct body`() {
    val premises = premisesEntityFactory
      .withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
      .withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      .withYieldedProbationRegion { probationRegionEntityFactory.produceAndPersist() }
      .produceAndPersistMultiple(5)

    val premisesToGet = premises[2]
    val expectedJson = objectMapper.writeValueAsString(premisesEntityToExpectedApiResponse(premises[2]))

    val jwt = jwtAuthHelper.createJwt(
      subject = "some-api",
      expiryTime = Duration.ofMinutes(2)
    )

    webTestClient.get()
      .uri("/premises/${premisesToGet.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Premises by ID returns Not Found with correct body`() {
    val idToRequest = UUID.randomUUID().toString()

    val jwt = jwtAuthHelper.createJwt(
      subject = "some-api",
      expiryTime = Duration.ofMinutes(2)
    )

    webTestClient.get()
      .uri("/premises/$idToRequest")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectHeader().contentType("application/problem+json")
      .expectStatus()
      .isNotFound
      .expectBody()
      .jsonPath("title").isEqualTo("Not Found")
      .jsonPath("status").isEqualTo(404)
      .jsonPath("detail").isEqualTo("No Premises with an ID of $idToRequest could be found")
  }

  private fun premisesEntityToExpectedApiResponse(premises: PremisesEntity) = Premises(
    id = premises.id,
    name = premises.name,
    apCode = premises.apCode,
    postcode = premises.postcode,
    bedCount = premises.totalBeds,
    probationRegion = ProbationRegion(id = premises.probationRegion.id, identifier = premises.probationRegion.identifier, name = premises.probationRegion.name),
    apArea = ApArea(id = premises.apArea.id, name = premises.apArea.name),
    localAuthorityArea = LocalAuthorityArea(id = premises.localAuthorityArea.id, identifier = premises.localAuthorityArea.identifier, name = premises.localAuthorityArea.name)
  )
}
