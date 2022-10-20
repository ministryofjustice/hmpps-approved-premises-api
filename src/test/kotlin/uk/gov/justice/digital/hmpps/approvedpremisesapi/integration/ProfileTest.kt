package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification as ApiUserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserRole as ApiUserRole

class ProfileTest : IntegrationTestBase() {
  @Test
  fun `Getting own profile without a JWT returns 401`() {
    webTestClient.get()
      .uri("/profile")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting own profile with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis"
    )

    webTestClient.get()
      .uri("/profile")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting own profile returns OK with correct body`() {
    val deliusUsername = "JimJimmerson"

    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = deliusUsername,
      authSource = "delius",
      roles = listOf("ROLE_PROBATION")
    )

    val userEntity = userEntityFactory.produceAndPersist {
      withDeliusUsername(deliusUsername)
    }

    userRoleAssignmentEntityFactory.produceAndPersist {
      withUser(userEntity)
      withRole(UserRole.ASSESSOR)
    }

    userQualificationAssignmentEntityFactory.produceAndPersist {
      withUser(userEntity)
      withQualification(UserQualification.PIPE)
    }

    webTestClient.get()
      .uri("/profile")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(
          User(
            deliusUsername = deliusUsername,
            roles = listOf(ApiUserRole.assessor),
            qualifications = listOf(ApiUserQualification.pipe)
          )
        )
      )
  }
}
