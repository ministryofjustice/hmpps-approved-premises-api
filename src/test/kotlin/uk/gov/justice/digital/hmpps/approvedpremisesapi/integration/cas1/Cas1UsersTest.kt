package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1UpdateUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserRolesAndQualifications
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.PersonName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.StaffDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory.probationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory.team
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddStaffDetailResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockNotFoundStaffDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification as APIUserQualification

class Cas1UsersTest : InitialiseDatabasePerClassTestBase() {
  val id: UUID = UUID.fromString("aff9a4dc-e208-4e4b-abe6-99aff7f6af8a")

  @Autowired
  lateinit var userTransformer: UserTransformer

  @Nested
  inner class GetUser {

    @Test
    fun `Getting a user without a JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/users/$id")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Getting a user with a non-Delius JWT returns 403`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "other-auth-source",
      )

      webTestClient.get()
        .uri("/cas1/users/$id")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Getting a user with a Nomis JWT returns 403`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
      )

      webTestClient.get()
        .uri("/cas1/users/$id")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Getting a user with the POM role returns 403`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_POM"),
      )

      webTestClient.get()
        .uri("/cas1/users/$id")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Getting a CAS1 Approved Premises user returns OK with correct body`() {
      val deliusUsername = "JimJimmerson"
      val forename = "Jim"
      val middleName = "C"
      val surname = "Jimmerson"
      val name = "$forename $middleName $surname"
      val email = "foo@bar.com"
      val telephoneNumber = "123445677"

      val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
        subject = deliusUsername,
        authSource = "delius",
        roles = listOf("ROLE_PROBATION"),
      )

      val region = givenAProbationRegion()

      userEntityFactory.produceAndPersist {
        withId(id)
        withDeliusUsername(deliusUsername)
        withName(name)
        withEmail(email)
        withTelephoneNumber(telephoneNumber)
        withYieldedProbationRegion { region }
      }

      apDeliusContextAddStaffDetailResponse(
        StaffDetail(
          email = email,
          telephoneNumber = telephoneNumber,
          teams = listOf(team()),
          probationArea = probationArea(),
          username = deliusUsername,
          name = PersonName(forename, surname, middleName),
          code = "STAFF1",
          active = true,
        ),
      )

      mockClientCredentialsJwtRequest("username", listOf("ROLE_COMMUNITY"), authSource = "delius")

      val result = webTestClient.get()
        .uri("/cas1/users/$id")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(ApprovedPremisesUser::class.java)
        .responseBody
        .blockFirst()!!

      assertThat(result.id).isEqualTo(id)
      assertThat(result.region).isEqualTo(ProbationRegion(region.id, region.name))
      assertThat(result.deliusUsername).isEqualTo(deliusUsername)
      assertThat(result.name).isEqualTo(name)
      assertThat(result.email).isEqualTo(email)
      assertThat(result.telephoneNumber).isEqualTo(telephoneNumber)
      assertThat(result.roles).isEqualTo(emptyList<ApprovedPremisesUserRole>())
      assertThat(result.qualifications).isEqualTo(emptyList<UserQualification>())
      assertThat(result.service).isEqualTo("CAS1")
      assertThat(result.isActive).isEqualTo(true)
      assertThat(result.apArea).isEqualTo(ApArea(region.apArea!!.id, region.apArea!!.identifier, region.apArea!!.name))
      assertThat(result.permissions).isEqualTo(emptyList<UserPermission>())
      assertThat(result.version).isNotZero()
    }

    @Test
    fun `should return the correct user when Delius user is not found`() {
      val deliusUsername = "JimJimmerson"
      val forename = "Jim"
      val middleName = "C"
      val surname = "Jimmerson"
      val name = "$forename $middleName $surname"
      val email = "foo@bar.com"
      val telephoneNumber = "123445677"

      val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
        subject = deliusUsername,
        authSource = "delius",
        roles = listOf("ROLE_PROBATION"),
      )

      val region = givenAProbationRegion()

      userEntityFactory.produceAndPersist {
        withId(id)
        withDeliusUsername(deliusUsername)
        withName(name)
        withEmail(email)
        withTelephoneNumber(telephoneNumber)
        withYieldedProbationRegion { region }
        withApArea(region.apArea)
        withCruManagementArea(givenACas1CruManagementArea())
      }

      apDeliusContextMockNotFoundStaffDetailCall("userName")

      mockClientCredentialsJwtRequest("username", listOf("ROLE_COMMUNITY"), authSource = "delius")

      val result = webTestClient.get()
        .uri("/cas1/users/$id")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(ApprovedPremisesUser::class.java)
        .responseBody
        .blockFirst()!!

      assertThat(result.id).isEqualTo(id)
    }

    @Test
    fun `should return not found when neither Delius user nor system user exists`() {
      val deliusUsername = "JimJimmerson"

      val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
        subject = deliusUsername,
        authSource = "delius",
        roles = listOf("ROLE_PROBATION"),
      )

      apDeliusContextMockNotFoundStaffDetailCall("userName")

      mockClientCredentialsJwtRequest("username", listOf("ROLE_COMMUNITY"), authSource = "delius")

      webTestClient.get()
        .uri("/cas1/users/$id")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Nested
  inner class UpdateUser {
    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_JANITOR", "CAS1_USER_MANAGER"], mode = EnumSource.Mode.EXCLUDE)
    fun `Updating a user without an approved role is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        val id = UUID.randomUUID()
        webTestClient.put()
          .uri("/cas1/users/$id")
          .header("Authorization", "Bearer $jwt")
          .header("Content-Type", "application/json")
          .bodyValue(
            UserRolesAndQualifications(
              listOf(),
              listOf(),
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Updating a users with no internal role (aka the Applicant pseudo-role) is forbidden`() {
      givenAUser { _, jwt ->
        val id = UUID.randomUUID()
        webTestClient.put()
          .uri("/cas1/users/$id")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UserRolesAndQualifications(
              listOf(),
              listOf(),
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `Updating a user returns OK`(role: UserRole) {
      val qualifications = listOf(APIUserQualification.emergency, APIUserQualification.pipe)
      val roles = listOf(
        ApprovedPremisesUserRole.assessor,
        ApprovedPremisesUserRole.reportViewer,
        ApprovedPremisesUserRole.excludedFromAssessAllocation,
        ApprovedPremisesUserRole.excludedFromMatchAllocation,
        ApprovedPremisesUserRole.excludedFromPlacementApplicationAllocation,
      )

      val apArea = givenAnApArea()
      val userId = givenAUser(probationRegion = givenAProbationRegion(apArea = apArea)).first.id

      val cruManagementAreaOverride = givenACas1CruManagementArea()

      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.put()
          .uri("/cas1/users/$userId")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            Cas1UpdateUser(
              roles = roles,
              qualifications = qualifications,
              cruManagementAreaOverrideId = cruManagementAreaOverride.id,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath(".qualifications").isArray
          .jsonPath(".qualifications[0]").isEqualTo("emergency")
          .jsonPath(".roles").isArray
          .jsonPath(".roles[0]").isEqualTo(ApprovedPremisesUserRole.assessor.value)
          .jsonPath(".roles[1]").isEqualTo(ApprovedPremisesUserRole.reportViewer.value)
          .jsonPath(".roles[2]").isEqualTo(ApprovedPremisesUserRole.excludedFromAssessAllocation.value)
          .jsonPath(".roles[3]").isEqualTo(ApprovedPremisesUserRole.excludedFromMatchAllocation.value)
          .jsonPath(".roles[4]").isEqualTo(ApprovedPremisesUserRole.excludedFromPlacementApplicationAllocation.value)
          .jsonPath(".isActive").isEqualTo(true)
          .jsonPath(".cruManagementArea.id").isEqualTo(cruManagementAreaOverride.id.toString())
          .jsonPath(".cruManagementAreaDefault.id").isEqualTo(apArea.defaultCruManagementArea.id.toString())
          .jsonPath(".cruManagementAreaOverride.id").isEqualTo(cruManagementAreaOverride.id.toString())
      }
    }
  }

  @Nested
  inner class DeleteUser {
    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_JANITOR", "CAS1_USER_MANAGER"], mode = EnumSource.Mode.EXCLUDE)
    fun `Deleting a user with an unapproved role is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        val id = UUID.randomUUID()
        webTestClient.delete()
          .uri("/cas1/users/$id")
          .header("Authorization", "Bearer $jwt")
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Deleting a user with no internal role (aka the Applicant pseudo-role) is forbidden`() {
      givenAUser { _, jwt ->
        val id = UUID.randomUUID()
        webTestClient.delete()
          .uri("/cas1/users/$id")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Deleting a user with a non-Delius JWT returns 403`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
      )

      webTestClient.delete()
        .uri("/cas1/users/$id")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `Deleting a user with an approved role deletes successfully`(role: UserRole) {
      userEntityFactory.produceAndPersist {
        withId(id)
        withYieldedProbationRegion { givenAProbationRegion() }
      }

      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.delete()
          .uri("/cas1/users/$id")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk

        val userFromDatabase = userRepository.findByIdOrNull(id)
        assertThat(userFromDatabase?.isActive).isEqualTo(false)
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_JANITOR", "CAS1_USER_MANAGER"], mode = EnumSource.Mode.EXCLUDE)
    fun `Deleting a user without an approved role is forbidden `(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.delete()
          .uri("/cas1/users/$id")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }

  @Nested
  inner class Cas1UserHeaders {

    @Test
    fun `Return CAS1 User Headers if CAS1 request and user has no roles`() {
      val (user, jwt) = givenAUser()

      val headers = webTestClient.get()
        .uri("/applications")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(ApprovedPremisesUser::class.java)
        .responseHeaders

      assertThat(headers["X-CAS-User-ID"]!![0]).isEqualTo(user.id.toString())
      assertThat(headers["X-CAS-User-Version"]!![0]).isEqualTo("993")
    }

    @Test
    fun `Return CAS1 User Headers if CAS1 request and user roles`() {
      val (user, jwt) = givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER))

      val headers = webTestClient.get()
        .uri("/applications")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(ApprovedPremisesUser::class.java)
        .responseHeaders

      assertThat(headers["X-CAS-User-ID"]!![0]).isEqualTo(user.id.toString())
      assertThat(headers["X-CAS-User-Version"]!![0]).isEqualTo("1173003502")
    }

    @Test
    fun `Don't return CAS1 User headers if not a CAS1 request`() {
      val (_, jwt) = givenAUser()

      val headers = webTestClient.get()
        .uri("/applications")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(ApprovedPremisesUser::class.java)
        .responseHeaders

      assertThat(headers.containsKey("X-CAS-User-ID")).isFalse()
      assertThat(headers.containsKey("X-CAS-User-Version")).isFalse()
    }
  }

  @Nested
  inner class GetUsers {

    @ParameterizedTest
    @EnumSource(
      value = UserRole::class,
      names = ["CAS1_CRU_MEMBER", "CAS1_JANITOR", "CAS1_USER_MANAGER", "CAS1_AP_AREA_MANAGER"],
      mode = EnumSource.Mode.EXCLUDE,
    )
    fun `GET users with a role without permission CAS1_USER_LIST is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/cas1/users")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns full list ordered by name`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { cruMember, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { manager, _ ->
          givenAUser { userWithNoRole, _ ->
            givenAUser(roles = listOf(role)) { requestUser, jwt ->
              webTestClient.get()
                .uri("/cas1/users")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectHeader().doesNotExist("X-Pagination-CurrentPage")
                .expectHeader().doesNotExist("X-Pagination-TotalPages")
                .expectHeader().doesNotExist("X-Pagination-TotalResults")
                .expectHeader().doesNotExist("X-Pagination-PageSize")
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(requestUser, userWithNoRole, cruMember, manager).map {
                      userTransformer.transformJpaToApi(it, ServiceName.approvedPremises)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns list filtered by region`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { cruMember, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { manager, _ ->
          givenAUser { userWithNoRole, _ ->
            givenAUser(roles = listOf(role)) { requestUser, jwt ->
              val apArea = givenAnApArea()
              val probationRegion = probationRegionEntityFactory.produceAndPersist {
                withApArea(apArea)
              }

              val userOne = userEntityFactory.produceAndPersist {
                withProbationRegion(probationRegion)
                withApArea(apArea)
                withCruManagementArea(givenACas1CruManagementArea())
              }

              val userTwo = userEntityFactory.produceAndPersist {
                withProbationRegion(probationRegion)
                withApArea(apArea)
                withCruManagementArea(givenACas1CruManagementArea())
              }

              webTestClient.get()
                .uri("/cas1/users?probationRegionId=${probationRegion.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectHeader().doesNotExist("X-Pagination-CurrentPage")
                .expectHeader().doesNotExist("X-Pagination-TotalPages")
                .expectHeader().doesNotExist("X-Pagination-TotalResults")
                .expectHeader().doesNotExist("X-Pagination-PageSize")
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(userOne, userTwo).map {
                      userTransformer.transformJpaToApi(it, ServiceName.approvedPremises)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns list filtered by user's AP area`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { cruMember, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { manager, _ ->
          givenAUser { userWithNoRole, _ ->
            givenAUser(roles = listOf(role)) { requestUser, jwt ->
              val apArea = givenAnApArea()

              val probationRegionApArea = givenAnApArea()
              val probationRegion = probationRegionEntityFactory.produceAndPersist {
                withApArea(probationRegionApArea)
              }

              val userOne = userEntityFactory.produceAndPersist {
                withProbationRegion(probationRegion)
                withApArea(apArea)
                withCruManagementArea(givenACas1CruManagementArea())
              }

              val userTwo = userEntityFactory.produceAndPersist {
                withProbationRegion(probationRegion)
                withApArea(apArea)
                withCruManagementArea(givenACas1CruManagementArea())
              }

              webTestClient.get()
                .uri("/cas1/users?apAreaId=${apArea.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectHeader().doesNotExist("X-Pagination-CurrentPage")
                .expectHeader().doesNotExist("X-Pagination-TotalPages")
                .expectHeader().doesNotExist("X-Pagination-TotalResults")
                .expectHeader().doesNotExist("X-Pagination-PageSize")
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(userOne, userTwo).map {
                      userTransformer.transformJpaToApi(it, ServiceName.approvedPremises)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns list filtered by user's CRU management area`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { cruMember, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { manager, _ ->
          givenAUser { userWithNoRole, _ ->
            givenAUser(roles = listOf(role)) { requestUser, jwt ->
              val apArea = givenAnApArea()

              val probationRegionApArea = givenAnApArea()
              val probationRegion = probationRegionEntityFactory.produceAndPersist {
                withApArea(probationRegionApArea)
              }

              val cruManagementArea = givenACas1CruManagementArea()

              val userOne = userEntityFactory.produceAndPersist {
                withProbationRegion(probationRegion)
                withApArea(apArea)
                withCruManagementArea(cruManagementArea)
              }

              val userTwo = userEntityFactory.produceAndPersist {
                withProbationRegion(probationRegion)
                withApArea(apArea)
                withCruManagementArea(cruManagementArea)
              }

              userEntityFactory.produceAndPersist {
                withProbationRegion(probationRegion)
                withApArea(apArea)
                withCruManagementArea(givenACas1CruManagementArea())
              }

              webTestClient.get()
                .uri("/cas1/users?cruManagementAreaId=${cruManagementArea.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectHeader().doesNotExist("X-Pagination-CurrentPage")
                .expectHeader().doesNotExist("X-Pagination-TotalPages")
                .expectHeader().doesNotExist("X-Pagination-TotalResults")
                .expectHeader().doesNotExist("X-Pagination-PageSize")
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(userOne, userTwo).map {
                      userTransformer.transformJpaToApi(it, ServiceName.approvedPremises)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns paginated list ordered by name`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { cruMember, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { manager, _ ->
          givenAUser { userWithNoRole, _ ->
            givenAUser(roles = listOf(role)) { requestUser, jwt ->
              webTestClient.get()
                .uri("/cas1/users?page=1")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
                .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
                .expectHeader().valueEquals("X-Pagination-TotalResults", 4)
                .expectHeader().valueEquals("X-Pagination-PageSize", 10)
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(requestUser, userWithNoRole, cruMember, manager).map {
                      userTransformer.transformJpaToApi(it, ServiceName.approvedPremises)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role allows filtering by roles`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { reportViewer, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER, UserRole.CAS1_FUTURE_MANAGER, UserRole.CAS1_FUTURE_MANAGER)) { futureManager, _ ->
          givenAUser { _, _ ->
            givenAUser(roles = listOf(role)) { _, jwt ->
              webTestClient.get()
                .uri("/cas1/users?roles=report_viewer,future_manager")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(reportViewer, futureManager).map {
                      userTransformer.transformJpaToApi(it, ServiceName.approvedPremises)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @Test
    fun `GET to users with an approved role allows filtering by name or email`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(
          name = PersonName(
            forename = "Lob",
            surname = "Roberts",
          ),
          email = "robert.roberts@test.com",
        ),
      )
      val (user1WithMatchingName, _) = givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(
          name = PersonName(
            forename = "Bob",
            surname = "Roberts",
          ),
          email = "robert.roberts@test.com",
        ),
      )
      val (user2WithMatchingEmail, _) = givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(
          name = PersonName(
            forename = "Jim",
            surname = "Jimmers",
          ),
          email = "jim.bob@test.com",
        ),
      )
      val (user3WithMatchingNameAndEmail, _) = givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(
          name = PersonName(
            forename = "Bobby",
            surname = "Jo",
          ),
          email = "bobby.jo@test.com",
        ),
      )

      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER))

      val results = webTestClient.get()
        .uri("/cas1/users?nameOrEmail=bob")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<ApprovedPremisesUser>()

      assertThat(results).hasSize(3)
      assertThat(results.map { it.id }).contains(user1WithMatchingName.id)
      assertThat(results.map { it.id }).contains(user2WithMatchingEmail.id)
      assertThat(results.map { it.id }).contains(user3WithMatchingNameAndEmail.id)
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role allows filtering by qualifications`(role: UserRole) {
      givenAUser(qualifications = listOf(UserQualification.EMERGENCY)) { emergencyUser, _ ->
        givenAUser(qualifications = listOf(UserQualification.PIPE)) { _, _ ->
          givenAUser { _, _ ->
            givenAUser(roles = listOf(role)) { _, jwt ->
              webTestClient.get()
                .uri("/cas1/users?qualifications=emergency")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(emergencyUser).map {
                      userTransformer.transformJpaToApi(it, ServiceName.approvedPremises)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role allows filtering by role and qualifications`(
      role: UserRole,
    ) {
      givenAUser(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        qualifications = listOf(UserQualification.EMERGENCY),
      ) { emergencyAssessor1, _ ->
        givenAUser(
          roles = listOf(UserRole.CAS1_ASSESSOR),
          qualifications = listOf(UserQualification.EMERGENCY),
        ) { emergencyAssessor2, _ ->
          givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR)) { _, _ ->
            givenAUser { _, _ ->
              givenAUser(roles = listOf(role)) { _, jwt ->
                webTestClient.get()
                  .uri("/cas1/users?roles=assessor&qualifications=emergency")
                  .header("Authorization", "Bearer $jwt")
                  .exchange()
                  .expectStatus()
                  .isOk
                  .expectBody()
                  .json(
                    objectMapper.writeValueAsString(
                      listOf(emergencyAssessor1, emergencyAssessor2).map {
                        userTransformer.transformJpaToApi(it, ServiceName.approvedPremises)
                      },
                    ),
                  )
              }
            }
          }
        }
      }
    }
  }

  @Nested
  inner class GetUsersSummary {

    @ParameterizedTest
    @EnumSource(
      value = UserRole::class,
      names = ["CAS1_CRU_MEMBER", "CAS1_JANITOR", "CAS1_USER_MANAGER", "CAS1_AP_AREA_MANAGER", "CAS1_FUTURE_MANAGER"],
      mode = EnumSource.Mode.EXCLUDE,
    )
    fun `GET user summary with a role without permission CAS1_USER_SUMMARY_LIST is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/cas1/users/summary")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_JANITOR", "CAS1_USER_MANAGER", "CAS1_FUTURE_MANAGER"])
    fun `GET user summary with permission CAS1_USER_SUMMARY_LIST returns full list ordered by name`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { cruMember, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { manager, _ ->
          givenAUser { userWithNoRole, _ ->
            givenAUser(roles = listOf(role)) { requestUser, jwt ->
              webTestClient.get()
                .uri("/cas1/users/summary")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectHeader().doesNotExist("X-Pagination-CurrentPage")
                .expectHeader().doesNotExist("X-Pagination-TotalPages")
                .expectHeader().doesNotExist("X-Pagination-TotalResults")
                .expectHeader().doesNotExist("X-Pagination-PageSize")
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(requestUser, userWithNoRole, cruMember, manager).map {
                      userTransformer.transformJpaToSummaryApi(it)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET user summary with an approved role returns list filtered by region`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { cruMember, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { manager, _ ->
          givenAUser { userWithNoRole, _ ->
            givenAUser(roles = listOf(role)) { requestUser, jwt ->
              val apArea = givenAnApArea()
              val probationRegion = probationRegionEntityFactory.produceAndPersist {
                withApArea(apArea)
              }

              val userOne = userEntityFactory.produceAndPersist {
                withProbationRegion(probationRegion)
                withApArea(apArea)
              }

              val userTwo = userEntityFactory.produceAndPersist {
                withProbationRegion(probationRegion)
                withApArea(apArea)
              }

              webTestClient.get()
                .uri("/cas1/users/summary?probationRegionId=${probationRegion.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectHeader().doesNotExist("X-Pagination-CurrentPage")
                .expectHeader().doesNotExist("X-Pagination-TotalPages")
                .expectHeader().doesNotExist("X-Pagination-TotalResults")
                .expectHeader().doesNotExist("X-Pagination-PageSize")
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(userOne, userTwo).map {
                      userTransformer.transformJpaToSummaryApi(it)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns list filtered by user's AP area`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { cruMember, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { manager, _ ->
          givenAUser { userWithNoRole, _ ->
            givenAUser(roles = listOf(role)) { requestUser, jwt ->
              val apArea = givenAnApArea()

              val probationRegionApArea = givenAnApArea()
              val probationRegion = probationRegionEntityFactory.produceAndPersist {
                withApArea(probationRegionApArea)
              }

              val userOne = userEntityFactory.produceAndPersist {
                withProbationRegion(probationRegion)
                withApArea(apArea)
              }

              val userTwo = userEntityFactory.produceAndPersist {
                withProbationRegion(probationRegion)
                withApArea(apArea)
              }

              webTestClient.get()
                .uri("/cas1/users/summary?apAreaId=${apArea.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectHeader().doesNotExist("X-Pagination-CurrentPage")
                .expectHeader().doesNotExist("X-Pagination-TotalPages")
                .expectHeader().doesNotExist("X-Pagination-TotalResults")
                .expectHeader().doesNotExist("X-Pagination-PageSize")
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(userOne, userTwo).map {
                      userTransformer.transformJpaToSummaryApi(it)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns paginated list ordered by name`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { cruMember, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { manager, _ ->
          givenAUser { userWithNoRole, _ ->
            givenAUser(roles = listOf(role)) { requestUser, jwt ->
              webTestClient.get()
                .uri("/cas1/users/summary?page=1")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
                .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
                .expectHeader().valueEquals("X-Pagination-TotalResults", 4)
                .expectHeader().valueEquals("X-Pagination-PageSize", 10)
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(requestUser, userWithNoRole, cruMember, manager).map {
                      userTransformer.transformJpaToSummaryApi(it)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role allows filtering by roles`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { reportViewer, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER, UserRole.CAS1_FUTURE_MANAGER, UserRole.CAS1_FUTURE_MANAGER)) { future_manager, _ ->
          givenAUser { _, _ ->
            givenAUser(roles = listOf(role)) { _, jwt ->
              webTestClient.get()
                .uri("/cas1/users/summary?roles=report_viewer,future_manager")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(reportViewer, future_manager).map {
                      userTransformer.transformJpaToSummaryApi(it)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @Test
    fun `GET to users with an approved role allows filtering by permission`() {
      val (user1WithRequiredPermission, _) = givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER))
      // user without required permission
      givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER))
      val (user2WithRequiredPermission, _) = givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER))
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER))

      val results = webTestClient.get()
        .uri("/cas1/users/summary?permission=cas1_keyworker_assignable_as")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<UserSummary>()

      assertThat(results).hasSize(2)
      assertThat(results.map { it.id }).contains(user1WithRequiredPermission.id)
      assertThat(results.map { it.id }).contains(user2WithRequiredPermission.id)
    }

    @Test
    fun `GET to users with an approved role allows filtering by name or email`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(
          name = PersonName(
            forename = "Lob",
            surname = "Roberts",
          ),
          email = "robert.roberts@test.com",
        ),
      )
      val (user1WithMatchingName, _) = givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(
          name = PersonName(
            forename = "Bob",
            surname = "Roberts",
          ),
          email = "robert.roberts@test.com",
        ),
      )
      val (user2WithMatchingEmail, _) = givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(
          name = PersonName(
            forename = "Jim",
            surname = "Jimmers",
          ),
          email = "jim.bob@test.com",
        ),
      )
      val (user3WithMatchingNameAndEmail, _) = givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(
          name = PersonName(
            forename = "Bobby",
            surname = "Jo",
          ),
          email = "bobby.jo@test.com",
        ),
      )

      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER))

      val results = webTestClient.get()
        .uri("/cas1/users/summary?nameOrEmail=bob")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<UserSummary>()

      assertThat(results).hasSize(3)
      assertThat(results.map { it.id }).contains(user1WithMatchingName.id)
      assertThat(results.map { it.id }).contains(user2WithMatchingEmail.id)
      assertThat(results.map { it.id }).contains(user3WithMatchingNameAndEmail.id)
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role allows filtering by qualifications`(role: UserRole) {
      givenAUser(qualifications = listOf(UserQualification.EMERGENCY)) { emergencyUser, _ ->
        givenAUser(qualifications = listOf(UserQualification.PIPE)) { _, _ ->
          givenAUser { _, _ ->
            givenAUser(roles = listOf(role)) { _, jwt ->
              webTestClient.get()
                .uri("/cas1/users/summary?qualifications=emergency")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(emergencyUser).map {
                      userTransformer.transformJpaToSummaryApi(it)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role allows filtering by role and qualifications`(
      role: UserRole,
    ) {
      givenAUser(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        qualifications = listOf(UserQualification.EMERGENCY),
      ) { emergencyAssessor1, _ ->
        givenAUser(
          roles = listOf(UserRole.CAS1_ASSESSOR),
          qualifications = listOf(UserQualification.EMERGENCY),
        ) { emergencyAssessor2, _ ->
          givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR)) { _, _ ->
            givenAUser { _, _ ->
              givenAUser(roles = listOf(role)) { _, jwt ->
                webTestClient.get()
                  .uri("/cas1/users/summary?roles=assessor&qualifications=emergency")
                  .header("Authorization", "Bearer $jwt")
                  .exchange()
                  .expectStatus()
                  .isOk
                  .expectBody()
                  .json(
                    objectMapper.writeValueAsString(
                      listOf(emergencyAssessor1, emergencyAssessor2).map {
                        userTransformer.transformJpaToSummaryApi(it)
                      },
                    ),
                  )
              }
            }
          }
        }
      }
    }
  }

  @Nested
  inner class SearchByUserName {

    @ParameterizedTest
    @EnumSource(
      value = UserRole::class,
      names = ["CAS1_CRU_MEMBER", "CAS1_JANITOR", "CAS1_USER_MANAGER", "CAS1_AP_AREA_MANAGER"],
      mode = EnumSource.Mode.EXCLUDE,
    )
    fun `GET to user search with an unapproved role is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/cas1/users/search?name=some")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `GET to user search with no internal role (aka the Applicant pseudo-role) is forbidden`() {
      givenAUser { _, jwt ->
        webTestClient.get()
          .uri("/cas1/users/search?name=some")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to search users with approved role returns a user`(role: UserRole) {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(name = PersonName(forename = "SomeUserName", surname = "")),
      ) { user, _ ->
        givenAUser(
          staffDetail = StaffDetailFactory.staffDetail(name = PersonName(forename = "fail", surname = "")),
        ) { _, _ ->
          givenAUser(roles = listOf(role)) { _, jwt ->
            webTestClient.get()
              .uri("/cas1/users/search?name=some")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(user).map {
                    userTransformer.transformJpaToApi(it, ServiceName.approvedPremises)
                  },
                ),
              )
          }
        }
      }
    }
  }

  @Nested
  inner class SearchByDeliusUserName {

    @ParameterizedTest
    @EnumSource(
      value = UserRole::class,
      names = ["CAS1_CRU_MEMBER", "CAS1_JANITOR", "CAS1_USER_MANAGER", "CAS1_AP_AREA_MANAGER"],
      mode = EnumSource.Mode.EXCLUDE,
    )
    fun `GET user search delius username with an unapproved role is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/cas1/users/delius?name=some")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `GET to user search delius username with no internal role (aka the Applicant pseudo-role) is forbidden`() {
      givenAUser { _, jwt ->
        webTestClient.get()
          .uri("/cas1/users/delius?name=some")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to search users delius username with an approved role returns a user`(role: UserRole) {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "SOME"),
      ) { user, _ ->
        givenAUser(
          staffDetail = StaffDetailFactory.staffDetail(name = PersonName(forename = "fail", surname = "r")),
        ) { _, _ ->
          givenAUser(roles = listOf(role)) { _, jwt ->
            webTestClient.get()
              .uri("/cas1/users/delius?name=some")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  userTransformer.transformJpaToApi(user, ServiceName.approvedPremises),
                ),
              )
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to search for a delius username that does not exist with an approved role returns 404`(
      role: UserRole,
    ) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/cas1/users/delius?name=noone")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectHeader().contentType("application/problem+json")
          .expectStatus()
          .isNotFound
          .expectBody()
          .jsonPath("title").isEqualTo("Not Found")
          .jsonPath("status").isEqualTo(404)
          .jsonPath("detail").isEqualTo("No user with username of noone could be found")
      }
    }
  }
}
