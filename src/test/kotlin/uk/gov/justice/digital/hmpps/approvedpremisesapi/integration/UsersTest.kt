package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import java.util.UUID

class UsersTest : IntegrationTestBase() {
  val id: UUID = UUID.fromString("aff9a4dc-e208-4e4b-abe6-99aff7f6af8a")

  @Test
  fun `Getting a user without a JWT returns 401`() {
    webTestClient.get()
      .uri("/users/$id")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting a user with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis"
    )

    webTestClient.get()
      .uri("/users/$id")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting a user returns OK with correct body`() {
    val deliusUsername = "JimJimmerson"
    val email = "foo@bar.com"
    val telephoneNumber = "123445677"

    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = deliusUsername,
      authSource = "delius",
      roles = listOf("ROLE_PROBATION")
    )

    userEntityFactory.produceAndPersist {
      withId(id)
      withDeliusUsername(deliusUsername)
      withEmail(email)
      withTelephoneNumber(telephoneNumber)
    }

    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withUsername(deliusUsername)
        .withEmail(email)
        .withTelephoneNumber(telephoneNumber)
        .produce()
    )

    mockClientCredentialsJwtRequest("username", listOf("ROLE_COMMUNITY"), authSource = "delius")

    webTestClient.get()
      .uri("/users/$id")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(
          User(
            deliusUsername = deliusUsername,
            email = email,
            telephoneNumber = telephoneNumber,
            roles = emptyList(),
            qualifications = emptyList(),
          )
        )
      )
  }
}
