package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.PersonName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer

class UsersTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var userTransformer: UserTransformer

  @Nested
  inner class GetUsers {
    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with X-Service-Name other than approved-premises is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
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
    @EnumSource(
      value = UserRole::class,
      names = ["CAS1_CRU_MEMBER", "CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"],
      mode = EnumSource.Mode.EXCLUDE,
    )
    fun `GET users with an unapproved role is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
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
    fun `GET users with no internal role (aka the Applicant pseudo-role) is forbidden`() {
      givenAUser { _, jwt ->
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
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns full list ordered by name`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER)) { matcher, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { manager, _ ->
          givenAUser { userWithNoRole, _ ->
            givenAUser(roles = listOf(role)) { requestUser, jwt ->
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
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns list filtered by region`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER)) { matcher, _ ->
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
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns list filtered by user's AP area`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER)) { matcher, _ ->
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
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns list filtered by user's CRU management area`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER)) { matcher, _ ->
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
                .uri("/users?cruManagementAreaId=${cruManagementArea.id}")
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
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns paginated list ordered by name`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER)) { matcher, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { manager, _ ->
          givenAUser { userWithNoRole, _ ->
            givenAUser(roles = listOf(role)) { requestUser, jwt ->
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
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role allows filtering by roles`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER, UserRole.CAS1_MATCHER)) { matcher, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER, UserRole.CAS1_FUTURE_MANAGER, UserRole.CAS1_FUTURE_MANAGER)) { future_manager, _ ->
          givenAUser { _, _ ->
            givenAUser(roles = listOf(role)) { _, jwt ->
              webTestClient.get()
                .uri("/users?roles=matcher,future_manager")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(matcher, future_manager).map {
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
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role allows filtering by qualifications`(role: UserRole) {
      givenAUser(qualifications = listOf(UserQualification.EMERGENCY)) { emergencyUser, _ ->
        givenAUser(qualifications = listOf(UserQualification.PIPE)) { _, _ ->
          givenAUser { _, _ ->
            givenAUser(roles = listOf(role)) { _, jwt ->
              webTestClient.get()
                .uri("/users?qualifications=emergency")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
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
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
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
                  .uri("/users?roles=assessor&qualifications=emergency")
                  .header("Authorization", "Bearer $jwt")
                  .header("X-Service-Name", ServiceName.approvedPremises.value)
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
      names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"],
    )
    fun `GET user summary with X-Service-Name other than approved-premises is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/users/summary")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(
      value = UserRole::class,
      names = ["CAS1_CRU_MEMBER", "CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"],
      mode = EnumSource.Mode.EXCLUDE,
    )
    fun `GET user summary with an unapproved role is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/users/summary")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `GET user summary with no internal role (aka the Applicant pseudo-role) is forbidden`() {
      givenAUser { _, jwt ->
        webTestClient.get()
          .uri("/users/summary")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET user summary with an approved role returns full list ordered by name`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER)) { matcher, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { manager, _ ->
          givenAUser { userWithNoRole, _ ->
            givenAUser(roles = listOf(role)) { requestUser, jwt ->
              webTestClient.get()
                .uri("/users/summary")
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
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET user summary with an approved role returns list filtered by region`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER)) { matcher, _ ->
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
                .uri("/users/summary?probationRegionId=${probationRegion.id}")
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
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns list filtered by user's AP area`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER)) { matcher, _ ->
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
                .uri("/users/summary?apAreaId=${apArea.id}")
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
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns paginated list ordered by name`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER)) { matcher, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { manager, _ ->
          givenAUser { userWithNoRole, _ ->
            givenAUser(roles = listOf(role)) { requestUser, jwt ->
              webTestClient.get()
                .uri("/users/summary?page=1")
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
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role allows filtering by roles`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER, UserRole.CAS1_MATCHER)) { matcher, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER, UserRole.CAS1_FUTURE_MANAGER, UserRole.CAS1_FUTURE_MANAGER)) { future_manager, _ ->
          givenAUser { _, _ ->
            givenAUser(roles = listOf(role)) { _, jwt ->
              webTestClient.get()
                .uri("/users/summary?roles=matcher,future_manager")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(matcher, future_manager).map {
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
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role allows filtering by qualifications`(role: UserRole) {
      givenAUser(qualifications = listOf(UserQualification.EMERGENCY)) { emergencyUser, _ ->
        givenAUser(qualifications = listOf(UserQualification.PIPE)) { _, _ ->
          givenAUser { _, _ ->
            givenAUser(roles = listOf(role)) { _, jwt ->
              webTestClient.get()
                .uri("/users/summary?qualifications=emergency")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
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
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
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
                  .uri("/users/summary?roles=assessor&qualifications=emergency")
                  .header("Authorization", "Bearer $jwt")
                  .header("X-Service-Name", ServiceName.approvedPremises.value)
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
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to user search with X-Service-Name other than approved-premises is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
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
    @EnumSource(
      value = UserRole::class,
      names = ["CAS1_CRU_MEMBER", "CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"],
      mode = EnumSource.Mode.EXCLUDE,
    )
    fun `GET to user search with an unapproved role is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
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
      givenAUser { _, jwt ->
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
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to search users with approved role returns a user`(role: UserRole) {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(name = PersonName(forename = "SomeUserName", surname = "")),
      ) { user, _ ->
        givenAUser(
          staffDetail = StaffDetailFactory.staffDetail(name = PersonName(forename = "fail", surname = "")),
        ) { _, _ ->
          givenAUser(roles = listOf(role)) { _, jwt ->
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
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to user search delius username with X-Service-Name other than approved-premises is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
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
    @EnumSource(
      value = UserRole::class,
      names = ["CAS1_CRU_MEMBER", "CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"],
      mode = EnumSource.Mode.EXCLUDE,
    )
    fun `GET user search delius username with an unapproved role is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
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
      givenAUser { _, jwt ->
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
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to search users delius username with an approved role returns a user`(role: UserRole) {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "SOME"),
      ) { user, _ ->
        givenAUser(
          staffDetail = StaffDetailFactory.staffDetail(name = PersonName(forename = "fail", surname = "r")),
        ) { _, _ ->
          givenAUser(roles = listOf(role)) { _, jwt ->
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
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to search for a delius username that does not exist with an approved role returns 404`(
      role: UserRole,
    ) {
      givenAUser(roles = listOf(role)) { _, jwt ->
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
}
