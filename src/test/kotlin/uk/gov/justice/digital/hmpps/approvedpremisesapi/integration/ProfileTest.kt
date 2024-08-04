package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProfileResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockSuccessfulStaffUserDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import java.util.UUID

class ProfileTest : IntegrationTestBase() {

  @Nested
  inner class Profile {
    val profileEndpoint = "/profile"

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
          .uri(profileEndpoint)
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
                permissions = listOf(
                  ApprovedPremisesUserPermission.assessApplication,
                  ApprovedPremisesUserPermission.assessAppealedApplication,
                  ApprovedPremisesUserPermission.assessPlacementApplication,
                  ApprovedPremisesUserPermission.viewAssignedAssessments,
                ),
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
        .uri(profileEndpoint)
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
              service = "CAS3",
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
        .uri(profileEndpoint)
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
              service = "CAS3",
              isActive = true,
            ),
          ),
        )
    }
  }

  @Nested
  inner class ProfileV2 {
    val profileV2Endpoint = "/profile/v2"

    @Test
    fun `Getting existing CAS1 profile returns OK with correct body`() {
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
          .uri(profileV2Endpoint)
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              ProfileResponse(
                deliusUsername = "JIMJIMMERSON",
                loadError = null,
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
                  permissions = listOf(
                    ApprovedPremisesUserPermission.assessApplication,
                    ApprovedPremisesUserPermission.assessAppealedApplication,
                    ApprovedPremisesUserPermission.assessPlacementApplication,
                    ApprovedPremisesUserPermission.viewAssignedAssessments,
                  ),
                ),
              ),
            ),
          )
      }
    }

    @Test
    fun `Getting existing CAS3 profile returns OK with correct body`() {
      val id = UUID.randomUUID()
      val deliusUsername = "JIMJIMMERSON"
      val email = "foo@bar.com"
      val telephoneNumber = "123445677"
      val forename = "Fore"
      val surname = "Sur"

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
        withName("$forename $surname")
        withYieldedProbationRegion { region }
        withDeliusUsername(deliusUsername)
        withEmail(email)
        withTelephoneNumber(telephoneNumber)
      }

      mockStaffUserInfoCommunityApiCall(
        StaffUserDetailsFactory()
          .withForenames(userEntity.name.split(" ")[0])
          .withSurname(userEntity.name.split(" ")[1])
          .withUsername(deliusUsername)
          .withEmail(email)
          .withTelephoneNumber(telephoneNumber)
          .produce(),
      )

      mockClientCredentialsJwtRequest(deliusUsername, listOf("ROLE_PROBATION"), authSource = "delius")

      userRoleAssignmentEntityFactory.produceAndPersist {
        withUser(userEntity)
        withRole(UserRole.CAS3_ASSESSOR)
      }

      userQualificationAssignmentEntityFactory.produceAndPersist {
        withUser(userEntity)
        withQualification(UserQualification.PIPE)
      }

      webTestClient.get()
        .uri(profileV2Endpoint)
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(
          objectMapper.writeValueAsString(
            ProfileResponse(
              deliusUsername = "JIMJIMMERSON",
              loadError = null,
              TemporaryAccommodationUser(
                id = id,
                region = ProbationRegion(region.id, region.name),
                deliusUsername = deliusUsername,
                email = email,
                name = userEntity.name,
                telephoneNumber = telephoneNumber,
                roles = listOf(TemporaryAccommodationUserRole.assessor),
                service = "CAS3",
                isActive = true,
              ),
            ),
          ),
        )
    }

    @Test
    fun `Getting existing profile with no Delius staff record returns load error`() {
      val id = UUID.randomUUID()
      val deliusUsername = "JIMJIMMERSON"
      val email = "foo@bar.com"
      val telephoneNumber = "123445677"

      mockClientCredentialsJwtRequest(deliusUsername, listOf("ROLE_PROBATION"), authSource = "delius")

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

      val response = webTestClient.get()
        .uri(profileV2Endpoint)
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(ProfileResponse::class.java)
        .returnResult()
        .responseBody!!

      assertThat(response.deliusUsername).isEqualTo(deliusUsername)
      assertThat(response.loadError).isEqualTo(ProfileResponse.LoadError.staffRecordNotFound)
    }

    @Test
    fun `Getting existing profile with Delius staff record updates user details`() {
      val id = UUID.randomUUID()
      val deliusUsername = "JIMJIMMERSON"
      val email = "foo@bar.com"
      val telephoneNumber = "123445677"

      mockStaffUserInfoCommunityApiCall(
        StaffUserDetailsFactory()
          .withForenames("Up")
          .withSurname("Dated")
          .withUsername(deliusUsername)
          .withEmail(email)
          .withTelephoneNumber(telephoneNumber)
          .produce(),
      )

      mockClientCredentialsJwtRequest(deliusUsername, listOf("ROLE_PROBATION"), authSource = "delius")

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

      val response = webTestClient.get()
        .uri(profileV2Endpoint)
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(ProfileResponse::class.java)
        .returnResult()
        .responseBody!!

      assertThat(response.deliusUsername).isEqualTo(deliusUsername)
      assertThat(response.user!!.name).isEqualTo("Up Dated")
    }

    @Test
    fun `Getting existing CAS3 profile returns OK for CAS3_REPORTER with correct body`() {
      val id = UUID.randomUUID()
      val deliusUsername = "JIMJIMMERSON"
      val email = "foo@bar.com"
      val telephoneNumber = "123445677"
      val forename = "Fore"
      val surname = "Sur"

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
        withName("$forename $surname")
        withYieldedProbationRegion { region }
        withDeliusUsername(deliusUsername)
        withEmail(email)
        withTelephoneNumber(telephoneNumber)
      }

      mockStaffUserInfoCommunityApiCall(
        StaffUserDetailsFactory()
          .withForenames(userEntity.name.split(" ")[0])
          .withSurname(userEntity.name.split(" ")[1])
          .withUsername(deliusUsername)
          .withEmail(email)
          .withTelephoneNumber(telephoneNumber)
          .produce(),
      )

      mockClientCredentialsJwtRequest(deliusUsername, listOf("ROLE_PROBATION"), authSource = "delius")

      userRoleAssignmentEntityFactory.produceAndPersist {
        withUser(userEntity)
        withRole(UserRole.CAS3_REPORTER)
      }

      userQualificationAssignmentEntityFactory.produceAndPersist {
        withUser(userEntity)
        withQualification(UserQualification.PIPE)
      }

      webTestClient.get()
        .uri(profileV2Endpoint)
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(
          objectMapper.writeValueAsString(
            ProfileResponse(
              deliusUsername = "JIMJIMMERSON",
              loadError = null,
              TemporaryAccommodationUser(
                id = id,
                region = ProbationRegion(region.id, region.name),
                deliusUsername = deliusUsername,
                email = email,
                name = userEntity.name,
                telephoneNumber = telephoneNumber,
                roles = listOf(TemporaryAccommodationUserRole.reporter),
                service = "CAS3",
                isActive = true,
              ),
            ),
          ),
        )
    }

    @Test
    fun `Getting new profile persists new user`() {
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

      probationAreaProbationRegionMappingFactory.produceAndPersist {
        withProbationRegion(region)
        withProbationAreaDeliusCode(region.deliusCode)
      }

      CommunityAPI_mockSuccessfulStaffUserDetailsCall(
        StaffUserDetailsFactory()
          .withUsername(deliusUsername)
          .withEmail(email)
          .withTelephoneNumber(telephoneNumber)
          .withProbationAreaCode(region.deliusCode)
          .produce(),
      )

      val response = webTestClient.get()
        .uri(profileV2Endpoint)
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult<ProfileResponse>()
        .responseBody
        .blockFirst()

      assertThat(response!!.user!!.deliusUsername).isEqualTo(deliusUsername)
    }

    @Test
    fun `Getting new profile with no Delius staff record returns load error`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt("nonStaffUser")
      mockOAuth2ClientCredentialsCallIfRequired()

      webTestClient.get()
        .uri(profileV2Endpoint)
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(
          objectMapper.writeValueAsString(
            ProfileResponse(
              deliusUsername = "nonStaffUser",
              loadError = ProfileResponse.LoadError.staffRecordNotFound,
              null,
            ),
          ),
        )
    }
  }

  @Nested
  inner class ProblemsCommonTests {

    @ParameterizedTest
    @ValueSource(strings = ["/profile", "/profile/v2"])
    fun `Getting own profile without a JWT returns 401`(endpoint: String) {
      webTestClient.get()
        .uri(endpoint)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @ValueSource(strings = ["/profile", "/profile/v2"])
    fun `Getting own profile with a non-Delius JWT returns 403`(endpoint: String) {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
      )

      webTestClient.get()
        .uri(endpoint)
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @ParameterizedTest
    @ValueSource(strings = ["/profile", "/profile/v2"])
    fun `Getting own profile with no X-Service-Name header returns 400`(endpoint: String) {
      `Given a User` { userEntity, jwt ->
        webTestClient.get()
          .uri(endpoint)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("detail").isEqualTo("Missing required header X-Service-Name")
      }
    }
  }
}
