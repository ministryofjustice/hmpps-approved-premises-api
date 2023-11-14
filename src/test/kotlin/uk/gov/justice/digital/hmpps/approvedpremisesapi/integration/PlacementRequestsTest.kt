package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementRequestBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Placement Request`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Application`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockOffenderUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestDetailTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestTransformer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class PlacementRequestsTest : IntegrationTestBase() {

  @Autowired
  lateinit var placementRequestTransformer: PlacementRequestTransformer

  @Autowired
  lateinit var realPlacementRequestRepository: PlacementRequestRepository

  @Nested
  inner class AllPlacementRequests {
    @Test
    fun `Get all placement requests without JWT returns 401`() {
      webTestClient.get()
        .uri("/placement-requests")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `It returns all the placement requests for a user`() {
      `Given a User` { user, jwt ->
        `Given an Offender` { offenderDetails1, inmateDetails1 ->
          `Given an Offender` { offenderDetails2, _ ->
            val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            }

            val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            }

            val application1 = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails1.otherIds.crn)
              withCreatedByUser(user)
              withApplicationSchema(applicationSchema)
              withSubmittedAt(OffsetDateTime.now())
              withReleaseType("licence")
            }

            val assessment1 = approvedPremisesAssessmentEntityFactory.produceAndPersist {
              withAssessmentSchema(assessmentSchema)
              withApplication(application1)
              withSubmittedAt(OffsetDateTime.now())
              withAllocatedToUser(user)
              withDecision(AssessmentDecision.ACCEPTED)
            }

            val application2 = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails2.otherIds.crn)
              withCreatedByUser(user)
              withSubmittedAt(OffsetDateTime.now())
              withApplicationSchema(applicationSchema)
              withReleaseType("licence")
            }

            val assessment2 = approvedPremisesAssessmentEntityFactory.produceAndPersist {
              withAssessmentSchema(assessmentSchema)
              withApplication(application2)
              withSubmittedAt(OffsetDateTime.now())
              withAllocatedToUser(user)
              withDecision(AssessmentDecision.ACCEPTED)
            }

            val placementRequirements = placementRequirementsFactory.produceAndPersist {
              withApplication(application1)
              withAssessment(assessment1)
              withPostcodeDistrict(postCodeDistrictFactory.produceAndPersist())
              withDesirableCriteria(
                characteristicEntityFactory.produceAndPersistMultiple(5),
              )
              withEssentialCriteria(
                characteristicEntityFactory.produceAndPersistMultiple(3),
              )
            }

            val postcodeDistrict = postCodeDistrictRepository.findAll()[0]

            val placementRequest = placementRequestFactory.produceAndPersist {
              withAllocatedToUser(user)
              withApplication(application1)
              withAssessment(assessment1)
              withPlacementRequirements(placementRequirements)
            }

            placementRequestFactory.produceAndPersistMultiple(2) {
              withAllocatedToUser(user)
              withReallocatedAt(OffsetDateTime.now())
              withApplication(application2)
              withAssessment(assessment2)
              withPlacementRequirements(placementRequirements)
            }

            webTestClient.get()
              .uri("/placement-requests")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    placementRequestTransformer.transformJpaToApi(
                      placementRequest,
                      PersonInfoResult.Success.Full(offenderDetails1.otherIds.crn, offenderDetails1, inmateDetails1),
                    ),
                  ),
                ),
              )
          }
        }
      }
    }
  }

  @Nested
  inner class Dashboard {
    @Test
    fun `Get dashboard without JWT returns 401`() {
      webTestClient.get()
        .uri("/placement-requests/dashboard")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER"], mode = EnumSource.Mode.EXCLUDE)
    fun `Get dashboard without for non-manager returns 401`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/placement-requests/dashboard")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `It returns the unmatched placement requests by default when the user is a manager`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given an Offender` { unmatchedOffender, unmatchedInmate ->
          `Given a Placement Request`(
            placementRequestAllocatedTo = user,
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = unmatchedOffender.otherIds.crn,
          ) { unmatchedPlacementRequest, _ ->
            webTestClient.get()
              .uri("/placement-requests/dashboard")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    placementRequestTransformer.transformJpaToApi(
                      unmatchedPlacementRequest,
                      PersonInfoResult.Success.Full(unmatchedOffender.otherIds.crn, unmatchedOffender, unmatchedInmate),
                    ),
                  ),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `It returns the unmatched placement requests when the user is a manager`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given an Offender` { unmatchedOffender, unmatchedInmate ->
          `Given a Placement Request`(
            placementRequestAllocatedTo = user,
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = unmatchedOffender.otherIds.crn,
          ) { unmatchedPlacementRequest, _ ->
            webTestClient.get()
              .uri("/placement-requests/dashboard?status=notMatched")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    placementRequestTransformer.transformJpaToApi(
                      unmatchedPlacementRequest,
                      PersonInfoResult.Success.Full(unmatchedOffender.otherIds.crn, unmatchedOffender, unmatchedInmate),
                    ),
                  ),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `It returns the matched placement requests when the user is a manager`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given an Offender` { matchedOffender, matchedInmate ->
          `Given a Placement Request`(
            placementRequestAllocatedTo = user,
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = matchedOffender.otherIds.crn,
          ) { matchedPlacementRequest, _ ->
            val premises = approvedPremisesEntityFactory.produceAndPersist {
              withProbationRegion(
                probationRegionEntityFactory.produceAndPersist {
                  withApArea(apAreaEntityFactory.produceAndPersist())
                },
              )
              withLocalAuthorityArea(
                localAuthorityEntityFactory.produceAndPersist(),
              )
            }

            val room = roomEntityFactory.produceAndPersist {
              withPremises(premises)
            }

            val bed = bedEntityFactory.produceAndPersist {
              withRoom(room)
            }

            matchedPlacementRequest.booking = bookingEntityFactory.produceAndPersist {
              withPremises(premises)
              withBed(bed)
            }

            realPlacementRequestRepository.save(matchedPlacementRequest)

            webTestClient.get()
              .uri("/placement-requests/dashboard?status=matched")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    placementRequestTransformer.transformJpaToApi(
                      matchedPlacementRequest,
                      PersonInfoResult.Success.Full(matchedOffender.otherIds.crn, matchedOffender, matchedInmate),
                    ),
                  ),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `It returns the unable to match placement requests when the user is a manager`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given an Offender` { unableToMatchOffender, unableToMatchInmate ->
          `Given a Placement Request`(
            placementRequestAllocatedTo = user,
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = unableToMatchOffender.otherIds.crn,
          ) { unableToMatchPlacementRequest, _ ->
            unableToMatchPlacementRequest.bookingNotMades = mutableListOf(
              bookingNotMadeFactory.produceAndPersist {
                withPlacementRequest(unableToMatchPlacementRequest)
              },
            )

            webTestClient.get()
              .uri("/placement-requests/dashboard?status=unableToMatch")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    placementRequestTransformer.transformJpaToApi(
                      unableToMatchPlacementRequest,
                      PersonInfoResult.Success.Full(unableToMatchOffender.otherIds.crn, unableToMatchOffender, unableToMatchInmate),
                    ),
                  ),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `It returns paginated placement requests when the user is a manager`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val placementRequest = createPlacementRequest(offenderDetails, user)

          webTestClient.get()
            .uri("/placement-requests/dashboard?page=1")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 1)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                listOf(
                  placementRequestTransformer.transformJpaToApi(
                    placementRequest,
                    PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                  ),
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `It searches by name when the user is a manager`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given an Offender`(offenderDetailsConfigBlock = {
          withFirstName("JOHN")
          withLastName("SMITH")
        },) { offenderDetails, inmateDetails ->
          `Given an Offender` { otherOffenderDetails, _ ->
            val placementRequest = createPlacementRequest(offenderDetails, user)
            createPlacementRequest(otherOffenderDetails, user)

            webTestClient.get()
              .uri("/placement-requests/dashboard?crnOrName=john")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    placementRequestTransformer.transformJpaToApi(
                      placementRequest,
                      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                    ),
                  ),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `It searches by crnOrName by CRN when the user is a manager`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given an Offender` { otherOffenderDetails, _ ->
          `Given an Offender` { offenderDetails, inmateDetails ->
            val placementRequest = createPlacementRequest(offenderDetails, user)
            createPlacementRequest(otherOffenderDetails, user)

            webTestClient.get()
              .uri("/placement-requests/dashboard?crnOrName=${offenderDetails.otherIds.crn}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    placementRequestTransformer.transformJpaToApi(
                      placementRequest,
                      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                    ),
                  ),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `It sorts by duration when the user is a manager`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val placementRequestWithOneDayDuration = createPlacementRequest(offenderDetails, user, duration = 1)
          val placementRequestWithFiveDayDuration = createPlacementRequest(offenderDetails, user, duration = 5)
          val placementRequestWithTwelveDayDuration = createPlacementRequest(offenderDetails, user, duration = 12)

          webTestClient.get()
            .uri("/placement-requests/dashboard?page=1&sortBy=duration")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequestWithOneDayDuration.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequestWithFiveDayDuration.id.toString())
            .jsonPath("$[2].id").isEqualTo(placementRequestWithTwelveDayDuration.id.toString())

          webTestClient.get()
            .uri("/placement-requests/dashboard?page=1&sortBy=duration&sortDirection=desc")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequestWithTwelveDayDuration.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequestWithFiveDayDuration.id.toString())
            .jsonPath("$[2].id").isEqualTo(placementRequestWithOneDayDuration.id.toString())
        }
      }
    }

    @Test
    fun `It sorts by expectedArrival when the user is a manager`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val placementRequestWithExpectedArrivalOfToday = createPlacementRequest(offenderDetails, user)
          val placementRequestWithExpectedArrivalInTwelveDays = createPlacementRequest(offenderDetails, user, expectedArrival = LocalDate.now().plusDays(12))
          val placementRequestWithExpectedArrivalInThirtyDays = createPlacementRequest(offenderDetails, user, expectedArrival = LocalDate.now().plusDays(30))

          webTestClient.get()
            .uri("/placement-requests/dashboard?page=1&sortBy=expected_arrival")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequestWithExpectedArrivalOfToday.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequestWithExpectedArrivalInTwelveDays.id.toString())
            .jsonPath("$[2].id").isEqualTo(placementRequestWithExpectedArrivalInThirtyDays.id.toString())

          webTestClient.get()
            .uri("/placement-requests/dashboard?page=1&sortBy=expected_arrival&sortDirection=desc")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequestWithExpectedArrivalInThirtyDays.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequestWithExpectedArrivalInTwelveDays.id.toString())
            .jsonPath("$[2].id").isEqualTo(placementRequestWithExpectedArrivalOfToday.id.toString())
        }
      }
    }

    @Test
    fun `It sorts by createdAt when the user is a manager`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val placementRequestCreatedToday = createPlacementRequest(offenderDetails, user)
          val placementRequestCreatedFiveDaysAgo = createPlacementRequest(offenderDetails, user, createdAt = OffsetDateTime.now().minusDays(5))
          val placementRequestCreatedThirtyDaysAgo = createPlacementRequest(offenderDetails, user, createdAt = OffsetDateTime.now().minusDays(30))

          webTestClient.get()
            .uri("/placement-requests/dashboard?page=1&sortBy=created_at")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequestCreatedThirtyDaysAgo.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequestCreatedFiveDaysAgo.id.toString())
            .jsonPath("$[2].id").isEqualTo(placementRequestCreatedToday.id.toString())

          webTestClient.get()
            .uri("/placement-requests/dashboard?page=1&sortBy=created_at&sortDirection=desc")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequestCreatedToday.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequestCreatedFiveDaysAgo.id.toString())
            .jsonPath("$[2].id").isEqualTo(placementRequestCreatedThirtyDaysAgo.id.toString())
        }
      }
    }

    @Test
    fun `It sorts by applicationSubmittedAt when the user is a manager`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val placementRequestWithApplicationCreatedToday = createPlacementRequest(offenderDetails, user)
          val placementRequestWithApplicationCreatedTwelveDaysAgo = createPlacementRequest(offenderDetails, user, applicationDate = OffsetDateTime.now().minusDays(12))
          val placementRequestWithApplicationCreatedThirtyDaysAgo = createPlacementRequest(offenderDetails, user, applicationDate = OffsetDateTime.now().minusDays(30))

          webTestClient.get()
            .uri("/placement-requests/dashboard?page=1&sortBy=application_date")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequestWithApplicationCreatedThirtyDaysAgo.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequestWithApplicationCreatedTwelveDaysAgo.id.toString())
            .jsonPath("$[2].id").isEqualTo(placementRequestWithApplicationCreatedToday.id.toString())

          webTestClient.get()
            .uri("/placement-requests/dashboard?page=1&sortBy=application_date&sortDirection=desc")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequestWithApplicationCreatedToday.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequestWithApplicationCreatedTwelveDaysAgo.id.toString())
            .jsonPath("$[2].id").isEqualTo(placementRequestWithApplicationCreatedThirtyDaysAgo.id.toString())
        }
      }
    }

    private fun createPlacementRequest(offenderDetails: OffenderDetailSummary, user: UserEntity, duration: Int = 12, expectedArrival: LocalDate = LocalDate.now(), createdAt: OffsetDateTime = OffsetDateTime.now(), applicationDate: OffsetDateTime = OffsetDateTime.now()): PlacementRequestEntity {
      val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
        withPermissiveSchema()
      }

      val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
        withPermissiveSchema()
      }

      val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCrn(offenderDetails.otherIds.crn)
        withCreatedByUser(user)
        withSubmittedAt(OffsetDateTime.now())
        withApplicationSchema(applicationSchema)
        withReleaseType("licence")
        withSubmittedAt(applicationDate)
        withName("${offenderDetails.firstName} ${offenderDetails.surname}")
      }

      val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
        withAssessmentSchema(assessmentSchema)
        withApplication(application)
        withSubmittedAt(OffsetDateTime.now())
        withAllocatedToUser(user)
        withDecision(AssessmentDecision.ACCEPTED)
      }

      val placementRequirements = placementRequirementsFactory.produceAndPersist {
        withApplication(application)
        withAssessment(assessment)
        withPostcodeDistrict(postCodeDistrictFactory.produceAndPersist())
        withDesirableCriteria(
          characteristicEntityFactory.produceAndPersistMultiple(5),
        )
        withEssentialCriteria(
          characteristicEntityFactory.produceAndPersistMultiple(3),
        )
      }

      return placementRequestFactory.produceAndPersist {
        withAllocatedToUser(
          userEntityFactory.produceAndPersist {
            withProbationRegion(
              probationRegionEntityFactory.produceAndPersist {
                withApArea(apAreaEntityFactory.produceAndPersist())
              },
            )
          },
        )
        withApplication(application)
        withAssessment(assessment)
        withPlacementRequirements(placementRequirements)
        withDuration(duration)
        withExpectedArrival(expectedArrival)
        withCreatedAt(createdAt)
      }
    }
  }

  @Nested
  inner class SinglePlacementRequest {

    @Autowired
    lateinit var placementRequestDetailTransformer: PlacementRequestDetailTransformer

    @Test
    fun `Get single Placement Request without a JWT returns 401`() {
      webTestClient.get()
        .uri("/placement-requests/62faf6f4-1dac-4139-9a18-09c1b2852a0f")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get single Placement Request that is not allocated to calling User and without WORKFLOW_MANAGER role returns 403`() {
      `Given a User` { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, inmateDetails ->
            `Given an Application`(createdByUser = otherUser) {
              `Given a Placement Request`(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              ) { placementRequest, _ ->
                webTestClient.get()
                  .uri("/placement-requests/${placementRequest.id}")
                  .header("Authorization", "Bearer $jwt")
                  .exchange()
                  .expectStatus()
                  .isForbidden
              }
            }
          }
        }
      }
    }

    @Test
    fun `Get single Placement Request that is allocated to calling User returns 200`() {
      `Given a User` { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, inmateDetails ->
            `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            ) { placementRequest, _ ->
              webTestClient.get()
                .uri("/placement-requests/${placementRequest.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    placementRequestDetailTransformer.transformJpaToApi(
                      placementRequest,
                      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                      listOf(),
                    ),
                  ),
                )
            }
          }
        }
      }
    }

    @Test
    fun `Get single Placement Request that is allocated to calling User where Offender is LAO but user does not pass LAO check, does not have LAO qualification returns 200 with restricted person info`() {
      `Given a User` { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender`(
            offenderDetailsConfigBlock = {
              withCurrentExclusion(true)
            },
          ) { offenderDetails, inmateDetails ->
            `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            ) { placementRequest, _ ->
              CommunityAPI_mockOffenderUserAccessCall(
                username = user.deliusUsername,
                crn = offenderDetails.otherIds.crn,
                inclusion = false,
                exclusion = true,
              )

              webTestClient.get()
                .uri("/placement-requests/${placementRequest.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.person.type").isEqualTo("RestrictedPerson")
                .jsonPath("$.person.crn").isEqualTo(placementRequest.application.crn)
            }
          }
        }
      }
    }

    @Test
    fun `Get single Placement Request that is allocated to calling User where Offender is LAO, user does not have LAO qualification but does pass LAO check returns 200`() {
      `Given a User` { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender`(
            offenderDetailsConfigBlock = {
              withCurrentExclusion(true)
            },
          ) { offenderDetails, inmateDetails ->
            `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            ) { placementRequest, _ ->
              CommunityAPI_mockOffenderUserAccessCall(
                username = user.deliusUsername,
                crn = offenderDetails.otherIds.crn,
                inclusion = false,
                exclusion = false,
              )

              webTestClient.get()
                .uri("/placement-requests/${placementRequest.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    placementRequestDetailTransformer.transformJpaToApi(
                      placementRequest,
                      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                      listOf(),
                    ),
                  ),
                )
            }
          }
        }
      }
    }

    @Test
    fun `Get single Placement Request that is allocated to calling User where Offender is LAO, user does not pass LAO check but does have LAO qualification returns 200`() {
      `Given a User`(qualifications = listOf(UserQualification.LAO)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender`(
            offenderDetailsConfigBlock = {
              withCurrentExclusion(true)
            },
          ) { offenderDetails, inmateDetails ->
            `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            ) { placementRequest, _ ->
              CommunityAPI_mockOffenderUserAccessCall(
                username = user.deliusUsername,
                crn = offenderDetails.otherIds.crn,
                inclusion = false,
                exclusion = true,
              )

              webTestClient.get()
                .uri("/placement-requests/${placementRequest.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    placementRequestDetailTransformer.transformJpaToApi(
                      placementRequest,
                      PersonInfoResult.Success.Restricted(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber),
                      listOf(),
                    ),
                  ),
                )
            }
          }
        }
      }
    }

    @Test
    fun `Get single Placement Request that is allocated to calling User returns 200 with cancellations when they exist`() {
      `Given a User` { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, inmateDetails ->
            `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            ) { placementRequest, _ ->
              val premises = approvedPremisesEntityFactory.produceAndPersist {
                withProbationRegion(
                  probationRegionEntityFactory.produceAndPersist {
                    withApArea(apAreaEntityFactory.produceAndPersist())
                  },
                )
                withLocalAuthorityArea(
                  localAuthorityEntityFactory.produceAndPersist(),
                )
              }

              val room = roomEntityFactory.produceAndPersist {
                withPremises(premises)
              }

              val bed = bedEntityFactory.produceAndPersist {
                withRoom(room)
              }

              val booking = bookingEntityFactory.produceAndPersist {
                withPremises(premises)
                withCrn(offenderDetails.otherIds.crn)
                withBed(bed)
                withServiceName(ServiceName.approvedPremises)
                withApplication(placementRequest.application)
              }

              val cancellations = cancellationEntityFactory.produceAndPersistMultiple(2) {
                withBooking(booking)
                withReason(cancellationReasonEntityFactory.produceAndPersist())
              }

              webTestClient.get()
                .uri("/placement-requests/${placementRequest.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    placementRequestDetailTransformer.transformJpaToApi(
                      placementRequest,
                      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                      cancellations,
                    ),
                  ),
                )
            }
          }
        }
      }
    }
  }

  @Nested
  inner class CreateBookingFromPlacementRequest {
    @Test
    fun `Create a Booking from a Placement Request without a JWT returns 401`() {
      webTestClient.post()
        .uri("/placement-requests/62faf6f4-1dac-4139-9a18-09c1b2852a0f/booking")
        .bodyValue(
          NewPlacementRequestBooking(
            arrivalDate = LocalDate.parse("2023-03-29"),
            departureDate = LocalDate.parse("2023-04-01"),
            bedId = UUID.fromString("d5dfd808-b8f4-4cc0-a0ac-fdce7144126e"),
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Creating a Booking from a Placement Request that is not allocated to the User returns a 403`() {
      `Given a User` { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, inmateDetails ->
            `Given an Application`(createdByUser = otherUser) {
              `Given a Placement Request`(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              ) { placementRequest, _ ->
                webTestClient.post()
                  .uri("/placement-requests/${placementRequest.id}/booking")
                  .header("Authorization", "Bearer $jwt")
                  .bodyValue(
                    NewPlacementRequestBooking(
                      arrivalDate = LocalDate.parse("2023-03-29"),
                      departureDate = LocalDate.parse("2023-04-01"),
                      bedId = UUID.fromString("d5dfd808-b8f4-4cc0-a0ac-fdce7144126e"),
                    ),
                  )
                  .exchange()
                  .expectStatus()
                  .isForbidden
              }
            }
          }
        }
      }
    }

    @Test
    fun `Creating a Booking from a Placement Request that is allocated to the User returns a 200`() {
      `Given a User` { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, inmateDetails ->
            `Given an Application`(createdByUser = otherUser) {
              `Given a Placement Request`(
                placementRequestAllocatedTo = user,
                assessmentAllocatedTo = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              ) { placementRequest, _ ->
                val premises = approvedPremisesEntityFactory.produceAndPersist {
                  withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
                  withYieldedProbationRegion {
                    probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
                  }
                }

                val room = roomEntityFactory.produceAndPersist {
                  withPremises(premises)
                }

                val bed = bedEntityFactory.produceAndPersist {
                  withRoom(room)
                }

                webTestClient.post()
                  .uri("/placement-requests/${placementRequest.id}/booking")
                  .header("Authorization", "Bearer $jwt")
                  .bodyValue(
                    NewPlacementRequestBooking(
                      arrivalDate = LocalDate.parse("2023-03-29"),
                      departureDate = LocalDate.parse("2023-04-01"),
                      bedId = bed.id,
                    ),
                  )
                  .exchange()
                  .expectStatus()
                  .isOk
              }
            }
          }
        }
      }
    }

    @Test
    fun `Creating a Booking from a Placement Request that is allocated to the User and a premisesId is specified returns a 200`() {
      `Given a User` { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, inmateDetails ->
            `Given an Application`(createdByUser = otherUser) {
              `Given a Placement Request`(
                placementRequestAllocatedTo = user,
                assessmentAllocatedTo = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              ) { placementRequest, _ ->
                val premises = approvedPremisesEntityFactory.produceAndPersist {
                  withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
                  withYieldedProbationRegion {
                    probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
                  }
                }

                webTestClient.post()
                  .uri("/placement-requests/${placementRequest.id}/booking")
                  .header("Authorization", "Bearer $jwt")
                  .bodyValue(
                    NewPlacementRequestBooking(
                      arrivalDate = LocalDate.parse("2023-03-29"),
                      departureDate = LocalDate.parse("2023-04-01"),
                      bedId = null,
                      premisesId = premises.id,
                    ),
                  )
                  .exchange()
                  .expectStatus()
                  .isOk
              }
            }
          }
        }
      }
    }

    @Test
    fun `Creating a Booking from a Placement Request that is not allocated to the User and the user is a Workflow Manager returns a 200`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, inmateDetails ->
            `Given an Application`(createdByUser = otherUser) {
              `Given a Placement Request`(
                placementRequestAllocatedTo = user,
                assessmentAllocatedTo = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              ) { placementRequest, _ ->
                val premises = approvedPremisesEntityFactory.produceAndPersist {
                  withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
                  withYieldedProbationRegion {
                    probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
                  }
                }

                webTestClient.post()
                  .uri("/placement-requests/${placementRequest.id}/booking")
                  .header("Authorization", "Bearer $jwt")
                  .bodyValue(
                    NewPlacementRequestBooking(
                      arrivalDate = LocalDate.parse("2023-03-29"),
                      departureDate = LocalDate.parse("2023-04-01"),
                      bedId = null,
                      premisesId = premises.id,
                    ),
                  )
                  .exchange()
                  .expectStatus()
                  .isOk
              }
            }
          }
        }
      }
    }
  }

  @Nested
  inner class CreateBookingNotMadeFromPlacementRequest {
    @Test
    fun `Create a Booking Not Made from a Placement Request without a JWT returns 401`() {
      webTestClient.post()
        .uri("/placement-requests/62faf6f4-1dac-4139-9a18-09c1b2852a0f/booking-not-made")
        .bodyValue(
          NewBookingNotMade(
            notes = "some notes",
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Create a Booking Not Made from a Placement Request returns 200`() {
      `Given a User` { _, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, inmateDetails ->
            `Given an Application`(createdByUser = otherUser) {
              `Given a Placement Request`(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              ) { placementRequest, _ ->
                webTestClient.post()
                  .uri("/placement-requests/${placementRequest.id}/booking-not-made")
                  .header("Authorization", "Bearer $jwt")
                  .bodyValue(
                    NewBookingNotMade(
                      notes = "some notes",
                    ),
                  )
                  .exchange()
                  .expectStatus()
                  .isOk
                  .expectBody()
                  .jsonPath("$.placementRequestId").isEqualTo(placementRequest.id.toString())
                  .jsonPath("$.notes").isEqualTo("some notes")
              }
            }
          }
        }
      }
    }

    @Nested
    inner class WithdrawPlacementRequest {
      @Test
      fun `Withdraw Placement Request without a JWT returns 401`() {
        webTestClient.post()
          .uri("/placement-requests/62faf6f4-1dac-4139-9a18-09c1b2852a0f/withdrawal")
          .exchange()
          .expectStatus()
          .isUnauthorized
      }

      @Test
      fun `Withdraw Placement Request without CAS1_WORKFLOW_MANAGER returns 403`() {
        `Given a User` { _, jwt ->
          webTestClient.post()
            .uri("/placement-requests/62faf6f4-1dac-4139-9a18-09c1b2852a0f/withdrawal")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }

      @Test
      fun `Withdraw Placement Request returns 200, sets isWithdrawn to true`() {
        `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
          `Given an Offender` { offenderDetails, inmateDetails ->
            `Given an Application`(createdByUser = user) {
              `Given a Placement Request`(
                placementRequestAllocatedTo = user,
                assessmentAllocatedTo = user,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              ) { placementRequest, _ ->
                webTestClient.post()
                  .uri("/placement-requests/${placementRequest.id}/withdrawal")
                  .header("Authorization", "Bearer $jwt")
                  .exchange()
                  .expectStatus()
                  .isOk

                val persistedPlacementRequest = placementRequestRepository.findByIdOrNull(placementRequest.id)!!
                assertThat(persistedPlacementRequest.isWithdrawn).isTrue
              }
            }
          }
        }
      }
    }
  }
}
