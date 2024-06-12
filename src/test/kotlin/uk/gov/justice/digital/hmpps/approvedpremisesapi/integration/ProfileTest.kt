package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import java.util.UUID

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
      authSource = "nomis",
    )

    webTestClient.get()
      .uri("/profile")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting own profile with no X-Service-Name header returns 400`() {
    `Given a User` { userEntity, jwt ->
      webTestClient.get()
        .uri("/profile")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody()
        .jsonPath("title").isEqualTo("Bad Request")
        .jsonPath("detail").isEqualTo("Missing required header X-Service-Name")
    }
  }

  @Test
  fun `Getting own Approved Premises profile returns OK with correct body`() {
    val id = UUID.randomUUID()
    val deliusUsername = "JIMJIMMERSON"
    val email = "foo@bar.com"
    val telephoneNumber = "123445677"

    val region = probationRegionEntityFactory.produceAndPersist {
      withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
    }

    `Given a User`(
      id = id,
      roles = listOf(UserRole.CAS1_ASSESSOR),
      qualifications = listOf(UserQualification.PIPE),
      staffUserDetailsConfigBlock = {
        withUsername(deliusUsername)
        withEmail(email)
        withTelephoneNumber(telephoneNumber)
      },
      probationRegion = region,
    ) { userEntity, jwt ->
      val userApArea = userEntity.apArea!!

      webTestClient.get()
        .uri("/profile")
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
              email = email,
              name = userEntity.name,
              telephoneNumber = telephoneNumber,
              roles = listOf(ApprovedPremisesUserRole.assessor),
              qualifications = listOf(uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification.pipe),
              service = "CAS1",
              isActive = true,
              apArea = ApArea(userApArea.id, userApArea.identifier, userApArea.name),
            ),
          ),
        )
    }
  }

  @Test
  fun `Getting own Temporary Accommodation profile returns OK with correct body`() {
    val id = UUID.randomUUID()
    val deliusUsername = "JIMJIMMERSON"
    val email = "foo@bar.com"
    val telephoneNumber = "123445677"

    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = deliusUsername,
      authSource = "delius",
      roles = listOf("ROLE_PROBATION"),
    )

    val region = probationRegionEntityFactory.produceAndPersist {
      withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
    }

    val userEntity = userEntityFactory.produceAndPersist {
      withId(id)
      withYieldedProbationRegion { region }
      withDeliusUsername(deliusUsername)
      withEmail(email)
      withTelephoneNumber(telephoneNumber)
    }

    userRoleAssignmentEntityFactory.produceAndPersist {
      withUser(userEntity)
      withRole(UserRole.CAS3_ASSESSOR)
    }

    userQualificationAssignmentEntityFactory.produceAndPersist {
      withUser(userEntity)
      withQualification(UserQualification.PIPE)
    }

    webTestClient.get()
      .uri("/profile")
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
            deliusUsername = deliusUsername,
            email = email,
            name = userEntity.name,
            telephoneNumber = telephoneNumber,
            roles = listOf(TemporaryAccommodationUserRole.assessor),
            service = ServiceName.temporaryAccommodation.value,
            isActive = true,
          ),
        ),
      )
  }

  @Test
  fun `Getting own Temporary Accommodation profile returns OK for CAS3_REPORTER with correct body`() {
    val id = UUID.randomUUID()
    val deliusUsername = "JIMJIMMERSON"
    val email = "foo@bar.com"
    val telephoneNumber = "123445677"

    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = deliusUsername,
      authSource = "delius",
      roles = listOf("ROLE_PROBATION"),
    )

    val region = probationRegionEntityFactory.produceAndPersist {
      withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
    }

    val userEntity = userEntityFactory.produceAndPersist {
      withId(id)
      withYieldedProbationRegion { region }
      withDeliusUsername(deliusUsername)
      withEmail(email)
      withTelephoneNumber(telephoneNumber)
    }

    userRoleAssignmentEntityFactory.produceAndPersist {
      withUser(userEntity)
      withRole(UserRole.CAS3_REPORTER)
    }

    userQualificationAssignmentEntityFactory.produceAndPersist {
      withUser(userEntity)
      withQualification(UserQualification.PIPE)
    }

    webTestClient.get()
      .uri("/profile")
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
            deliusUsername = deliusUsername,
            email = email,
            name = userEntity.name,
            telephoneNumber = telephoneNumber,
            roles = listOf(TemporaryAccommodationUserRole.reporter),
            service = ServiceName.temporaryAccommodation.value,
            isActive = true,
          ),
        ),
      )
  }
}
