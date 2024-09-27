package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssignKeyWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Placement Request`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Probation Region`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockSuccessfulStaffMembersCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_ASSESSOR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_FUTURE_MANAGER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class Cas1SpaceBookingTest {

  @Nested
  inner class CreateABooking : InitialiseDatabasePerClassTestBase() {

    @Autowired
    lateinit var transformer: Cas1SpaceBookingTransformer

    @Test
    fun `Booking a space without JWT returns 401`() {
      `Given a User` { user, _ ->
        `Given a Placement Request`(
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
    fun `Booking a space for an unknown placement request returns 400 Bad Request`() {
      `Given a User` { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedProbationRegion { `Given a Probation Region`() }
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
                apType = ApType.esap,
                gender = Gender.male,
                essentialCharacteristics = listOf(),
                desirableCharacteristics = listOf(),
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
      `Given a User` { user, jwt ->
        `Given a Placement Request`(
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
                  apType = ApType.esap,
                  gender = Gender.male,
                  essentialCharacteristics = listOf(),
                  desirableCharacteristics = listOf(),
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
      `Given a User` { user, jwt ->
        `Given a Placement Request`(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
        ) { placementRequest, _ ->
          val premises = approvedPremisesEntityFactory.produceAndPersist {
            withYieldedProbationRegion { `Given a Probation Region`() }
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
                  apType = ApType.esap,
                  gender = Gender.male,
                  essentialCharacteristics = listOf(),
                  desirableCharacteristics = listOf(),
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
      `Given a User` { applicant, jwt ->
        `Given a Placement Request`(
          placementRequestAllocatedTo = applicant,
          assessmentAllocatedTo = applicant,
          createdByUser = applicant,
        ) { placementRequest, application ->
          val essentialCharacteristics = listOf(
            Cas1SpaceCharacteristic.hasBrailleSignage,
            Cas1SpaceCharacteristic.hasTactileFlooring,
          )

          val desirableCharacteristics = listOf(
            Cas1SpaceCharacteristic.hasEnSuite,
            Cas1SpaceCharacteristic.hasCallForAssistance,
            Cas1SpaceCharacteristic.isGroundFloor,
            Cas1SpaceCharacteristic.isCatered,
          )

          val essentialCriteria = essentialCharacteristics.map {
            it.asCharacteristicEntity()
          }

          val desirableCriteria = desirableCharacteristics.map {
            it.asCharacteristicEntity()
          }

          placementRequest.placementRequirements = placementRequirementsFactory.produceAndPersist {
            withYieldedPostcodeDistrict {
              postCodeDistrictFactory.produceAndPersist()
            }
            withApplication(application as ApprovedPremisesApplicationEntity)
            withAssessment(placementRequest.assessment)
            withEssentialCriteria(essentialCriteria)
            withDesirableCriteria(desirableCriteria)
          }

          placementRequestRepository.saveAndFlush(placementRequest)

          val premises = approvedPremisesEntityFactory.produceAndPersist {
            withYieldedProbationRegion { `Given a Probation Region`() }
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
                  apType = placementRequest.placementRequirements.apType,
                  gender = Gender.male,
                  essentialCharacteristics = essentialCharacteristics,
                  desirableCharacteristics = desirableCharacteristics,
                ),
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(Cas1SpaceBooking::class.java)

          val result = response.responseBody.blockFirst()!!

          assertThat(result.person)
          assertThat(result.requirements.apType).isEqualTo(placementRequest.placementRequirements.apType)
          assertThat(result.requirements.gender).isEqualTo(placementRequest.placementRequirements.gender)
          assertThat(result.requirements.essentialCharacteristics).containsExactlyInAnyOrderElementsOf(
            essentialCharacteristics,
          )
          assertThat(result.requirements.desirableCharacteristics).containsExactlyInAnyOrderElementsOf(
            desirableCharacteristics,
          )
          assertThat(result.premises.id).isEqualTo(premises.id)
          assertThat(result.premises.name).isEqualTo(premises.name)
          assertThat(result.apArea.id).isEqualTo(premises.probationRegion.apArea!!.id)
          assertThat(result.apArea.name).isEqualTo(premises.probationRegion.apArea!!.name)
          assertThat(result.bookedBy.id).isEqualTo(applicant.id)
          assertThat(result.bookedBy.name).isEqualTo(applicant.name)
          assertThat(result.bookedBy.deliusUsername).isEqualTo(applicant.deliusUsername)
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

    private fun Cas1SpaceCharacteristic.asCharacteristicEntity() = characteristicEntityFactory.produceAndPersist {
      withName(this@asCharacteristicEntity.value)
      withPropertyName(this@asCharacteristicEntity.value)
      withServiceScope(ServiceName.approvedPremises.value)
      withModelScope("*")
    }
  }

  @Nested
  inner class SearchForSpaceBookings : InitialiseDatabasePerClassTestBase() {
    lateinit var premisesWithNoBooking: ApprovedPremisesEntity
    lateinit var premisesWithBookings: ApprovedPremisesEntity

    lateinit var currentSpaceBooking1: Cas1SpaceBookingEntity
    lateinit var currentSpaceBooking2: Cas1SpaceBookingEntity
    lateinit var currentSpaceBooking3: Cas1SpaceBookingEntity
    lateinit var upcomingSpaceBooking: Cas1SpaceBookingEntity
    lateinit var upcomingCancelledSpaceBooking: Cas1SpaceBookingEntity
    lateinit var departedSpaceBooking: Cas1SpaceBookingEntity

    @BeforeAll
    fun setupTestData() {
      val region = `Given a Probation Region`()

      premisesWithNoBooking = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedProbationRegion { region }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      }

      premisesWithBookings = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedProbationRegion { region }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      }

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

      upcomingSpaceBooking = createSpaceBooking(crn = "CRN_UPCOMING", firstName = "up", lastName = "coming", tier = "U") {
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
    }

    private fun createSpaceBooking(
      crn: String,
      firstName: String,
      lastName: String,
      tier: String,
      configuration: Cas1SpaceBookingEntityFactory.() -> Unit,
    ): Cas1SpaceBookingEntity {
      val (user) = `Given a User`()
      val (offender) = `Given an Offender`(offenderDetailsConfigBlock = {
        withCrn(crn)
        withFirstName(firstName)
        withLastName(lastName)
      },)
      val (placementRequest) = `Given a Placement Request`(
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

    @Test
    fun `Returns 403 Forbidden if user does not have correct role`() {
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_ASSESSOR))

      webTestClient.get()
        .uri("/cas1/premises/${premisesWithNoBooking.id}/space-bookings")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Empty list returned if no results for given premises`() {
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_FUTURE_MANAGER))

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
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_FUTURE_MANAGER))

      val response = webTestClient.get()
        .uri("/cas1/premises/${premisesWithBookings.id}/space-bookings?sortBy=canonicalArrivalDate&sortDirection=asc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1SpaceBookingSummary>()

      assertThat(response).hasSize(5)
      assertThat(response[0].person.crn).isEqualTo("CRN_DEPARTED")
      assertThat(response[1].person.crn).isEqualTo("CRN_CURRENT1")
      assertThat(response[2].person.crn).isEqualTo("CRN_CURRENT2")
      assertThat(response[3].person.crn).isEqualTo("CRN_CURRENT3")
      assertThat(response[4].person.crn).isEqualTo("CRN_UPCOMING")
    }

    @Test
    fun `Filter on residency historic`() {
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_FUTURE_MANAGER))

      val response = webTestClient.get()
        .uri("/cas1/premises/${premisesWithBookings.id}/space-bookings?residency=historic&sortBy=canonicalArrivalDate&sortDirection=asc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1SpaceBookingSummary>()

      assertThat(response).hasSize(1)
      assertThat(response[0].person.crn).isEqualTo("CRN_DEPARTED")
    }

    @Test
    fun `Filter on residency upcoming`() {
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_FUTURE_MANAGER))

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
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_FUTURE_MANAGER))

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
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_FUTURE_MANAGER))

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
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_FUTURE_MANAGER))

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
    fun `Sort on Name`() {
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_FUTURE_MANAGER))

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
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_FUTURE_MANAGER))

      val response = webTestClient.get()
        .uri("/cas1/premises/${premisesWithBookings.id}/space-bookings?sortBy=canonicalArrivalDate&sortDirection=desc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1SpaceBookingSummary>()

      assertThat(response).hasSize(5)
      assertThat(response[0].person.crn).isEqualTo("CRN_UPCOMING")
      assertThat(response[1].person.crn).isEqualTo("CRN_CURRENT3")
      assertThat(response[2].person.crn).isEqualTo("CRN_CURRENT2")
      assertThat(response[3].person.crn).isEqualTo("CRN_CURRENT1")
      assertThat(response[4].person.crn).isEqualTo("CRN_DEPARTED")
    }

    @Test
    fun `Sort on Canonical Departure Date`() {
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_FUTURE_MANAGER))

      val response = webTestClient.get()
        .uri("/cas1/premises/${premisesWithBookings.id}/space-bookings?sortBy=canonicalDepartureDate&sortDirection=asc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1SpaceBookingSummary>()

      assertThat(response).hasSize(5)
      assertThat(response[0].person.crn).isEqualTo("CRN_DEPARTED")
      assertThat(response[1].person.crn).isEqualTo("CRN_CURRENT1")
      assertThat(response[2].person.crn).isEqualTo("CRN_CURRENT2")
      assertThat(response[3].person.crn).isEqualTo("CRN_CURRENT3")
      assertThat(response[4].person.crn).isEqualTo("CRN_UPCOMING")
    }

    @Test
    fun `Sort on Keyworker`() {
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_FUTURE_MANAGER))

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
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_FUTURE_MANAGER))

      val response = webTestClient.get()
        .uri("/cas1/premises/${premisesWithBookings.id}/space-bookings?sortBy=tier&sortDirection=desc")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1SpaceBookingSummary>()

      assertThat(response).hasSize(5)

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
      val region = `Given a Probation Region`()

      premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedProbationRegion { region }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      }

      otherPremises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedProbationRegion { region }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      }

      val (user) = `Given a User`()
      val (offender) = `Given an Offender`()
      val (placementRequest) = `Given a Placement Request`(
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
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_ASSESSOR))

      webTestClient.get()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Returns 404 if premise doesn't exist`() {
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_FUTURE_MANAGER))

      webTestClient.get()
        .uri("/cas1/premises/${UUID.randomUUID()}/space-bookings/${spaceBooking.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `Returns 404 if space booking doesn't exist`() {
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_FUTURE_MANAGER))

      webTestClient.get()
        .uri("/cas1/premises/${premises.id}/space-bookings/${UUID.randomUUID()}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `Returns premises information if have correct role`() {
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_FUTURE_MANAGER))

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
    }
  }

  @Nested
  inner class RecordArrival : InitialiseDatabasePerClassTestBase() {

    lateinit var region: ProbationRegionEntity
    lateinit var premises: ApprovedPremisesEntity
    lateinit var spaceBooking: Cas1SpaceBookingEntity

    @BeforeAll
    fun setupTestData() {
      region = `Given a Probation Region`()

      premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedProbationRegion { region }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      }

      val (user) = `Given a User`()
      val (offender) = `Given an Offender`()
      val (placementRequest) = `Given a Placement Request`(
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
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_ASSESSOR))

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/arrival")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NewArrival(
            expectedDepartureDate = LocalDate.now().plusMonths(1),
            arrivalDateTime = LocalDateTime.now().toInstant(ZoneOffset.UTC),
          ),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Returns 500 Internal Server Error if unexpected failure occurs - invalid key worker )`() {
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_FUTURE_MANAGER))

      val (user) = `Given a User`()
      val (offender) = `Given an Offender`()
      val (placementRequest) = `Given a Placement Request`(
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
            expectedDepartureDate = LocalDate.now().plusMonths(1),
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
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_FUTURE_MANAGER))

      val (user) = `Given a User`()
      val (offender) = `Given an Offender`()
      val (placementRequest) = `Given a Placement Request`(
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
      }

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/arrival")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NewArrival(
            expectedDepartureDate = LocalDate.now().plusMonths(1),
            arrivalDateTime = LocalDateTime.now().toInstant(ZoneOffset.UTC),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
      domainEventAsserter.assertDomainEventOfTypeStored(spaceBooking.application.id, DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED)
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
      region = `Given a Probation Region`()

      premises = approvedPremisesEntityFactory.produceAndPersist {
        withQCode("QCODE")
        withYieldedProbationRegion { region }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      }

      keyWorker = `Given a User`().first
      val (user) = `Given a User`()
      val (offender) = `Given an Offender`()
      val (placementRequest) = `Given a Placement Request`(
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
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_ASSESSOR))
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
    fun `Recording key worker returns OK`() {
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_FUTURE_MANAGER))

      val keyWorker = ContextStaffMemberFactory().produce()
      APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, "QCODE")

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
      region = `Given a Probation Region`()

      premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedProbationRegion { region }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      }

      val (user) = `Given a User`()
      val (offender) = `Given an Offender`()
      val (placementRequest) = `Given a Placement Request`(
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
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_ASSESSOR))

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
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_FUTURE_MANAGER))

      val (user) = `Given a User`()
      val (offender) = `Given an Offender`()
      val (placementRequest) = `Given a Placement Request`(
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
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_FUTURE_MANAGER))

      val (user) = `Given a User`()
      val (offender) = `Given an Offender`()
      val (placementRequest) = `Given a Placement Request`(
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
      domainEventAsserter.assertDomainEventOfTypeStored(spaceBooking.application.id, DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED)
    }
  }
}
