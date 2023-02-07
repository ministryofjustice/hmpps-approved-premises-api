package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import java.util.UUID

class UsersTest : IntegrationTestBase() {
  @Autowired
  lateinit var userTransformer: UserTransformer

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

  @Nested
  inner class GetUsers {
    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["MANAGER", "MATCHER"])
    fun `GET to users with a role other than ROLE_ADMIN or WORKFLOW_MANAGER is forbidden`(role: UserRole) {
      val deliusUsername = "AnyUser"

      val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
        subject = deliusUsername,
        authSource = "delius",
        roles = listOf("ROLE_PROBATION")
      )

      val region = probationRegionEntityFactory.produceAndPersist {
        withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
      }

      val user = userEntityFactory.produceAndPersist {
        withDeliusUsername(deliusUsername)
        withName("Any User")
        withYieldedProbationRegion { region }
      }

      user.roles += userRoleAssignmentEntityFactory.produceAndPersist {
        withUser(user)
        withRole(role)
      }

      mockClientCredentialsJwtRequest("username", listOf("ROLE_COMMUNITY"), authSource = "delius")

      webTestClient.get()
        .uri("/users")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `GET to users with no internal role (aka the Applicant pseudo-role) is forbidden`() {
      val deliusUsername = "ProbationPractitioner"

      val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
        subject = deliusUsername,
        authSource = "delius",
        roles = listOf("ROLE_PROBATION")
      )

      val region = probationRegionEntityFactory.produceAndPersist {
        withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
      }

      val user = userEntityFactory.produceAndPersist {
        withDeliusUsername(deliusUsername)
        withName("Probation Practitioner")
        withYieldedProbationRegion { region }
      }

      mockClientCredentialsJwtRequest("username", listOf("ROLE_COMMUNITY"), authSource = "delius")

      webTestClient.get()
        .uri("/users")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["ROLE_ADMIN", "WORKFLOW_MANAGER"])
    fun `GET to users with a role of either ROLE_ADMIN or WORKFLOW_MANAGER returns full list ordered by name`(role: UserRole) {
      val deliusUsername = "ArthurAdmin"

      val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
        subject = deliusUsername,
        authSource = "delius",
        roles = listOf("ROLE_PROBATION")
      )

      val region = probationRegionEntityFactory.produceAndPersist {
        withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
      }

      val arthurAdmin = userEntityFactory.produceAndPersist {
        withDeliusUsername(deliusUsername)
        withName("Arthur Admin")
        withYieldedProbationRegion { region }
      }

      arthurAdmin.roles += userRoleAssignmentEntityFactory.produceAndPersist {
        withUser(arthurAdmin)
        withRole(role)
      }

      val ben = userEntityFactory.produceAndPersist {
        withDeliusUsername("BenJones")
        withName("Ben Jones")
        withYieldedProbationRegion { region }
      }

      val cary = userEntityFactory.produceAndPersist {
        withDeliusUsername("CaryJones")
        withName("Cary Jones")
        withYieldedProbationRegion { region }
      }

      val avril = userEntityFactory.produceAndPersist {
        withDeliusUsername("AvrilJones")
        withName("Avril Jones")
        withYieldedProbationRegion { region }
      }

      mockClientCredentialsJwtRequest("username", listOf("ROLE_COMMUNITY"), authSource = "delius")

      webTestClient.get()
        .uri("/users")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(
          objectMapper.writeValueAsString(
            listOf(arthurAdmin, avril, ben, cary).map {
              userTransformer.transformJpaToApi(it, ServiceName.approvedPremises)
            }
          )
        )
    }
  }
}
