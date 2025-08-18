package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementRequestSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementRequestSummary.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1ChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestDetailTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PlacementRequestSummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas1PlacementRequestTest : IntegrationTestBase() {

  @Autowired
  lateinit var cas1PlacementRequestSummaryTransformer: Cas1PlacementRequestSummaryTransformer

  @Autowired
  lateinit var personTransformer: PersonTransformer

  @Autowired
  lateinit var realPlacementRequestRepository: PlacementRequestRepository

  lateinit var probationRegion: ProbationRegionEntity

  @BeforeEach
  fun before() {
    probationRegion = givenAProbationRegion()
  }

  @Nested
  @SuppressWarnings("LargeClass")
  inner class Search {

    private fun createSpaceBooking(placementRequest: PlacementRequestEntity, premises: ApprovedPremisesEntity? = null, canonicalArrivalDate: LocalDate? = LocalDate.now()): Cas1SpaceBookingEntity = givenACas1SpaceBooking(
      premises = premises,
      crn = placementRequest.application.crn,
      placementRequest = placementRequest,
      canonicalArrivalDate = canonicalArrivalDate!!,
    )

    private fun createBookingNotMadeRecord(placementRequest: PlacementRequestEntity) {
      placementRequest.bookingNotMades = mutableListOf(
        bookingNotMadeFactory.produceAndPersist {
          withPlacementRequest(placementRequest)
        },
      )
    }

    @Test
    fun `Get dashboard without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/placement-requests")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_JANITOR"], mode = EnumSource.Mode.EXCLUDE)
    fun `Get dashboard without CAS1_VIEW_CRU_DASHBOARD permission returns 401`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/cas1/placement-requests")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `should return 1 placement request when there are 0 active bookings`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { unmatchedOffender, unmatchedInmate ->
          givenAPlacementRequest(
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = unmatchedOffender.otherIds.crn,
          ) { placementRequest, _ ->

            // create a cancelled space booking
            createBookingNotMadeRecord(placementRequest)
            val spaceBooking = createSpaceBooking(placementRequest)
            spaceBooking.cancellationOccurredAt = LocalDate.now()
            cas1SpaceBookingRepository.save(spaceBooking)

            // should return 1 placement request from GET /cas1/placement-requests
            val summaries = webTestClient.get()
              .uri("/cas1/placement-requests")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .bodyAsListOfObjects<Cas1PlacementRequestSummary>()

            assertThat(summaries).hasSize(1)

            assertThat(summaries[0].id).isEqualTo(placementRequest.id)
            assertThat(summaries[0].person.crn).isEqualTo(unmatchedOffender.otherIds.crn)
            assertThat(summaries[0].placementRequestStatus).isEqualTo(Cas1PlacementRequestSummary.PlacementRequestStatus.unableToMatch)
            assertThat(summaries[0].isParole).isEqualTo(placementRequest.isParole)
            assertThat(summaries[0].requestedPlacementDuration).isEqualTo(placementRequest.duration)
            assertThat(summaries[0].requestedPlacementArrivalDate).isEqualTo(placementRequest.expectedArrival)
            assertThat(summaries[0].personTier).isEqualTo(placementRequest.application.riskRatings?.tier?.value?.level)
            assertThat(summaries[0].applicationId).isEqualTo(placementRequest.application.id)
            assertThat(summaries[0].applicationSubmittedDate).isEqualTo(placementRequest.application.submittedAt!!.toLocalDate())
            assertThat(summaries[0].firstBookingPremisesName).isNull()
            assertThat(summaries[0].firstBookingArrivalDate).isNull()
          }
        }
      }
    }

    @Test
    fun `should return 1 placement request when it has an active space booking`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { unmatchedOffender, unmatchedInmate ->
          givenAPlacementRequest(
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = unmatchedOffender.otherIds.crn,
          ) { placementRequest, _ ->

            // create a cancelled space booking
            createBookingNotMadeRecord(placementRequest)
            val spaceBooking = createSpaceBooking(placementRequest)
            spaceBooking.cancellationOccurredAt = LocalDate.now()
            cas1SpaceBookingRepository.save(spaceBooking)

            // create a space booking
            createSpaceBooking(
              placementRequest,
              premises = givenAnApprovedPremises("space_booking_premises"),
              canonicalArrivalDate = LocalDate.parse("2025-01-01"),
            )

            // should return 1 placement request from GET /cas1/placement-requests
            val summaries = webTestClient.get()
              .uri("/cas1/placement-requests")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .bodyAsListOfObjects<Cas1PlacementRequestSummary>()

            assertThat(summaries).hasSize(1)

            assertThat(summaries[0].id).isEqualTo(placementRequest.id)
            assertThat(summaries[0].person.crn).isEqualTo(unmatchedOffender.otherIds.crn)
            assertThat(summaries[0].placementRequestStatus).isEqualTo(Cas1PlacementRequestSummary.PlacementRequestStatus.matched)
            assertThat(summaries[0].isParole).isEqualTo(placementRequest.isParole)
            assertThat(summaries[0].requestedPlacementDuration).isEqualTo(placementRequest.duration)
            assertThat(summaries[0].requestedPlacementArrivalDate).isEqualTo(placementRequest.expectedArrival)
            assertThat(summaries[0].personTier).isEqualTo(placementRequest.application.riskRatings?.tier?.value?.level)
            assertThat(summaries[0].applicationId).isEqualTo(placementRequest.application.id)
            assertThat(summaries[0].applicationSubmittedDate).isEqualTo(placementRequest.application.submittedAt!!.toLocalDate())
            assertThat(summaries[0].firstBookingPremisesName).isEqualTo("space_booking_premises")
            assertThat(summaries[0].firstBookingArrivalDate).isEqualTo(LocalDate.parse("2025-01-01"))
          }
        }
      }
    }

    @Test
    fun `should return a placement request with the earliest space booking`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { unmatchedOffender, unmatchedInmate ->
          givenAPlacementRequest(
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = unmatchedOffender.otherIds.crn,
          ) { placementRequest, _ ->

            // create a cancelled space booking
            createBookingNotMadeRecord(placementRequest)
            val spaceBooking = createSpaceBooking(placementRequest)
            spaceBooking.cancellationOccurredAt = LocalDate.now()
            cas1SpaceBookingRepository.save(spaceBooking)

            // create a space booking
            createSpaceBooking(
              placementRequest,
              premises = givenAnApprovedPremises("space_booking_premises"),
              canonicalArrivalDate = LocalDate.parse("2025-01-02"),
            )

            // create an earlier space booking
            createSpaceBooking(
              placementRequest,
              premises = givenAnApprovedPremises("earlier_space_booking_premises"),
              canonicalArrivalDate = LocalDate.parse("2025-01-01"),
            )

            // should return 1 placement request from GET /cas1/placement-requests
            val summaries = webTestClient.get()
              .uri("/cas1/placement-requests")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .bodyAsListOfObjects<Cas1PlacementRequestSummary>()

            assertThat(summaries).hasSize(1)

            assertThat(summaries[0].id).isEqualTo(placementRequest.id)
            assertThat(summaries[0].person.crn).isEqualTo(unmatchedOffender.otherIds.crn)
            assertThat(summaries[0].placementRequestStatus).isEqualTo(Cas1PlacementRequestSummary.PlacementRequestStatus.matched)
            assertThat(summaries[0].isParole).isEqualTo(placementRequest.isParole)
            assertThat(summaries[0].requestedPlacementDuration).isEqualTo(placementRequest.duration)
            assertThat(summaries[0].requestedPlacementArrivalDate).isEqualTo(placementRequest.expectedArrival)
            assertThat(summaries[0].personTier).isEqualTo(placementRequest.application.riskRatings?.tier?.value?.level)
            assertThat(summaries[0].applicationId).isEqualTo(placementRequest.application.id)
            assertThat(summaries[0].applicationSubmittedDate).isEqualTo(placementRequest.application.submittedAt!!.toLocalDate())
            assertThat(summaries[0].firstBookingPremisesName).isEqualTo("earlier_space_booking_premises")
            assertThat(summaries[0].firstBookingArrivalDate).isEqualTo(LocalDate.parse("2025-01-01"))
          }
        }
      }
    }

    @Test
    fun `If status filter is not defined, returns the unmatched placement requests and withdrawn placement requests`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { unmatchedOffender, unmatchedInmate ->
          givenAPlacementRequest(
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = unmatchedOffender.otherIds.crn,
          ) { unmatchedPlacementRequest, _ ->
            val withdrawnPlacementRequest = createPlacementRequest(unmatchedOffender, user, isWithdrawn = true)

            webTestClient.get()
              .uri("/cas1/placement-requests")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    transformPlacementRequestJpaToApi(
                      unmatchedPlacementRequest,
                      PersonInfoResult.Success.Full(unmatchedOffender.otherIds.crn, unmatchedOffender, unmatchedInmate),
                    ),
                    transformPlacementRequestJpaToApi(
                      withdrawnPlacementRequest,
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
    fun `If status filter is 'notMatched', returns the unmatched placement requests, ignoring withdrawn`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { unmatchedOffender, _ ->
          val (unmatchedPlacementRequest) = givenAPlacementRequest(
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = unmatchedOffender.otherIds.crn,
          )

          // withdrawn, ignored
          createPlacementRequest(unmatchedOffender, user, isWithdrawn = true)

          val result = webTestClient.get()
            .uri("/cas1/placement-requests?status=notMatched")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .bodyAsListOfObjects<Cas1PlacementRequestSummary>()

          assertThat(result.map { it.id }).containsExactlyInAnyOrder(
            unmatchedPlacementRequest.id,
          )
        }
      }
    }

    @Test
    fun `If status filter is 'matched', returns the matched placement requests, ignoring withdrawn`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { matchedOffender, _ ->

          // withdrawn placement request with space booking, ignored
          givenAPlacementRequest(
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = matchedOffender.otherIds.crn,
            isWithdrawn = true,
          ) { placementRequest, _ ->
            createSpaceBooking(placementRequest)
          }

          val (placementRequestWithSpaceBooking) = givenAPlacementRequest(
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = matchedOffender.otherIds.crn,
          ) { placementRequest, _ ->
            createSpaceBooking(placementRequest)
          }

          val (placementRequestPreviouslyUnableToMatchNowHasSpaceBooking) = givenAPlacementRequest(
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = matchedOffender.otherIds.crn,
          ) { placementRequest, _ ->
            createBookingNotMadeRecord(placementRequest)
            createSpaceBooking(placementRequest)
          }

          val result = webTestClient.get()
            .uri("/cas1/placement-requests?status=matched")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .bodyAsListOfObjects<Cas1PlacementRequestSummary>()

          assertThat(result.map { it.id }).containsExactlyInAnyOrder(
            placementRequestWithSpaceBooking.id,
            placementRequestPreviouslyUnableToMatchNowHasSpaceBooking.id,
          )
        }
      }
    }

    @Test
    fun `If status filter is 'unableToMatch', returns the unable to match placement requests, ignoring withdrawn and those subsequently matched`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { unableToMatchOffender, _ ->

          // withdrawn, ignore
          givenAPlacementRequest(
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = unableToMatchOffender.otherIds.crn,
            isWithdrawn = true,
          ) { unableToMatchPlacementRequest, _ ->
            createBookingNotMadeRecord(unableToMatchPlacementRequest)
          }

          val (unableToMatchPlacementRequest) = givenAPlacementRequest(
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = unableToMatchOffender.otherIds.crn,
          ) { placementRequest, _ ->
            createBookingNotMadeRecord(placementRequest)
          }

          // previously unable to match, now has space booking, ignored
          givenAPlacementRequest(
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = unableToMatchOffender.otherIds.crn,
          ) { placementRequest, _ ->
            createBookingNotMadeRecord(placementRequest)
            createSpaceBooking(placementRequest)
          }

          val (hasCancelledSpaceBookingPlacementRequest) = givenAPlacementRequest(
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = unableToMatchOffender.otherIds.crn,
          ) { placementRequest, _ ->
            createBookingNotMadeRecord(placementRequest)
            val spaceBooking = createSpaceBooking(placementRequest)
            spaceBooking.cancellationOccurredAt = LocalDate.now()
            cas1SpaceBookingRepository.save(spaceBooking)
          }

          val result = webTestClient.get()
            .uri("/cas1/placement-requests?status=unableToMatch")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .bodyAsListOfObjects<Cas1PlacementRequestSummary>()

          assertThat(result.map { it.id }).containsExactlyInAnyOrder(
            unableToMatchPlacementRequest.id,
            hasCancelledSpaceBookingPlacementRequest.id,
          )
        }
      }
    }

    @Test
    fun `Returns paginated placement requests`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          repeat(11) { createPlacementRequest(offenderDetails, user) }

          val response = webTestClient.get()
            .uri("/cas1/placement-requests?page=1")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 11)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .bodyAsListOfObjects<Cas1PlacementRequestSummary>()

          assertThat(response).hasSize(10)
        }
      }
    }

    @Test
    fun `It searches by name via crnOrName when the user is a manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = {
            withFirstName("JOHN")
            withLastName("SMITH")
          },
        ) { offenderDetails, inmateDetails ->
          givenAnOffender { otherOffenderDetails, _ ->
            val placementRequest = createPlacementRequest(offenderDetails, user)
            createPlacementRequest(otherOffenderDetails, user)

            webTestClient.get()
              .uri("/cas1/placement-requests?crnOrName=john")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    transformPlacementRequestJpaToApi(
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
    fun `It searches by crn via crnOrName when the user is a manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { otherOffenderDetails, _ ->
          givenAnOffender { offenderDetails, inmateDetails ->
            val placementRequest = createPlacementRequest(offenderDetails, user)
            createPlacementRequest(otherOffenderDetails, user)

            webTestClient.get()
              .uri("/cas1/placement-requests?crnOrName=${offenderDetails.otherIds.crn}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    transformPlacementRequestJpaToApi(
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
    fun `It searches by arrivalDateStart where user is manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          createPlacementRequest(offenderDetails, user, expectedArrival = LocalDate.of(2022, 1, 1))
          val placementRequest5thJan = createPlacementRequest(offenderDetails, user, expectedArrival = LocalDate.of(2022, 1, 5))
          val placementRequest10thJan = createPlacementRequest(offenderDetails, user, expectedArrival = LocalDate.of(2022, 1, 10))

          webTestClient.get()
            .uri("/cas1/placement-requests?arrivalDateStart=2022-01-04")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                listOf(
                  transformPlacementRequestJpaToApi(
                    placementRequest5thJan,
                    PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                  ),
                  transformPlacementRequestJpaToApi(
                    placementRequest10thJan,
                    PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                  ),
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `It searches by arrivalDateEnd where user is manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val placementRequest1stJan = createPlacementRequest(offenderDetails, user, expectedArrival = LocalDate.of(2022, 1, 1))
          val placementRequest5thJan = createPlacementRequest(offenderDetails, user, expectedArrival = LocalDate.of(2022, 1, 5))
          createPlacementRequest(offenderDetails, user, expectedArrival = LocalDate.of(2022, 1, 10))

          webTestClient.get()
            .uri("/cas1/placement-requests?arrivalDateEnd=2022-01-09")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                listOf(
                  transformPlacementRequestJpaToApi(
                    placementRequest1stJan,
                    PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                  ),
                  transformPlacementRequestJpaToApi(
                    placementRequest5thJan,
                    PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                  ),
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `It searches by tier where user is manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          createPlacementRequest(offenderDetails, user, tier = RiskTierLevel.a0)
          val placementRequestA1 = createPlacementRequest(offenderDetails, user, tier = RiskTierLevel.a1)
          createPlacementRequest(offenderDetails, user, tier = RiskTierLevel.a2)

          webTestClient.get()
            .uri("/cas1/placement-requests?tier=A1")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                listOf(
                  transformPlacementRequestJpaToApi(
                    placementRequestA1,
                    PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                  ),
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `It searches by CRU Management Area ID where user is manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          val cruArea1 = givenACas1CruManagementArea()
          val cruArea2 = givenACas1CruManagementArea()

          createPlacementRequest(offenderDetails, user, cruManagementArea = cruArea1)
          val placementRequestA1 = createPlacementRequest(offenderDetails, user, cruManagementArea = cruArea2)
          createPlacementRequest(offenderDetails, user, cruManagementArea = cruArea1)

          webTestClient.get()
            .uri("/cas1/placement-requests?cruManagementAreaId=${cruArea2.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                listOf(
                  transformPlacementRequestJpaToApi(
                    placementRequestA1,
                    PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                  ),
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `It searches by requestType where type is standardRelease`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          createPlacementRequest(offenderDetails, user, isParole = true)
          val standardRelease = createPlacementRequest(offenderDetails, user, isParole = false)

          webTestClient.get()
            .uri("/cas1/placement-requests?requestType=${PlacementRequestRequestType.standardRelease.name}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                listOf(
                  transformPlacementRequestJpaToApi(
                    standardRelease,
                    PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                  ),
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `It searches by requestType where type is parole`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          createPlacementRequest(offenderDetails, user, isParole = false)
          val parole = createPlacementRequest(offenderDetails, user, isParole = true)

          webTestClient.get()
            .uri("/cas1/placement-requests?requestType=${PlacementRequestRequestType.parole.name}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                listOf(
                  transformPlacementRequestJpaToApi(
                    parole,
                    PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                  ),
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `It searches using multiple criteria including CRU management area id where user is manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { offender1Details, inmate1Details ->
          givenAnOffender { offender2Details, _ ->

            val cruArea1 = givenACas1CruManagementArea()
            val cruArea2 = givenACas1CruManagementArea()

            createPlacementRequest(offender1Details, user, expectedArrival = LocalDate.of(2022, 1, 1), tier = RiskTierLevel.a2)
            createPlacementRequest(offender1Details, user, expectedArrival = LocalDate.of(2022, 1, 5), tier = RiskTierLevel.a1)
            val placementOffender1On5thJanTierA2Parole =
              createPlacementRequest(offender1Details, user, expectedArrival = LocalDate.of(2022, 1, 5), tier = RiskTierLevel.a2, isParole = true, cruManagementArea = cruArea1)
            createPlacementRequest(offender1Details, user, expectedArrival = LocalDate.of(2022, 1, 5), tier = RiskTierLevel.a2, isParole = true, cruManagementArea = cruArea2)
            createPlacementRequest(offender1Details, user, expectedArrival = LocalDate.of(2022, 1, 5), tier = RiskTierLevel.a2, isParole = false)
            createPlacementRequest(offender2Details, user, expectedArrival = LocalDate.of(2022, 1, 5), tier = RiskTierLevel.a2)
            createPlacementRequest(offender1Details, user, expectedArrival = LocalDate.of(2022, 1, 10), tier = RiskTierLevel.a2)

            webTestClient.get()
              .uri(
                "/cas1/placement-requests?arrivalDateStart=2022-01-02&arrivalDateEnd=2022-01-09&crnOrName=${offender1Details.otherIds.crn}" +
                  "&tier=A2&requestType=parole&cruManagementAreaId=${cruArea1.id}",
              )
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    transformPlacementRequestJpaToApi(
                      placementOffender1On5thJanTierA2Parole,
                      PersonInfoResult.Success.Full(offender1Details.otherIds.crn, offender1Details, inmate1Details),
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
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val placementRequestWithOneDayDuration = createPlacementRequest(offenderDetails, user, duration = 1)
          val placementRequestWithFiveDayDuration = createPlacementRequest(offenderDetails, user, duration = 5)
          val placementRequestWithTwelveDayDuration = createPlacementRequest(offenderDetails, user, duration = 12)

          webTestClient.get()
            .uri("/cas1/placement-requests?page=1&sortBy=duration")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequestWithOneDayDuration.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequestWithFiveDayDuration.id.toString())
            .jsonPath("$[2].id").isEqualTo(placementRequestWithTwelveDayDuration.id.toString())

          webTestClient.get()
            .uri("/cas1/placement-requests?page=1&sortBy=duration&sortDirection=desc")
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
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val placementRequestWithExpectedArrivalOfToday = createPlacementRequest(offenderDetails, user)
          val placementRequestWithExpectedArrivalInTwelveDays =
            createPlacementRequest(offenderDetails, user, expectedArrival = LocalDate.now().plusDays(12))
          val placementRequestWithExpectedArrivalInThirtyDays =
            createPlacementRequest(offenderDetails, user, expectedArrival = LocalDate.now().plusDays(30))

          webTestClient.get()
            .uri("/cas1/placement-requests?page=1&sortBy=expected_arrival")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequestWithExpectedArrivalOfToday.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequestWithExpectedArrivalInTwelveDays.id.toString())
            .jsonPath("$[2].id").isEqualTo(placementRequestWithExpectedArrivalInThirtyDays.id.toString())

          webTestClient.get()
            .uri("/cas1/placement-requests?page=1&sortBy=expected_arrival&sortDirection=desc")
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
    fun `It sorts by requestType when the user is a manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val placementRequest1WithStatusParole = createPlacementRequest(offenderDetails, user, isParole = true)
          val placementRequest2WithStatusStandard = createPlacementRequest(offenderDetails, user, isParole = false)

          webTestClient.get()
            .uri("/cas1/placement-requests?page=1&sortBy=request_type&sortDirection=asc")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequest1WithStatusParole.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequest2WithStatusStandard.id.toString())

          webTestClient.get()
            .uri("/cas1/placement-requests?page=1&sortBy=request_type&sortDirection=desc")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequest2WithStatusStandard.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequest1WithStatusParole.id.toString())
        }
      }
    }

    @Test
    fun `It sorts by personName when the user is a manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->

        val (offenderJohnSmithDetails, _) = givenAnOffender(
          offenderDetailsConfigBlock = {
            withFirstName("JOHN")
            withLastName("SMITH")
          },
        )

        val (offenderZackeryZoikesDetails, _) = givenAnOffender(
          offenderDetailsConfigBlock = {
            withFirstName("ZAKERY")
            withLastName("ZOIKES")
          },
        )

        val (offenderHarryHarrisonDetails, _) = givenAnOffender(
          offenderDetailsConfigBlock = {
            withFirstName("HARRY")
            withLastName("HARRISON")
          },
        )

        val placementRequestJohnSmith = createPlacementRequest(offenderJohnSmithDetails, user)
        val placementRequestZakeryZoikes = createPlacementRequest(offenderZackeryZoikesDetails, user)
        val placementRequestHarryHarrisonDetails = createPlacementRequest(offenderHarryHarrisonDetails, user)

        webTestClient.get()
          .uri("/cas1/placement-requests?page=1&sortBy=person_name&sortDirection=asc")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$[0].id").isEqualTo(placementRequestHarryHarrisonDetails.id.toString())
          .jsonPath("$[1].id").isEqualTo(placementRequestJohnSmith.id.toString())
          .jsonPath("$[2].id").isEqualTo(placementRequestZakeryZoikes.id.toString())

        webTestClient.get()
          .uri("/cas1/placement-requests?page=1&sortBy=person_name&sortDirection=desc")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$[0].id").isEqualTo(placementRequestZakeryZoikes.id.toString())
          .jsonPath("$[1].id").isEqualTo(placementRequestJohnSmith.id.toString())
          .jsonPath("$[2].id").isEqualTo(placementRequestHarryHarrisonDetails.id.toString())
      }
    }

    @Test
    fun `It sorts by personRisksTier when the user is a manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val placementRequest1TierB1 = createPlacementRequest(offenderDetails, user, tier = RiskTierLevel.b1)
          val placementRequest2TierA0 = createPlacementRequest(offenderDetails, user, tier = RiskTierLevel.a0)
          val placementRequest3TierA1 = createPlacementRequest(offenderDetails, user, tier = RiskTierLevel.a1)

          webTestClient.get()
            .uri("/cas1/placement-requests?page=1&sortBy=person_risks_tier&sortDirection=asc")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequest2TierA0.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequest3TierA1.id.toString())
            .jsonPath("$[2].id").isEqualTo(placementRequest1TierB1.id.toString())

          webTestClient.get()
            .uri("/cas1/placement-requests?page=1&sortBy=person_risks_tier&sortDirection=desc")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequest1TierB1.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequest3TierA1.id.toString())
            .jsonPath("$[2].id").isEqualTo(placementRequest2TierA0.id.toString())
        }
      }
    }

    @Test
    fun `It sorts by createdAt when the user is a manager`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val placementRequestCreatedToday = createPlacementRequest(offenderDetails, user)
          val placementRequestCreatedFiveDaysAgo =
            createPlacementRequest(offenderDetails, user, createdAt = OffsetDateTime.now().minusDays(5))
          val placementRequestCreatedThirtyDaysAgo =
            createPlacementRequest(offenderDetails, user, createdAt = OffsetDateTime.now().minusDays(30))

          webTestClient.get()
            .uri("/cas1/placement-requests?page=1&sortBy=created_at")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequestCreatedThirtyDaysAgo.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequestCreatedFiveDaysAgo.id.toString())
            .jsonPath("$[2].id").isEqualTo(placementRequestCreatedToday.id.toString())

          webTestClient.get()
            .uri("/cas1/placement-requests?page=1&sortBy=created_at&sortDirection=desc")
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
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val placementRequestWithApplicationCreatedToday = createPlacementRequest(offenderDetails, user)
          val placementRequestWithApplicationCreatedTwelveDaysAgo =
            createPlacementRequest(offenderDetails, user, applicationDate = OffsetDateTime.now().minusDays(12))
          val placementRequestWithApplicationCreatedThirtyDaysAgo =
            createPlacementRequest(offenderDetails, user, applicationDate = OffsetDateTime.now().minusDays(30))

          webTestClient.get()
            .uri("/cas1/placement-requests?page=1&sortBy=application_date")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(placementRequestWithApplicationCreatedThirtyDaysAgo.id.toString())
            .jsonPath("$[1].id").isEqualTo(placementRequestWithApplicationCreatedTwelveDaysAgo.id.toString())
            .jsonPath("$[2].id").isEqualTo(placementRequestWithApplicationCreatedToday.id.toString())

          webTestClient.get()
            .uri("/cas1/placement-requests?page=1&sortBy=application_date&sortDirection=desc")
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

    @Suppress("LongParameterList")
    private fun createPlacementRequest(
      offenderDetails: OffenderDetailSummary,
      user: UserEntity,
      duration: Int = 12,
      expectedArrival: LocalDate = LocalDate.now(),
      createdAt: OffsetDateTime = OffsetDateTime.now(),
      applicationDate: OffsetDateTime = OffsetDateTime.now(),
      isWithdrawn: Boolean = false,
      tier: RiskTierLevel = RiskTierLevel.b1,
      isParole: Boolean = false,
      apArea: ApAreaEntity? = null,
      cruManagementArea: Cas1CruManagementAreaEntity? = null,
    ): PlacementRequestEntity {
      val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCrn(offenderDetails.otherIds.crn)
        withCreatedByUser(user)
        withSubmittedAt(OffsetDateTime.now())
        withReleaseType("licence")
        withSubmittedAt(applicationDate)
        withName("${offenderDetails.firstName} ${offenderDetails.surname}")
        withRiskRatings(
          PersonRisksFactory()
            .withTier(
              RiskWithStatus(
                RiskTier(
                  level = tier.value,
                  lastUpdated = LocalDate.parse("2023-06-26"),
                ),
              ),
            ).produce(),
        )
        withApArea(apArea)
        cruManagementArea?.let {
          withCruManagementArea(it)
        }
      }

      val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
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
        withApplication(application)
        withAssessment(assessment)
        withPlacementRequirements(placementRequirements)
        withDuration(duration)
        withExpectedArrival(expectedArrival)
        withCreatedAt(createdAt)
        withIsWithdrawn(isWithdrawn)
        withIsParole(isParole)
      }
    }
  }

  @Nested
  inner class GetPlacementRequest {

    @Autowired
    lateinit var placementRequestDetailTransformer: PlacementRequestDetailTransformer

    @Test
    fun `Without a JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/placement-requests/62faf6f4-1dac-4139-9a18-09c1b2852a0f")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Offender not LAO, returns 200`() {
      givenAUser { _, jwt ->
        givenAUser { otherUser, _ ->
          givenAnOffender { offenderDetails, inmateDetails ->
            givenAPlacementRequest(
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            ) { placementRequest, _ ->

              val premises = givenAnApprovedPremises(emailAddress = "premises@test.com")

              val application = placementRequest.application

              val spaceBooking = givenACas1SpaceBooking(
                crn = application.crn,
                application = application,
                placementRequest = placementRequest,
                canonicalArrivalDate = LocalDate.of(2024, 6, 1),
                canonicalDepartureDate = LocalDate.of(2024, 6, 15),
                premises = premises,
              )

              val changeRequest = givenACas1ChangeRequest(
                type = ChangeRequestType.PLACEMENT_APPEAL,
                spaceBooking = spaceBooking,
                decisionJson = "{\"test\": 1}",

              )

              webTestClient.get()
                .uri("/cas1/placement-requests/${placementRequest.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    placementRequestDetailTransformer.transformJpaToCas1PlacementRequestDetail(
                      spaceBooking.placementRequest!!,
                      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                      listOf(changeRequest),
                    ),
                  ),
                )
            }
          }
        }
      }
    }

    @Test
    fun `Offender is LAO, user doesn't have LAO access or LAO qualification, returns 403`() {
      givenAUser { _, jwt ->
        givenAUser { otherUser, _ ->
          givenAnOffender(
            offenderDetailsConfigBlock = {
              withCurrentExclusion(true)
            },
          ) { offenderDetails, _ ->
            givenAPlacementRequest(
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            ) { placementRequest, _ ->
              webTestClient.get()
                .uri("/cas1/placement-requests/${placementRequest.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isForbidden
            }
          }
        }
      }
    }

    @Test
    fun `Offender is LAO, user has LAO access, returns 200 and FullPerson`() {
      givenAUser { user, jwt ->
        givenAUser { otherUser, _ ->
          givenAnOffender(
            offenderDetailsConfigBlock = {
              withCurrentExclusion(true)
            },
          ) { offenderDetails, inmateDetails ->
            givenAPlacementRequest(
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            ) { placementRequest, _ ->
              apDeliusContextAddResponseToUserAccessCall(
                listOf(
                  CaseAccessFactory()
                    .withCrn(offenderDetails.otherIds.crn)
                    .produce(),
                ),
                user.deliusUsername,
              )

              val premises = givenAnApprovedPremises(emailAddress = "premises@test.com")

              val application = placementRequest.application

              val spaceBooking = givenACas1SpaceBooking(
                crn = application.crn,
                application = application,
                placementRequest = placementRequest,
                canonicalArrivalDate = LocalDate.of(2024, 6, 1),
                canonicalDepartureDate = LocalDate.of(2024, 6, 15),
                premises = premises,
              )

              val changeRequest = givenACas1ChangeRequest(
                type = ChangeRequestType.PLACEMENT_APPEAL,
                spaceBooking = spaceBooking,
                decisionJson = "{\"test\": 1}",

              )

              webTestClient.get()
                .uri("/cas1/placement-requests/${placementRequest.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    placementRequestDetailTransformer.transformJpaToCas1PlacementRequestDetail(
                      spaceBooking.placementRequest!!,
                      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                      listOf(changeRequest),
                    ),
                  ),
                )
            }
          }
        }
      }
    }

    @Test
    fun `Offender is LAO, user doesn't have LAO access but has LAO qualification, returns 200 and FullPerson`() {
      givenAUser(qualifications = listOf(UserQualification.LAO)) { user, jwt ->
        givenAUser { otherUser, _ ->
          givenAnOffender(
            offenderDetailsConfigBlock = {
              withCurrentExclusion(true)
            },
          ) { offenderDetails, inmateDetails ->
            givenAPlacementRequest(
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            ) { placementRequest, _ ->

              val premises = givenAnApprovedPremises(emailAddress = "premises@test.com")

              val application = placementRequest.application

              val spaceBooking = givenACas1SpaceBooking(
                crn = application.crn,
                application = application,
                placementRequest = placementRequest,
                canonicalArrivalDate = LocalDate.of(2024, 6, 1),
                canonicalDepartureDate = LocalDate.of(2024, 6, 15),
                premises = premises,
              )

              val changeRequest = givenACas1ChangeRequest(
                type = ChangeRequestType.PLACEMENT_APPEAL,
                spaceBooking = spaceBooking,
                decisionJson = "{\"test\": 1}",

              )
              webTestClient.get()
                .uri("/cas1/placement-requests/${placementRequest.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    placementRequestDetailTransformer.transformJpaToCas1PlacementRequestDetail(
                      spaceBooking.placementRequest!!,
                      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                      listOf(changeRequest),
                    ),
                  ),
                )
            }
          }
        }
      }
    }

    @Test
    fun `Offender not LAO, returns 200 with cancellations when they exist`() {
      givenAUser { _, jwt ->
        givenAUser { otherUser, _ ->
          givenAnOffender { offenderDetails, inmateDetails ->
            givenAPlacementRequest(
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            ) { placementRequest, _ ->
              val premises = givenAnApprovedPremises()

              webTestClient.get()
                .uri("/cas1/placement-requests/${placementRequest.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    placementRequestDetailTransformer.transformJpaToCas1PlacementRequestDetail(
                      placementRequestRepository.findByIdOrNull(placementRequest.id)!!,
                      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                      emptyList(),
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
  inner class WithdrawPlacementRequest {
    @Test
    fun `Withdraw Placement Request without a JWT returns 401`() {
      webTestClient.post()
        .uri("/cas1/placement-requests/62faf6f4-1dac-4139-9a18-09c1b2852a0f/withdrawal")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Withdraw Placement Request without CAS1_CRU_MEMBER returns 403`() {
      givenAUser { creator, _ ->
        givenAUser(roles = UserRole.getAllRolesExcept(UserRole.CAS1_CRU_MEMBER, UserRole.CAS1_JANITOR)) { user, jwt ->
          givenAnOffender { offenderDetails, _ ->
            givenAnApplication(createdByUser = user) {
              givenAPlacementRequest(
                assessmentAllocatedTo = user,
                createdByUser = creator,
                crn = offenderDetails.otherIds.crn,
              ) { placementRequest, _ ->
                webTestClient.post()
                  .uri("/cas1/placement-requests/${placementRequest.id}/withdrawal")
                  .header("Authorization", "Bearer $jwt")
                  .bodyValue(
                    WithdrawPlacementRequest(
                      reason = WithdrawPlacementRequestReason.duplicatePlacementRequest,
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

    @SuppressWarnings("MaxLineLength")
    @Test
    fun `Withdraw Placement Request returns 200, sets isWithdrawn to true, raises domain event, sends email to CRU and Applicant if it represents dates included on application on submission`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          givenAPlacementRequest(
            assessmentAllocatedTo = user,
            createdByUser = user,
            crn = offenderDetails.otherIds.crn,
            apArea = givenAnApArea(),
            cruManagementArea = givenACas1CruManagementArea(),
          ) { placementRequest, _ ->

            webTestClient.post()
              .uri("/cas1/placement-requests/${placementRequest.id}/withdrawal")
              .bodyValue(
                WithdrawPlacementRequest(
                  reason = WithdrawPlacementRequestReason.duplicatePlacementRequest,
                ),
              )
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .jsonPath("$.person.crn").isEqualTo(placementRequest.application.crn)

            val persistedPlacementRequest = placementRequestRepository.findByIdOrNull(placementRequest.id)!!
            assertThat(persistedPlacementRequest.isWithdrawn).isTrue
            assertThat(persistedPlacementRequest.withdrawalReason).isEqualTo(PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST)

            snsDomainEventListener.blockForMessage(DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN)

            emailAsserter.assertEmailsRequestedCount(2)
            emailAsserter.assertEmailRequested(
              placementRequest.application.cruManagementArea!!.emailAddress!!,
              Cas1NotifyTemplates.MATCH_REQUEST_WITHDRAWN_V2,
            )
            emailAsserter.assertEmailRequested(
              placementRequest.application.createdByUser.email!!,
              Cas1NotifyTemplates.PLACEMENT_REQUEST_WITHDRAWN_V2,
            )
          }
        }
      }
    }
  }

  fun transformPlacementRequestJpaToApi(jpa: PlacementRequestEntity, personInfo: PersonInfoResult): Cas1PlacementRequestSummary = Cas1PlacementRequestSummary(
    requestedPlacementDuration = jpa.duration,
    requestedPlacementArrivalDate = jpa.expectedArrival,
    id = jpa.id,
    person = personTransformer.transformModelToPersonApi(personInfo),
    placementRequestStatus = getStatus(jpa),
    isParole = jpa.isParole,
    personTier = jpa.application.riskRatings?.tier?.value?.level,
    applicationId = jpa.application.id,
    applicationSubmittedDate = jpa.application.submittedAt?.toLocalDate(),
    firstBookingPremisesName = null,
    firstBookingArrivalDate = null,
  )

  fun getStatus(placementRequest: PlacementRequestEntity): PlacementRequestStatus {
    if (placementRequest.hasActiveBooking()) {
      return PlacementRequestStatus.matched
    }

    if (placementRequest.bookingNotMades.any()) {
      return PlacementRequestStatus.unableToMatch
    }

    return PlacementRequestStatus.notMatched
  }
}
