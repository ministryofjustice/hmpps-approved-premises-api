package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.StaffMemberTransformer
import java.time.LocalDate
import java.util.UUID

class PremisesTest : IntegrationTestBase() {
  @Autowired
  lateinit var premisesTransformer: PremisesTransformer

  @Autowired
  lateinit var staffMemberTransformer: StaffMemberTransformer

  @Test
  fun `Create new premises returns 201`() {

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt("PROBATIONPERSON")

    webTestClient.post()
      .uri("/premises")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewPremises(
          addressLine1 = "1 somewhere",
          postcode = "AB123CD",
          service = "CAS3",
          localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f")
        )
      )
      .exchange()
      .expectStatus()
      .isCreated
  }

  @Test
  fun `When a new premises is created then all field data is persisted`() {

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt("PROBATIONPERSON")

    webTestClient.post()
      .uri("/premises")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewPremises(
          addressLine1 = "1 somewhere",
          postcode = "AB123CD",
          service = "CAS3",
          notes = "some arbitrary notes",
          name = "some arbitrary name",
          localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f")
        )
      )
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("addressLine1").isEqualTo("1 somewhere")
      .jsonPath("postcode").isEqualTo("AB123CD")
      .jsonPath("service").isEqualTo("CAS3")
      .jsonPath("notes").isEqualTo("some arbitrary notes")
      .jsonPath("name").isEqualTo("some arbitrary name")
      .jsonPath("localAuthorityArea.id").isEqualTo("a5f52443-6b55-498c-a697-7c6fad70cc3f")
  }

  @Test
  fun `When a new premises is created with no name then it defaults to unknown`() {

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt("PROBATIONPERSON")

    webTestClient.post()
      .uri("/premises")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewPremises(
          addressLine1 = "1 somewhere",
          postcode = "AB123CD",
          service = "CAS3",
          notes = "some arbitrary notes",
          localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f")
        )
      )
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("name").isEqualTo("Unknown")
  }

  @Test
  fun `When a new premises is created with no notes then it defaults to empty`() {

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt("PROBATIONPERSON")

    webTestClient.post()
      .uri("/premises")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewPremises(
          addressLine1 = "1 somewhere",
          postcode = "AB123CD",
          service = "CAS3",
          name = "some arbitrary name",
          localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f")
        )
      )
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("notes").isEqualTo("")
  }

  @Test
  fun `Trying to create a new premises without an address returns 400`() {

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt("PROBATIONPERSON")

    webTestClient.post()
      .uri("/premises?service=temporary-accommodation")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewPremises(
          name = "arbitrary_test_name",
          postcode = "AB123CD",
          addressLine1 = "",
          localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
          notes = "some notes",
          service = "CAS3"
        )
      )
      .exchange()
      .expectStatus()
      .is4xxClientError
      .expectBody()
      .jsonPath("title").isEqualTo("Bad Request")
      .jsonPath("invalid-params[0].errorType").isEqualTo("empty")
  }

  @Test
  fun `Trying to create a new premises without a postcode returns 400`() {

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt("PROBATIONPERSON")

    webTestClient.post()
      .uri("/premises?service=temporary-accommodation")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewPremises(
          name = "arbitrary_test_name",
          postcode = "",
          addressLine1 = "FIRST LINE OF THE ADDRESS",
          localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
          notes = "some notes",
          service = "CAS3"
        )
      )
      .exchange()
      .expectStatus()
      .is4xxClientError
      .expectBody()
      .jsonPath("title").isEqualTo("Bad Request")
      .jsonPath("invalid-params[0].errorType").isEqualTo("empty")
  }

  @Test
  fun `Trying to create a new premises without a service returns 400`() {

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt("PROBATIONPERSON")

    webTestClient.post()
      .uri("/premises?service=temporary-accommodation")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewPremises(
          name = "arbitrary_test_name",
          postcode = "AB123CD",
          addressLine1 = "FIRST LINE OF THE ADDRESS",
          localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
          notes = "some notes",
          service = ""
        )
      )
      .exchange()
      .expectStatus()
      .is4xxClientError
      .expectBody()
      .jsonPath("title").isEqualTo("Bad Request")
      .jsonPath("invalid-params[0].errorType").isEqualTo("empty")
  }

  @Test
  fun `Get all Premises returns OK with correct body`() {
    val cas1Premises = approvedPremisesEntityFactory.produceAndPersistMultiple(5) {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
      withService("CAS1")
      withTotalBeds(20)
    }

    val cas3Premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(5) {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
      withService("CAS3")
      withTotalBeds(20)
    }

    val premises = cas1Premises + cas3Premises

    val expectedJson = objectMapper.writeValueAsString(
      premises.map {
        premisesTransformer.transformJpaToApi(it, 20)
      }
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

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
  fun `Get Premises for CAS1 returns OK with correct body`() {
    val premises = approvedPremisesEntityFactory.produceAndPersistMultiple(5) {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
      withService("CAS1")
      withTotalBeds(20)
    }

    // Add some extra premises for the other service that shouldn't be returned
    temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(5) {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
      withService("CAS3")
      withTotalBeds(20)
    }

    val expectedJson = objectMapper.writeValueAsString(
      premises.map {
        premisesTransformer.transformJpaToApi(it, 20)
      }
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/premises")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", "approved-premises")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Premises for CAS3 returns OK with correct body`() {
    val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(5) {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
      withService("CAS3")
      withTotalBeds(20)
    }

    // Add some extra premises for the other service that shouldn't be returned
    approvedPremisesEntityFactory.produceAndPersistMultiple(5) {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
      withService("CAS1")
      withTotalBeds(20)
    }

    val expectedJson = objectMapper.writeValueAsString(
      premises.map {
        premisesTransformer.transformJpaToApi(it, 20)
      }
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/premises")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", "temporary-accommodation")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Premises by ID returns OK with correct body`() {
    val premises = approvedPremisesEntityFactory.produceAndPersistMultiple(5) {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
      withTotalBeds(20)
    }

    val premisesToGet = premises[2]
    val expectedJson = objectMapper.writeValueAsString(premisesTransformer.transformJpaToApi(premises[2], 20))

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

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
  fun `Get Premises by ID returns OK with correct body when capacity is used`() {
    val premises = approvedPremisesEntityFactory.produceAndPersistMultiple(5) {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
      withTotalBeds(20)
    }

    val keyWorker = StaffMemberFactory().produce()
    mockStaffMemberCommunityApiCall(keyWorker)

    bookingEntityFactory.produceAndPersist {
      withPremises(premises[2])
      withArrivalDate(LocalDate.now().minusDays(2))
      withDepartureDate(LocalDate.now().plusDays(4))
      withStaffKeyWorkerId(keyWorker.staffIdentifier)
    }

    val premisesToGet = premises[2]
    val expectedJson = objectMapper.writeValueAsString(premisesTransformer.transformJpaToApi(premises[2], 19))

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

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

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

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

  @Test
  fun `Get Premises Staff without JWT returns 401`() {
    webTestClient.get()
      .uri("/premises/e0f03aa2-1468-441c-aa98-0b98d86b67f9/staff")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get Premises Staff where delius team cannot be found returns 500`() {
    val deliusTeamCode = "NOTFOUND"

    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
      withDeliusTeamCode(deliusTeamCode)
    }

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    mockClientCredentialsJwtRequest()

    wiremockServer.stubFor(
      WireMock.get(WireMock.urlEqualTo("/secure/teams/$deliusTeamCode/staff"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(404)
        )
    )

    webTestClient.get()
      .uri("/premises/${premises.id}/staff")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
      .jsonPath("$.detail").isEqualTo("No team found for Delius team code: ${premises.deliusTeamCode}")
  }

  @Test
  fun `Get Premises Staff for Temporary Accommodation Premises returns 501`() {
    val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/premises/${premises.id}/staff")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.NOT_IMPLEMENTED)
  }

  @Test
  fun `Get Premises Staff for Approved Premises returns 200 with correct body`() {
    val deliusTeamCode = "FOUND"

    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
      withDeliusTeamCode(deliusTeamCode)
    }

    val staffMembers = listOf(
      StaffMemberFactory().produce(),
      StaffMemberFactory().produce(),
      StaffMemberFactory().produce(),
      StaffMemberFactory().produce(),
      StaffMemberFactory().produce()
    )

    mockClientCredentialsJwtRequest()

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    wiremockServer.stubFor(
      WireMock.get(WireMock.urlEqualTo("/secure/teams/$deliusTeamCode/staff"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              objectMapper.writeValueAsString(staffMembers)
            )
        )
    )

    webTestClient.get()
      .uri("/premises/${premises.id}/staff")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(
          staffMembers.map(staffMemberTransformer::transformDomainToApi)
        )
      )
  }

  @Test
  fun `Get Premises Staff caches response`() {
    val deliusTeamCode = "FOUND"

    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
      withDeliusTeamCode(deliusTeamCode)
    }

    val staffMembers = listOf(
      StaffMemberFactory().produce(),
      StaffMemberFactory().produce(),
      StaffMemberFactory().produce(),
      StaffMemberFactory().produce(),
      StaffMemberFactory().produce()
    )

    mockClientCredentialsJwtRequest()

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    wiremockServer.stubFor(
      WireMock.get(WireMock.urlEqualTo("/secure/teams/$deliusTeamCode/staff"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              objectMapper.writeValueAsString(staffMembers)
            )
        )
    )

    repeat(2) {
      webTestClient.get()
        .uri("/premises/${premises.id}/staff")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(
          objectMapper.writeValueAsString(
            staffMembers.map(staffMemberTransformer::transformDomainToApi)
          )
        )
    }

    wiremockServer.verify(exactly(1), getRequestedFor(urlEqualTo("/secure/teams/$deliusTeamCode/staff")))
  }
}
