package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawableType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawables
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.jsonForObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

/**
 * This test class tests common withdrawal functionality that spans multiple entity types and accordingly,
 * multiple API endpoints. Specifically:
 *
 * 1. Getting a list of withdrawable elements
 * 2. Cascading withdrawals
 *
 * When considering withdrawals, an application can be considered as a tree of elements:
 *
 * ```
 * application
 *  - assessment
 *    - request for placement (PlacementRequest)
 *      - placement (Booking)
 *    - request for placement (PlacementApplication)
 *      - matching request (PlacementRequest)
 *        - placement (Booking)
 *    - adhoc placement (Booking)
 * ```
 *
 * Withdrawals should cascade down the tree, although a booking with arrivals will block any ancestors in the tree
 * from being withdrawn
 *
 * Note: The general functionality of each entities' withdrawal endpoint is tested in the corresponding API Test Class
 */
@SuppressWarnings("LongParameterList")
class Cas1WithdrawalTest : IntegrationTestBase() {

  @Nested
  inner class GetWithdrawables {

    /**
     * ```
     * | Entities                         | Withdrawable |
     * | -------------------------------- | ------------ |
     * | Application                      | -            |
     * ```
     */
    @Test
    fun `Returns application only for a sparse application if user is not the applicant`() {
      givenAUser { applicationCreator, _ ->
        givenAUser { _, jwt ->
          givenAnOffender { offenderDetails, _ ->
            val application =
              produceAndPersistBasicApplication(offenderDetails.otherIds.crn, applicationCreator, "TEAM")

            val expected = Withdrawables(
              notes = emptyList(),
              withdrawables = emptyList(),
            )

            webTestClient.get()
              .uri("/cas1/applications/${application.id}/withdrawablesWithNotes")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .jsonForObject(expected)
          }
        }
      }
    }

    /**
     * ```
     * | Entities                         | Withdrawable |
     * | -------------------------------- | ------------ |
     * | Application                      | YES            |
     * ```
     */
    @Test
    fun `Returns application only for a sparse application if user is applicant`() {
      givenAUser { applicationCreator, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val application = produceAndPersistBasicApplication(offenderDetails.otherIds.crn, applicationCreator, "TEAM")

          val expected = Withdrawables(
            notes = emptyList(),
            withdrawables = listOf(toWithdrawable(application)),
          )

          webTestClient.get()
            .uri("/cas1/applications/${application.id}/withdrawablesWithNotes")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonForObject(expected)
        }
      }
    }

    /**
     * ```
     * | Entities                           | Withdrawable |
     * | ---------------------------------- | ------------ |
     * | Application                        | -            |
     * | -> Placement Request 1             | Yes          |
     * | ---> Booking without arrival       | -            |
     * | -> Placement Request (reallocated) | -            |
     * | -> Placement Request (withdrawn)   | -            |
     * | -> Placement Application           | Yes          |
     * | ---> Placement Request 2           | -            |
     * ```
     */
    @Test
    fun `Returns match request for the original app dates only`() {
      givenAUser { applicant, jwt ->
        givenAUser { allocatedTo, _ ->
          givenAnOffender { offenderDetails, _ ->

            val (application, _) = createApplicationAndAssessment(applicant, allocatedTo, offenderDetails)

            val placementRequest = createPlacementRequest(application)
            givenACas1SpaceBooking(
              application = application,
              placementRequest = placementRequest,
              actualArrivalDate = null,
              nonArrivalConfirmedAt = null,
            )

            createPlacementRequest(application, isWithdrawn = true)

            val placementApplication = createPlacementApplication(application, DateSpan(nowPlusDays(50), duration = 6))
            createPlacementRequest(application, placementApplication = placementApplication)

            val expected = Withdrawables(
              notes = emptyList(),
              withdrawables = listOf(
                toWithdrawable(application),
                toWithdrawable(placementRequest),
                toWithdrawable(placementApplication),
              ),
            )

            webTestClient.get()
              .uri("/cas1/applications/${application.id}/withdrawablesWithNotes")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .jsonForObject(expected)
          }
        }
      }
    }

    /**
     * ```
     * | Entities                                     | Withdrawable |
     * | -------------------------------------------- | ------------ |
     * | Application                                  | -            |
     * | -> Placement Application 1                   | Yes          |
     * | -> Placement Application 2                   | Yes          |
     * | -> Placement Application 3 (Unsubmitted)     | -            |
     * | -> Placement Application 4 (Reallocated)     | -            |
     * | -> Placement Application 5 (With Decision)   | Yes          |
     * | -> Placement Application 6 (Withdrawn)       | -            |
     * | -> Placement Application 7 (Rejected)        | Yes          |
     * ```
     */
    @Test
    fun `Returns requests for placements applications`() {
      givenAUser { applicant, jwt ->
        givenAUser { allocatedTo, _ ->
          givenAnOffender { offenderDetails, _ ->

            val (application, _) = createApplicationAndAssessment(applicant, allocatedTo, offenderDetails)

            val submittedPlacementApplication1 = createPlacementApplication(
              application,
              dateSpan = DateSpan(nowPlusDays(1), duration = 5),
            )

            val submittedPlacementApplication2 = createPlacementApplication(
              application,
              DateSpan(nowPlusDays(50), duration = 6),
            )

            createPlacementApplication(
              application,
              isSubmitted = false,
              decision = null,
            )

            createPlacementApplication(
              application,
              DateSpan(LocalDate.now(), duration = 2),
              reallocatedAt = OffsetDateTime.now(),
            )

            val applicationWithAcceptedDecision = createPlacementApplication(
              application,
              DateSpan(nowPlusDays(50), duration = 6),
              decision = PlacementApplicationDecision.ACCEPTED,
            )

            createPlacementApplication(
              application,
              DateSpan(now(), duration = 2),
              decision = PlacementApplicationDecision.ACCEPTED,
              isWithdrawn = true,
            )

            val applicationWithRejectedDecision = createPlacementApplication(
              application,
              DateSpan(nowPlusDays(50), duration = 6),
              decision = PlacementApplicationDecision.REJECTED,
            )

            val expected = Withdrawables(
              notes = emptyList(),
              withdrawables = listOf(
                toWithdrawable(application),
                toWithdrawable(submittedPlacementApplication1),
                toWithdrawable(submittedPlacementApplication2),
                toWithdrawable(applicationWithAcceptedDecision),
                toWithdrawable(applicationWithRejectedDecision),
              ),
            )

            webTestClient.get()
              .uri("/cas1/applications/${application.id}/withdrawablesWithNotes")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .jsonForObject(expected)
          }
        }
      }
    }

    /**
     * ```
     * | Entities                               | Withdrawable |
     * | -------------------------------------- | ------------ |
     * | Application                            | BLOCKED      |
     * | -> Placement application 1             | YES          |
     * | ---> Placement request 1               | -            |
     * | -----> Space Booking 1 arrival pending | YES          |
     * | -> Placement application 2             | YES          |
     * | -> Placement request 2                 | BLOCKED      |
     * | ---> Space Booking 2 has arrival       | BLOCKING     |
     * ```
     */
    @Test
    fun `Returns all possible types when a user can manage bookings, with booking arrivals in CAS1 blocking bookings`() {
      givenAUser { applicant, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { _, jwt ->
          givenAUser { requestForPlacementAssessor, _ ->
            givenAnOffender { offenderDetails, _ ->
              val (application, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)
              val (otherApplication, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)

              val placementApplication1 = createPlacementApplication(application, DateSpan(now(), duration = 2))
              val placementRequest1 = createPlacementRequest(application, placementApplication = placementApplication1)
              val booking1ArrivalPending = givenACas1SpaceBooking(
                application = application,
                nonArrivalConfirmedAt = null,
                actualArrivalDate = null,
                actualDepartureDate = null,
                placementRequest = placementRequest1,
              )
              // addBookingToPlacementRequest(placementRequest1, booking1NoArrival)

              // createPlacementRequest(application, placementApplication = placementApplication1)

              val placementApplication2 = createPlacementApplication(
                application,
                DateSpan(now(), duration = 2),
                allocatedTo = requestForPlacementAssessor,
              )

              val placementRequest2 = createPlacementRequest(application)
              givenACas1SpaceBooking(
                crn = application.crn,
                expectedArrivalDate = LocalDate.now(),
                expectedDepartureDate = nowPlusDays(1),
                actualArrivalDate = LocalDate.now(),
                nonArrivalConfirmedAt = null,
                placementRequest = placementRequest2,
              )

              givenACas1SpaceBooking(
                application = otherApplication,
              )
              givenACas1SpaceBooking(
                application = otherApplication,
              )

              val expected = Withdrawables(
                notes = listOf("1 or more placements cannot be withdrawn as they have an arrival"),
                withdrawables = listOf(
                  toWithdrawable(placementApplication1),
                  toWithdrawable(booking1ArrivalPending),
                  toWithdrawable(placementApplication2),
                ),
              )

              webTestClient.get()
                .uri("/cas1/applications/${application.id}/withdrawablesWithNotes")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonForObject(expected)
            }
          }
        }
      }
    }

    /**
     * ```
     * | Entities                          | Withdrawable |
     * | --------------------------------- | ------------ |
     * | Application                       | BLOCKED      |
     * | -> Placement Application 1        | YES          |
     * | ---> Placement request 1          | -            |
     * | -----> Booking 1 arrival pending  | YES          |
     * | -> Placement Application 2        | YES          |
     * | -> Placement request 2            | BLOCKED      |
     * | ---> Space Booking 2 non arrival  | BLOCKING     |
     * ```
     */
    @Test
    fun `Returns all possible types when a user can manage bookings, with booking non arrivals blocking bookings`() {
      givenAUser { applicant, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { _, jwt ->
          givenAUser { requestForPlacementAssessor, _ ->
            givenAnOffender { offenderDetails, _ ->
              val (application, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)
              val (otherApplication, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)

              val placementApplication1 = createPlacementApplication(application, DateSpan(now(), duration = 2))
              val placementRequest1 = createPlacementRequest(application, placementApplication = placementApplication1)
              val booking1NoArrival = givenACas1SpaceBooking(
                application = application,
                placementRequest = placementRequest1,
                actualArrivalDate = null,
                nonArrivalConfirmedAt = null,
              )

              val placementApplication2 = createPlacementApplication(
                application,
                DateSpan(now(), duration = 2),
                allocatedTo = requestForPlacementAssessor,
              )

              val placementRequest2 = createPlacementRequest(application)
              // spaceBooking2HasNonArrival
              givenACas1SpaceBooking(
                crn = application.crn,
                expectedArrivalDate = LocalDate.now(),
                expectedDepartureDate = nowPlusDays(1),
                nonArrivalConfirmedAt = Instant.now(),
                placementRequest = placementRequest2,
              )

              givenACas1SpaceBooking(application = otherApplication)
              givenACas1SpaceBooking(application = otherApplication)

              val expected = Withdrawables(
                notes = listOf("1 or more placements cannot be withdrawn as they have a non-arrival"),
                withdrawables = listOf(
                  toWithdrawable(placementApplication1),
                  toWithdrawable(booking1NoArrival),
                  toWithdrawable(placementApplication2),
                ),
              )

              webTestClient.get()
                .uri("/cas1/applications/${application.id}/withdrawablesWithNotes")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonForObject(expected)
            }
          }
        }
      }
    }

    /**
     * ```
     * | Entities                             | Withdrawable |
     * | ------------------------------------ | ------------ |
     * | Application                          | BLOCKED      |
     * | -> Placement application 1           | YES          |
     * | ---> Placement request 1             | -            |
     * | -----> Booking arrival pending       | YES          |
     * | -> Placement application 2           | YES          |
     * | -> Placement request 3               | BLOCKED      |
     * | ---> Booking has arrival             | BLOCKING     |
     * ```
     */
    @Test
    fun `Returns all possible types when a user can manage bookings`() {
      givenAUser { applicant, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { _, jwt ->
          givenAUser { requestForPlacementAssessor, _ ->
            givenAnOffender { offenderDetails, _ ->
              val (application, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)
              val (otherApplication, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)

              val placementApplication1 = createPlacementApplication(application, DateSpan(now(), duration = 2))
              val placementRequest1 = createPlacementRequest(application, placementApplication = placementApplication1)
              val spaceBookingNoArrival = createSpaceBooking(
                application = application,
                startDate = nowPlusDays(1),
                endDate = nowPlusDays(6),
                arrivalDate = null,
                placementRequest = placementRequest1,
              )

              val placementApplication2 = createPlacementApplication(
                application,
                DateSpan(now(), duration = 2),
                allocatedTo = requestForPlacementAssessor,
              )

              val placementRequest2 = createPlacementRequest(application)
              givenACas1SpaceBooking(
                application = application,
                placementRequest = placementRequest2,
                actualArrivalDate = LocalDate.now(),
                nonArrivalConfirmedAt = null,
              )

              givenACas1SpaceBooking(application = otherApplication)
              givenACas1SpaceBooking(application = otherApplication)

              val expected = Withdrawables(
                notes = listOf("1 or more placements cannot be withdrawn as they have an arrival"),
                withdrawables = listOf(
                  toWithdrawable(placementApplication1),
                  toWithdrawable(spaceBookingNoArrival),
                  toWithdrawable(placementApplication2),
                ),
              )

              webTestClient.get()
                .uri("/cas1/applications/${application.id}/withdrawablesWithNotes")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonForObject(expected)
            }
          }
        }
      }
    }
  }

  @Nested
  inner class WithdrawalCascading {

    /**
     * ```
     * |                                  |           |         Receive Email          |
     * | Entities                         | Withdrawn | PP   | Case Manager | Assessor |
     * | -------------------------------- | --------- | ---- | ------------ | -------- |
     * | Application                      | YES       | YES  | YES          | -        |
     * | -> Assessment (pending)          | YES       | -    | -            | YES      |
     * ```
     */
    @Test
    fun `Withdrawing an application cascades to an assessment and sends mail to assessor if pending`() {
      givenAUser { applicant, jwt ->
        givenAUser { assessor, _ ->
          givenAnOffender { offenderDetails, _ ->
            val caseManagerEmail = "caseManager@test.com"

            val (application, assessment) = createApplicationAndAssessment(
              applicant = applicant,
              assignee = applicant,
              offenderDetails = offenderDetails,
              assessmentSubmitted = false,
              assessmentAllocatedTo = assessor,
              caseManager = cas1ApplicationUserDetailsEntityFactory.produceAndPersist {
                withEmailAddress(caseManagerEmail)
              },
            )

            withdrawApplication(application, jwt)

            assertApplicationWithdrawn(application)
            assertAssessmentWithdrawn(assessment)

            val applicantEmail = applicant.email!!
            val assessorEmail = assessment.allocatedToUser!!.email!!

            emailAsserter.assertEmailsRequestedCount(3)
            assertApplicationWithdrawnEmail(applicantEmail, application)
            assertApplicationWithdrawnEmail(caseManagerEmail, application)
            assertAssessmentWithdrawnEmail(assessorEmail)
          }
        }
      }
    }

    /**
     * For this test the applicant is not the same person as the case manager, meaning
     * emails typically sent to the applicant will also be sent to the case manager
     *
     * ```
     * |                                                |             Receive Email                 |
     * | Entities                           | Withdrawn | PP  | Case Manager | AP  | CRU | Assessor |
     * | ---------------------------------- | --------- | --- | ------------ | --- | --- | -------- |
     * | Application                        | YES       | YES | YES          | -   | -   | -        |
     * | -> Assessment                      | YES       | -   | -            | -   | -   | -        |
     * | -> Placement Application 1         | YES       | YES | YES          | -   | -   | -        |
     * | ---> Placement request 1           | YES       | -   | -            | -   | -   | -        |
     * | -----> Booking arrival pending     | YES       | YES | YES          | YES | YES | -        |
     * | ---> Placement request 2           | YES       | -   | -            | -   | YES | -        |
     * | -> Placement Application 2         | YES       | YES | YES          | -   | -   | YES      |
     * | -> Placement request 2             | YES       | YES | YES          | -   | -   | -        |
     * | ---> Booking arrival pending       | YES       | YES | YES          | YES | YES | -        |
     * ```
     */
    @Test
    fun `Withdrawing an application cascades to applicable entities`() {
      givenAUser { applicant, jwt ->
        givenAUser { requestForPlacementAssessor, _ ->
          givenAnOffender { offenderDetails, _ ->
            val caseManagerEmail = "caseManager@test.com"

            val (application, assessment) = createApplicationAndAssessment(
              applicant = applicant,
              assignee = applicant,
              offenderDetails = offenderDetails,
              caseManager = cas1ApplicationUserDetailsEntityFactory.produceAndPersist {
                withEmailAddress(caseManagerEmail)
              },
            )
            val (otherApplication, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)

            val placementApplication1 = createPlacementApplication(application, DateSpan(now(), duration = 2))
            val placementRequest1 = createPlacementRequest(application, placementApplication = placementApplication1)
            val bookingNoArrival = givenACas1SpaceBooking(
              crn = application.crn,
              application = application,
              placementRequest = placementRequest1,
              actualArrivalDate = null,
              nonArrivalConfirmedAt = null,
            )

            val placementApplication2NoBookingBeingAssessed = createPlacementApplication(
              application,
              DateSpan(nowPlusDays(2), duration = 2),
              allocatedTo = requestForPlacementAssessor,
              decision = null,
            )

            val placementRequest2 = createPlacementRequest(application)
            val spaceBookingNoArrival = createSpaceBooking(
              application = application,
              startDate = nowPlusDays(5),
              endDate = nowPlusDays(15),
              arrivalDate = null,
              placementRequest = placementRequest2,
            )

            givenACas1SpaceBooking(application = otherApplication)
            givenACas1SpaceBooking(application = otherApplication)

            withdrawApplication(application, jwt)

            assertApplicationWithdrawn(application)
            assertAssessmentWithdrawn(assessment)

            assertPlacementApplicationWithdrawn(
              placementApplication1,
              PlacementApplicationWithdrawalReason.RELATED_APPLICATION_WITHDRAWN,
            )
            assertPlacementRequestWithdrawn(
              placementRequest1,
              PlacementRequestWithdrawalReason.RELATED_APPLICATION_WITHDRAWN,
            )
            assertSpaceBookingWithdrawn(bookingNoArrival, "Related application withdrawn")

            assertPlacementApplicationWithdrawn(
              placementApplication2NoBookingBeingAssessed,
              PlacementApplicationWithdrawalReason.RELATED_APPLICATION_WITHDRAWN,
            )

            assertPlacementRequestWithdrawn(
              placementRequest2,
              PlacementRequestWithdrawalReason.RELATED_APPLICATION_WITHDRAWN,
            )
            assertSpaceBookingWithdrawn(spaceBookingNoArrival, "Related application withdrawn")

            val applicantEmail = applicant.email!!
            val cruEmail = application.cruManagementArea!!.emailAddress!!
            val requestForPlacementAssessorEmail = requestForPlacementAssessor.email!!

            emailAsserter.assertEmailsRequestedCount(17)
            assertApplicationWithdrawnEmail(applicantEmail, application)
            assertApplicationWithdrawnEmail(caseManagerEmail, application)

            assertPlacementRequestWithdrawnEmail(applicantEmail, placementApplication1)
            assertPlacementRequestWithdrawnEmail(caseManagerEmail, placementApplication1)

            assertSpaceBookingWithdrawnEmail(applicantEmail, bookingNoArrival)
            assertSpaceBookingWithdrawnEmail(caseManagerEmail, bookingNoArrival)
            assertSpaceBookingWithdrawnEmail(bookingNoArrival.premises.emailAddress!!, bookingNoArrival)
            assertSpaceBookingWithdrawnEmail(cruEmail, bookingNoArrival)

            assertPlacementRequestWithdrawnEmail(applicantEmail, placementApplication2NoBookingBeingAssessed)
            assertPlacementRequestWithdrawnEmail(caseManagerEmail, placementApplication2NoBookingBeingAssessed)
            assertPlacementRequestWithdrawnEmail(requestForPlacementAssessorEmail, placementApplication2NoBookingBeingAssessed)

            assertPlacementRequestWithdrawnEmail(applicantEmail, placementRequest2)
            assertPlacementRequestWithdrawnEmail(caseManagerEmail, placementRequest2)

            assertSpaceBookingWithdrawnEmail(applicantEmail, spaceBookingNoArrival)
            assertSpaceBookingWithdrawnEmail(caseManagerEmail, spaceBookingNoArrival)
            assertSpaceBookingWithdrawnEmail(spaceBookingNoArrival.premises.emailAddress!!, spaceBookingNoArrival)
            assertSpaceBookingWithdrawnEmail(cruEmail, spaceBookingNoArrival)
          }
        }
      }
    }

    /**
     * For this test the applicant is also the case manager
     *
     * ```
     * | Entities                         | Withdrawn | Email PP | Email AP | Email CRU | Email Assessor |
     * | ---------------------------------| --------- | -------- | -------- | --------- | -------------- |
     * | Application                      | BLOCKED   | -        | -        | -         | -              |
     * | -> Placement Application         | BLOCKED   | -        | -        | -         | -              |
     * | ---> Placement request           | BLOCKED   | -        | -        | -         | -              |
     * | -----> Space Booking has arrival | BLOCKED   | -        | -        | -         | -              |
     * ```
     */
    @Test
    fun `Withdrawing an application is not allowed if has a space booking with arrivals`() {
      givenAUser { applicant, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val (application, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)

          val placementApplication = createPlacementApplication(application, DateSpan(now(), duration = 2))
          val placementRequest = createPlacementRequest(application, placementApplication = placementApplication)
          createSpaceBooking(
            application = application,
            startDate = nowPlusDays(1),
            endDate = nowPlusDays(6),
            arrivalDate = nowPlusDays(1).atStartOfDay(),
            placementRequest = placementRequest,
          )

          webTestClient.post()
            .uri("/cas1/applications/${application.id}/withdrawal")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewWithdrawal(
                reason = WithdrawalReason.duplicateApplication,
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
        }
      }
    }

    /**
     * For this test the applicant is also the case manager
     *
     * ```
     * | Entities                         | Withdrawn | Email PP | Email AP | Email CRU | Email Assessor |
     * | -------------------------------- | --------- | -------- | -------- | --------- | -------------- |
     * | Application                      | -         | -        | -        | -         | -              |
     * | -> Assessment                    | -         | -        | -        | -         | -              |
     * | -> Placement application 1       | YES       | YES      | -        | -         | -              |
     * | ---> Placement request 1         | YES       | -        | -        | -         | -              |
     * | -----> Booking 1 arrival pending | YES       | YES      | YES      | YES       | -              |
     * | -> Placement application 2       | -         | -        | -        | -         | -              |
     * | ---> Placement request 2         | -         | -        | -        | -         | -              |
     * | -----> Booking 2 arrival pending | -         | YES      | -        | -         | -              |
     * ```
     */
    @Test
    fun `Withdrawing a request for placement cascades to applicable entities`() {
      givenAUser { applicant, _ ->
        givenAUser { placementAppCreator, jwt ->
          givenAnOffender { offenderDetails, _ ->
            val (application, assessment) = createApplicationAndAssessment(applicant, applicant, offenderDetails)

            val placementApplication1 =
              createPlacementApplication(application, DateSpan(now(), duration = 2), createdBy = placementAppCreator)
            val placementRequest1 = createPlacementRequest(application, placementApplication = placementApplication1)
            val booking1PendingArrival = givenACas1SpaceBooking(
              crn = application.crn,
              application = application,
              placementRequest = placementRequest1,
              actualArrivalDate = null,
              nonArrivalConfirmedAt = null,
            )

            val placementApplication2 = createPlacementApplication(application, DateSpan(now(), duration = 2))
            val placementRequest2 = createPlacementRequest(application, placementApplication = placementApplication2)
            val booking2PendingArrival = givenACas1SpaceBooking(
              crn = application.crn,
              application = application,
              placementRequest = placementRequest2,
              actualArrivalDate = null,
              nonArrivalConfirmedAt = null,
            )

            withdrawPlacementApplication(
              placementApplication1,
              WithdrawPlacementRequestReason.duplicatePlacementRequest,
              jwt,
            )

            assertApplicationNotWithdrawn(application)
            assertAssessmentNotWithdrawn(assessment)

            assertPlacementApplicationWithdrawn(
              placementApplication1,
              PlacementApplicationWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST,
            )

            assertPlacementRequestWithdrawn(
              placementRequest1,
              PlacementRequestWithdrawalReason.RELATED_PLACEMENT_APPLICATION_WITHDRAWN,
            )
            assertSpaceBookingWithdrawn(booking1PendingArrival, "Related request for placement withdrawn")

            assertPlacementApplicationNotWithdrawn(placementApplication2)
            assertPlacementRequestNotWithdrawn(placementRequest2)
            assertSpaceBookingNotWithdrawn(booking2PendingArrival)

            val applicantEmail = applicant.email!!
            val placementAppCreatorEmail = placementAppCreator.email!!
            val cruEmail = application.cruManagementArea!!.emailAddress!!

            emailAsserter.assertEmailsRequestedCount(6)
            assertPlacementRequestWithdrawnEmail(applicantEmail, placementRequest1)
            assertPlacementRequestWithdrawnEmail(placementAppCreatorEmail, placementRequest1)

            assertSpaceBookingWithdrawnEmail(applicantEmail, booking1PendingArrival)
            assertSpaceBookingWithdrawnEmail(placementAppCreatorEmail, booking1PendingArrival)
            assertSpaceBookingWithdrawnEmail(booking1PendingArrival.premises.emailAddress!!, booking1PendingArrival)
            assertSpaceBookingWithdrawnEmail(cruEmail, booking1PendingArrival)
          }
        }
      }
    }

    /**
     * For this test the applicant is also the case manager
     *
     * ```
     * | Entities                         | Withdrawn | Email PP | Email AP | Email CRU | Email Assessor |
     * | -------------------------------- | --------- | -------- | -------- | --------- | -------------- |
     * | Application                      | -         | -        | -        | -         | -              |
     * | -> Assessment                    | -         | -        | -        | -         | -              |
     * | -> Placement request             | YES       | YES      | -        | YES       | -              |
     * ```
     */
    @Test
    fun `Withdrawing a match request for original app dates without booking emails CRU`() {
      givenAUser { applicant, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val (application, assessment) = createApplicationAndAssessment(
            applicant = applicant,
            assignee = applicant,
            offenderDetails = offenderDetails,
            assessmentAllocatedTo = applicant,
          )

          val placementRequest = createPlacementRequest(application)

          Assertions.assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.STARTED)

          withdrawPlacementRequest(
            placementRequest,
            WithdrawPlacementRequestReason.duplicatePlacementRequest,
            jwt,
          )

          assertApplicationStatus(application, ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST)

          assertApplicationNotWithdrawn(application)
          assertAssessmentNotWithdrawn(assessment)

          assertPlacementRequestWithdrawn(placementRequest, PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST)

          val applicantEmail = applicant.email!!
          val cruEmail = application.cruManagementArea!!.emailAddress!!

          emailAsserter.assertEmailsRequestedCount(2)
          assertPlacementRequestWithdrawnEmail(applicantEmail, placementRequest)
          assertMatchRequestWithdrawnEmail(cruEmail, placementRequest)
        }
      }
    }

    /**
     * For this test the applicant is also the case manager
     *
     * ```
     * | Entities                         | Withdrawn | Email PP | Email AP | Email CRU | Email Assessor |
     * | -------------------------------- | --------- | -------- | -------- | --------- | -------------- |
     * | Application                      | -         | -        | -        | -         | -              |
     * | -> Assessment                    | -         | -        | -        | -         | -              |
     * | -> Placement request             | YES       | YES      | -        | -         | -              |
     * | ---> Booking arrival pending     | YES       | YES      | YES      | YES       | -              |
     * ```
     */
    @Test
    fun `Withdrawing a match request for original app dates cascades to applicable booking entity and updates the application status`() {
      givenAUser { applicant, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val (application, assessment) = createApplicationAndAssessment(
            applicant = applicant,
            assignee = applicant,
            offenderDetails = offenderDetails,
            assessmentAllocatedTo = applicant,
          )

          val placementRequest = createPlacementRequest(application)
          val bookingPendingArrival = givenACas1SpaceBooking(
            crn = application.crn,
            application = application,
            placementRequest = placementRequest,
            actualArrivalDate = null,
            nonArrivalConfirmedAt = null,
          )

          Assertions.assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.STARTED)

          withdrawPlacementRequest(
            placementRequest,
            WithdrawPlacementRequestReason.duplicatePlacementRequest,
            jwt,
          )

          assertApplicationStatus(application, ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST)

          assertApplicationNotWithdrawn(application)
          assertAssessmentNotWithdrawn(assessment)

          assertPlacementRequestWithdrawn(placementRequest, PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST)
          assertSpaceBookingWithdrawn(bookingPendingArrival, "Related placement request withdrawn")

          val applicantEmail = applicant.email!!
          val cruEmail = application.cruManagementArea!!.emailAddress!!
          val apEmail = bookingPendingArrival.premises.emailAddress!!

          emailAsserter.assertEmailsRequestedCount(4)
          assertPlacementRequestWithdrawnEmail(applicantEmail, placementRequest)
          assertSpaceBookingWithdrawnEmail(applicantEmail, bookingPendingArrival)
          assertSpaceBookingWithdrawnEmail(apEmail, bookingPendingArrival)
          assertSpaceBookingWithdrawnEmail(cruEmail, bookingPendingArrival)
        }
      }
    }

    /**
     * For this test the applicant is also the case manager
     *
     * ```
     * | Entities                           | Withdrawn | Email PP | Email AP | Email CRU | Email Assessor |
     * | ---------------------------------- | --------- | -------- | -------- | --------- | -------------- |
     * | Application                        | -         | -        | -        | -         | -              |
     * | -> Assessment                      | -         | -        | -        | -         | -              |
     * | -> Placement request               | YES       | YES      | -        | -         | -              |
     * | ---> Space Booking arrival pending | YES       | YES      | YES      | YES       | -              |
     * ```
     */
    @Test
    fun `Withdrawing a match request for original app dates cascades to applicable space booking entity and updates the application status`() {
      givenAUser { applicant, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val (application, assessment) = createApplicationAndAssessment(
            applicant = applicant,
            assignee = applicant,
            offenderDetails = offenderDetails,
            assessmentAllocatedTo = applicant,
          )

          val placementRequest = createPlacementRequest(application)
          val spaceBookingNoArrival = createSpaceBooking(
            application = application,
            startDate = nowPlusDays(1),
            endDate = nowPlusDays(6),
            arrivalDate = null,
            placementRequest = placementRequest,
          )

          Assertions.assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.STARTED)

          withdrawPlacementRequest(
            placementRequest,
            WithdrawPlacementRequestReason.duplicatePlacementRequest,
            jwt,
          )

          assertApplicationStatus(application, ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST)

          assertApplicationNotWithdrawn(application)
          assertAssessmentNotWithdrawn(assessment)

          assertPlacementRequestWithdrawn(placementRequest, PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST)
          assertSpaceBookingWithdrawn(spaceBookingNoArrival, "Related placement request withdrawn")

          val applicantEmail = applicant.email!!
          val cruEmail = application.cruManagementArea!!.emailAddress!!
          val apEmail = spaceBookingNoArrival.premises.emailAddress!!

          emailAsserter.assertEmailsRequestedCount(4)
          assertPlacementRequestWithdrawnEmail(applicantEmail, placementRequest)
          assertSpaceBookingWithdrawnEmail(applicantEmail, spaceBookingNoArrival)
          assertSpaceBookingWithdrawnEmail(apEmail, spaceBookingNoArrival)
          assertSpaceBookingWithdrawnEmail(cruEmail, spaceBookingNoArrival)
        }
      }
    }

    private fun assertAssessmentWithdrawnEmail(emailAddress: String) = emailAsserter.assertEmailRequested(
      emailAddress,
      Cas1NotifyTemplates.ASSESSMENT_WITHDRAWN_V2,
    )

    private fun assertApplicationWithdrawnEmail(emailAddress: String, application: ApplicationEntity) = emailAsserter.assertEmailRequested(
      emailAddress,
      Cas1NotifyTemplates.APPLICATION_WITHDRAWN_V2,
      mapOf("crn" to application.crn),
    )

    private fun assertSpaceBookingWithdrawnEmail(emailAddress: String, booking: Cas1SpaceBookingEntity) = emailAsserter.assertEmailRequested(
      emailAddress,
      Cas1NotifyTemplates.BOOKING_WITHDRAWN_V2,
      mapOf(
        "startDate" to booking.canonicalArrivalDate.toString(),
        "endDate" to booking.canonicalDepartureDate.toString(),
      ),
    )

    private fun assertPlacementRequestWithdrawnEmail(emailAddress: String, placementApplication: PlacementApplicationEntity) = emailAsserter.assertEmailRequested(
      emailAddress,
      Cas1NotifyTemplates.PLACEMENT_REQUEST_WITHDRAWN_V2,
      mapOf("startDate" to placementApplication.placementDates()!!.expectedArrival.toString()),
    )

    private fun assertPlacementRequestWithdrawnEmail(emailAddress: String, placementRequest: PlacementRequestEntity) = emailAsserter.assertEmailRequested(
      emailAddress,
      Cas1NotifyTemplates.PLACEMENT_REQUEST_WITHDRAWN_V2,
      mapOf("startDate" to placementRequest.expectedArrival.toString()),
    )

    private fun assertMatchRequestWithdrawnEmail(emailAddress: String, placementRequest: PlacementRequestEntity) = emailAsserter.assertEmailRequested(
      emailAddress,
      Cas1NotifyTemplates.MATCH_REQUEST_WITHDRAWN_V2,
      mapOf("startDate" to placementRequest.expectedArrival.toString()),
    )
  }

  private fun withdrawApplication(application: ApprovedPremisesApplicationEntity, jwt: String) {
    webTestClient.post()
      .uri("/cas1/applications/${application.id}/withdrawal")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewWithdrawal(
          reason = WithdrawalReason.duplicateApplication,
        ),
      )
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun withdrawPlacementApplication(
    placementApplication: PlacementApplicationEntity,
    reason: WithdrawPlacementRequestReason,
    jwt: String,
  ) {
    webTestClient.post()
      .uri("/cas1/placement-applications/${placementApplication.id}/withdraw")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(WithdrawPlacementApplication(reason))
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun withdrawPlacementRequest(
    placementRequest: PlacementRequestEntity,
    reason: WithdrawPlacementRequestReason,
    jwt: String,
  ) {
    webTestClient.post()
      .uri("/cas1/placement-requests/${placementRequest.id}/withdrawal")
      .bodyValue(WithdrawPlacementRequest(reason))
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun assertAssessmentNotWithdrawn(assessment: AssessmentEntity) {
    val updatedAssessment = approvedPremisesAssessmentRepository.findByIdOrNull(assessment.id)!!
    Assertions.assertThat(updatedAssessment.isWithdrawn).isFalse
  }

  private fun assertAssessmentWithdrawn(assessment: AssessmentEntity) {
    val updatedAssessment = approvedPremisesAssessmentRepository.findByIdOrNull(assessment.id)!!
    Assertions.assertThat(updatedAssessment.isWithdrawn).isTrue
  }

  private fun assertPlacementApplicationWithdrawn(
    placementApplication: PlacementApplicationEntity,
    reason: PlacementApplicationWithdrawalReason,
  ) {
    val updatedPlacementApplication = placementApplicationRepository.findByIdOrNull(placementApplication.id)!!
    Assertions.assertThat(updatedPlacementApplication.withdrawalReason).isEqualTo(reason)
    Assertions.assertThat(updatedPlacementApplication.isWithdrawn).isTrue()
  }

  private fun assertPlacementApplicationNotWithdrawn(placementApplication: PlacementApplicationEntity) {
    val updatedPlacementApplication = placementApplicationRepository.findByIdOrNull(placementApplication.id)!!
    Assertions.assertThat(updatedPlacementApplication.isWithdrawn).isFalse()
    Assertions.assertThat(updatedPlacementApplication.withdrawalReason).isNull()
  }

  private fun assertApplicationStatus(
    application: ApprovedPremisesApplicationEntity,
    expectedStatus: ApprovedPremisesApplicationStatus,
  ) {
    val updatedApplication = approvedPremisesApplicationRepository.findByIdOrNull(application.id)!!
    Assertions.assertThat(updatedApplication.status).isEqualTo(expectedStatus)
  }

  private fun assertApplicationNotWithdrawn(application: ApprovedPremisesApplicationEntity) {
    val updatedApplication = approvedPremisesApplicationRepository.findByIdOrNull(application.id)!!
    Assertions.assertThat(updatedApplication.isWithdrawn).isFalse
    Assertions.assertThat(updatedApplication.status).isNotEqualTo(ApprovedPremisesApplicationStatus.WITHDRAWN)
  }

  private fun assertApplicationWithdrawn(application: ApprovedPremisesApplicationEntity) {
    val updatedApplication = approvedPremisesApplicationRepository.findByIdOrNull(application.id)!!
    Assertions.assertThat(updatedApplication.isWithdrawn).isTrue
    Assertions.assertThat(updatedApplication.status).isEqualTo(ApprovedPremisesApplicationStatus.WITHDRAWN)
  }

  private fun assertPlacementRequestWithdrawn(
    placementRequest: PlacementRequestEntity,
    reason: PlacementRequestWithdrawalReason,
  ) {
    val updatedPlacementRequest = placementRequestRepository.findByIdOrNull(placementRequest.id)!!
    Assertions.assertThat(updatedPlacementRequest.isWithdrawn).isEqualTo(true)
    Assertions.assertThat(updatedPlacementRequest.withdrawalReason).isEqualTo(reason)
  }

  private fun assertPlacementRequestNotWithdrawn(placementRequest: PlacementRequestEntity) {
    val updatedPlacementRequest = placementRequestRepository.findByIdOrNull(placementRequest.id)!!
    Assertions.assertThat(updatedPlacementRequest.isWithdrawn).isEqualTo(false)
  }

  private fun assertSpaceBookingWithdrawn(spaceBooking: Cas1SpaceBookingEntity, cancellationReason: String) {
    val updatedBooking = cas1SpaceBookingRepository.findByIdOrNull(spaceBooking.id)!!
    Assertions.assertThat(updatedBooking.cancellationOccurredAt).isNotNull()
    Assertions.assertThat(updatedBooking.cancellationReason!!.name).isEqualTo(cancellationReason)
  }

  private fun assertSpaceBookingNotWithdrawn(spaceBooking: Cas1SpaceBookingEntity) {
    val updatedBooking = cas1SpaceBookingRepository.findByIdOrNull(spaceBooking.id)!!
    Assertions.assertThat(updatedBooking.cancellationOccurredAt).isNull()
    Assertions.assertThat(updatedBooking.cancellationReason).isNull()
  }

  @SuppressWarnings("LongParameterList")
  private fun createApplicationAndAssessment(
    applicant: UserEntity,
    assignee: UserEntity,
    offenderDetails: OffenderDetailSummary,
    assessmentSubmitted: Boolean = true,
    assessmentAllocatedTo: UserEntity? = null,
    caseManager: Cas1ApplicationUserDetailsEntity? = null,
  ): Pair<ApprovedPremisesApplicationEntity, AssessmentEntity> {
    val apArea = givenAnApArea()

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(applicant)
      withSubmittedAt(OffsetDateTime.now())
      withApArea(apArea)
      withCruManagementArea(givenACas1CruManagementArea())
      withReleaseType("licence")
      withCaseManagerUserDetails(caseManager)
      withCaseManagerIsNotApplicant(caseManager != null)
    }

    val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
      withAllocatedToUser(assignee)
      withApplication(application)
      withSubmittedAt(if (assessmentSubmitted) OffsetDateTime.now() else null)
      withAllocatedToUser(assessmentAllocatedTo)
    }

    application.assessments.add(assessment)

    return Pair(application, assessment)
  }

  private fun produceAndPersistBasicApplication(
    crn: String,
    userEntity: UserEntity,
    managingTeamCode: String,
  ): ApplicationEntity {
    val application =
      approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCrn(crn)
        withCreatedByUser(userEntity)
        withData(
          """
          {
             "thingId": 123
          }
          """,
        )
      }

    application.teamCodes += applicationTeamCodeRepository.save(
      ApplicationTeamCodeEntity(
        id = UUID.randomUUID(),
        application = application,
        teamCode = managingTeamCode,
      ),
    )

    return application
  }

  private data class DateSpan(val start: LocalDate, val duration: Int)

  private fun createPlacementApplication(
    application: ApprovedPremisesApplicationEntity,
    dateSpan: DateSpan? = null,
    isSubmitted: Boolean = true,
    reallocatedAt: OffsetDateTime? = null,
    decision: PlacementApplicationDecision? = PlacementApplicationDecision.ACCEPTED,
    allocatedTo: UserEntity? = null,
    isWithdrawn: Boolean = false,
    createdBy: UserEntity? = null,
  ) = placementApplicationFactory.produceAndPersist {
    withCreatedByUser(createdBy ?: application.createdByUser)
    withApplication(application)
    withSubmittedAt(if (isSubmitted) OffsetDateTime.now() else null)
    withDecision(decision)
    withPlacementType(PlacementType.ADDITIONAL_PLACEMENT)
    withReallocatedAt(reallocatedAt)
    withAllocatedToUser(allocatedTo)
    withIsWithdrawn(isWithdrawn)
    withExpectedArrival(if (isSubmitted) dateSpan?.start else null)
    withRequestedDuration(if (isSubmitted) dateSpan?.duration else null)
  }

  private fun createPlacementRequest(
    application: ApprovedPremisesApplicationEntity,
    isWithdrawn: Boolean = false,
    placementApplication: PlacementApplicationEntity? = null,
  ) = placementRequestFactory.produceAndPersist {
    val assessment = application.assessments[0] as ApprovedPremisesAssessmentEntity

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

    withApplication(application)
    withAssessment(assessment)
    withPlacementRequirements(placementRequirements)
    withIsWithdrawn(isWithdrawn)
    withPlacementApplication(placementApplication)
  }

  private fun createSpaceBooking(
    application: ApprovedPremisesApplicationEntity,
    startDate: LocalDate,
    endDate: LocalDate,
    arrivalDate: LocalDateTime? = null,
    placementRequest: PlacementRequestEntity,
  ): Cas1SpaceBookingEntity {
    val premises = givenAnApprovedPremises()

    val spaceBooking = cas1SpaceBookingEntityFactory.produceAndPersist {
      withApplication(application)
      withPlacementRequest(placementRequest)
      withCreatedBy(application.createdByUser)
      withPremises(premises)
      withCrn(application.crn)
      withExpectedArrivalDate(startDate)
      withExpectedDepartureDate(endDate)
      withActualArrivalDate(arrivalDate?.toLocalDate())
      withActualArrivalTime(arrivalDate?.toLocalTime())
    }

    return spaceBooking
  }

  private fun now() = LocalDate.now()

  private fun nowPlusDays(days: Long) = LocalDate.now().plusDays(days)

  private fun toDatePeriod(start: LocalDate, duration: Int) = DatePeriod(start, start.plusDays(duration.toLong()))

  fun toWithdrawable(application: ApplicationEntity) = Withdrawable(
    application.id,
    WithdrawableType.application,
    emptyList(),
  )

  fun toWithdrawable(placementRequest: PlacementRequestEntity) = Withdrawable(
    placementRequest.id,
    WithdrawableType.placementRequest,
    listOf(toDatePeriod(placementRequest.expectedArrival, placementRequest.duration)),
  )

  fun toWithdrawable(placementApplication: PlacementApplicationEntity) = Withdrawable(
    placementApplication.id,
    WithdrawableType.placementApplication,
    listOfNotNull(
      placementApplication.placementDates()?.let { toDatePeriod(it.expectedArrival, it.duration) },
    ),
  )

  fun toWithdrawable(spaceBooking: Cas1SpaceBookingEntity) = Withdrawable(
    spaceBooking.id,
    WithdrawableType.spaceBooking,
    listOf(DatePeriod(spaceBooking.canonicalArrivalDate, spaceBooking.canonicalDepartureDate)),
  )
}
