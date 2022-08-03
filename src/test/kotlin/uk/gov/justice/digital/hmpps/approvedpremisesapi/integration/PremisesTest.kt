package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.LocalAuthorityArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApAreaTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.LocalAuthorityAreaTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.PremisesTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ProbationRegionTestRepository
import java.util.UUID

class PremisesTest : IntegrationTestBase() {
  @Autowired
  lateinit var objectMapper: ObjectMapper

  @Autowired
  lateinit var probationRegionRepository: ProbationRegionTestRepository

  @Autowired
  lateinit var apAreaRepository: ApAreaTestRepository

  @Autowired
  lateinit var localAuthorityAreaRepository: LocalAuthorityAreaTestRepository

  @Autowired
  lateinit var premisesRepository: PremisesTestRepository

  @Test
  fun `Get all Premises returns OK with correct body`() {
    val pbReg1 = probationRegionRepository.saveAndFlush(
      ProbationRegionEntity(id = UUID.fromString("d520e141-fb24-4f39-839b-666e69b29f17"), name = "PBREG1", identifier = "PBREG1ID", premises = mutableListOf())
    )
    val apArea1 = apAreaRepository.saveAndFlush(
      ApAreaEntity(id = UUID.fromString("73f5aa38-a44a-41ce-b50f-6bb7df51cab7"), name = "APAREA1", premises = mutableListOf())
    )
    val localAuthorityArea1 = localAuthorityAreaRepository.saveAndFlush(
      LocalAuthorityAreaEntity(id = UUID.fromString("311dc6ec-3925-4f42-aaca-13b6f3de96e0"), name = "ATAREA1", identifier = "ATAREA1ID", premises = mutableListOf())
    )
    premisesRepository.saveAndFlush(
      PremisesEntity(
        id = UUID.fromString("322e3614-2dc0-4166-8dec-63c016e20a0e"),
        name = "Premises One",
        apCode = "PREMONE",
        postcode = "ST8ST8",
        totalBeds = 1,
        probationRegion = pbReg1,
        apArea = apArea1,
        localAuthorityArea = localAuthorityArea1
      )
    )

    val pbReg2 = probationRegionRepository.saveAndFlush(
      ProbationRegionEntity(id = UUID.fromString("c1585959-91d7-4ee6-b0c1-30df25cd4983"), name = "PBREG2", identifier = "PBREG2ID", premises = mutableListOf())
    )
    val apArea2 = apAreaRepository.saveAndFlush(
      ApAreaEntity(id = UUID.fromString("1387e449-c8f7-486a-9f39-3d6cf0210ecd"), name = "APAREA2", premises = mutableListOf())
    )
    val localAuthorityArea2 = localAuthorityAreaRepository.saveAndFlush(
      LocalAuthorityAreaEntity(id = UUID.fromString("6758b2a9-36f6-4b15-aff9-0902e121f5ac"), name = "ATAREA2", identifier = "ATAREA2ID", premises = mutableListOf())
    )
    premisesRepository.saveAndFlush(
      PremisesEntity(
        id = UUID.fromString("f04743a7-d9f1-4818-8847-5c15f4874ec0"),
        name = "Premises Two",
        apCode = "PREMTWO",
        postcode = "ST8ST7",
        totalBeds = 2,
        probationRegion = pbReg2,
        apArea = apArea2,
        localAuthorityArea = localAuthorityArea2
      )
    )

    val expectedJson = objectMapper.writeValueAsString(
      listOf(
        Premises(
          id = UUID.fromString("322e3614-2dc0-4166-8dec-63c016e20a0e"),
          name = "Premises One",
          apCode = "PREMONE",
          postcode = "ST8ST8",
          bedCount = 1,
          probationRegion = ProbationRegion(id = UUID.fromString("d520e141-fb24-4f39-839b-666e69b29f17"), name = "PBREG1"),
          apArea = ApArea(id = UUID.fromString("73f5aa38-a44a-41ce-b50f-6bb7df51cab7"), name = "APAREA1"),
          localAuthorityArea = LocalAuthorityArea(id = UUID.fromString("311dc6ec-3925-4f42-aaca-13b6f3de96e0"), name = "ATAREA1")
        ),
        Premises(
          id = UUID.fromString("f04743a7-d9f1-4818-8847-5c15f4874ec0"),
          name = "Premises Two",
          apCode = "PREMTWO",
          postcode = "ST8ST7",
          bedCount = 2,
          probationRegion = ProbationRegion(id = UUID.fromString("c1585959-91d7-4ee6-b0c1-30df25cd4983"), name = "PBREG2"),
          apArea = ApArea(id = UUID.fromString("1387e449-c8f7-486a-9f39-3d6cf0210ecd"), name = "APAREA2"),
          localAuthorityArea = LocalAuthorityArea(id = UUID.fromString("6758b2a9-36f6-4b15-aff9-0902e121f5ac"), name = "ATAREA2")
        )
      )
    )

    webTestClient.get()
      .uri("/premises")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }
}
