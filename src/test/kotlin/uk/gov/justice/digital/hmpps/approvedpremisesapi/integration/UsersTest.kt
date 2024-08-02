package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationDeliveryUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserRolesAndQualifications
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserTeamMembershipFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.KeyValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification as APIUserQualification

class UsersTest : InitialiseDatabasePerClassTestBase() {
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
      authSource = "other-auth-source",
    )

    webTestClient.get()
      .uri("/users/$id")
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
      .uri("/users/$id")
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
      roles = listOf("ROLE_PROBATION"),
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
        .produce(),
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
      .jsonPath("detail").isEqualTo("Missing required header X-Service-Name")
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
      roles = listOf("ROLE_PROBATION"),
    )

    val apArea = apAreaEntityFactory.produceAndPersist()

    val region = probationRegionEntityFactory.produceAndPersist {
      withYieldedApArea { apArea }
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
        .produce(),
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
            service = "CAS1",
            isActive = true,
            apArea = ApArea(apArea.id, apArea.identifier, apArea.name),
            permissions = emptyList(),
          ),
        ),
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
    val brought = KeyValue(
      code = randomStringMultiCaseWithNumbers(7),
      description = randomStringMultiCaseWithNumbers(10),
    )

    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = deliusUsername,
      authSource = "delius",
      roles = listOf("ROLE_PROBATION"),
    )

    val region = probationRegionEntityFactory.produceAndPersist {
      withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
    }

    val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
      withDeliusCode(brought.code)
      withName(brought.description)
      withProbationRegion(region)
    }

    userEntityFactory.produceAndPersist {
      withId(id)
      withDeliusUsername(deliusUsername)
      withName(name)
      withEmail(email)
      withTelephoneNumber(telephoneNumber)
      withYieldedProbationRegion { region }
      withProbationDeliveryUnit { probationDeliveryUnit }
    }

    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withForenames(forename)
        .withSurname(surname)
        .withUsername(deliusUsername)
        .withEmail(email)
        .withTelephoneNumber(telephoneNumber)
        .withTeams(
          listOf(StaffUserTeamMembershipFactory().withBorough(brought).produce()),
        )
        .produce(),
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
            probationDeliveryUnit = ProbationDeliveryUnit(probationDeliveryUnit.id, probationDeliveryUnit.name),
            deliusUsername = deliusUsername,
            name = name,
            email = email,
            telephoneNumber = telephoneNumber,
            roles = emptyList(),
            service = "CAS3",
            isActive = true,
          ),
        ),
      )
  }

  @Nested
  inner class GetUsers {
    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_ADMIN", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with X-Service-Name other than approved-premises is forbidden`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/users")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_ADMIN", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"], mode = EnumSource.Mode.EXCLUDE)
    fun `GET to users with an unapproved role is forbidden`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/users")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `GET to users with no internal role (aka the Applicant pseudo-role) is forbidden`() {
      `Given a User` { _, jwt ->
        webTestClient.get()
          .uri("/users")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_ADMIN", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns full list ordered by name`(role: UserRole) {
      `Given a User`(roles = listOf(UserRole.CAS1_MATCHER)) { matcher, _ ->
        `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { manager, _ ->
          `Given a User` { userWithNoRole, _ ->
            `Given a User`(roles = listOf(role)) { requestUser, jwt ->
              webTestClient.get()
                .uri("/users")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
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
                    listOf(requestUser, userWithNoRole, matcher, manager).map {
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
    @EnumSource(value = UserRole::class, names = ["CAS1_ADMIN", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns list filtered by region`(role: UserRole) {
      `Given a User`(roles = listOf(UserRole.CAS1_MATCHER)) { matcher, _ ->
        `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { manager, _ ->
          `Given a User` { userWithNoRole, _ ->
            `Given a User`(roles = listOf(role)) { requestUser, jwt ->
              val apArea = apAreaEntityFactory.produceAndPersist()
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
                .uri("/users?probationRegionId=${probationRegion.id}")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
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
    @EnumSource(value = UserRole::class, names = ["CAS1_ADMIN", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns list filtered by user's AP area`(role: UserRole) {
      `Given a User`(roles = listOf(UserRole.CAS1_MATCHER)) { matcher, _ ->
        `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { manager, _ ->
          `Given a User` { userWithNoRole, _ ->
            `Given a User`(roles = listOf(role)) { requestUser, jwt ->
              val apArea = apAreaEntityFactory.produceAndPersist()

              val probationRegionApArea = apAreaEntityFactory.produceAndPersist()
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
                .uri("/users?apAreaId=${apArea.id}")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
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
    @EnumSource(value = UserRole::class, names = ["CAS1_ADMIN", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns paginated list ordered by name`(role: UserRole) {
      `Given a User`(roles = listOf(UserRole.CAS1_MATCHER)) { matcher, _ ->
        `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { manager, _ ->
          `Given a User` { userWithNoRole, _ ->
            `Given a User`(roles = listOf(role)) { requestUser, jwt ->
              webTestClient.get()
                .uri("/users?page=1")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
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
                    listOf(requestUser, userWithNoRole, matcher, manager).map {
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
    @EnumSource(value = UserRole::class, names = ["CAS1_ADMIN", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role allows filtering by roles`(role: UserRole) {
      `Given a User`(roles = listOf(UserRole.CAS1_MATCHER, UserRole.CAS1_MATCHER)) { matcher, _ ->
        `Given a User`(roles = listOf(UserRole.CAS1_MANAGER, UserRole.CAS1_MANAGER, UserRole.CAS1_MANAGER)) { manager, _ ->
          `Given a User` { _, _ ->
            `Given a User`(roles = listOf(role)) { _, jwt ->
              webTestClient.get()
                .uri("/users?roles=matcher,manager")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(matcher, manager).map {
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
    @EnumSource(value = UserRole::class, names = ["CAS1_ADMIN", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role allows filtering by qualifications`(role: UserRole) {
      `Given a User`(qualifications = listOf(UserQualification.WOMENS)) { womensUser, _ ->
        `Given a User`(qualifications = listOf(UserQualification.PIPE)) { _, _ ->
          `Given a User` { _, _ ->
            `Given a User`(roles = listOf(role)) { _, jwt ->
              webTestClient.get()
                .uri("/users?qualifications=womens")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(womensUser).map {
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
    @EnumSource(value = UserRole::class, names = ["CAS1_ADMIN", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role allows filtering by role and qualifications`(
      role: UserRole,
    ) {
      `Given a User`(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        qualifications = listOf(UserQualification.WOMENS),
      ) { womensAssessor1, _ ->
        `Given a User`(
          roles = listOf(UserRole.CAS1_ASSESSOR),
          qualifications = listOf(UserQualification.WOMENS),
        ) { womensAssessor2, _ ->
          `Given a User`(roles = listOf(UserRole.CAS1_ASSESSOR)) { _, _ ->
            `Given a User` { _, _ ->
              `Given a User`(roles = listOf(role)) { _, jwt ->
                webTestClient.get()
                  .uri("/users?roles=assessor&qualifications=womens")
                  .header("Authorization", "Bearer $jwt")
                  .header("X-Service-Name", ServiceName.approvedPremises.value)
                  .exchange()
                  .expectStatus()
                  .isOk
                  .expectBody()
                  .json(
                    objectMapper.writeValueAsString(
                      listOf(womensAssessor1, womensAssessor2).map {
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
  inner class SearchByUserName {
    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_ADMIN", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to user search with X-Service-Name other than approved-premises is forbidden`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/users/search?name=som")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_ADMIN", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"], mode = EnumSource.Mode.EXCLUDE)
    fun `GET to user search with an unapproved role is forbidden`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/users/search?name=some")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `GET to user search with no internal role (aka the Applicant pseudo-role) is forbidden`() {
      `Given a User`() { _, jwt ->
        webTestClient.get()
          .uri("/users/search?name=some")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_ADMIN", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to search users with approved role returns a user`(role: UserRole) {
      `Given a User`(
        staffUserDetailsConfigBlock = {
          withForenames("SomeUserName")
        },
      ) { user, _ ->
        `Given a User`(
          staffUserDetailsConfigBlock = {
            withForenames("fail")
          },
        ) { _, _ ->
          `Given a User`(roles = listOf(role)) { _, jwt ->
            webTestClient.get()
              .uri("/users/search?name=some")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
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
    @EnumSource(value = UserRole::class, names = ["CAS1_ADMIN", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to user search delius username with X-Service-Name other than approved-premises is forbidden`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/users/delius?name=som")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_ADMIN", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"], mode = EnumSource.Mode.EXCLUDE)
    fun `GET to user search delius username with an unapproved role is forbidden`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/users/delius?name=some")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `GET to user search delius username with no internal role (aka the Applicant pseudo-role) is forbidden`() {
      `Given a User`() { _, jwt ->
        webTestClient.get()
          .uri("/users/delius?name=some")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_ADMIN", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to search users delius username with an approved role returns a user`(role: UserRole) {
      `Given a User`(
        staffUserDetailsConfigBlock = {
          withUsername("SOME")
        },
      ) { user, _ ->
        `Given a User`(
          staffUserDetailsConfigBlock = {
            withForenames("fail")
          },
        ) { _, _ ->
          `Given a User`(roles = listOf(role)) { _, jwt ->
            webTestClient.get()
              .uri("/users/delius?name=some")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
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
    @EnumSource(value = UserRole::class, names = ["CAS1_ADMIN", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to search for a delius username that does not exist with an approved role returns 404`(
      role: UserRole,
    ) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/users/delius?name=noone")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
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

  @Nested
  inner class UpdateUser {
    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_ADMIN", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `Updating a user update with X-Service-Name other than approved-premises is forbidden`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        val id = UUID.randomUUID()
        webTestClient.put()
          .uri("/users/$id")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .header("Content-Type", "application/json")
          .bodyValue(
            UserRolesAndQualifications(
              listOf<ApprovedPremisesUserRole>(),
              listOf<APIUserQualification>(),
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_MATCHER", "CAS1_JANITOR", "CAS1_USER_MANAGER"], mode = EnumSource.Mode.EXCLUDE)
    fun `Updating a user without an approved role is forbidden`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        val id = UUID.randomUUID()
        webTestClient.put()
          .uri("/users/$id")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .header("Content-Type", "application/json")
          .bodyValue(
            UserRolesAndQualifications(
              listOf<ApprovedPremisesUserRole>(),
              listOf<APIUserQualification>(),
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Updating a users with no internal role (aka the Applicant pseudo-role) is forbidden`() {
      `Given a User`() { _, jwt ->
        val id = UUID.randomUUID()
        webTestClient.put()
          .uri("/users/$id")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .bodyValue(
            UserRolesAndQualifications(
              listOf<ApprovedPremisesUserRole>(),
              listOf<APIUserQualification>(),
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_ADMIN", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `Updating a user returns OK with correct body when user has an approved role`(role: UserRole) {
      val id = UUID.randomUUID()
      val qualifications = listOf(APIUserQualification.emergency, APIUserQualification.pipe)
      val roles = listOf(
        ApprovedPremisesUserRole.assessor,
        ApprovedPremisesUserRole.reportViewer,
        ApprovedPremisesUserRole.excludedFromAssessAllocation,
        ApprovedPremisesUserRole.excludedFromMatchAllocation,
        ApprovedPremisesUserRole.excludedFromPlacementApplicationAllocation,
      )
      val region = probationRegionEntityFactory.produceAndPersist {
        withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
      }

      userEntityFactory.produceAndPersist {
        withId(id)
        withIsActive(false)
        withYieldedProbationRegion { region }
        withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
      }

      `Given a User`(roles = listOf(role)) { _, jwt ->
        webTestClient.put()
          .uri("/users/$id")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .bodyValue(
            UserRolesAndQualifications(
              roles = roles,
              qualifications = qualifications,
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
      }
    }
  }

  @Nested
  inner class DeleteUser {
    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_ADMIN", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `Deleting a user with X-Service-Name other than approved-premises is forbidden`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        val id = UUID.randomUUID()
        webTestClient.delete()
          .uri("/users/$id")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_ADMIN", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"], mode = EnumSource.Mode.EXCLUDE)
    fun `Deleting a user with an unapproved role is forbidden`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        val id = UUID.randomUUID()
        webTestClient.delete()
          .uri("/users/$id")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Deleting a user with no internal role (aka the Applicant pseudo-role) is forbidden`() {
      `Given a User`() { _, jwt ->
        val id = UUID.randomUUID()
        webTestClient.delete()
          .uri("/users/$id")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_ADMIN", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `Deleting a user with no X-Service-Name is forbidden`() {
      `Given a User`() { _, jwt ->
        val id = UUID.randomUUID()
        webTestClient.delete()
          .uri("/users/$id")
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
    fun `Deleting a user with a non-Delius JWT returns 403`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
      )

      webTestClient.delete()
        .uri("/users/$id")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_ADMIN", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `Deleting a user with an approved role deletes successfully`(role: UserRole) {
      userEntityFactory.produceAndPersist {
        withId(id)

        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist {
            withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
          }
        }
      }

      `Given a User`(roles = listOf(role)) { _, jwt ->
        webTestClient.delete()
          .uri("/users/$id")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk

        val userFromDatabase = userRepository.findByIdOrNull(id)
        assertThat(userFromDatabase?.isActive).isEqualTo(false)
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_ADMIN", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"], mode = EnumSource.Mode.EXCLUDE)
    fun `Deleting a user without an approved role is forbidden `(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        webTestClient.delete()
          .uri("/users/$id")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }
}
