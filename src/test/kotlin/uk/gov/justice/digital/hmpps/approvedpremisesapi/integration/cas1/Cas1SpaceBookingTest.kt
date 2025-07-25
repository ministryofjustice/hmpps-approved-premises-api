package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApprovedPlacementAppeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssignKeyWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewEmergencyTransfer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewPlannedTransfer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewSpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewSpaceBookingCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NonArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1UpdateSpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonSummaryDiscriminator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1ChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASubmittedCas1Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOfflineApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddSingleCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddSingleResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulStaffMembersCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository.Companion.UPCOMING_EXPECTED_DEPARTURE_THRESHOLD_DATE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository.Constants.MOVE_ON_CATEGORY_NOT_APPLICABLE_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TransferType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_ASSESSOR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_FUTURE_MANAGER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestRejectionReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsObject
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class Cas1SpaceBookingTest {

  @Nested
  inner class CreateABooking : InitialiseDatabasePerClassTestBase() {

    @Autowired
    lateinit var transformer: Cas1SpaceBookingTransformer

    @Test
    fun `Booking a space without JWT returns 401`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA)) { user, _ ->
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
      givenAUser(roles = listOf(CAS1_FUTURE_MANAGER)) { _, jwt ->
        val placementRequestId = UUID.randomUUID()
        val premises = givenAnApprovedPremises()

        webTestClient.post()
          .uri("/cas1/placement-requests/$placementRequestId/space-bookings")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            Cas1NewSpaceBooking(
              arrivalDate = LocalDate.now().plusDays(1),
              departureDate = LocalDate.now().plusDays(8),
              premisesId = premises.id,
              characteristics = listOf(),
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Booking a space for an unknown placement request returns 400 Bad Request`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA)) { _, jwt ->
        val premises = givenAnApprovedPremises(supportsSpaceBookings = true)

        val placementRequestId = UUID.randomUUID()

        webTestClient.post()
          .uri("/cas1/placement-requests/$placementRequestId/space-bookings")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            Cas1NewSpaceBooking(
              arrivalDate = LocalDate.now().plusDays(1),
              departureDate = LocalDate.now().plusDays(8),
              premisesId = premises.id,
              characteristics = listOf(),
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
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA)) { user, jwt ->
        givenAPlacementRequest(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
        ) { placementRequest, _ ->
          webTestClient.post()
            .uri("/cas1/placement-requests/${placementRequest.id}/space-bookings")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas1NewSpaceBooking(
                arrivalDate = LocalDate.now().plusDays(1),
                departureDate = LocalDate.now().plusDays(8),
                premisesId = UUID.randomUUID(),
                characteristics = listOf(),
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
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA)) { user, jwt ->
        givenAPlacementRequest(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
        ) { placementRequest, _ ->
          val premises = givenAnApprovedPremises(supportsSpaceBookings = true)

          webTestClient.post()
            .uri("/cas1/placement-requests/${placementRequest.id}/space-bookings")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas1NewSpaceBooking(
                arrivalDate = LocalDate.now().plusDays(1),
                departureDate = LocalDate.now(),
                premisesId = premises.id,
                characteristics = listOf(),
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
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA)) { applicant, jwt ->
        givenAPlacementRequest(
          placementRequestAllocatedTo = applicant,
          assessmentAllocatedTo = applicant,
          createdByUser = applicant,
          caseManager = cas1ApplicationUserDetailsEntityFactory.produceAndPersist {
            withEmailAddress("caseManager@test.com")
          },
        ) { placementRequest, application ->
          val characteristics = listOf(
            Cas1SpaceCharacteristic.hasEnSuite,
            Cas1SpaceCharacteristic.isArsonSuitable,
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

          val premises = givenAnApprovedPremises(supportsSpaceBookings = true)

          apDeliusContextAddSingleCaseSummaryToBulkResponse(
            CaseSummaryFactory()
              .withCrn(application.crn)
              .produce(),
          )

          val response = webTestClient.post()
            .uri("/cas1/placement-requests/${placementRequest.id}/space-bookings")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas1NewSpaceBooking(
                arrivalDate = LocalDate.now().plusDays(1),
                departureDate = LocalDate.now().plusDays(8),
                premisesId = premises.id,
                characteristics = characteristics,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(Cas1SpaceBooking::class.java)

          val result = response.responseBody.blockFirst()!!

          assertThat(result.person).isInstanceOf(FullPerson::class.java)
          assertThat(result.characteristics).containsExactlyInAnyOrderElementsOf(
            characteristics,
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

          domainEventAsserter.assertDomainEventOfTypeStored(
            placementRequest.application.id,
            DomainEventType.APPROVED_PREMISES_BOOKING_MADE,
          )

          emailAsserter.assertEmailsRequestedCount(3)
          emailAsserter.assertEmailRequested(applicant.email!!, Cas1NotifyTemplates.BOOKING_MADE)
          emailAsserter.assertEmailRequested(premises.emailAddress!!, Cas1NotifyTemplates.BOOKING_MADE_FOR_PREMISES)
          emailAsserter.assertEmailRequested(
            placementRequest.application.caseManagerUserDetails!!.email!!,
            Cas1NotifyTemplates.BOOKING_MADE,
          )

          assertThat(approvedPremisesApplicationRepository.findByIdOrNull(placementRequest.application.id)!!.status)
            .isEqualTo(ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED)
        }
      }
    }
  }

  @Nested
  inner class SearchForSpaceBookings : SpaceBookingIntegrationTestBase() {
    lateinit var currentSpaceBooking2OfflineApplication: Cas1SpaceBookingEntity
    lateinit var currentSpaceBooking3: Cas1SpaceBookingEntity
    lateinit var currentSpaceBooking4Restricted: Cas1SpaceBookingEntity
    lateinit var upcomingSpaceBookingWithKeyWorker: Cas1SpaceBookingEntity
    lateinit var upcomingCancelledSpaceBooking: Cas1SpaceBookingEntity
    lateinit var departedSpaceBooking: Cas1SpaceBookingEntity
    lateinit var nonArrivedSpaceBooking: Cas1SpaceBookingEntity
    lateinit var legacySpaceBookingNoArrival: Cas1SpaceBookingEntity
    lateinit var legacySpaceBookingNoDeparture: Cas1SpaceBookingEntity
    lateinit var upcomingSpaceBookingBeforeThreshold: Cas1SpaceBookingEntity

    @SuppressWarnings("LongMethod")
    @BeforeAll
    fun setupTestData() {
      super.setupRegionAndKeyWorkerAndPremises()
      characteristicRepository.deleteAll()

      val criteria = mutableListOf(
        characteristicEntityFactory.produceAndPersist {
          withName("Single room")
          withPropertyName("isSingle")
          withServiceScope("approved-premises")

          withModelScope("room")
        },
        characteristicEntityFactory.produceAndPersist {
          withName("Wheelchair accessible")
          withPropertyName("isWheelchairAccessible")
          withServiceScope("approved-premises")
          withModelScope("premises")
        },
      )

      currentSpaceBooking1 =
        createSpaceBooking(crn = "CRN_CURRENT1", firstName = "curt", lastName = "rent 1", tier = "A") {
          withPremises(premisesWithBookings)
          withExpectedArrivalDate(LocalDate.parse("2026-01-02"))
          withExpectedDepartureDate(LocalDate.parse("2026-02-02"))
          withActualArrivalDate(LocalDate.parse("2026-01-02"))
          withActualDepartureDate(null)
          withCanonicalArrivalDate(LocalDate.parse("2026-01-02"))
          withCanonicalDepartureDate(LocalDate.parse("2026-02-02"))
          withKeyworkerName(null)
          withKeyworkerStaffCode(null)
          withKeyworkerAssignedAt(Instant.now())
          withCriteria(criteria)
        }

      givenACas1ChangeRequest(
        type = ChangeRequestType.PLACEMENT_APPEAL,
        decision = null,
        decisionJson = null,
        spaceBooking = currentSpaceBooking1,
        resolved = false,
      )

      givenACas1ChangeRequest(
        type = ChangeRequestType.PLACEMENT_APPEAL,
        decision = ChangeRequestDecision.APPROVED,
        decisionJson = "{\"test\": 1}",
        spaceBooking = currentSpaceBooking1,
        resolved = true,
      )

      currentSpaceBooking2OfflineApplication = createSpaceBookingWithOfflineApplication(
        crn = "CRN_CURRENT2_OFFLINE",
        firstName = "curt",
        lastName = "rent 2",
      ) {
        withPremises(premisesWithBookings)
        withExpectedArrivalDate(LocalDate.parse("2026-02-02"))
        withExpectedDepartureDate(LocalDate.parse("2026-09-02"))
        withActualArrivalDate(LocalDate.parse("2026-01-02"))
        withActualDepartureDate(null)
        withCanonicalArrivalDate(LocalDate.parse("2026-02-02"))
        withCanonicalDepartureDate(LocalDate.parse("2026-03-02"))
        withKeyworkerName("Kathy Keyworker")
        withKeyworkerStaffCode("kathyk")
        withKeyworkerAssignedAt(Instant.now())
      }

      currentSpaceBooking3 =
        createSpaceBooking(crn = "CRN_CURRENT3", firstName = "curt", lastName = "rent 3", tier = "B") {
          withPremises(premisesWithBookings)
          withExpectedArrivalDate(LocalDate.parse("2026-03-02"))
          withExpectedDepartureDate(LocalDate.parse("2026-04-02"))
          withActualArrivalDate(LocalDate.parse("2026-01-02"))
          withActualDepartureDate(null)
          withCanonicalArrivalDate(LocalDate.parse("2026-04-02"))
          withCanonicalDepartureDate(LocalDate.parse("2026-05-02"))
          withKeyworkerName("Clive Keyworker")
          withKeyworkerStaffCode("clivek")
          withKeyworkerAssignedAt(Instant.now())
        }

      givenACas1ChangeRequest(
        type = ChangeRequestType.PLANNED_TRANSFER,
        decision = null,
        decisionJson = null,
        spaceBooking = currentSpaceBooking3,
        resolved = false,
      )

      givenACas1ChangeRequest(
        type = ChangeRequestType.PLANNED_TRANSFER,
        decision = ChangeRequestDecision.APPROVED,
        decisionJson = "{\"test\": 1}",
        spaceBooking = currentSpaceBooking3,
        resolved = true,
      )

      currentSpaceBooking4Restricted = createSpaceBooking(
        crn = "CRN_CURRENT4",
        firstName = "curt",
        lastName = "rent 4",
        tier = "B",
        isRestricted = true,
      ) {
        withPremises(premisesWithBookings)
        withExpectedArrivalDate(LocalDate.parse("2026-03-03"))
        withExpectedDepartureDate(LocalDate.parse("2026-04-03"))
        withActualArrivalDate(LocalDate.parse("2026-01-02"))
        withActualDepartureDate(null)
        withCanonicalArrivalDate(LocalDate.parse("2026-04-03"))
        withCanonicalDepartureDate(LocalDate.parse("2026-05-03"))
        withKeyworkerName("Clive Keyworker")
        withKeyworkerStaffCode("clivek")
        withKeyworkerAssignedAt(Instant.now())
      }

      departedSpaceBooking =
        createSpaceBooking(crn = "CRN_DEPARTED", firstName = "de", lastName = "parted", tier = "D") {
          withPremises(premisesWithBookings)
          withExpectedArrivalDate(LocalDate.parse("2025-01-03"))
          withExpectedDepartureDate(LocalDate.parse("2025-02-03"))
          withActualArrivalDate(LocalDate.parse("2025-01-03"))
          withActualDepartureDate(LocalDate.parse("2025-02-03"))
          withCanonicalArrivalDate(LocalDate.parse("2025-01-03"))
          withCanonicalDepartureDate(LocalDate.parse("2025-02-03"))
          withKeyworkerName(null)
          withKeyworkerStaffCode(null)
          withKeyworkerAssignedAt(null)
        }

      upcomingSpaceBookingWithKeyWorker =
        createSpaceBooking(crn = "CRN_UPCOMING", firstName = "up", lastName = "coming senior", tier = "U") {
          withPremises(premisesWithBookings)
          withExpectedArrivalDate(LocalDate.parse("2027-01-01"))
          withExpectedDepartureDate(LocalDate.parse("2027-02-01"))
          withActualArrivalDate(null)
          withActualDepartureDate(null)
          withCanonicalArrivalDate(LocalDate.parse("2027-01-01"))
          withCanonicalDepartureDate(LocalDate.parse("2027-02-01"))
          withKeyworkerName(keyWorker.name)
          withKeyworkerStaffCode(keyWorker.deliusStaffCode)
          withKeyworkerAssignedAt(Instant.now())
        }

      upcomingCancelledSpaceBooking =
        createSpaceBooking(crn = "CRN_UPCOMING_CANCELLED", firstName = "up", lastName = "coming", tier = "U") {
          withPremises(premisesWithBookings)
          withExpectedArrivalDate(LocalDate.parse("2027-01-01"))
          withExpectedDepartureDate(LocalDate.parse("2027-02-01"))
          withActualArrivalDate(null)
          withActualDepartureDate(null)
          withCanonicalArrivalDate(LocalDate.parse("2027-01-01"))
          withCanonicalDepartureDate(LocalDate.parse("2027-02-01"))
          withKeyworkerName(null)
          withKeyworkerStaffCode(null)
          withKeyworkerAssignedAt(null)
          withCancellationOccurredAt(LocalDate.now())
        }

      nonArrivedSpaceBooking =
        createSpaceBooking(crn = "CRN_NONARRIVAL", firstName = "None", lastName = "Arrived", tier = "A") {
          withPremises(premisesWithBookings)
          withExpectedArrivalDate(LocalDate.parse("2028-01-02"))
          withExpectedDepartureDate(LocalDate.parse("2028-02-02"))
          withActualArrivalDate(null)
          withActualDepartureDate(null)
          withCanonicalArrivalDate(LocalDate.parse("2028-01-02"))
          withCanonicalDepartureDate(LocalDate.parse("2028-02-02"))
          withKeyworkerName(null)
          withKeyworkerStaffCode(null)
          withKeyworkerAssignedAt(Instant.now())
          withNonArrivalNotes("Non Arrival Notes")
          withNonArrivalReason(nonArrivalReason)
          withNonArrivalConfirmedAt(Instant.now())
        }

      legacySpaceBookingNoArrival =
        createSpaceBooking(crn = "CRN_LEGACY_NO_ARRIVAL", firstName = "None", lastName = "Historic", tier = "A") {
          withPremises(premisesWithBookings)
          withExpectedArrivalDate(LocalDate.parse("2024-05-02"))
          withExpectedDepartureDate(LocalDate.parse("2024-05-31"))
          withActualArrivalDate(null)
          withActualDepartureDate(null)
          withCanonicalArrivalDate(LocalDate.parse("2024-05-02"))
          withCanonicalDepartureDate(LocalDate.parse("2024-05-31"))
          withKeyworkerName(null)
          withKeyworkerStaffCode(null)
          withKeyworkerAssignedAt(Instant.now())
          withNonArrivalConfirmedAt(Instant.now())
        }

      legacySpaceBookingNoDeparture =
        createSpaceBooking(crn = "CRN_LEGACY_NO_DEPARTURE", firstName = "None", lastName = "Historic", tier = "Z") {
          withPremises(premisesWithBookings)
          withExpectedArrivalDate(LocalDate.parse("2024-05-01"))
          withExpectedDepartureDate(LocalDate.parse("2024-05-30"))
          withActualArrivalDate(LocalDate.parse("2024-05-02"))
          withActualDepartureDate(null)
          withCanonicalArrivalDate(LocalDate.parse("2024-05-01"))
          withCanonicalDepartureDate(LocalDate.parse("2024-05-30"))
          withKeyworkerName(null)
          withKeyworkerStaffCode(null)
          withKeyworkerAssignedAt(Instant.now())
        }

      upcomingSpaceBookingBeforeThreshold =
        createSpaceBooking(crn = "CRN_UPCOMING_BEFORE_THRESHOLD", firstName = "up", lastName = "coming before threshold esquire", tier = "S") {
          withPremises(premisesWithBookings)
          withExpectedArrivalDate(LocalDate.parse("2024-12-31"))
          withExpectedDepartureDate(LocalDate.parse("2024-12-31"))
          withActualArrivalDate(null)
          withActualDepartureDate(null)
          withCanonicalArrivalDate(LocalDate.parse("2024-12-31"))
          withCanonicalDepartureDate(LocalDate.parse("2024-12-31"))
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

      assertThat(response).hasSize(10)
      assertThat(response[0].person.crn).isEqualTo("CRN_LEGACY_NO_DEPARTURE")
      assertThat(response[1].person.crn).isEqualTo("CRN_LEGACY_NO_ARRIVAL")
      assertThat(response[2].person.crn).isEqualTo("CRN_UPCOMING_BEFORE_THRESHOLD")
      assertThat(response[3].person.crn).isEqualTo("CRN_DEPARTED")
      assertThat(response[4].person.crn).isEqualTo("CRN_CURRENT1")
      assertThat(response[4].appealRequested).isFalse
      assertThat(response[4].plannedTransferRequested).isFalse
      assertThat(response[4].openChangeRequestTypes).containsExactly(Cas1ChangeRequestType.PLACEMENT_APPEAL)
      assertThat(response[5].person.crn).isEqualTo("CRN_CURRENT2_OFFLINE")
      assertThat(response[6].person.crn).isEqualTo("CRN_CURRENT3")
      assertThat(response[6].appealRequested).isFalse
      assertThat(response[6].plannedTransferRequested).isFalse
      assertThat(response[6].openChangeRequestTypes).containsExactly(Cas1ChangeRequestType.PLANNED_TRANSFER)
      assertThat(response[7].person.crn).isEqualTo("CRN_CURRENT4")
      assertThat(response[7].appealRequested).isFalse
      assertThat(response[7].plannedTransferRequested).isFalse
      assertThat(response[7].openChangeRequestTypes).isEmpty()
      assertThat(response[8].person.crn).isEqualTo("CRN_UPCOMING")
      assertThat(response[9].person.crn).isEqualTo("CRN_NONARRIVAL")
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

      assertThat(response).hasSize(5)
      assertThat(response[0].person.crn).isEqualTo("CRN_LEGACY_NO_DEPARTURE")
      assertThat(response[1].person.crn).isEqualTo("CRN_LEGACY_NO_ARRIVAL")
      assertThat(response[2].person.crn).isEqualTo("CRN_UPCOMING_BEFORE_THRESHOLD")
      assertThat(response[3].person.crn).isEqualTo("CRN_DEPARTED")
      assertThat(response[4].person.crn).isEqualTo("CRN_NONARRIVAL")
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
      assertThat(response[0].expectedArrivalDate).isEqualTo(LocalDate.parse("2027-01-01"))
      assertThat(response[0].expectedDepartureDate).isEqualTo(LocalDate.parse("2027-02-01"))
      assertThat(response[0].actualArrivalDate).isNull()
      assertThat(response[0].actualDepartureDate).isNull()
      assertThat(response[0].isNonArrival).isNull()
      assertThat(response[0].characteristics).isEmpty()
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

      assertThat(response).hasSize(4)
      assertThat(response[0].person.crn).isEqualTo("CRN_CURRENT1")
      assertThat(response[0].characteristics).isEqualTo(
        listOf(
          Cas1SpaceCharacteristic.isSingle,
          Cas1SpaceCharacteristic.isWheelchairAccessible,
        ),
      )
      assertThat(response[1].person.crn).isEqualTo("CRN_CURRENT2_OFFLINE")
      assertThat(response[2].person.crn).isEqualTo("CRN_CURRENT3")
      assertThat(response[3].person.crn).isEqualTo("CRN_CURRENT4")
      assertThat(response[3].isNonArrival).isFalse
    }

    @Test
    fun `Filter on CRN`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val response = webTestClient.get()
        .uri("/cas1/premises/${premisesWithBookings.id}/space-bookings?crnOrName=CRN_CURRENT2_OFFLINE&sortBy=canonicalArrivalDate&sortDirection=asc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1SpaceBookingSummary>()

      assertThat(response).hasSize(1)
      assertThat(response[0].person.crn).isEqualTo("CRN_CURRENT2_OFFLINE")
      assertThat(response[0].person.personType).isEqualTo(PersonSummaryDiscriminator.fullPersonSummary)
    }

    @Test
    fun `Filter on CRN, RestrictedPerson`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      apDeliusContextAddSingleResponseToUserAccessCall(
        caseAccess = CaseAccessFactory()
          .withUserExcluded(true)
          .withUserRestricted(true)
          .withCrn("CRN_CURRENT2_OFFLINE")
          .produce(),
      )

      val response = webTestClient.get()
        .uri("/cas1/premises/${premisesWithBookings.id}/space-bookings?crnOrName=CRN_CURRENT4&sortBy=canonicalArrivalDate&sortDirection=asc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1SpaceBookingSummary>()

      assertThat(response).hasSize(1)
      assertThat(response[0].person.crn).isEqualTo("CRN_CURRENT4")
      assertThat(response[0].person.personType).isEqualTo(PersonSummaryDiscriminator.restrictedPersonSummary)
    }

    @Test
    fun `Filter on Name`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val response = webTestClient.get()
        .uri("/cas1/premises/${premisesWithBookings.id}/space-bookings?crnOrName=senior&sortBy=canonicalArrivalDate&sortDirection=asc")
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

      assertThat(response).hasSize(4)
      assertThat(response[0].person.crn).isEqualTo("CRN_CURRENT4")
      assertThat(response[1].person.crn).isEqualTo("CRN_CURRENT3")
      assertThat(response[2].person.crn).isEqualTo("CRN_CURRENT2_OFFLINE")
      assertThat(response[3].person.crn).isEqualTo("CRN_CURRENT1")
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

      assertThat(response).hasSize(10)
      assertThat(response[0].person.crn).isEqualTo("CRN_NONARRIVAL")
      assertThat(response[1].person.crn).isEqualTo("CRN_UPCOMING")
      assertThat(response[2].person.crn).isEqualTo("CRN_CURRENT4")
      assertThat(response[3].person.crn).isEqualTo("CRN_CURRENT3")
      assertThat(response[4].person.crn).isEqualTo("CRN_CURRENT2_OFFLINE")
      assertThat(response[5].person.crn).isEqualTo("CRN_CURRENT1")
      assertThat(response[6].person.crn).isEqualTo("CRN_DEPARTED")
      assertThat(response[7].person.crn).isEqualTo("CRN_UPCOMING_BEFORE_THRESHOLD")
      assertThat(response[8].person.crn).isEqualTo("CRN_LEGACY_NO_ARRIVAL")
      assertThat(response[8].isNonArrival).isTrue
      assertThat(response[9].person.crn).isEqualTo("CRN_LEGACY_NO_DEPARTURE")
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

      assertThat(response).hasSize(10)
      assertThat(response[0].person.crn).isEqualTo("CRN_LEGACY_NO_DEPARTURE")
      assertThat(response[1].person.crn).isEqualTo("CRN_LEGACY_NO_ARRIVAL")
      assertThat(response[2].person.crn).isEqualTo("CRN_UPCOMING_BEFORE_THRESHOLD")
      assertThat(response[3].person.crn).isEqualTo("CRN_DEPARTED")
      assertThat(response[4].person.crn).isEqualTo("CRN_CURRENT1")
      assertThat(response[5].person.crn).isEqualTo("CRN_CURRENT2_OFFLINE")
      assertThat(response[6].person.crn).isEqualTo("CRN_CURRENT3")
      assertThat(response[7].person.crn).isEqualTo("CRN_CURRENT4")
      assertThat(response[8].person.crn).isEqualTo("CRN_UPCOMING")
      assertThat(response[9].person.crn).isEqualTo("CRN_NONARRIVAL")
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

      assertThat(response).hasSize(4)
      assertThat(response[0].keyWorkerAllocation!!.keyWorker.name).isEqualTo("Clive Keyworker")
      assertThat(response[1].keyWorkerAllocation!!.keyWorker.name).isEqualTo("Clive Keyworker")
      assertThat(response[2].keyWorkerAllocation!!.keyWorker.name).isEqualTo("Kathy Keyworker")
      assertThat(response[3].keyWorkerAllocation).isNull()
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

      assertThat(response).hasSize(10)

      assertThat(response[0].tier).isNull()
      assertThat(response[0].person.crn).isEqualTo("CRN_CURRENT2_OFFLINE")

      assertThat(response[1].tier).isEqualTo("Z")
      assertThat(response[1].person.crn).isEqualTo("CRN_LEGACY_NO_DEPARTURE")

      assertThat(response[2].tier).isEqualTo("U")
      assertThat(response[2].person.crn).isEqualTo("CRN_UPCOMING")

      assertThat(response[3].tier).isEqualTo("S")
      assertThat(response[3].person.crn).isEqualTo("CRN_UPCOMING_BEFORE_THRESHOLD")

      assertThat(response[4].tier).isEqualTo("D")
      assertThat(response[4].person.crn).isEqualTo("CRN_DEPARTED")

      assertThat(response[5].tier).isEqualTo("B")
      assertThat(response[5].person.crn).isEqualTo("CRN_CURRENT4")

      assertThat(response[6].tier).isEqualTo("B")
      assertThat(response[6].person.crn).isEqualTo("CRN_CURRENT3")

      assertThat(response[7].tier).isEqualTo("A")
      assertThat(response[7].person.crn).isEqualTo("CRN_NONARRIVAL")

      assertThat(response[8].tier).isEqualTo("A")
      assertThat(response[8].person.crn).isEqualTo("CRN_LEGACY_NO_ARRIVAL")

      assertThat(response[9].tier).isEqualTo("A")
      assertThat(response[9].person.crn).isEqualTo("CRN_CURRENT1")
    }
  }

  @Nested
  inner class GetASpaceBooking : InitialiseDatabasePerClassTestBase() {
    lateinit var premises: ApprovedPremisesEntity
    lateinit var spaceBooking: Cas1SpaceBookingEntity
    lateinit var otherSpaceBookingAtPremises2021: Cas1SpaceBookingEntity
    lateinit var otherSpaceBookingAtPremises2020: Cas1SpaceBookingEntity
    lateinit var cas1ChangeRequestEntity: Cas1ChangeRequestEntity
    lateinit var appealRejectionReason: Cas1ChangeRequestRejectionReasonEntity
    lateinit var changeRequestReason: Cas1ChangeRequestReasonEntity

    @BeforeAll
    fun setupTestData() {
      val region = givenAProbationRegion()

      premises = givenAnApprovedPremises(
        region = region,
        supportsSpaceBookings = true,
      )

      val otherPremises = givenAnApprovedPremises(
        region = region,
        supportsSpaceBookings = true,
      )

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
        withActualArrivalTime(LocalTime.parse("11:24:35"))
        withActualDepartureTime(LocalTime.parse("10:24:35"))
      }

      otherSpaceBookingAtPremises2021 = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.parse("2021-05-29"))
        withCanonicalDepartureDate(LocalDate.parse("2021-06-29"))
      }

      otherSpaceBookingAtPremises2020 = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.parse("2020-06-29"))
        withCanonicalDepartureDate(LocalDate.parse("2020-07-29"))
      }

      // otherSpaceBookingAtPremisesDifferentCrn
      cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn("othercrn")
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.parse("2031-05-29"))
        withCanonicalDepartureDate(LocalDate.parse("2031-06-29"))
      }

      // otherSpaceBookingAtPremisesCancelled
      cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.parse("2031-05-29"))
        withCanonicalDepartureDate(LocalDate.parse("2031-06-29"))
        withCancellationOccurredAt(LocalDate.parse("2020-01-01"))
      }

      // otherSpaceBookingNotAtPremises
      cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withPremises(otherPremises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.parse("2032-05-29"))
        withCanonicalDepartureDate(LocalDate.parse("2032-06-29"))
      }

      appealRejectionReason = cas1ChangeRequestRejectionReasonEntityFactory
        .produceAndPersist {
          withChangeRequestType(ChangeRequestType.PLACEMENT_APPEAL)
        }

      changeRequestReason = cas1ChangeRequestReasonEntityFactory.produceAndPersist {
        withChangeRequestType(ChangeRequestType.PLACEMENT_APPEAL)
      }

      cas1ChangeRequestEntity = cas1ChangeRequestEntityFactory.produceAndPersist {
        withSpaceBooking(spaceBooking)
        withChangeRequestReason(changeRequestReason)
        withPlacementRequest(spaceBooking.placementRequest!!)
      }
    }

    @Test
    fun `Returns 404 if space booking doesn't exist`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      webTestClient.get()
        .uri("/cas1/space-bookings/${UUID.randomUUID()}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun success() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val response = webTestClient.get()
        .uri("/cas1/space-bookings/${spaceBooking.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1SpaceBooking::class.java).responseBody.blockFirst()!!

      assertThat(response.id).isEqualTo(spaceBooking.id)
      assertThat(response.otherBookingsInPremisesForCrn).hasSize(2)
      assertThat(response.otherBookingsInPremisesForCrn.map { it.id })
        .containsExactly(
          otherSpaceBookingAtPremises2020.id,
          otherSpaceBookingAtPremises2021.id,
        )
      assertThat(response.requestForPlacementId).isEqualTo(spaceBooking.placementRequest!!.id)
      assertThat(response.actualArrivalTime).isEqualTo("11:24")
      assertThat(response.actualDepartureTime).isEqualTo("10:24")
      assertThat(response.openChangeRequests.size).isEqualTo(1)
    }
  }

  @Nested
  inner class GetASpaceBookingByPremise : InitialiseDatabasePerClassTestBase() {
    lateinit var premises: ApprovedPremisesEntity
    lateinit var spaceBooking: Cas1SpaceBookingEntity
    lateinit var otherSpaceBookingAtPremises: Cas1SpaceBookingEntity

    @BeforeAll
    fun setupTestData() {
      val region = givenAProbationRegion()

      premises = givenAnApprovedPremises(
        region = region,
        supportsSpaceBookings = true,
      )

      val otherPremises = givenAnApprovedPremises(
        region = region,
        supportsSpaceBookings = false,
      )

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

      // otherSpaceBookingAtPremisesDifferentCrn
      cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn("othercrn")
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.parse("2031-05-29"))
        withCanonicalDepartureDate(LocalDate.parse("2031-06-29"))
      }

      // otherSpaceBookingAtPremisesCancelled
      cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.parse("2031-05-29"))
        withCanonicalDepartureDate(LocalDate.parse("2031-06-29"))
        withCancellationOccurredAt(LocalDate.parse("2020-01-01"))
      }

      // otherSpaceBookingNotAtPremises
      cas1SpaceBookingEntityFactory.produceAndPersist {
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
    fun `Success`() {
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

      premises = givenAnApprovedPremises(
        region = region,
        supportsSpaceBookings = true,
      )

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
    fun `Recording arrival returns OK, creates and emits a domain event and no emails`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val (user) = givenAUser()
      val (offender) = givenAnOffender()
      val (placementRequest) = givenAPlacementRequest(
        placementRequestAllocatedTo = user,
        assessmentAllocatedTo = user,
        createdByUser = user,
      )

      // departed, ignored when checking existing residence
      givenACas1SpaceBooking(
        crn = offender.otherIds.crn,
        actualArrivalDate = LocalDate.parse("2020-01-01"),
        expectedDepartureDate = UPCOMING_EXPECTED_DEPARTURE_THRESHOLD_DATE.plusDays(1),
        actualDepartureDate = UPCOMING_EXPECTED_DEPARTURE_THRESHOLD_DATE.plusDays(2),
      )

      // not departed but departure is before UPCOMING_EXPECTED_DEPARTURE_THRESHOLD, ignored when checking existing residence
      givenACas1SpaceBooking(
        crn = offender.otherIds.crn,
        actualArrivalDate = UPCOMING_EXPECTED_DEPARTURE_THRESHOLD_DATE.minusDays(10),
        expectedDepartureDate = UPCOMING_EXPECTED_DEPARTURE_THRESHOLD_DATE.minusDays(1),
        actualDepartureDate = null,
      )

      // not departed but cancelled (legacy booking state), ignored when checking existing residence
      givenACas1SpaceBooking(
        crn = offender.otherIds.crn,
        actualArrivalDate = UPCOMING_EXPECTED_DEPARTURE_THRESHOLD_DATE.minusDays(1),
        expectedDepartureDate = UPCOMING_EXPECTED_DEPARTURE_THRESHOLD_DATE.plusDays(10),
        actualDepartureDate = null,
        cancellationOccurredAt = LocalDate.now(),
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
        withTransferType(null)
      }

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/arrival")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NewArrival(
            arrivalDate = LocalDate.now().minusDays(1),
            arrivalTime = "12:00:00",
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      domainEventAsserter.blockForEmittedDomainEvent(DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED)
      domainEventAsserter.assertDomainEventOfTypeStored(
        spaceBooking.application!!.id,
        DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED,
      )

      emailAsserter.assertNoEmailsRequested()
    }

    @Test
    fun `Recording arrival blocked if CRN is already resident elsewhere`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      givenACas1SpaceBooking(
        crn = "CRN123",
        actualArrivalDate = LocalDate.parse("2020-01-01"),
        expectedDepartureDate = UPCOMING_EXPECTED_DEPARTURE_THRESHOLD_DATE.plusDays(1),
        actualDepartureDate = null,
        premises = givenAnApprovedPremises(name = "Other Premises Name", cruManagementArea = givenACas1CruManagementArea(name = "NE")),
      )

      val spaceBooking = givenACas1SpaceBooking(
        crn = "CRN123",
        premises = premises,
        deliusEventNumber = "1",
      )

      val response = webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/arrival")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NewArrival(
            arrivalDate = LocalDate.now().minusDays(1),
            arrivalTime = "12:00:00",
          ),
        )
        .exchange()
        .expectStatus()
        .isBadRequest
        .bodyAsObject<ValidationError>()

      assertThat(response.detail).isEqualTo("Arrival cannot be recorded as CRN123 is recorded as resident at Other Premises Name (NE)")
    }

    @Test
    fun `Recording arrival for emergency transfer returns OK, creates and emits a domain event and emails`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val (user) = givenAUser()
      val (offender) = givenAnOffender()
      val cruManagementArea = givenACas1CruManagementArea(emailAddress = "theCru@test.com")

      val application = givenASubmittedCas1Application(
        offender = offender.asCaseSummary(),
        caseManager = cas1ApplicationUserDetailsEntityFactory.produceAndPersist {
          withEmailAddress("caseManager@test.com")
        },
        cruManagementArea = cruManagementArea,
      )
      val placementApplication = givenAPlacementApplication(application = application)
      val (placementRequest) = givenAPlacementRequest(
        placementRequestAllocatedTo = user,
        assessmentAllocatedTo = user,
        application = application,
        placementApplication = placementApplication,
      )

      val transferredBooking = givenACas1SpaceBooking(
        application = application,
        placementRequest = placementRequest,
      )

      spaceBooking = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(application)
        withCreatedBy(user)
        withCanonicalArrivalDate(LocalDate.parse("2029-05-29"))
        withCanonicalDepartureDate(LocalDate.parse("2029-06-29"))
        withKeyworkerName(user.name)
        withKeyworkerStaffCode(user.deliusStaffCode)
        withKeyworkerAssignedAt(Instant.now())
        withDeliusEventNumber("25")
        withTransferType(TransferType.EMERGENCY)
        withTransferredFrom(transferredBooking)
      }

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/arrival")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NewArrival(
            arrivalDate = LocalDate.now().minusDays(1),
            arrivalTime = "12:00:00",
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      domainEventAsserter.blockForEmittedDomainEvent(DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED)
      domainEventAsserter.assertDomainEventOfTypeStored(
        spaceBooking.application!!.id,
        DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED,
      )

      emailAsserter.assertEmailsRequestedCount(4)
      emailAsserter.assertEmailRequested(application.createdByUser.email!!, Cas1NotifyTemplates.TRANSFER_COMPLETE)
      emailAsserter.assertEmailRequested("caseManager@test.com", Cas1NotifyTemplates.TRANSFER_COMPLETE)
      emailAsserter.assertEmailRequested(placementApplication.createdByUser.email!!, Cas1NotifyTemplates.TRANSFER_COMPLETE)
      emailAsserter.assertEmailRequested("theCru@test.com", Cas1NotifyTemplates.TRANSFER_COMPLETE_EMERGENCY_FOR_CRU)
    }

    @Test
    fun `Recording arrival with deprecated date time returns OK and creates a domain event`() {
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
      domainEventAsserter.assertDomainEventOfTypeStored(
        spaceBooking.application!!.id,
        DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED,
      )
    }

    @Test
    fun `Returns error if arrivaldate is null`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/arrival")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NewArrival(),
        )
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("invalid-params[0].propertyName").isEqualTo("arrivalDate")
        .jsonPath("invalid-params[0].errorType").isEqualTo("is required")
    }

    @Test
    fun `Returns error if the arrivaltime is null`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/arrival")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NewArrival(
            arrivalDate = LocalDate.now().minusDays(1),
          ),
        )
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("invalid-params[0].propertyName").isEqualTo("arrivalTime")
        .jsonPath("invalid-params[0].errorType").isEqualTo("is required")
    }

    @Test
    fun `Returns error if the arrivaldate is in the future`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/arrival")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NewArrival(
            arrivalDate = LocalDate.now().plusDays(1),
            arrivalTime = LocalTime.now().minusMinutes(1).toString(),
          ),
        )
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("invalid-params[0].propertyName").isEqualTo("arrivalDate")
        .jsonPath("invalid-params[0].errorType").isEqualTo("must be in the past")
    }

    @Test
    fun `Returns error if the arrivaltime is in the future`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/arrival")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NewArrival(
            arrivalDate = LocalDate.now(),
            arrivalTime = LocalTime.now().plusMinutes(1).toString(),
          ),
        )
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("invalid-params[0].propertyName").isEqualTo("arrivalTime")
        .jsonPath("invalid-params[0].errorType").isEqualTo("must be in the past")
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

      premises = givenAnApprovedPremises(
        region = region,
        supportsSpaceBookings = true,
      )

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

      domainEventAsserter.assertDomainEventOfTypeStored(
        spaceBooking.application!!.id,
        DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED,
      )
      snsDomainEventListener.blockForMessage(DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED)

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

      premises = givenAnApprovedPremises(
        region = region,
        supportsSpaceBookings = true,
        qCode = "QCODE",
      )

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

      domainEventAsserter.assertDomainEventOfTypeStored(
        spaceBooking.application!!.id,
        DomainEventType.APPROVED_PREMISES_BOOKING_KEYWORKER_ASSIGNED,
      )

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

      premises = givenAnApprovedPremises(
        region = region,
        supportsSpaceBookings = true,
      )

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
        withActualArrivalDate(LocalDate.now().minusDays(30))
        withActualArrivalTime(LocalTime.now())
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
    fun `Recording departure returns OK, creates and emits a domain event`() {
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
        withActualArrivalDate(LocalDate.now().minusDays(30))
        withActualArrivalTime(LocalTime.now())
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
            departureDate = LocalDate.now(),
            departureTime = "23:59:59",
            reasonId = departureReasonId,
            moveOnCategoryId = departureMoveOnCategoryId,
            notes = "these are departure notes",
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      domainEventAsserter.blockForEmittedDomainEvent(DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED)
    }

    @Test
    fun `Recording departure using deprecated date time returns OK and creates a domain event`() {
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
        withActualArrivalDate(LocalDate.now().minusDays(30))
        withActualArrivalTime(LocalTime.now())
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
            notes = "these are departure notes",
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
      domainEventAsserter.assertDomainEventOfTypeStored(
        spaceBooking.application!!.id,
        DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED,
      )
    }

    @Test
    fun `Recording departure returns OK, creates and emits a domain event with 'Not Applicable' move on category if no category is supplied `() {
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
        withActualArrivalDate(LocalDate.now().minusDays(30))
        withActualArrivalTime(LocalTime.now())
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
            moveOnCategoryId = null,
            notes = null,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      domainEventAsserter.blockForEmittedDomainEvent(DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED)
      val domainEvent = domainEventAsserter.assertDomainEventOfTypeStored(
        spaceBooking.application!!.id,
        DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED,
      )
      assertThat(domainEvent.data).contains("""moveOnCategory": {"id": "${MOVE_ON_CATEGORY_NOT_APPLICABLE_ID}""")
    }
  }

  @Nested
  inner class Cancellation : InitialiseDatabasePerClassTestBase() {

    lateinit var applicant: UserEntity
    lateinit var placementApplicationCreator: UserEntity
    lateinit var application: ApprovedPremisesApplicationEntity
    lateinit var premises: ApprovedPremisesEntity
    lateinit var spaceBooking: Cas1SpaceBookingEntity
    lateinit var cancellationReason: CancellationReasonEntity
    lateinit var changeRequests: List<Cas1ChangeRequestEntity>

    @BeforeAll
    fun setupTestData() {
      premises = givenAnApprovedPremises(emailAddress = "premises@test.com")

      applicant = givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(email = "applicant@test.com"),
      ).first

      placementApplicationCreator = givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(email = "placementApplicant@test.com"),
      ).first

      val (offender) = givenAnOffender()

      application = givenACas1Application(
        crn = offender.otherIds.crn,
        createdByUser = applicant,
        cruManagementArea = givenACas1CruManagementArea(),
      )

      val placementApplication = placementApplicationFactory.produceAndPersist {
        withCreatedByUser(placementApplicationCreator)
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

      changeRequests = (0..3).map {
        givenACas1ChangeRequest(
          type = ChangeRequestType.PLACEMENT_APPEAL,
          spaceBooking = spaceBooking,
          resolved = false,
          decisionJson = "{\"test\": 1}",
        )
      }
    }

    @Test
    fun `Cancellation without JWT returns 401`() {
      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/cancellations")
        .bodyValue(
          Cas1NewSpaceBookingCancellation(
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
      names = ["CAS1_CRU_MEMBER", "CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA", "CAS1_JANITOR", "CAS1_CHANGE_REQUEST_DEV"],
      mode = EnumSource.Mode.EXCLUDE,
    )
    fun `Cancellation with invalid role returns 401`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.post()
          .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/cancellations")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            Cas1NewSpaceBookingCancellation(
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
    fun `Cancellation returns OK, updates status and sends emails when user has role CRU_MEMBER`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA)) { _, jwt ->
        webTestClient.post()
          .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/cancellations")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            Cas1NewSpaceBookingCancellation(
              occurredAt = LocalDate.parse("2022-08-17"),
              reasonId = cancellationReason.id,
              reasonNotes = null,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
      }

      emailAsserter.assertEmailRequested(applicant.email!!, Cas1NotifyTemplates.BOOKING_WITHDRAWN_V2)
      emailAsserter.assertEmailRequested(placementApplicationCreator.email!!, Cas1NotifyTemplates.BOOKING_WITHDRAWN_V2)
      emailAsserter.assertEmailRequested(
        spaceBooking.premises.emailAddress!!,
        Cas1NotifyTemplates.BOOKING_WITHDRAWN_V2,
      )
      emailAsserter.assertEmailRequested(
        application.cruManagementArea!!.emailAddress!!,
        Cas1NotifyTemplates.BOOKING_WITHDRAWN_V2,
      )
      emailAsserter.assertEmailsRequestedCount(4)

      domainEventAsserter.assertDomainEventOfTypeStored(spaceBooking.application!!.id, DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED)

      assertThat(approvedPremisesApplicationRepository.findByIdOrNull(spaceBooking.application!!.id)!!.status)
        .isEqualTo(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)

      changeRequests.forEach {
        assertThat(cas1ChangeRequestRepository.findByIdOrNull(it.id)!!.resolved).isTrue()
      }
    }

    @Test
    fun `Cancellation (due to appeal) without JWT returns 401`() {
      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/appeal")
        .bodyValue(
          Cas1ApprovedPlacementAppeal(
            occurredAt = LocalDate.parse("2022-08-17"),
            reasonNotes = null,
            placementAppealChangeRequestId = UUID.randomUUID(),
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @EnumSource(
      value = UserRole::class,
      names = ["CAS1_JANITOR", "CAS1_CHANGE_REQUEST_DEV"],
      mode = EnumSource.Mode.EXCLUDE,
    )
    fun `Cancellation (due to appeal) with invalid role returns 401`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.post()
          .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/appeal")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            Cas1ApprovedPlacementAppeal(
              occurredAt = LocalDate.parse("2022-08-17"),
              reasonNotes = null,
              placementAppealChangeRequestId = UUID.randomUUID(),
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Create Cancellation (due to appeal) on CAS1 Booking returns OK with correct body and updates status when user has role CAS1_CHANGE_REQUEST_DEV`() {
      givenAUser(roles = listOf(UserRole.CAS1_CHANGE_REQUEST_DEV)) { user, jwt ->

        val (placementRequest) = givenAPlacementRequest(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = applicant,
        )

        val reason = cas1ChangeRequestReasonEntityFactory.produceAndPersist()

        val spaceBooking = cas1SpaceBookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withPlacementRequest(placementRequest)
          withCreatedBy(applicant)
          withApplication(placementRequest.application)
        }

        val changeRequestId = cas1ChangeRequestEntityFactory
          .produceAndPersist {
            withSpaceBooking(spaceBooking)
            withUser(user)
            withChangeRequestReason(reason)
            withPlacementRequest(placementRequest)
          }.id

        webTestClient.post()
          .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/appeal")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            Cas1ApprovedPlacementAppeal(
              occurredAt = LocalDate.parse("2022-08-17"),
              reasonNotes = null,
              placementAppealChangeRequestId = changeRequestId,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk

        assertThat(approvedPremisesApplicationRepository.findByIdOrNull(spaceBooking.application!!.id)!!.status).isEqualTo(
          ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT,
        )

        val cas1PersistedSpaceBooking = cas1SpaceBookingRepository.findByIdOrNull(spaceBooking.id)!!
        assertThat(cas1PersistedSpaceBooking.cancellationReason!!.name).isEqualTo("Booking successfully appealed")
        assertThat(cas1PersistedSpaceBooking.cancellationReason!!.id).isEqualTo(CancellationReasonRepository.CAS1_BOOKING_SUCCESSFULLY_APPEALED_ID)

        assertThat(cas1ChangeRequestRepository.findByIdOrNull(changeRequestId)!!.decision).isEqualTo(
          ChangeRequestDecision.APPROVED,
        )

        emailAsserter.assertEmailRequested(applicant.email!!, Cas1NotifyTemplates.BOOKING_WITHDRAWN_V2)
        emailAsserter.assertEmailRequested(premises.emailAddress!!, Cas1NotifyTemplates.BOOKING_WITHDRAWN_V2)
        emailAsserter.assertEmailRequested(applicant.email!!, Cas1NotifyTemplates.PLACEMENT_APPEAL_ACCEPTED_FOR_APPLICANT)
        emailAsserter.assertEmailRequested(premises.emailAddress!!, Cas1NotifyTemplates.PLACEMENT_APPEAL_ACCEPTED_FOR_PREMISES)

        emailAsserter.assertEmailsRequestedCount(4)

        domainEventAsserter.assertDomainEventOfTypeStored(placementRequest.application.id, DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED)
      }
    }
  }

  @Nested
  inner class UpdateSpaceBooking : InitialiseDatabasePerClassTestBase() {
    lateinit var region: ProbationRegionEntity
    lateinit var premises: ApprovedPremisesEntity
    lateinit var spaceBooking: Cas1SpaceBookingEntity
    lateinit var applicant: UserEntity
    lateinit var application: ApprovedPremisesApplicationEntity

    @BeforeAll
    fun setupTestData() {
      characteristicRepository.deleteAll()

      region = givenAProbationRegion()

      val (user) = givenAUser()
      val (offender) = givenAnOffender()

      premises = givenAnApprovedPremises(
        region = region,
        supportsSpaceBookings = true,
        emailAddress = "premises@test.com",
      )

      applicant = givenAUser(
        staffDetail =
        StaffDetailFactory.staffDetail(email = "applicant@test.com"),
      ).first

      application = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withCreatedByUser(applicant)
        withApArea(givenAnApArea())
        withSubmittedAt(OffsetDateTime.now())
      }

      val (placementRequest) = givenAPlacementRequest(
        placementRequestAllocatedTo = user,
        assessmentAllocatedTo = user,
        createdByUser = user,
        application = application,
      )

      spaceBooking = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withExpectedArrivalDate(LocalDate.parse("2025-02-05"))
        withExpectedDepartureDate(LocalDate.parse("2025-06-29"))
      }
    }

    @Test
    fun `Returns 403 Forbidden if user does not have correct permission`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_ASSESSOR))

      webTestClient.patch()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1UpdateSpaceBooking(
            arrivalDate = LocalDate.now(),
            departureDate = LocalDate.now().plusMonths(1),
          ),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Update space booking returns OK, and correctly update booking dates`() {
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA))

      webTestClient.patch()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1UpdateSpaceBooking(
            arrivalDate = LocalDate.parse("2025-03-15"),
            departureDate = LocalDate.parse("2025-04-05"),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      val updatedSpaceBooking = cas1SpaceBookingRepository.findByIdOrNull(spaceBooking.id)!!

      assertThat(updatedSpaceBooking.expectedArrivalDate).isEqualTo(LocalDate.parse("2025-03-15"))
      assertThat(updatedSpaceBooking.expectedDepartureDate).isEqualTo(LocalDate.parse("2025-04-05"))

      domainEventAsserter.assertDomainEventOfTypeStored(
        updatedSpaceBooking.application?.id!!,
        DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED,
      )

      emailAsserter.assertEmailsRequestedCount(2)
      emailAsserter.assertEmailRequested(applicant.email!!, Cas1NotifyTemplates.BOOKING_AMENDED)
      emailAsserter.assertEmailRequested(spaceBooking.premises.emailAddress!!, Cas1NotifyTemplates.BOOKING_AMENDED)
    }

    @Test
    fun `Update space booking returns OK, and correctly update the room characteristics`() {
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA))

      val (user) = givenAUser()

      val (placementRequest) = givenAPlacementRequest(
        placementRequestAllocatedTo = user,
        assessmentAllocatedTo = user,
        createdByUser = user,
      )

      val premises = givenAnApprovedPremises(supportsSpaceBookings = true)

      val characteristics = mutableListOf(
        characteristicEntityFactory.produceAndPersist {
          withName("Arson Room")
          withPropertyName("isArsonSuitable")
          withServiceScope("approved-premises")
          withModelScope("room")
        },
        characteristicEntityFactory.produceAndPersist {
          withName("En-Suit")
          withPropertyName("hasEnSuite")
          withServiceScope("approved-premises")
          withModelScope("room")
        },
        characteristicEntityFactory.produceAndPersist {
          withName("Single room")
          withPropertyName("isSingle")
          withServiceScope("approved-premises")
          withModelScope("room")
        },
        characteristicEntityFactory.produceAndPersist {
          withName("Wheelchair accessible")
          withPropertyName("isWheelchairAccessible")
          withServiceScope("approved-premises")
          withModelScope("premises")
        },
      )

      val spaceBookingBeforeUpdate = cas1SpaceBookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withCriteria(characteristics)
      }

      assertThat(spaceBookingBeforeUpdate.criteria.size).isEqualTo(4)
      assertThat(spaceBookingBeforeUpdate.criteria)
        .extracting("modelScope", "propertyName")
        .containsExactlyInAnyOrder(
          tuple("room", "isArsonSuitable"),
          tuple("room", "hasEnSuite"),
          tuple("room", "isSingle"),
          tuple("premises", "isWheelchairAccessible"),
        )

      webTestClient.patch()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBookingBeforeUpdate.id}")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1UpdateSpaceBooking(
            characteristics = listOf(Cas1SpaceBookingCharacteristic.HAS_EN_SUITE),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      val updatedSpaceBooking = cas1SpaceBookingRepository.findByIdOrNull(spaceBookingBeforeUpdate.id)!!
      assertThat(updatedSpaceBooking.criteria.size).isEqualTo(2)
      assertThat(updatedSpaceBooking.criteria)
        .extracting("modelScope", "propertyName")
        .containsExactlyInAnyOrder(
          tuple("room", "hasEnSuite"),
          tuple("premises", "isWheelchairAccessible"),
        )

      domainEventAsserter.assertDomainEventOfTypeStored(
        updatedSpaceBooking.application?.id!!,
        DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED,
      )

      emailAsserter.assertNoEmailsRequested()
    }

    @Test
    fun `Update space booking correctly removes existing room characteristics when none provided`() {
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA))

      val (user) = givenAUser()

      val (placementRequest) = givenAPlacementRequest(
        placementRequestAllocatedTo = user,
        assessmentAllocatedTo = user,
        createdByUser = user,
      )

      val premises = givenAnApprovedPremises(
        region = region,
        supportsSpaceBookings = true,
      )

      val characteristics = mutableListOf(
        characteristicEntityFactory.produceAndPersist {
          withName("Step Free Designated")
          withPropertyName("isStepFreeDesignated")
          withServiceScope("approved-premises")
          withModelScope("room")
        },
        characteristicEntityFactory.produceAndPersist {
          withName("Catered")
          withPropertyName("isCatered")
          withServiceScope("approved-premises")
          withModelScope("premises")
        },
      )

      val spaceBookingBeforeUpdate = cas1SpaceBookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withCriteria(characteristics)
      }

      assertThat(spaceBookingBeforeUpdate.criteria.size).isEqualTo(2)
      assertThat(spaceBookingBeforeUpdate.criteria)
        .extracting("modelScope", "propertyName")
        .containsExactlyInAnyOrder(
          tuple("room", "isStepFreeDesignated"),
          tuple("premises", "isCatered"),
        )

      webTestClient.patch()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBookingBeforeUpdate.id}")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1UpdateSpaceBooking(
            characteristics = emptyList(),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      val updatedSpaceBooking = cas1SpaceBookingRepository.findByIdOrNull(spaceBookingBeforeUpdate.id)!!
      assertThat(updatedSpaceBooking.criteria.size).isEqualTo(1)
      assertTrue(updatedSpaceBooking.criteria.none { it.modelScope == "room" })

      domainEventAsserter.assertDomainEventOfTypeStored(
        updatedSpaceBooking.application?.id!!,
        DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED,
      )

      emailAsserter.assertNoEmailsRequested()
    }
  }

  @Nested
  inner class ShortenSpaceBooking : InitialiseDatabasePerClassTestBase() {
    lateinit var spaceBooking: Cas1SpaceBookingEntity

    @BeforeAll
    fun setupTestData() {
      spaceBooking = givenACas1SpaceBooking(
        expectedArrivalDate = LocalDate.now().minusDays(7),
        actualArrivalDate = LocalDate.now().minusDays(7),
        expectedDepartureDate = LocalDate.now().plusDays(7),
        caseManager = cas1ApplicationUserDetailsEntityFactory.produceAndPersist(),
        cruManagementArea = cas1CruManagementAreaEntityFactory.produceAndPersist(),
      )
    }

    @Test
    fun `Returns 403 Forbidden if user does not have correct permission`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_ASSESSOR))

      webTestClient.patch()
        .uri("/cas1/premises/${spaceBooking.premises.id}/space-bookings/${spaceBooking.id}/shorten")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1UpdateSpaceBooking(
            departureDate = LocalDate.now(),
          ),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Update space booking returns OK, correctly updates departure date and sends emails`() {
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_CHANGE_REQUEST_DEV))

      val today = LocalDate.now()

      webTestClient.patch()
        .uri("/cas1/premises/${spaceBooking.premises.id}/space-bookings/${spaceBooking.id}/shorten")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1UpdateSpaceBooking(
            departureDate = today,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      val updatedSpaceBooking = cas1SpaceBookingRepository.findByIdOrNull(spaceBooking.id)!!

      assertThat(updatedSpaceBooking.expectedDepartureDate).isEqualTo(today)

      emailAsserter.assertEmailsRequestedCount(4)
      emailAsserter.assertEmailRequested(
        spaceBooking.application!!.createdByUser.email!!,
        Cas1NotifyTemplates.BOOKING_AMENDED,
      )
      emailAsserter.assertEmailRequested(
        spaceBooking.application!!.caseManagerUserDetails!!.email!!,
        Cas1NotifyTemplates.BOOKING_AMENDED,
      )
      emailAsserter.assertEmailRequested(
        spaceBooking.placementRequest!!.application.createdByUser.email!!,
        Cas1NotifyTemplates.BOOKING_AMENDED,
      )
      emailAsserter.assertEmailRequested(
        spaceBooking.premises.emailAddress!!,
        Cas1NotifyTemplates.BOOKING_AMENDED,
      )
      emailAsserter.assertEmailRequested(
        spaceBooking.application!!.cruManagementArea!!.emailAddress!!,
        Cas1NotifyTemplates.BOOKING_AMENDED,
      )

      domainEventAsserter.assertDomainEventOfTypeStored(
        spaceBooking.application!!.id,
        DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED,
      )
    }
  }

  @Nested
  inner class EmergencyTransfer : InitialiseDatabasePerClassTestBase() {
    lateinit var region: ProbationRegionEntity
    lateinit var premises: ApprovedPremisesEntity
    lateinit var existingSpaceBooking: Cas1SpaceBookingEntity
    lateinit var applicant: UserEntity
    lateinit var application: ApprovedPremisesApplicationEntity
    lateinit var destinationPremises: ApprovedPremisesEntity

    @BeforeAll
    fun setupTestData() {
      characteristicRepository.deleteAll()

      region = givenAProbationRegion()

      val (user) = givenAUser()
      val (offender) = givenAnOffender()

      premises = givenAnApprovedPremises(
        region = region,
        supportsSpaceBookings = true,
        emailAddress = "premises@test.com",
      )

      destinationPremises = givenAnApprovedPremises(
        region = region,
        supportsSpaceBookings = true,
        emailAddress = "destPremises@test.com",
      )

      applicant = givenAUser(
        staffDetail =
        StaffDetailFactory.staffDetail(email = "applicant@test.com"),
      ).first

      application = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withCreatedByUser(applicant)
        withApArea(givenAnApArea())
        withSubmittedAt(OffsetDateTime.now())
      }

      val (placementRequest) = givenAPlacementRequest(
        placementRequestAllocatedTo = user,
        assessmentAllocatedTo = user,
        createdByUser = user,
        application = application,
      )

      existingSpaceBooking = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withExpectedArrivalDate(LocalDate.parse("2025-02-05"))
        withActualArrivalDate(LocalDate.parse("2025-02-05"))
        withExpectedDepartureDate(LocalDate.parse("2025-06-29"))
      }
    }

    @Test
    fun `Returns 403 Forbidden if user does not have correct permission`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_ASSESSOR))

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${existingSpaceBooking.id}/emergency-transfer")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NewEmergencyTransfer(
            arrivalDate = LocalDate.now(),
            departureDate = LocalDate.now().plusMonths(1),
            destinationPremisesId = UUID.randomUUID(),
          ),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Successfully creates an emergency booking and updates the existing booking`() {
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_CHANGE_REQUEST_DEV))

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${existingSpaceBooking.id}/emergency-transfer")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NewEmergencyTransfer(
            arrivalDate = LocalDate.now(),
            departureDate = LocalDate.now().plusMonths(1),
            destinationPremisesId = destinationPremises.id,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      val updatedSpaceBooking = cas1SpaceBookingRepository.findByIdOrNull(existingSpaceBooking.id)!!

      val emergencyTransferredBooking = cas1SpaceBookingRepository.findByTransferredFrom(updatedSpaceBooking)!!

      assertThat(updatedSpaceBooking.expectedDepartureDate).isEqualTo(emergencyTransferredBooking.expectedArrivalDate)
      assertThat(updatedSpaceBooking.canonicalDepartureDate).isEqualTo(emergencyTransferredBooking.expectedArrivalDate)

      assertThat(emergencyTransferredBooking.premises.id).isEqualTo(destinationPremises.id)
      assertThat(emergencyTransferredBooking.expectedArrivalDate).isEqualTo(LocalDate.now())
      assertThat(emergencyTransferredBooking.expectedDepartureDate).isEqualTo(LocalDate.now().plusMonths(1))

      domainEventAsserter.assertDomainEventsStoredInSpecificOrder(
        application.id,
        DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED,
        DomainEventType.APPROVED_PREMISES_BOOKING_MADE,
      )

      emailAsserter.assertEmailsRequestedCount(4)
      emailAsserter.assertEmailRequested(applicant.email!!, Cas1NotifyTemplates.BOOKING_MADE)
      emailAsserter.assertEmailRequested(destinationPremises.emailAddress!!, Cas1NotifyTemplates.BOOKING_MADE_FOR_PREMISES)
      emailAsserter.assertEmailRequested(applicant.email!!, Cas1NotifyTemplates.BOOKING_AMENDED)
      emailAsserter.assertEmailRequested(existingSpaceBooking.premises.emailAddress!!, Cas1NotifyTemplates.BOOKING_AMENDED)
    }
  }

  @Nested
  inner class PlannedTransfer : InitialiseDatabasePerClassTestBase() {
    lateinit var region: ProbationRegionEntity
    lateinit var premises: ApprovedPremisesEntity
    lateinit var existingSpaceBooking: Cas1SpaceBookingEntity
    lateinit var applicant: UserEntity
    lateinit var application: ApprovedPremisesApplicationEntity
    lateinit var destinationPremises: ApprovedPremisesEntity
    lateinit var existingChangeRequest: Cas1ChangeRequestEntity

    @BeforeAll
    fun setupTestData() {
      characteristicRepository.deleteAll()

      region = givenAProbationRegion()

      val (user) = givenAUser()
      val (offender) = givenAnOffender()

      premises = givenAnApprovedPremises(
        region = region,
        supportsSpaceBookings = true,
        emailAddress = "premises@test.com",
      )

      destinationPremises = givenAnApprovedPremises(
        region = region,
        supportsSpaceBookings = true,
        emailAddress = "destPremises@test.com",
      )

      applicant = givenAUser(
        staffDetail =
        StaffDetailFactory.staffDetail(email = "applicant@test.com"),
      ).first

      application = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withCreatedByUser(applicant)
        withApArea(givenAnApArea())
        withSubmittedAt(OffsetDateTime.now())
      }

      val (placementRequest) = givenAPlacementRequest(
        placementRequestAllocatedTo = user,
        assessmentAllocatedTo = user,
        createdByUser = user,
        application = application,
      )

      existingSpaceBooking = cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(offender.otherIds.crn)
        withPremises(premises)
        withPlacementRequest(placementRequest)
        withApplication(placementRequest.application)
        withCreatedBy(user)
        withActualArrivalDate(LocalDate.parse("2025-02-05"))
        withExpectedArrivalDate(LocalDate.parse("2025-02-05"))
        withExpectedDepartureDate(LocalDate.parse("2025-06-29"))
      }

      val changeRequestReason = cas1ChangeRequestReasonEntityFactory.produceAndPersist {
        withCode("TRANSFERRED")
        withChangeRequestType(ChangeRequestType.PLANNED_TRANSFER)
      }

      existingChangeRequest = cas1ChangeRequestEntityFactory.produceAndPersist {
        withSpaceBooking(existingSpaceBooking)
        withPlacementRequest(existingSpaceBooking.placementRequest!!)
        withType(ChangeRequestType.PLANNED_TRANSFER)
        withChangeRequestReason(changeRequestReason)
        withResolved(false)
      }
    }

    @Test
    fun `Returns 403 Forbidden if user does not have correct permission`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_ASSESSOR))

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${existingSpaceBooking.id}/planned-transfer")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NewPlannedTransfer(
            arrivalDate = LocalDate.now(),
            departureDate = LocalDate.now().plusMonths(1),
            destinationPremisesId = UUID.randomUUID(),
            changeRequestId = UUID.randomUUID(),
            characteristics = emptyList(),
          ),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Should create a planned booking, update the existing booking, and approve the change request`() {
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_CHANGE_REQUEST_DEV))

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${existingSpaceBooking.id}/planned-transfer")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NewPlannedTransfer(
            arrivalDate = LocalDate.now().plusDays(1),
            departureDate = LocalDate.now().plusMonths(1),
            destinationPremisesId = destinationPremises.id,
            changeRequestId = existingChangeRequest.id,
            characteristics = emptyList(),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      val updatedSpaceBooking = cas1SpaceBookingRepository.findByIdOrNull(existingSpaceBooking.id)!!

      val updatedChangedRequest = cas1ChangeRequestRepository.findByIdOrNull(existingChangeRequest.id)!!

      val plannedTransferredBooking = cas1SpaceBookingRepository.findByTransferredFrom(updatedSpaceBooking)!!

      assertThat(updatedSpaceBooking.expectedDepartureDate).isEqualTo(plannedTransferredBooking.expectedArrivalDate)
      assertThat(updatedSpaceBooking.canonicalDepartureDate).isEqualTo(plannedTransferredBooking.expectedArrivalDate)

      assertThat(plannedTransferredBooking.premises.id).isEqualTo(destinationPremises.id)
      assertThat(plannedTransferredBooking.expectedArrivalDate).isEqualTo(LocalDate.now().plusDays(1))
      assertThat(plannedTransferredBooking.expectedDepartureDate).isEqualTo(LocalDate.now().plusMonths(1))

      assertThat(updatedChangedRequest.resolved).isTrue
      assertThat(updatedChangedRequest.decision).isEqualTo(ChangeRequestDecision.APPROVED)

      domainEventAsserter.assertDomainEventsStoredInSpecificOrder(
        application.id,
        DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED,
        DomainEventType.APPROVED_PREMISES_BOOKING_MADE,
      )

      emailAsserter.assertEmailsRequestedCount(6)
      emailAsserter.assertEmailRequested(applicant.email!!, Cas1NotifyTemplates.BOOKING_MADE)
      emailAsserter.assertEmailRequested(destinationPremises.emailAddress!!, Cas1NotifyTemplates.BOOKING_MADE_FOR_PREMISES)
      emailAsserter.assertEmailRequested(applicant.email!!, Cas1NotifyTemplates.BOOKING_AMENDED)
      emailAsserter.assertEmailRequested(existingSpaceBooking.premises.emailAddress!!, Cas1NotifyTemplates.BOOKING_AMENDED)

      emailAsserter.assertEmailRequested(existingSpaceBooking.premises.emailAddress!!, Cas1NotifyTemplates.PLANNED_TRANSFER_REQUEST_ACCEPTED_FOR_REQUESTING_AP)
      emailAsserter.assertEmailRequested(destinationPremises.emailAddress!!, Cas1NotifyTemplates.PLANNED_TRANSFER_REQUEST_ACCEPTED_FOR_TARGET_AP)
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

    premisesWithNoBooking = givenAnApprovedPremises(
      region = region,
      supportsSpaceBookings = true,
    )

    premisesWithBookings = givenAnApprovedPremises(
      region = region,
      supportsSpaceBookings = true,
    )
  }

  @SuppressWarnings("LongParameterList")
  protected fun createSpaceBooking(
    crn: String,
    firstName: String,
    lastName: String,
    tier: String,
    isRestricted: Boolean = false,
    configuration: Cas1SpaceBookingEntityFactory.() -> Unit,
  ): Cas1SpaceBookingEntity {
    val (user) = givenAUser()
    val (offender) = givenAnOffender(offenderDetailsConfigBlock = {
      withCrn(crn)
      withFirstName(firstName)
      withLastName(lastName)
      withCurrentRestriction(isRestricted)
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

  protected fun createSpaceBookingWithOfflineApplication(
    crn: String,
    firstName: String,
    lastName: String,
    configuration: Cas1SpaceBookingEntityFactory.() -> Unit,
  ): Cas1SpaceBookingEntity {
    val (user) = givenAUser()
    val (offender) = givenAnOffender(offenderDetailsConfigBlock = {
      withCrn(crn)
      withFirstName(firstName)
      withLastName(lastName)
    })
    val offlineApplication = givenAnOfflineApplication(
      crn = crn,
      name = "$firstName $lastName",
    )
    return cas1SpaceBookingEntityFactory.produceAndPersist {
      withCrn(offender.otherIds.crn)
      withPremises(premisesWithBookings)
      withPlacementRequest(null)
      withApplication(null)
      withOfflineApplication(offlineApplication)
      withCreatedBy(user)

      configuration.invoke(this)
    }
  }
}
