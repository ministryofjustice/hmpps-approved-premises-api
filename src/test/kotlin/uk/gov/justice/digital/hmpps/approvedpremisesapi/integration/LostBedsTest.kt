package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LostBedsTransformer
import java.time.LocalDate
import java.util.UUID

class LostBedsTest : IntegrationTestBase() {
  @Autowired
  lateinit var lostBedsTransformer: LostBedsTransformer

  @Test
  fun `List Lost Beds without JWT returns 401`() {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    webTestClient.get()
      .uri("/premises/${premises.id}/lost-beds")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `List Lost Beds on non existent Premises returns 404`() {
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/lost-beds")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `List Lost Beds returns OK with correct body`() {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    val lostBeds = lostBedsEntityFactory.produceAndPersist {
      withStartDate(LocalDate.now().plusDays(2))
      withEndDate(LocalDate.now().plusDays(4))
      withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
      withNumberOfBeds(5)
      withPremises(premises)
    }

    val expectedJson = objectMapper.writeValueAsString(listOf(lostBedsTransformer.transformJpaToApi(lostBeds)))

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/premises/${premises.id}/lost-beds")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Create Lost Beds without JWT returns 401`() {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    webTestClient.post()
      .uri("/premises/${premises.id}/lost-beds")
      .bodyValue(
        NewLostBed(
          startDate = LocalDate.parse("2022-08-15"),
          endDate = LocalDate.parse("2022-08-18"),
          numberOfBeds = 1,
          reason = UUID.randomUUID(),
          referenceNumber = "REF-123",
          notes = null
        )
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "MANAGER", "MATCHER" ])
  fun `Create Lost Beds on Approved Premises returns OK with correct body when user has one of roles MANAGER, MATCHER`(role: UserRole) {
    val username = "PROBATIONUSER"
    val user = userEntityFactory.produceAndPersist {
      withDeliusUsername(username)
    }

    userRoleAssignmentEntityFactory.produceAndPersist {
      withUser(user)
      withRole(role)
    }

    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withTotalBeds(3)
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    val reason = lostBedReasonEntityFactory.produceAndPersist()

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt(username)

    webTestClient.post()
      .uri("/premises/${premises.id}/lost-beds")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewLostBed(
          startDate = LocalDate.parse("2022-08-17"),
          endDate = LocalDate.parse("2022-08-18"),
          numberOfBeds = 3,
          reason = reason.id,
          referenceNumber = "REF-123",
          notes = "notes"
        )
      )
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath(".startDate").isEqualTo("2022-08-17")
      .jsonPath(".endDate").isEqualTo("2022-08-18")
      .jsonPath(".numberOfBeds").isEqualTo(3)
      .jsonPath(".reason.id").isEqualTo(reason.id.toString())
      .jsonPath(".reason.name").isEqualTo(reason.name)
      .jsonPath(".reason.isActive").isEqualTo(true)
      .jsonPath(".referenceNumber").isEqualTo("REF-123")
      .jsonPath(".notes").isEqualTo("notes")
  }
}
