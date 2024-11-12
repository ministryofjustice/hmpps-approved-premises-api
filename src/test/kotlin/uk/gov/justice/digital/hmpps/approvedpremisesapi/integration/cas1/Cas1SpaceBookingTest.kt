package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssignKeyWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NonArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummaryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas1SpaceBookingCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulStaffMembersCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_ASSESSOR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_FUTURE_MANAGER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1.Cas1SpaceBookingSummaryStatusTestHelper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1.TestCaseForSpaceBookingSummaryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.stream.Stream

class Cas1SpaceBookingTest {

  @Nested
  inner class CreateABooking : InitialiseDatabasePerClassTestBase() {

    @Autowired
    lateinit var transformer: Cas1SpaceBookingTransformer

    @Test
    fun `Booking a space without JWT returns 401`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, _ ->
        givenAPlacementRequest(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
        ) { placementRequest, _ ->
          webTestClient.post()
            .uri("/cas1/placement-requests/${placementRequest.id}/space-bookings")
            .exchange()
            .expectStatus()
            .isUnauthorized
        }
      }
    }

    @Test
    fun `Returns 403 Forbidden if user does not have correct role`() {
      givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { user, jwt ->
        val placementRequestId = UUID.randomUUID()
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedProbationRegion { givenAProbationRegion() }
          withYieldedLocalAuthorityArea {
            localAuthorityEntityFactory.produceAndPersist()
          }
        }

        webTestClient.post()
          .uri("/cas1/placement-requests/$placementRequestId/space-bookings")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewCas1SpaceBooking(
              arrivalDate = LocalDate.now().plusDays(1),
              departureDate = LocalDate.now().plusDays(8),
              premisesId = premises.id,
              requirements = Cas1SpaceBookingRequirements(
                essentialCharacteristics = listOf(),
              ),
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Booking a space for an unknown placement request returns 400 Bad Request`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedProbationRegion { givenAProbationRegion() }
          withYieldedLocalAuthorityArea {
            localAuthorityEntityFactory.produceAndPersist()
          }
        }

        val placementRequestId = UUID.randomUUID()

        webTestClient.post()
          .uri("/cas1/placement-requests/$placementRequestId/space-bookings")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewCas1SpaceBooking(
              arrivalDate = LocalDate.now().plusDays(1),
              departureDate = LocalDate.now().plusDays(8),
              premisesId = premises.id,
              requirements = Cas1SpaceBookingRequirements(
                essentialCharacteristics = listOf(),
              ),
            ),
          )
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("invalid-params[0].propertyName").isEqualTo("$.placementRequestId")
          .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
      }
    }

    @Test
    fun `Booking a space for an unknown premises returns 400 Bad Request`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAPlacementRequest(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
        ) { placementRequest, _ ->
          webTestClient.post()
            .uri("/cas1/placement-requests/${placementRequest.id}/space-bookings")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewCas1SpaceBooking(
                arrivalDate = LocalDate.now().plusDays(1),
                departureDate = LocalDate.now().plusDays(8),
                premisesId = UUID.randomUUID(),
                requirements = Cas1SpaceBookingRequirements(
                  essentialCharacteristics = listOf(),
                ),
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("invalid-params[0].propertyName").isEqualTo("$.premisesId")
            .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
        }
      }
    }

    @Test
    fun `Booking a space where the departure date is before the arrival date returns 400 Bad Request`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAPlacementRequest(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
        ) { placementRequest, _ ->
          val premises = approvedPremisesEntityFactory.produceAndPersist {
            withYieldedProbationRegion { givenAProbationRegion() }
            withYieldedLocalAuthorityArea {
              localAuthorityEntityFactory.produceAndPersist()
            }
          }

          webTestClient.post()
            .uri("/cas1/placement-requests/${placementRequest.id}/space-bookings")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewCas1SpaceBooking(
                arrivalDate = LocalDate.now().plusDays(1),
                departureDate = LocalDate.now(),
                premisesId = premises.id,
                requirements = Cas1SpaceBookingRequirements(
                  essentialCharacteristics = listOf(),
                ),
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("invalid-params[0].propertyName").isEqualTo("$.departureDate")
            .jsonPath("invalid-params[0].errorType").isEqualTo("shouldBeAfterArrivalDate")
        }
      }
    }

    @Test
    fun `Booking a space returns OK with the correct data, updates app status, emits domain event and emails`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { applicant, jwt ->
        givenAPlacementRequest(
          placementRequestAllocatedTo = applicant,
          assessmentAllocatedTo = applicant,
          createdByUser = applicant,
        ) { placementRequest, application ->
          val essentialCharacteristics = listOf(
            Cas1SpaceCharacteristic.hasBrailleSignage,
            Cas1SpaceCharacteristic.hasTactileFlooring,
          )

          placementRequest.placementRequirements = placementRequirementsFactory.produceAndPersist {
            withYieldedPostcodeDistrict {
              postCodeDistrictFactory.produceAndPersist()
            }
            withApplication(application as ApprovedPremisesApplicationEntity)
            withAssessment(placementRequest.assessment)
            withEssentialCriteria(emptyList())
            withDesirableCriteria(emptyList())
          }

          placementRequestRepository.saveAndFlush(placementRequest)

          val premises = approvedPremisesEntityFactory.produceAndPersist {
            withYieldedProbationRegion { givenAProbationRegion() }
            withYieldedLocalAuthorityArea {
              localAuthorityEntityFactory.produceAndPersist()
            }
          }

          val response = webTestClient.post()
            .uri("/cas1/placement-requests/${placementRequest.id}/space-bookings")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewCas1SpaceBooking(
                arrivalDate = LocalDate.now().plusDays(1),
                departureDate = LocalDate.now().plusDays(8),
                premisesId = premises.id,
                requirements = Cas1SpaceBookingRequirements(
                  essentialCharacteristics = essentialCharacteristics,
                ),
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(Cas1SpaceBooking::class.java)

          val result = response.responseBody.blockFirst()!!

          assertThat(result.person)
          assertThat(result.requirements.essentialCharacteristics).containsExactlyInAnyOrderElementsOf(
            essentialCharacteristics,
          )
          assertThat(result.premises.id).isEqualTo(premises.id)
          assertThat(result.premises.name).isEqualTo(premises.name)
          assertThat(result.apArea.id).isEqualTo(premises.probationRegion.apArea!!.id)
          assertThat(result.apArea.name).isEqualTo(premises.probationRegion.apArea!!.name)
          assertThat(result.bookedBy!!.id).isEqualTo(applicant.id)
          assertThat(result.bookedBy!!.name).isEqualTo(applicant.name)
          assertThat(result.bookedBy!!.deliusUsername).isEqualTo(applicant.deliusUsername)
          assertThat(result.expectedArrivalDate).isEqualTo(LocalDate.now().plusDays(1))
          assertThat(result.expectedDepartureDate).isEqualTo(LocalDate.now().plusDays(8))
          assertThat(result.createdAt).satisfies(
            { it.isAfter(Instant.now().minusSeconds(10)) },
          )

          domainEventAsserter.assertDomainEventOfTypeStored(placementRequest.application.id, DomainEventType.APPROVED_PREMISES_BOOKING_MADE)

          emailAsserter.assertEmailsRequestedCount(2)
          emailAsserter.assertEmailRequested(applicant.email!!, notifyConfig.templates.bookingMade)
          emailAsserter.assertEmailRequested(premises.emailAddress!!, notifyConfig.templates.bookingMadePremises)

          assertThat(approvedPremisesApplicationRepository.findByIdOrNull(placementRequest.application.id)!!.status)
            .isEqualTo(ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED)
        }
      }
    }
  }

  @Nested
  inner class SearchForSpaceBookings : SpaceBookingIntegrationTestBase() {
    lateinit var currentSpaceBooking2: Cas1SpaceBookingEntity
    lateinit var currentSpaceBooking3: Cas1SpaceBookingEntity
    lateinit var upcomingSpaceBookingWithKeyWorker: Cas1SpaceBookingEntity
    lateinit var upcomingCancelledSpaceBooking: Cas1SpaceBookingEntity
    lateinit var departedSpaceBooking: Cas1SpaceBookingEntity
    lateinit var nonArrivedSpaceBooking: Cas1SpaceBookingEntity

    @BeforeAll
    fun setupTestData() {
      super.setupRegionAndKeyWorkerAndPremises()

      currentSpaceBooking1 = createSpaceBooking(crn = "CRN_CURRENT1", firstName = "curt", lastName = "rent 1", tier = "A") {
        withPremises(premisesWithBookings)
        withExpectedArrivalDate(LocalDate.parse("2022-01-02"))
        withExpectedDepartureDate(LocalDate.parse("2022-02-02"))
        withActualArrivalDateTime(Instant.parse("2022-01-02T12:45:00.00Z"))
        withActualDepartureDateTime(null)
        withCanonicalArrivalDate(LocalDate.parse("2022-01-02"))
        withCanonicalDepartureDate(LocalDate.parse("2022-02-02"))
        withKeyworkerName(null)
        withKeyworkerStaffCode(null)
        withKeyworkerAssignedAt(Instant.now())
      }

      currentSpaceBooking2 = createSpaceBooking(crn = "CRN_CURRENT2", firstName = "curt", lastName = "rent 2", tier = "C") {
        withPremises(premisesWithBookings)
        withExpectedArrivalDate(LocalDate.parse("2022-02-02"))
        withExpectedDepartureDate(LocalDate.parse("2022-09-02"))
        withActualArrivalDateTime(Instant.parse("2022-01-02T12:45:00.00Z"))
        withActualDepartureDateTime(null)
        withCanonicalArrivalDate(LocalDate.parse("2022-02-02"))
        withCanonicalDepartureDate(LocalDate.parse("2022-03-02"))
        withKeyworkerName("Kathy Keyworker")
        withKeyworkerStaffCode("kathyk")
        withKeyworkerAssignedAt(Instant.now())
      }

      currentSpaceBooking3 = createSpaceBooking(crn = "CRN_CURRENT3", firstName = "curt", lastName = "rent 3", tier = "B") {
        withPremises(premisesWithBookings)
        withExpectedArrivalDate(LocalDate.parse("2022-03-02"))
        withExpectedDepartureDate(LocalDate.parse("2022-04-02"))
        withActualArrivalDateTime(Instant.parse("2022-01-02T12:45:00.00Z"))
        withActualDepartureDateTime(null)
        withCanonicalArrivalDate(LocalDate.parse("2022-04-02"))
        withCanonicalDepartureDate(LocalDate.parse("2022-05-02"))
        withKeyworkerName("Clive Keyworker")
        withKeyworkerStaffCode("clivek")
        withKeyworkerAssignedAt(Instant.now())
      }

      departedSpaceBooking = createSpaceBooking(crn = "CRN_DEPARTED", firstName = "de", lastName = "parted", tier = "D") {
        withPremises(premisesWithBookings)
        withExpectedArrivalDate(LocalDate.parse("2021-01-03"))
        withExpectedDepartureDate(LocalDate.parse("2021-02-03"))
        withActualArrivalDateTime(Instant.parse("2021-01-03T12:45:00.00Z"))
        withActualDepartureDateTime(Instant.parse("2021-02-03T12:45:00.00Z"))
        withCanonicalArrivalDate(LocalDate.parse("2021-01-03"))
        withCanonicalDepartureDate(LocalDate.parse("2021-02-03"))
        withKeyworkerName(null)
        withKeyworkerStaffCode(null)
        withKeyworkerAssignedAt(null)
      }

      upcomingSpaceBookingWithKeyWorker = createSpaceBooking(crn = "CRN_UPCOMING", firstName = "up", lastName = "coming", tier = "U") {
        withPremises(premisesWithBookings)
        withExpectedArrivalDate(LocalDate.parse("2023-01-01"))
        withExpectedDepartureDate(LocalDate.parse("2023-02-01"))
        withActualArrivalDateTime(null)
        withActualDepartureDateTime(null)
        withCanonicalArrivalDate(LocalDate.parse("2023-01-01"))
        withCanonicalDepartureDate(LocalDate.parse("2023-02-01"))
        withKeyworkerName(keyWorker.name)
        withKeyworkerStaffCode(keyWorker.deliusStaffCode)
        withKeyworkerAssignedAt(Instant.now())
      }

      upcomingCancelledSpaceBooking = createSpaceBooking(crn = "CRN_UPCOMING_CANCELLED", firstName = "up", lastName = "coming", tier = "U") {
        withPremises(premisesWithBookings)
        withExpectedArrivalDate(LocalDate.parse("2023-01-01"))
        withExpectedDepartureDate(LocalDate.parse("2023-02-01"))
        withActualArrivalDateTime(null)
        withActualDepartureDateTime(null)
        withCanonicalArrivalDate(LocalDate.parse("2023-01-01"))
        withCanonicalDepartureDate(LocalDate.parse("2023-02-01"))
        withKeyworkerName(null)
        withKeyworkerStaffCode(null)
        withKeyworkerAssignedAt(null)
        withCancellationOccurredAt(LocalDate.now())
      }

      nonArrivedSpaceBooking = createSpaceBooking(crn = "CRN_NONARRIVAL", firstName = "None", lastName = "Arrived", tier = "A") {
        withPremises(premisesWithBookings)
        withExpectedArrivalDate(LocalDate.parse("2024-01-02"))
        withExpectedDepartureDate(LocalDate.parse("2024-02-02"))
        withActualArrivalDateTime(null)
        withActualDepartureDateTime(null)
        withCanonicalArrivalDate(LocalDate.parse("2024-01-02"))
        withCanonicalDepartureDate(LocalDate.parse("2024-02-02"))
        withKeyworkerName(null)
        withKeyworkerStaffCode(null)
        withKeyworkerAssignedAt(Instant.now())
        withNonArrivalNotes("Non Arrival Notes")
        withNonArrivalReason(nonArrivalReason)
        withNonArrivalConfirmedAt(Instant.now())
      }
    }

    @Test
    fun `Returns 403 Forbidden if user does not have correct role`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_ASSESSOR))

      webTestClient.get()
        .uri("/cas1/premises/${premisesWithNoBooking.id}/space-bookings")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Empty list returned if no results for given premises`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val response = webTestClient.get()
        .uri("/cas1/premises/${premisesWithNoBooking.id}/space-bookings")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1SpaceBookingSummary>()

      assertThat(response).isEmpty()
    }

    @Test
    fun `Search with no filters excludes cancelled bookings`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val response = webTestClient.get()
        .uri("/cas1/premises/${premisesWithBookings.id}/space-bookings?sortBy=canonicalArrivalDate&sortDirection=asc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1SpaceBookingSummary>()

      assertThat(response).hasSize(6)
      assertThat(response[0].person.crn).isEqualTo("CRN_DEPARTED")
      assertThat(response[1].person.crn).isEqualTo("CRN_CURRENT1")
      assertThat(response[2].person.crn).isEqualTo("CRN_CURRENT2")
      assertThat(response[3].person.crn).isEqualTo("CRN_CURRENT3")
      assertThat(response[4].person.crn).isEqualTo("CRN_UPCOMING")
      assertThat(response[5].person.crn).isEqualTo("CRN_NONARRIVAL")
    }

    @Test
    fun `Filter on residency historic`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val response = webTestClient.get()
        .uri("/cas1/premises/${premisesWithBookings.id}/space-bookings?residency=historic&sortBy=canonicalArrivalDate&sortDirection=asc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1SpaceBookingSummary>()

      assertThat(response).hasSize(2)
      assertThat(response[0].person.crn).isEqualTo("CRN_DEPARTED")
      assertThat(response[1].person.crn).isEqualTo("CRN_NONARRIVAL")
    }

    @Test
    fun `Filter on residency upcoming`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val response = webTestClient.get()
        .uri("/cas1/premises/${premisesWithBookings.id}/space-bookings?residency=upcoming&sortBy=canonicalArrivalDate&sortDirection=asc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1SpaceBookingSummary>()

      assertThat(response).hasSize(1)
      assertThat(response[0].person.crn).isEqualTo("CRN_UPCOMING")
    }

    @Test
    fun `Filter on residency current`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val response = webTestClient.get()
        .uri("/cas1/premises/${premisesWithBookings.id}/space-bookings?residency=current&sortBy=canonicalArrivalDate&sortDirection=asc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1SpaceBookingSummary>()

      assertThat(response).hasSize(3)
      assertThat(response[0].person.crn).isEqualTo("CRN_CURRENT1")
      assertThat(response[1].person.crn).isEqualTo("CRN_CURRENT2")
      assertThat(response[2].person.crn).isEqualTo("CRN_CURRENT3")
    }

    @Test
    fun `Filter on CRN`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val response = webTestClient.get()
        .uri("/cas1/premises/${premisesWithBookings.id}/space-bookings?crnOrName=CRN_CURRENT2&sortBy=canonicalArrivalDate&sortDirection=asc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1SpaceBookingSummary>()

      assertThat(response).hasSize(1)
      assertThat(response[0].person.crn).isEqualTo("CRN_CURRENT2")
    }

    @Test
    fun `Filter on Name`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val response = webTestClient.get()
        .uri("/cas1/premises/${premisesWithBookings.id}/space-bookings?crnOrName=comING&sortBy=canonicalArrivalDate&sortDirection=asc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1SpaceBookingSummary>()

      assertThat(response).hasSize(1)
      assertThat(response[0].person.crn).isEqualTo("CRN_UPCOMING")
    }

    @Test
    fun `Filter on Key Worker Staff Code`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val response = webTestClient.get()
        .uri("/cas1/premises/${premisesWithBookings.id}/space-bookings?keyWorkerStaffCode=${keyWorker.deliusStaffCode}&sortBy=canonicalArrivalDate&sortDirection=asc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1SpaceBookingSummary>()

      assertThat(response).hasSize(1)
      assertThat(response[0].person.crn).isEqualTo("CRN_UPCOMING")

      val spaceBookingKeyWorker = response[0].keyWorkerAllocation!!.keyWorker

      assertThat(spaceBookingKeyWorker.name).isEqualTo(keyWorker.name)
      assertThat(spaceBookingKeyWorker.code).isEqualTo(keyWorker.deliusStaffCode)
    }

    @Test
    fun `Sort on Name`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val response = webTestClient.get()
        .uri("/cas1/premises/${premisesWithBookings.id}/space-bookings?crnOrName=CUrt&sortBy=personName&sortDirection=desc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1SpaceBookingSummary>()

      assertThat(response).hasSize(3)
      assertThat(response[0].person.crn).isEqualTo("CRN_CURRENT3")
      assertThat(response[1].person.crn).isEqualTo("CRN_CURRENT2")
      assertThat(response[2].person.crn).isEqualTo("CRN_CURRENT1")
    }

    @Test
    fun `Sort on Canonical Arrival Date`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val response = webTestClient.get()
        .uri("/cas1/premises/${premisesWithBookings.id}/space-bookings?sortBy=canonicalArrivalDate&sortDirection=desc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1SpaceBookingSummary>()

      assertThat(response).hasSize(6)
      assertThat(response[0].person.crn).isEqualTo("CRN_NONARRIVAL")
      assertThat(response[1].person.crn).isEqualTo("CRN_UPCOMING")
      assertThat(response[2].person.crn).isEqualTo("CRN_CURRENT3")
      assertThat(response[3].person.crn).isEqualTo("CRN_CURRENT2")
      assertThat(response[4].person.crn).isEqualTo("CRN_CURRENT1")
      assertThat(response[5].person.crn).isEqualTo("CRN_DEPARTED")
    }

    @Test
    fun `Sort on Canonical Departure Date`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val response = webTestClient.get()
        .uri("/cas1/premises/${premisesWithBookings.id}/space-bookings?sortBy=canonicalDepartureDate&sortDirection=asc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1SpaceBookingSummary>()

      assertThat(response).hasSize(6)
      assertThat(response[0].person.crn).isEqualTo("CRN_DEPARTED")
      assertThat(response[1].person.crn).isEqualTo("CRN_CURRENT1")
      assertThat(response[2].person.crn).isEqualTo("CRN_CURRENT2")
      assertThat(response[3].person.crn).isEqualTo("CRN_CURRENT3")
      assertThat(response[4].person.crn).isEqualTo("CRN_UPCOMING")
      assertThat(response[5].person.crn).isEqualTo("CRN_NONARRIVAL")
    }

    @Test
    fun `Sort on Keyworker`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val response = webTestClient.get()
        .uri("/cas1/premises/${premisesWithBookings.id}/space-bookings?residency=current&sortBy=keyWorkerName&sortDirection=asc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1SpaceBookingSummary>()

      assertThat(response).hasSize(3)
      assertThat(response[0].keyWorkerAllocation!!.keyWorker.name).isEqualTo("Clive Keyworker")
      assertThat(response[1].keyWorkerAllocation!!.keyWorker.name).isEqualTo("Kathy Keyworker")
      assertThat(response[2].keyWorkerAllocation).isNull()
    }

    @Test
    fun `Sort on Tier`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val response = webTestClient.get()
        .uri("/cas1/premises/${premisesWithBookings.id}/space-bookings?sortBy=tier&sortDirection=desc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1SpaceBookingSummary>()

      assertThat(response).hasSize(6)

      assertThat(response[0].tier).isEqualTo("U")
      assertThat(response[0].person.crn).isEqualTo("CRN_UPCOMING")

      assertThat(response[1].tier).isEqualTo("D")
      assertThat(response[1].person.crn).isEqualTo("CRN_DEPARTED")

      assertThat(response[2].tier).isEqualTo("C")
      assertThat(response[2].person.crn).isEqualTo("CRN_CURRENT2")

      assertThat(response[3].tier).isEqualTo("B")
      assertThat(response[3].person.crn).isEqualTo("CRN_CURRENT3")

      assertThat(response[4].tier).isEqualTo("A")
      assertThat(response[4].person.crn).isEqualTo("CRN_CURRENT1")

      assertThat(response[5].tier).isEqualTo("A")
      assertThat(response[5].person.crn).isEqualTo("CRN_NONARRIVAL")
    }
  }

  @Nested
  inner class StatusesForSpaceBooking : SpaceBookingIntegrationTestBase() {

    @BeforeAll
    fun setupTestData() {
      super.setupRegionAndKeyWorkerAndPremises()
    }

    @ParameterizedTest
    @MethodSource("spaceBookingSummaryStatusCases")
    fun `Search generates the correct statuses`(
      testCaseForSpaceBookingSummaryStatus: TestCaseForSpaceBookingSummaryStatus,
      expectedResultStatus: Cas1SpaceBookingSummaryStatus,
    ) {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val crnForStatusTest = UUID.randomUUID().toString()

      currentSpaceBooking1 = createSpaceBooking(crn = crnForStatusTest, firstName = "curt", lastName = "rent 1", tier = "A") {
        withPremises(premisesWithBookings)
        withExpectedArrivalDate(testCaseForSpaceBookingSummaryStatus.expectedArrivalDate)
        withExpectedDepartureDate(testCaseForSpaceBookingSummaryStatus.expectedDepartureDate)
        withActualArrivalDateTime(testCaseForSpaceBookingSummaryStatus.actualArrivalDateTime?.toInstant(ZoneOffset.UTC))
        withActualDepartureDateTime(testCaseForSpaceBookingSummaryStatus.actualDepartureDateTime?.toInstant(ZoneOffset.UTC))
        withCanonicalArrivalDate(testCaseForSpaceBookingSummaryStatus.expectedArrivalDate)
        withCanonicalDepartureDate(testCaseForSpaceBookingSummaryStatus.expectedDepartureDate)
        withNonArrivalConfirmedAt(testCaseForSpaceBookingSummaryStatus.nonArrivalConfirmedAtDateTime?.toInstant(ZoneOffset.UTC))
        withKeyworkerName(null)
        withKeyworkerStaffCode(null)
        withKeyworkerAssignedAt(Instant.now())
      }

      val response = webTestClient.get()
        .uri("/cas1/premises/${premisesWithBookings.id}/space-bookings?sortBy=canonicalArrivalDate&sortDirection=asc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1SpaceBookingSummary>()

      val result = response
        .first { it.person.crn == crnForStatusTest }

      assertThat(result.person.crn).isEqualTo(crnForStatusTest)
      assertThat(result.status).isEqualTo(expectedResultStatus)
    }

    fun spaceBookingSummaryStatusCases(): Stream<Arguments> {
      return Cas1SpaceBookingSummaryStatusTestHelper().spaceBookingSummaryStatusCases()
    }
  }

  @Nested
  inner class GetASpaceBooking : InitialiseDatabasePerClassTestBase() {
    lateinit var premises: ApprovedPremisesEntity
    lateinit var otherPremises: ApprovedPremisesEntity

    lateinit var spaceBooking: Cas1SpaceBookingEntity
    lateinit var otherSpaceBookingAtPremises: Cas1SpaceBookingEntity
    lateinit var otherSpaceBookingAtPremisesCancelled: Cas1SpaceBookingEntity
    lateinit var otherSpaceBookingAtPremisesDifferentCrn: Cas1SpaceBookingEntity
    lateinit var otherSpaceBookingNotAtPremises: Cas1SpaceBookingEntity

    @BeforeAll
    fun setupTestData() {
      val region = givenAProbationRegion()

      premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedProbationRegion { region }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      }

      otherPremises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedProbationRegion { region }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      }

      val (user) = givenAUser()
      val (offender) = givenAnOffender()
      val (placementRequest) = givenAPlacementRequest(
        placementRequestAllocatedTo = user,
        assessmentAllocatedTo = user,
        createdByUser = user,
      )

      spaceBooking = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.parse("2029-05-29"))
        withCanonicalDepartureDate(LocalDate.parse("2029-06-29"))
      }

      otherSpaceBookingAtPremises = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.parse("2030-05-29"))
        withCanonicalDepartureDate(LocalDate.parse("2030-06-29"))
      }

      otherSpaceBookingAtPremisesDifferentCrn = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn("othercrn")
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.parse("2031-05-29"))
        withCanonicalDepartureDate(LocalDate.parse("2031-06-29"))
      }

      otherSpaceBookingAtPremisesCancelled = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.parse("2031-05-29"))
        withCanonicalDepartureDate(LocalDate.parse("2031-06-29"))
        withCancellationOccurredAt(LocalDate.parse("2020-01-01"))
      }

      otherSpaceBookingNotAtPremises = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withPremises(otherPremises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.parse("2032-05-29"))
        withCanonicalDepartureDate(LocalDate.parse("2032-06-29"))
      }
    }

    @Test
    fun `Returns 403 Forbidden if user does not have correct role`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_ASSESSOR))

      webTestClient.get()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Returns 404 if premise doesn't exist`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      webTestClient.get()
        .uri("/cas1/premises/${UUID.randomUUID()}/space-bookings/${spaceBooking.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `Returns 404 if space booking doesn't exist`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      webTestClient.get()
        .uri("/cas1/premises/${premises.id}/space-bookings/${UUID.randomUUID()}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `Returns premises information if have correct role`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val response = webTestClient.get()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1SpaceBooking::class.java).responseBody.blockFirst()!!

      assertThat(response.id).isEqualTo(spaceBooking.id)
      assertThat(response.otherBookingsInPremisesForCrn).hasSize(1)
      assertThat(response.otherBookingsInPremisesForCrn[0].id).isEqualTo(otherSpaceBookingAtPremises.id)
      assertThat(response.requestForPlacementId).isEqualTo(spaceBooking.placementRequest!!.id)
    }
  }

  @Nested
  inner class RecordArrival : InitialiseDatabasePerClassTestBase() {

    lateinit var region: ProbationRegionEntity
    lateinit var premises: ApprovedPremisesEntity
    lateinit var spaceBooking: Cas1SpaceBookingEntity

    @BeforeAll
    fun setupTestData() {
      region = givenAProbationRegion()

      premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedProbationRegion { region }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      }

      val (user) = givenAUser()
      val (offender) = givenAnOffender()
      val (placementRequest) = givenAPlacementRequest(
        placementRequestAllocatedTo = user,
        assessmentAllocatedTo = user,
        createdByUser = user,
      )

      spaceBooking = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.parse("2029-05-29"))
        withCanonicalDepartureDate(LocalDate.parse("2029-06-29"))
        withKeyworkerAssignedAt(Instant.now())
      }
    }

    @Test
    fun `Returns 403 Forbidden if user does not have correct permission`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_ASSESSOR))

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/arrival")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NewArrival(
            arrivalDateTime = LocalDateTime.now().toInstant(ZoneOffset.UTC),
          ),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Returns 500 Internal Server Error if unexpected failure occurs - invalid key worker )`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val (user) = givenAUser()
      val (offender) = givenAnOffender()
      val (placementRequest) = givenAPlacementRequest(
        placementRequestAllocatedTo = user,
        assessmentAllocatedTo = user,
        createdByUser = user,
      )

      val unknownKeyWorker = UserEntityFactory()
        .withDefaults()
        .produce()

      val spaceBooking = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.parse("2029-05-29"))
        withCanonicalDepartureDate(LocalDate.parse("2029-06-29"))
        withKeyworkerName(unknownKeyWorker.name)
        withKeyworkerStaffCode(unknownKeyWorker.deliusStaffCode)
        withKeyworkerAssignedAt(Instant.now())
      }

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/arrival")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NewArrival(
            arrivalDateTime = LocalDateTime.now().toInstant(ZoneOffset.UTC),
          ),
        )
        .exchange()
        .expectStatus()
        .is5xxServerError
        .expectBody()
        .jsonPath("title").isEqualTo("Internal Server Error")
        .jsonPath("status").isEqualTo(500)
        .jsonPath("detail").isEqualTo("There was an unexpected problem")
    }

    @Test
    fun `Recording arrival returns OK and creates a domain event`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val (user) = givenAUser()
      val (offender) = givenAnOffender()
      val (placementRequest) = givenAPlacementRequest(
        placementRequestAllocatedTo = user,
        assessmentAllocatedTo = user,
        createdByUser = user,
      )

      spaceBooking = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.parse("2029-05-29"))
        withCanonicalDepartureDate(LocalDate.parse("2029-06-29"))
        withKeyworkerName(user.name)
        withKeyworkerStaffCode(user.deliusStaffCode)
        withKeyworkerAssignedAt(Instant.now())
        withDeliusEventNumber("25")
      }

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/arrival")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NewArrival(
            arrivalDateTime = LocalDateTime.now().toInstant(ZoneOffset.UTC),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
      domainEventAsserter.assertDomainEventOfTypeStored(spaceBooking.application!!.id, DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED)
    }
  }

  @Nested
  inner class RecordNonArrival : InitialiseDatabasePerClassTestBase() {

    lateinit var region: ProbationRegionEntity
    lateinit var premises: ApprovedPremisesEntity
    lateinit var nonArrivalReason: NonArrivalReasonEntity
    lateinit var spaceBooking: Cas1SpaceBookingEntity

    @BeforeAll
    fun setupTestData() {
      region = givenAProbationRegion()

      premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedProbationRegion { region }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      }

      nonArrivalReason = nonArrivalReasonEntityFactory.produceAndPersist {
        withName("nonArrivalName")
        withLegacyDeliusReasonCode("legacyDeliusCode")
      }

      val (user) = givenAUser()
      val (offender) = givenAnOffender()
      val (placementRequest) = givenAPlacementRequest(
        placementRequestAllocatedTo = user,
        assessmentAllocatedTo = user,
        createdByUser = user,
      )

      spaceBooking = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.parse("2029-05-29"))
        withCanonicalDepartureDate(LocalDate.parse("2029-06-29"))
        withKeyworkerAssignedAt(Instant.now())
      }
    }

    @Test
    fun `Returns 403 Forbidden if user does not have correct permission`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_ASSESSOR))

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/non-arrival")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NonArrival(
            reason = nonArrivalReason.id,
            notes = null,
          ),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Returns 500 Internal Server Error if unexpected failure occurs - invalid offender CRN )`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val (user) = givenAUser()
      val (placementRequest) = givenAPlacementRequest(
        placementRequestAllocatedTo = user,
        assessmentAllocatedTo = user,
        createdByUser = user,
      )

      val unknownKeyWorker = UserEntityFactory()
        .withDefaults()
        .produce()

      val spaceBooking = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn("unknownCRN")
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.parse("2029-05-29"))
        withCanonicalDepartureDate(LocalDate.parse("2029-06-29"))
        withKeyworkerName(unknownKeyWorker.name)
        withKeyworkerStaffCode(unknownKeyWorker.deliusStaffCode)
        withKeyworkerAssignedAt(Instant.now())
      }

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/non-arrival")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NonArrival(
            reason = nonArrivalReason.id,
            notes = null,
          ),
        )
        .exchange()
        .expectStatus()
        .is5xxServerError
        .expectBody()
        .jsonPath("title").isEqualTo("Internal Server Error")
        .jsonPath("status").isEqualTo(500)
        .jsonPath("detail").isEqualTo("There was an unexpected problem")
    }

    @Test
    fun `Recording non-arrival returns OK and creates a domain event`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val (user) = givenAUser()
      val (offender) = givenAnOffender()
      val (placementRequest) = givenAPlacementRequest(
        placementRequestAllocatedTo = user,
        assessmentAllocatedTo = user,
        createdByUser = user,
      )

      spaceBooking = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.parse("2029-05-29"))
        withCanonicalDepartureDate(LocalDate.parse("2029-06-29"))
        withKeyworkerName(user.name)
        withKeyworkerStaffCode(user.deliusStaffCode)
        withKeyworkerAssignedAt(Instant.now())
        withDeliusEventNumber("25")
      }

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/non-arrival")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NonArrival(
            reason = nonArrivalReason.id,
            notes = "non arrival reason notes",
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
      domainEventAsserter.assertDomainEventOfTypeStored(spaceBooking.application!!.id, DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED)

      val updatedSpaceBooking = cas1SpaceBookingRepository.findByIdOrNull(spaceBooking.id)!!
      assertThat(updatedSpaceBooking.nonArrivalNotes).isEqualTo("non arrival reason notes")
      assertThat(updatedSpaceBooking.nonArrivalReason).isEqualTo(nonArrivalReason)
      assertThat(updatedSpaceBooking.nonArrivalConfirmedAt).isWithinTheLastMinute()
    }
  }

  @Nested
  inner class RecordKeyWorker : InitialiseDatabasePerClassTestBase() {
    lateinit var region: ProbationRegionEntity
    lateinit var premises: ApprovedPremisesEntity
    lateinit var spaceBooking: Cas1SpaceBookingEntity
    lateinit var keyWorker: UserEntity

    @BeforeAll
    fun setupTestData() {
      region = givenAProbationRegion()

      premises = approvedPremisesEntityFactory.produceAndPersist {
        withQCode("QCODE")
        withYieldedProbationRegion { region }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      }

      keyWorker = givenAUser().first
      val (user) = givenAUser()
      val (offender) = givenAnOffender()
      val (placementRequest) = givenAPlacementRequest(
        placementRequestAllocatedTo = user,
        assessmentAllocatedTo = user,
        createdByUser = user,
      )

      spaceBooking = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.parse("2029-05-29"))
        withCanonicalDepartureDate(LocalDate.parse("2029-06-29"))
        withKeyworkerAssignedAt(Instant.now())
        withDeliusEventNumber("75")
      }
    }

    @Test
    fun `Returns 403 Forbidden if user does not have correct permission`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_ASSESSOR))
      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/keyworker")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1AssignKeyWorker(
            keyWorker.deliusStaffCode,
          ),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Recording key worker returns OK and emits domain event`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val keyWorker = ContextStaffMemberFactory().produce()
      apDeliusContextMockSuccessfulStaffMembersCall(keyWorker, "QCODE")

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/keyworker")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1AssignKeyWorker(
            keyWorker.code,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      domainEventAsserter.assertDomainEventOfTypeStored(spaceBooking.application!!.id, DomainEventType.APPROVED_PREMISES_BOOKING_KEYWORKER_ASSIGNED)

      val updatedSpaceBooking = cas1SpaceBookingRepository.findByIdOrNull(spaceBooking.id)!!
      assertThat(updatedSpaceBooking.keyWorkerName).isEqualTo("${keyWorker.name.forename} ${keyWorker.name.surname}")
      assertThat(updatedSpaceBooking.keyWorkerStaffCode).isEqualTo(keyWorker.code)
      assertThat(updatedSpaceBooking.keyWorkerAssignedAt).isWithinTheLastMinute()
    }
  }

  @Nested
  inner class RecordDeparture : InitialiseDatabasePerClassTestBase() {

    val departureReasonId = UUID.fromString("a9f64800-9f16-4096-b8f1-b03960fc728a")
    val departureMoveOnCategoryId = UUID.fromString("48ad4a94-f81f-4cd5-a564-ad1974d5cf67")

    lateinit var region: ProbationRegionEntity
    lateinit var premises: ApprovedPremisesEntity
    lateinit var spaceBooking: Cas1SpaceBookingEntity

    @BeforeAll
    fun setupTestData() {
      region = givenAProbationRegion()

      premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedProbationRegion { region }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      }

      val (user) = givenAUser()
      val (offender) = givenAnOffender()
      val (placementRequest) = givenAPlacementRequest(
        placementRequestAllocatedTo = user,
        assessmentAllocatedTo = user,
        createdByUser = user,
      )

      spaceBooking = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.parse("2029-05-29"))
        withCanonicalDepartureDate(LocalDate.parse("2029-06-29"))
        withKeyworkerAssignedAt(Instant.now())
      }
    }

    @Test
    fun `Returns 403 Forbidden if user does not have correct permission`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_ASSESSOR))

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/departure")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NewDeparture(
            departureDateTime = LocalDateTime.now().toInstant(ZoneOffset.UTC),
            reasonId = departureReasonId,
            moveOnCategoryId = departureMoveOnCategoryId,
          ),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Returns 500 Internal Server Error if unexpected failure occurs - invalid key worker )`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val (user) = givenAUser()
      val (offender) = givenAnOffender()
      val (placementRequest) = givenAPlacementRequest(
        placementRequestAllocatedTo = user,
        assessmentAllocatedTo = user,
        createdByUser = user,
      )

      val unknownKeyWorker = UserEntityFactory()
        .withDefaults()
        .produce()

      val spaceBooking = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.now().minusDays(30))
        withActualArrivalDateTime(LocalDateTime.now().minusDays(30).toInstant(ZoneOffset.UTC))
        withCanonicalDepartureDate(LocalDate.now())
        withKeyworkerName(unknownKeyWorker.name)
        withKeyworkerStaffCode(unknownKeyWorker.deliusStaffCode)
        withKeyworkerAssignedAt(Instant.now())
      }

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/departure")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NewDeparture(
            departureDateTime = LocalDateTime.now().toInstant(ZoneOffset.UTC),
            reasonId = departureReasonId,
            moveOnCategoryId = departureMoveOnCategoryId,
          ),
        )
        .exchange()
        .expectStatus()
        .is5xxServerError
        .expectBody()
        .jsonPath("title").isEqualTo("Internal Server Error")
        .jsonPath("status").isEqualTo(500)
        .jsonPath("detail").isEqualTo("There was an unexpected problem")
    }

    @Test
    fun `Recording departure returns OK and creates a domain event`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val (user) = givenAUser()
      val (offender) = givenAnOffender()
      val (placementRequest) = givenAPlacementRequest(
        placementRequestAllocatedTo = user,
        assessmentAllocatedTo = user,
        createdByUser = user,
      )

      spaceBooking = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.now().minusDays(30))
        withActualArrivalDateTime(LocalDateTime.now().minusDays(30).toInstant(ZoneOffset.UTC))
        withCanonicalDepartureDate(LocalDate.now())
        withKeyworkerName(user.name)
        withKeyworkerStaffCode(user.deliusStaffCode)
        withKeyworkerAssignedAt(Instant.now())
        withDeliusEventNumber("50")
      }

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/departure")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NewDeparture(
            departureDateTime = LocalDateTime.now().toInstant(ZoneOffset.UTC),
            reasonId = departureReasonId,
            moveOnCategoryId = departureMoveOnCategoryId,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
      domainEventAsserter.assertDomainEventOfTypeStored(spaceBooking.application!!.id, DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED)
    }
  }

  @Nested
  inner class Cancellation : InitialiseDatabasePerClassTestBase() {

    lateinit var applicant: UserEntity
    lateinit var placementApplicationCreator: UserEntity
    lateinit var application: ApprovedPremisesApplicationEntity
    lateinit var region: ProbationRegionEntity
    lateinit var premises: ApprovedPremisesEntity
    lateinit var spaceBooking: Cas1SpaceBookingEntity
    lateinit var cancellationReason: CancellationReasonEntity

    @BeforeAll
    fun setupTestData() {
      region = givenAProbationRegion()

      premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedProbationRegion { region }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withEmailAddress("premises@test.com")
      }

      applicant = givenAUser(
        staffDetail =
        StaffDetailFactory.staffDetail(email = "applicant@test.com"),
      ).first

      placementApplicationCreator = givenAUser(
        staffDetail =
        StaffDetailFactory.staffDetail(email = "placementApplicant@test.com"),
      ).first

      val (offender) = givenAnOffender()

      application = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withCreatedByUser(applicant)
        withApplicationSchema(approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist())
        withApArea(givenAnApArea())
        withSubmittedAt(OffsetDateTime.now())
        withCruManagementArea(givenACas1CruManagementArea())
      }

      val placementApplication = placementApplicationFactory.produceAndPersist {
        withCreatedByUser(placementApplicationCreator)
        withSchemaVersion(approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist())
        withApplication(application)
      }

      val (placementRequest) = givenAPlacementRequest(
        application = application,
        placementRequestAllocatedTo = applicant,
        assessmentAllocatedTo = applicant,
        createdByUser = applicant,
        placementApplication = placementApplication,
      )

      application = placementRequest.application

      spaceBooking = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(applicant)
        withCanonicalArrivalDate(LocalDate.parse("2029-05-29"))
        withCanonicalDepartureDate(LocalDate.parse("2029-06-29"))
        withKeyworkerAssignedAt(Instant.now())
      }

      cancellationReason = cancellationReasonEntityFactory.produceAndPersist {
        withServiceScope("*")
      }
    }

    @Test
    fun `Create Cancellation without JWT returns 401`() {
      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/cancellations")
        .bodyValue(
          NewCas1SpaceBookingCancellation(
            occurredAt = LocalDate.parse("2022-08-17"),
            reasonId = cancellationReason.id,
            reasonNotes = null,
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @EnumSource(
      value = UserRole::class,
      names = ["CAS1_WORKFLOW_MANAGER", "CAS1_CRU_MEMBER", "CAS1_JANITOR"],
      mode = EnumSource.Mode.EXCLUDE,
    )
    fun `Create Cancellation with invalid role returns 401`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.post()
          .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/cancellations")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewCas1SpaceBookingCancellation(
              occurredAt = LocalDate.parse("2022-08-17"),
              reasonId = cancellationReason.id,
              reasonNotes = null,
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Create Cancellation on CAS1 Booking returns OK with correct body, updates status and sends emails when user has role CRU_MEMBER`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { _, jwt ->
        webTestClient.post()
          .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/cancellations")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewCas1SpaceBookingCancellation(
              occurredAt = LocalDate.parse("2022-08-17"),
              reasonId = cancellationReason.id,
              reasonNotes = null,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
      }

      emailAsserter.assertEmailsRequestedCount(4)
      emailAsserter.assertEmailRequested(applicant.email!!, notifyConfig.templates.bookingWithdrawnV2)
      emailAsserter.assertEmailRequested(placementApplicationCreator.email!!, notifyConfig.templates.bookingWithdrawnV2)
      emailAsserter.assertEmailRequested(spaceBooking.premises.emailAddress!!, notifyConfig.templates.bookingWithdrawnV2)
      emailAsserter.assertEmailRequested(application.cruManagementArea!!.emailAddress!!, notifyConfig.templates.bookingWithdrawnV2)

      assertThat(approvedPremisesApplicationRepository.findByIdOrNull(spaceBooking.application!!.id)!!.status)
        .isEqualTo(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
    }
  }
}

abstract class SpaceBookingIntegrationTestBase : InitialiseDatabasePerClassTestBase() {
  lateinit var premisesWithNoBooking: ApprovedPremisesEntity
  lateinit var premisesWithBookings: ApprovedPremisesEntity
  lateinit var nonArrivalReason: NonArrivalReasonEntity
  lateinit var currentSpaceBooking1: Cas1SpaceBookingEntity

  lateinit var keyWorker: UserEntity

  protected fun setupRegionAndKeyWorkerAndPremises() {
    val region = givenAProbationRegion()
    keyWorker = givenAUser().first

    nonArrivalReason = nonArrivalReasonEntityFactory.produceAndPersist {
      withName("nonArrivalName")
      withLegacyDeliusReasonCode("legacyDeliusCode")
    }

    premisesWithNoBooking = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedProbationRegion { region }
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
    }

    premisesWithBookings = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedProbationRegion { region }
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
    }
  }

  protected fun createSpaceBooking(
    crn: String,
    firstName: String,
    lastName: String,
    tier: String,
    configuration: Cas1SpaceBookingEntityFactory.() -> Unit,
  ): Cas1SpaceBookingEntity {
    val (user) = givenAUser()
    val (offender) = givenAnOffender(offenderDetailsConfigBlock = {
      withCrn(crn)
      withFirstName(firstName)
      withLastName(lastName)
    })
    val (placementRequest) = givenAPlacementRequest(
      placementRequestAllocatedTo = user,
      assessmentAllocatedTo = user,
      createdByUser = user,
      crn = offender.otherIds.crn,
      name = "$firstName $lastName",
      tier = tier,
    )

    return cas1SpaceBookingEntityFactory.produceAndPersist {
      withCrn(offender.otherIds.crn)
      withPremises(premisesWithBookings)
      withPlacementRequest(placementRequest)
      withApplication(placementRequest.application)
      withCreatedBy(user)

      configuration.invoke(this)
    }
  }
}
