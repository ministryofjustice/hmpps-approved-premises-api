package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUser
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
  fun `Getting a user with no X-Service-Name header returns 400`() {
    val deliusUsername = "JimJimmerson"
    val forename = "Jim"
    val surname = "Jimmerson"
    val name = "$forename $surname"
    val email = "foo@bar.com"
    val telephoneNumber = "123445677"

    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = deliusUsername,
      authSource = "delius",
      roles = listOf("ROLE_PROBATION")
    )

    val region = probationRegionEntityFactory.produceAndPersist {
      withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
    }

    userEntityFactory.produceAndPersist {
      withId(id)
      withDeliusUsername(deliusUsername)
      withName(name)
      withEmail(email)
      withTelephoneNumber(telephoneNumber)
      withYieldedProbationRegion { region }
    }

    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withForenames(forename)
        .withSurname(surname)
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
      .is4xxClientError
      .expectBody()
      .jsonPath("title").isEqualTo("Bad Request")
      .jsonPath("detail").isEqualTo("Required request header 'X-Service-Name' for method parameter type ServiceName is not present")
  }

  @Test
  fun `Getting an Approved Premises user returns OK with correct body`() {
    val deliusUsername = "JimJimmerson"
    val forename = "Jim"
    val surname = "Jimmerson"
    val name = "$forename $surname"
    val email = "foo@bar.com"
    val telephoneNumber = "123445677"

    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = deliusUsername,
      authSource = "delius",
      roles = listOf("ROLE_PROBATION")
    )

    val region = probationRegionEntityFactory.produceAndPersist {
      withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
    }

    userEntityFactory.produceAndPersist {
      withId(id)
      withDeliusUsername(deliusUsername)
      withName(name)
      withEmail(email)
      withTelephoneNumber(telephoneNumber)
      withYieldedProbationRegion { region }
    }

    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withForenames(forename)
        .withSurname(surname)
        .withUsername(deliusUsername)
        .withEmail(email)
        .withTelephoneNumber(telephoneNumber)
        .produce()
    )

    mockClientCredentialsJwtRequest("username", listOf("ROLE_COMMUNITY"), authSource = "delius")

    webTestClient.get()
      .uri("/users/$id")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.approvedPremises.value)
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(
          ApprovedPremisesUser(
            id = id,
            region = ProbationRegion(region.id, region.name),
            deliusUsername = deliusUsername,
            name = name,
            email = email,
            telephoneNumber = telephoneNumber,
            roles = emptyList(),
            qualifications = emptyList(),
            service = ServiceName.approvedPremises.value,
          )
        )
      )
  }

  @Test
  fun `Getting a Temporary Accommodation user returns OK with correct body`() {
    val deliusUsername = "JimJimmerson"
    val forename = "Jim"
    val surname = "Jimmerson"
    val name = "$forename $surname"
    val email = "foo@bar.com"
    val telephoneNumber = "123445677"

    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = deliusUsername,
      authSource = "delius",
      roles = listOf("ROLE_PROBATION")
    )

    val region = probationRegionEntityFactory.produceAndPersist {
      withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
    }

    userEntityFactory.produceAndPersist {
      withId(id)
      withDeliusUsername(deliusUsername)
      withName(name)
      withEmail(email)
      withTelephoneNumber(telephoneNumber)
      withYieldedProbationRegion { region }
    }

    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withForenames(forename)
        .withSurname(surname)
        .withUsername(deliusUsername)
        .withEmail(email)
        .withTelephoneNumber(telephoneNumber)
        .produce()
    )

    mockClientCredentialsJwtRequest("username", listOf("ROLE_COMMUNITY"), authSource = "delius")

    webTestClient.get()
      .uri("/users/$id")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(
          TemporaryAccommodationUser(
            id = id,
            region = ProbationRegion(region.id, region.name),
            roles = emptyList(),
            service = ServiceName.temporaryAccommodation.value,
          )
        )
      )
  }
}
