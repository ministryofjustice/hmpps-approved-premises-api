package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.jsonForObject
import java.time.LocalDate
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
class WithdrawalTest : IntegrationTestBase() {

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
    fun `Get withdrawables returns application only for a sparse application if user is not the application creator`() {
      `Given a User` { applicationCreator, _ ->
        `Given a User` { _, jwt ->
          `Given an Offender` { offenderDetails, _ ->
            val application = produceAndPersistBasicApplication(offenderDetails.otherIds.crn, applicationCreator, "TEAM")

            webTestClient.get()
              .uri("/applications/${application.id}/withdrawables")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json("[]")
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
    fun `Get withdrawables returns application only for a sparse application if user is application creator`() {
      `Given a User` { applicationCreator, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val application = produceAndPersistBasicApplication(offenderDetails.otherIds.crn, applicationCreator, "TEAM")

          val expected = listOf(
            Withdrawable(
              application.id,
              WithdrawableType.application,
              emptyList(),
            ),
          )

          webTestClient.get()
            .uri("/applications/${application.id}/withdrawables")
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
     * | Entities                         | Withdrawable |
     * | -------------------------------- | ------------ |
     * | Application                      | -            |
     * | -> Match Request 1               | Yes          |
     * | ---> Booking without arrival     | -            |
     * | -> Match Request (reallocated)   | -            |
     * | -> Match Request (withdrawn)     | -            |
     * | -> Request for placement         | Yes          |
     * | ---> Match Request 2             | -            |
     * ```
     */
    @Test
    fun `Get withdrawables returns match request for the original app dates only`() {
      `Given a User` { applicant, jwt ->
        `Given a User` { allocatedTo, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val (application, _) = createApplicationAndAssessment(applicant, allocatedTo, offenderDetails)

            val placementRequest = createPlacementRequest(application)
            val bookingNoArrival = createBooking(
              application = application,
              hasArrival = false,
              startDate = LocalDate.now().plusDays(1),
              endDate = LocalDate.now().plusDays(6),
            )
            addBookingToPlacementRequest(placementRequest, bookingNoArrival)

            createPlacementRequest(application) {
              withReallocatedAt(OffsetDateTime.now())
            }

            createPlacementRequest(application) {
              withIsWithdrawn(true)
            }

            val placementApplicationExpectedArrival = LocalDate.now().plusDays(50)
            val placementApplicationDuration = 6
            val placementApplication = createPlacementApplication(
              application,
              listOf(placementApplicationExpectedArrival to placementApplicationDuration),
            )
            createPlacementRequest(application) {
              withPlacementApplication(placementApplication)
            }

            val expected = listOf(
              Withdrawable(
                application.id,
                WithdrawableType.application,
                emptyList(),
              ),
              Withdrawable(
                placementRequest.id,
                WithdrawableType.placementRequest,
                listOf(datePeriodForDuration(placementRequest.expectedArrival, placementRequest.duration)),
              ),
              Withdrawable(
                placementApplication.id,
                WithdrawableType.placementApplication,
                listOf(datePeriodForDuration(placementApplicationExpectedArrival, placementApplicationDuration)),
              ),
            )

            webTestClient.get()
              .uri("/applications/${application.id}/withdrawables")
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
     * | -> Request for placement 1                   | Yes          |
     * | -> Request for placement 2                   | Yes          |
     * | -> Request for placement 3 (Unsubmitted)     | -            |
     * | -> Request for placement 4 (Reallocated)     | -            |
     * | -> Request for placement 5 (With Decision)   | Yes          |
     * | -> Request for placement 6 (Withdrawn)       | -            |
     * | -> Request for placement 7 (Withdrawn by PP) | -            |
     * | -> Request for placement 7 (Rejected)        | Yes            |
     * ```
     */
    @Test
    fun `Get withdrawables returns requests for placements applications`() {
      `Given a User` { applicant, jwt ->
        `Given a User` { allocatedTo, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val (application, _) = createApplicationAndAssessment(applicant, allocatedTo, offenderDetails)

            val submittedApplication1ExpectedArrival1 = LocalDate.now().plusDays(1)
            val submittedApplication1Duration1 = 5
            val submittedApplication1ExpectedArrival2 = LocalDate.now().plusDays(10)
            val submittedApplication1Duration2 = 10

            val submittedPlacementApplication1 = createPlacementApplication(
              application,
              listOf(
                submittedApplication1ExpectedArrival1 to submittedApplication1Duration1,
                submittedApplication1ExpectedArrival2 to submittedApplication1Duration2,
              ),
            )

            val submittedApplication2ExpectedArrival = LocalDate.now().plusDays(50)
            val submittedApplication2Duration = 6
            val submittedPlacementApplication2 = createPlacementApplication(
              application,
              listOf(submittedApplication2ExpectedArrival to submittedApplication2Duration),
            )

            createPlacementApplication(
              application,
              isSubmitted = false,
            )

            createPlacementApplication(application, listOf(LocalDate.now() to 2)) {
              withReallocatedAt(OffsetDateTime.now())
            }

            val applicationWithAcceptedDecisionExpectedArrival = LocalDate.now().plusDays(50)
            val applicationWithAcceptedDecisionDuration = 6
            val applicationWithAcceptedDecision = createPlacementApplication(
              application,
              listOf(applicationWithAcceptedDecisionExpectedArrival to applicationWithAcceptedDecisionDuration),
            ) {
              withDecision(PlacementApplicationDecision.ACCEPTED)
            }

            createPlacementApplication(application, listOf(LocalDate.now() to 2)) {
              withDecision(PlacementApplicationDecision.WITHDRAW)
            }

            createPlacementApplication(application, listOf(LocalDate.now() to 2)) {
              withDecision(PlacementApplicationDecision.WITHDRAWN_BY_PP)
            }

            val applicationWithRejectedDecisionExpectedArrival = LocalDate.now().plusDays(50)
            val applicationWithRejectedDecisionDuration = 6
            val applicationWithRejectedDecision = createPlacementApplication(
              application,
              listOf(applicationWithRejectedDecisionExpectedArrival to applicationWithRejectedDecisionDuration),
            ) {
              withDecision(PlacementApplicationDecision.REJECTED)
            }

            val expected = listOf(
              Withdrawable(
                application.id,
                WithdrawableType.application,
                emptyList(),
              ),
              Withdrawable(
                submittedPlacementApplication1.id,
                WithdrawableType.placementApplication,
                listOf(
                  datePeriodForDuration(submittedApplication1ExpectedArrival1, submittedApplication1Duration1),
                  datePeriodForDuration(submittedApplication1ExpectedArrival2, submittedApplication1Duration2),
                ),
              ),
              Withdrawable(
                submittedPlacementApplication2.id,
                WithdrawableType.placementApplication,
                listOf(datePeriodForDuration(submittedApplication2ExpectedArrival, submittedApplication2Duration)),
              ),
              Withdrawable(
                applicationWithAcceptedDecision.id,
                WithdrawableType.placementApplication,
                listOf(datePeriodForDuration(applicationWithAcceptedDecisionExpectedArrival, applicationWithAcceptedDecisionDuration)),
              ),
              Withdrawable(
                applicationWithRejectedDecision.id,
                WithdrawableType.placementApplication,
                listOf(datePeriodForDuration(applicationWithRejectedDecisionExpectedArrival, applicationWithRejectedDecisionDuration)),
              ),
            )

            webTestClient.get()
              .uri("/applications/${application.id}/withdrawables")
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
     * | Application                      | BLOCKED      |
     * | -> Request for placement 1       | YES          |
     * | ---> Match request 1             | -            |
     * | -----> Booking 1 arrival pending | YES          |
     * | ---> Match request 2             | -            |
     * | -> Request for placement 2       | YES          |
     * | -> Match request 3               | BLOCKED      |
     * | ---> Booking 2 has arrival       | BLOCKING     |
     * | -> Adhoc Booking                 | YES          |
     * ```
     */
    @Test
    fun `Get withdrawables returns all possible types when a user can manage bookings, with blocked bookings`() {
      `Given a User` { applicant, _ ->
        `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
          `Given a User` { requestForPlacementAssessor, _ ->
            `Given an Offender` { offenderDetails, _ ->
              val (application, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)
              val (otherApplication, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)

              val placementApplication1 = createPlacementApplication(application, listOf(LocalDate.now() to 2))
              val placementRequest1 = createPlacementRequest(application) {
                withPlacementApplication(placementApplication1)
              }
              val booking1NoArrival = createBooking(
                application = application,
                hasArrival = false,
                adhoc = false,
                startDate = LocalDate.now().plusDays(1),
                endDate = LocalDate.now().plusDays(6),
              )
              addBookingToPlacementRequest(placementRequest1, booking1NoArrival)

              createPlacementRequest(application) {
                withPlacementApplication(placementApplication1)
              }

              val placementApplication2 = createPlacementApplication(application, listOf(LocalDate.now() to 2)) {
                withDecision(null)
                withAllocatedToUser(requestForPlacementAssessor)
              }

              val placementRequest3 = createPlacementRequest(application)
              val booking2HasArrival = createBooking(
                application = application,
                hasArrival = true,
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusDays(1),
              )
              addBookingToPlacementRequest(placementRequest3, booking2HasArrival)

              val adhocBooking = createBooking(
                application = application,
                adhoc = true,
                hasArrival = false,
                startDate = LocalDate.now().plusDays(20),
                endDate = LocalDate.now().plusDays(26),
              )

              createBooking(
                application = otherApplication,
                adhoc = true,
                hasArrival = false,
                startDate = LocalDate.now().plusDays(20),
                endDate = LocalDate.now().plusDays(26),
              )
              createBooking(
                application = otherApplication,
                adhoc = null,
                hasArrival = false,
                startDate = LocalDate.now().plusDays(20),
                endDate = LocalDate.now().plusDays(26),
              )

              val expected = listOf(
                Withdrawable(
                  placementApplication1.id,
                  WithdrawableType.placementApplication,
                  listOf(datePeriodForDuration(placementApplication1.placementDates[0].expectedArrival, placementApplication1.placementDates[0].duration)),
                ),
                Withdrawable(
                  booking1NoArrival.id,
                  WithdrawableType.booking,
                  listOf(DatePeriod(booking1NoArrival.arrivalDate, booking1NoArrival.departureDate)),
                ),
                Withdrawable(
                  placementApplication2.id,
                  WithdrawableType.placementApplication,
                  listOf(datePeriodForDuration(placementApplication2.placementDates[0].expectedArrival, placementApplication2.placementDates[0].duration)),
                ),
                Withdrawable(
                  adhocBooking.id,
                  WithdrawableType.booking,
                  listOf(DatePeriod(adhocBooking.arrivalDate, adhocBooking.departureDate)),
                ),
              )

              webTestClient.get()
                .uri("/applications/${application.id}/withdrawables")
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
     * | Entities                         | Withdrawable |
     * | -------------------------------- | ------------ |
     * | Application                      | BLOCKED      |
     * | -> Request for placement 1       | YES          |
     * | ---> Match request 1             | -            |
     * | -----> Booking 1 arrival pending | -            |
     * | ---> Match request 2             | -            |
     * | -> Request for placement 2       | YES          |
     * | -> Match request 3               | BLOCKED      |
     * | ---> Booking 2 has arrival       | -            |
     * | -> Adhoc Booking                 | -            |
     * ```
     */
    @Test
    fun `Get withdrawables returns all possible types when a user cannot manage bookings, with blocked bookings`() {
      `Given a User` { applicant, jwt ->
        `Given a User` { requestForPlacementAssessor, _ ->
          `Given an Offender` { offenderDetails, _ ->
            val (application, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)

            val placementApplication1 = createPlacementApplication(application, listOf(LocalDate.now() to 2))
            val placementRequest1 = createPlacementRequest(application) {
              withPlacementApplication(placementApplication1)
            }
            val booking1NoArrival = createBooking(
              application = application,
              hasArrival = false,
              startDate = LocalDate.now().plusDays(1),
              endDate = LocalDate.now().plusDays(6),
            )
            addBookingToPlacementRequest(placementRequest1, booking1NoArrival)

            createPlacementRequest(application) {
              withPlacementApplication(placementApplication1)
            }

            val placementApplication2 = createPlacementApplication(application, listOf(LocalDate.now() to 2)) {
              withDecision(null)
              withAllocatedToUser(requestForPlacementAssessor)
            }

            val placementRequest3 = createPlacementRequest(application)
            val booking2HasArrival = createBooking(
              application = application,
              hasArrival = true,
              startDate = LocalDate.now(),
              endDate = LocalDate.now().plusDays(1),
            )
            addBookingToPlacementRequest(placementRequest3, booking2HasArrival)

            createBooking(
              application = application,
              hasArrival = false,
              startDate = LocalDate.now().plusDays(20),
              endDate = LocalDate.now().plusDays(26),
            )

            val expected = listOf(
              Withdrawable(
                placementApplication1.id,
                WithdrawableType.placementApplication,
                listOf(datePeriodForDuration(placementApplication1.placementDates[0].expectedArrival, placementApplication1.placementDates[0].duration)),
              ),
              Withdrawable(
                placementApplication2.id,
                WithdrawableType.placementApplication,
                listOf(datePeriodForDuration(placementApplication2.placementDates[0].expectedArrival, placementApplication2.placementDates[0].duration)),
              ),
            )

            webTestClient.get()
              .uri("/applications/${application.id}/withdrawables")
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

  @Nested
  inner class WithdrawalCascading {

    /**
     * ```
     * | Entities                         | Withdrawn | Email PP | Email Assessor |
     * | -------------------------------- | --------- | -------- | -------------- |
     * | Application                      | YES       | YES      | -              |
     * | -> Assessment (pending)          | YES       | -        | YES            |
     * ```
     */
    @Test
    fun `Withdrawing an application cascades to an assessment and sends mail to assessor if pending`() {
      `Given a User` { applicant, jwt ->
        `Given a User` { assessor, _ ->
          `Given an Offender` { offenderDetails, _ ->
            val (application, assessment) = createApplicationAndAssessment(
              applicant = applicant,
              assignee = applicant,
              offenderDetails = offenderDetails,
              assessmentSubmitted = false,
              assessmentAllocatedTo = assessor,
            )

            withdrawApplication(application, jwt)

            assertApplicationWithdrawn(application)
            assertAssessmentWithdrawn(assessment)

            val applicantEmail = applicant.email!!
            val assessorEmail = assessment.allocatedToUser!!.email!!

            emailAsserter.assertEmailsRequestedCount(2)
            assertApplicationWithdrawnEmail(applicantEmail, application)
            assertAssessmentWithdrawnEmail(assessorEmail)
          }
        }
      }
    }

    /**
     * ```
     * | Entities                           | Withdrawn | Email PP | Email AP | Email CRU | Email Assessor |
     * | ---------------------------------- | --------- | -------- | -------- | --------- | -------------- |
     * | Application                        | YES       | YES      | -        | -         | -              |
     * | -> Assessment                      | YES       | -        | -        | -         | -              |
     * | -> Request for placement 1         | YES       | YES      | -        | -         | -              |
     * | ---> Match request 1               | YES       | -        | -        | -         | -              |
     * | -----> Booking 1 arrival pending   | YES       | YES      | YES      | YES       | -              |
     * | ---> Match request 2               | YES       | -        | -        | YES       | -              |
     * | -> Request for placement 2         | YES       | YES      | -        | -         | YES            |
     * | -> Match request 2                 | YES       | YES      | -        | -         | -              |
     * | ---> Booking 2 arrival pending     | YES       | YES      | YES      | YES       | -              |
     * | -> Adhoc Booking 1 arrival pending | YES       | YES      | YES      | YES       | -              |
     * | -> Adhoc Booking 2 arrival pending | YES       | YES      | YES      | YES       | -              |
     * ```
     */
    @Test
    fun `Withdrawing an application cascades to applicable entities`() {
      `Given a User` { applicant, jwt ->
        `Given a User` { requestForPlacementAssessor, _ ->
          `Given an Offender` { offenderDetails, _ ->
            val (application, assessment) = createApplicationAndAssessment(applicant, applicant, offenderDetails)
            val (otherApplication, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)

            val placementApplication1 = createPlacementApplication(application, listOf(LocalDate.now() to 2))
            val placementRequest1 = createPlacementRequest(application) {
              withPlacementApplication(placementApplication1)
            }
            val booking1NoArrival = createBooking(
              application = application,
              hasArrival = false,
              startDate = LocalDate.now().plusDays(1),
              endDate = LocalDate.now().plusDays(6),
            )
            addBookingToPlacementRequest(placementRequest1, booking1NoArrival)

            val placementApplication2NoBookingBeingAssessed = createPlacementApplication(application, listOf(LocalDate.now().plusDays(2) to 2)) {
              withAllocatedToUser(requestForPlacementAssessor)
              withDecision(null)
            }

            val placementRequest2 = createPlacementRequest(application)
            val booking2NoArrival = createBooking(
              application = application,
              hasArrival = false,
              startDate = LocalDate.now().plusDays(5),
              endDate = LocalDate.now().plusDays(15),
            )
            addBookingToPlacementRequest(placementRequest2, booking2NoArrival)

            val adhocBooking1NoArrival = createBooking(
              application = application,
              hasArrival = false,
              startDate = LocalDate.now().plusDays(1),
              endDate = LocalDate.now().plusDays(6),
              adhoc = true,
            )

            // we don't know the adhoc status for some legacy
            // applications. in this case adhoc is 'null'
            // for these cases we treat them as adhoc bookings
            val adhocBooking2NoArrival = createBooking(
              application = application,
              hasArrival = false,
              startDate = LocalDate.now().plusDays(1),
              endDate = LocalDate.now().plusDays(6),
              adhoc = null,
            )

            // regression test to ensure other application's
            // bookings aren't affected
            createBooking(
              application = otherApplication,
              adhoc = true,
              hasArrival = false,
              startDate = LocalDate.now().plusDays(20),
              endDate = LocalDate.now().plusDays(26),
            )
            createBooking(
              application = otherApplication,
              adhoc = null,
              hasArrival = false,
              startDate = LocalDate.now().plusDays(20),
              endDate = LocalDate.now().plusDays(26),
            )

            withdrawApplication(application, jwt)

            assertApplicationWithdrawn(application)
            assertAssessmentWithdrawn(assessment)

            assertPlacementApplicationWithdrawn(
              placementApplication1,
              PlacementApplicationDecision.WITHDRAW,
              PlacementApplicationWithdrawalReason.RELATED_APPLICATION_WITHDRAWN,
            )
            assertPlacementRequestWithdrawn(
              placementRequest1,
              PlacementRequestWithdrawalReason.RELATED_APPLICATION_WITHDRAWN,
            )
            assertBookingWithdrawn(booking1NoArrival, "Related application withdrawn")

            assertPlacementApplicationWithdrawn(
              placementApplication2NoBookingBeingAssessed,
              PlacementApplicationDecision.WITHDRAW,
              PlacementApplicationWithdrawalReason.RELATED_APPLICATION_WITHDRAWN,
            )

            assertPlacementRequestWithdrawn(
              placementRequest2,
              PlacementRequestWithdrawalReason.RELATED_APPLICATION_WITHDRAWN,
            )
            assertBookingWithdrawn(booking2NoArrival, "Related application withdrawn")

            assertBookingWithdrawn(adhocBooking1NoArrival, "Related application withdrawn")
            assertBookingWithdrawn(adhocBooking2NoArrival, "Related application withdrawn")

            val applicantEmail = applicant.email!!
            val cruEmail = application.apArea!!.emailAddress!!
            val requestForPlacementAssessorEmail = requestForPlacementAssessor.email!!

            emailAsserter.assertEmailsRequestedCount(17)
            assertApplicationWithdrawnEmail(applicantEmail, application)

            assertPlacementRequestWithdrawnEmail(applicantEmail, placementApplication1)

            assertBookingWithdrawnEmail(applicantEmail, booking1NoArrival)
            assertBookingWithdrawnEmail(booking1NoArrival.premises.emailAddress!!, booking1NoArrival)
            assertBookingWithdrawnEmail(cruEmail, booking1NoArrival)

            assertPlacementRequestWithdrawnEmail(applicantEmail, placementApplication2NoBookingBeingAssessed)
            assertPlacementRequestWithdrawnEmail(requestForPlacementAssessorEmail, placementApplication2NoBookingBeingAssessed)

            assertPlacementRequestWithdrawnEmail(applicantEmail, placementRequest2)

            assertBookingWithdrawnEmail(applicantEmail, booking2NoArrival)
            assertBookingWithdrawnEmail(booking2NoArrival.premises.emailAddress!!, booking2NoArrival)
            assertBookingWithdrawnEmail(cruEmail, booking2NoArrival)

            assertBookingWithdrawnEmail(applicantEmail, adhocBooking1NoArrival)
            assertBookingWithdrawnEmail(adhocBooking1NoArrival.premises.emailAddress!!, adhocBooking1NoArrival)
            assertBookingWithdrawnEmail(cruEmail, adhocBooking1NoArrival)

            assertBookingWithdrawnEmail(applicantEmail, adhocBooking2NoArrival)
            assertBookingWithdrawnEmail(adhocBooking1NoArrival.premises.emailAddress!!, adhocBooking2NoArrival)
            assertBookingWithdrawnEmail(cruEmail, adhocBooking2NoArrival)
          }
        }
      }
    }

    /**
     * ```
     * | Entities                         | Withdrawn | Email PP | Email AP | Email CRU | Email Assessor |
     * | ---------------------------------| --------- | -------- | -------- | --------- | -------------- |
     * | Application                      | BLOCKED   | -        | -        | -         | -              |
     * | -> Placement Request             | BLOCKED   | -        | -        | -         | -              |
     * | ---> Match request               | BLOCKED   | -        | -        | -         | -              |
     * | -----> Booking has arrival       | BLOCKED   | -        | -        | -         | -              |
     * ```
     */
    @Test
    fun `Withdrawing an application is not allowed if has a booking with arrivals`() {
      `Given a User` { applicant, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val (application, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)

          val placementApplication = createPlacementApplication(application, listOf(LocalDate.now() to 2))
          val placementRequest = createPlacementRequest(application) {
            withPlacementApplication(placementApplication)
          }
          val bookingWithArrival = createBooking(
            application = application,
            hasArrival = true,
            startDate = LocalDate.now().plusDays(1),
            endDate = LocalDate.now().plusDays(6),
          )
          addBookingToPlacementRequest(placementRequest, bookingWithArrival)

          webTestClient.post()
            .uri("/applications/${application.id}/withdrawal")
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
     * ```
     * | Entities                         | Withdrawn | Email PP | Email AP | Email CRU | Email Assessor |
     * | -------------------------------- | --------- | -------- | -------- | --------- | -------------- |
     * | Application                      | -         | -        | -        | -         | -              |
     * | -> Assessment                    | -         | -        | -        | -         | -              |
     * | -> Request for placement 1       | YES       | YES      | -        | -         | -              |
     * | ---> Match request 1             | YES       | -        | -        | -         | -              |
     * | -----> Booking 1 arrival pending | YES       | YES      | YES      | YES       | -              |
     * | ---> Match request 2             | YES       | -        | -        | -         | -              |
     * | -----> Booking 2 arrival pending | YES       | YES      | YES      | YES       | -              |
     * | -> Request for placement 2       | -         | -        | -        | -         | -              |
     * | ---> Match request 3             | -         | -        | -        | -         | -              |
     * | -----> Booking 3 arrival pending | -         | Y        | -        | -         | -              |
     * ```
     */
    @Test
    fun `Withdrawing a request for placement cascades to applicable entities`() {
      `Given a User` { applicant, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val (application, assessment) = createApplicationAndAssessment(applicant, applicant, offenderDetails)

          val placementApplication1 = createPlacementApplication(application, listOf(LocalDate.now() to 2))
          val placementRequest1 = createPlacementRequest(application) {
            withPlacementApplication(placementApplication1)
          }
          val booking1NoArrival = createBooking(
            application = application,
            hasArrival = false,
            startDate = LocalDate.now().plusDays(1),
            endDate = LocalDate.now().plusDays(6),
          )
          addBookingToPlacementRequest(placementRequest1, booking1NoArrival)

          val placementRequest2 = createPlacementRequest(application) {
            withPlacementApplication(placementApplication1)
          }
          val booking2NoArrival = createBooking(
            application = application,
            hasArrival = false,
            startDate = LocalDate.now().plusDays(10),
            endDate = LocalDate.now().plusDays(21),
          )
          addBookingToPlacementRequest(placementRequest2, booking2NoArrival)

          val placementApplication2 = createPlacementApplication(application, listOf(LocalDate.now() to 2))
          val placementRequest3 = createPlacementRequest(application) {
            withPlacementApplication(placementApplication2)
          }
          val booking3NoArrival = createBooking(
            application = application,
            hasArrival = false,
            startDate = LocalDate.now().plusDays(10),
            endDate = LocalDate.now().plusDays(21),
          )
          addBookingToPlacementRequest(placementRequest3, booking3NoArrival)

          withdrawPlacementApplication(
            placementApplication1,
            WithdrawPlacementRequestReason.duplicatePlacementRequest,
            jwt,
          )

          assertApplicationNotWithdrawn(application)
          assertAssessmentNotWithdrawn(assessment)

          assertPlacementApplicationWithdrawn(
            placementApplication1,
            PlacementApplicationDecision.WITHDRAW,
            PlacementApplicationWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST,
          )

          assertPlacementRequestWithdrawn(
            placementRequest1,
            PlacementRequestWithdrawalReason.RELATED_PLACEMENT_APPLICATION_WITHDRAWN,
          )
          assertBookingWithdrawn(booking1NoArrival, "Related request for placement withdrawn")

          assertPlacementRequestWithdrawn(
            placementRequest2,
            PlacementRequestWithdrawalReason.RELATED_PLACEMENT_APPLICATION_WITHDRAWN,
          )
          assertBookingWithdrawn(booking2NoArrival, "Related request for placement withdrawn")

          assertPlacementApplicationNotWithdrawn(placementApplication2)
          assertPlacementRequestNotWithdrawn(placementRequest3)
          assertBookingNotWithdrawn(booking3NoArrival)

          val applicantEmail = applicant.email!!
          val cruEmail = application.apArea!!.emailAddress!!

          emailAsserter.assertEmailsRequestedCount(7)
          assertPlacementRequestWithdrawnEmail(applicantEmail, placementRequest1)

          assertBookingWithdrawnEmail(applicantEmail, booking1NoArrival)
          assertBookingWithdrawnEmail(booking1NoArrival.premises.emailAddress!!, booking1NoArrival)
          assertBookingWithdrawnEmail(cruEmail, booking1NoArrival)

          assertBookingWithdrawnEmail(applicantEmail, booking2NoArrival)
          assertBookingWithdrawnEmail(booking2NoArrival.premises.emailAddress!!, booking2NoArrival)
          assertBookingWithdrawnEmail(cruEmail, booking2NoArrival)
        }
      }
    }

    /**
     * ```
     * | Entities                           | Withdrawn | Email PP | Email AP | Email CRU | Email Assessor |
     * | ---------------------------------- | --------- | -------- | -------- | --------- | -------------- |
     * | Application                        | -         | -        | -        | -         | -              |
     * | -> Assessment                      | -         | -        | -        | -         | -              |
     * | -> Request for placement 1         | YES       | YES      | -        | -         | -              |
     * | ---> Match request 1               | YES       | -        | -        | -         | -              |
     * | -----> Booking 1 adhoc             | -         | -        | -        | -         | -              |
     * | ---> Match request 2               | YES       | -        | -        | -         | -              |
     * | -----> Booking 2 not adhoc         | YES       | YES      | YES      | YES       | -              |
     * | ---> Match request 3               | YES       | -        | -        | -         | -              |
     * | -----> Booking 3 potentially adhoc | -         | -        | -        | -         | -              |
     * ```
     */
    @Test
    fun `Withdrawing a request for placement does not cascade to incorrectly linked adhoc placements`() {
      `Given a User` { applicant, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val (application, assessment) = createApplicationAndAssessment(applicant, applicant, offenderDetails)

          val placementApplication1 = createPlacementApplication(application, listOf(LocalDate.now() to 2))
          val placementRequest1 = createPlacementRequest(application) {
            withPlacementApplication(placementApplication1)
          }
          val booking1Adhoc = createBooking(
            application = application,
            adhoc = true,
            hasArrival = false,
            startDate = LocalDate.now().plusDays(1),
            endDate = LocalDate.now().plusDays(6),
          )
          addBookingToPlacementRequest(placementRequest1, booking1Adhoc)

          val placementRequest2 = createPlacementRequest(application) {
            withPlacementApplication(placementApplication1)
          }
          val booking2NoArrival = createBooking(
            application = application,
            adhoc = false,
            hasArrival = false,
            startDate = LocalDate.now().plusDays(10),
            endDate = LocalDate.now().plusDays(21),
          )
          addBookingToPlacementRequest(placementRequest2, booking2NoArrival)

          val placementRequest3 = createPlacementRequest(application) {
            withPlacementApplication(placementApplication1)
          }
          val booking3PotentiallyAdhoc = createBooking(
            application = application,
            adhoc = null,
            hasArrival = false,
            startDate = LocalDate.now().plusDays(10),
            endDate = LocalDate.now().plusDays(21),
          )
          addBookingToPlacementRequest(placementRequest3, booking3PotentiallyAdhoc)

          withdrawPlacementApplication(
            placementApplication1,
            WithdrawPlacementRequestReason.duplicatePlacementRequest,
            jwt,
          )

          assertApplicationNotWithdrawn(application)
          assertAssessmentNotWithdrawn(assessment)

          assertPlacementApplicationWithdrawn(
            placementApplication1,
            PlacementApplicationDecision.WITHDRAW,
            PlacementApplicationWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST,
          )

          assertPlacementRequestWithdrawn(
            placementRequest1,
            PlacementRequestWithdrawalReason.RELATED_PLACEMENT_APPLICATION_WITHDRAWN,
          )
          assertBookingNotWithdrawn(booking1Adhoc)

          assertPlacementRequestWithdrawn(
            placementRequest2,
            PlacementRequestWithdrawalReason.RELATED_PLACEMENT_APPLICATION_WITHDRAWN,
          )
          assertBookingWithdrawn(booking2NoArrival, "Related request for placement withdrawn")

          assertPlacementRequestWithdrawn(
            placementRequest3,
            PlacementRequestWithdrawalReason.RELATED_PLACEMENT_APPLICATION_WITHDRAWN,
          )
          assertBookingNotWithdrawn(booking3PotentiallyAdhoc)

          val applicantEmail = applicant.email!!
          val cruEmail = application.apArea!!.emailAddress!!

          emailAsserter.assertEmailsRequestedCount(4)
          assertPlacementRequestWithdrawnEmail(applicantEmail, placementRequest1)

          assertBookingWithdrawnEmail(applicantEmail, booking2NoArrival)
          assertBookingWithdrawnEmail(booking2NoArrival.premises.emailAddress!!, booking2NoArrival)
          assertBookingWithdrawnEmail(cruEmail, booking2NoArrival)
        }
      }
    }

    /**
     * ```
     * | Entities                         | Withdrawn | Email PP | Email AP | Email CRU | Email Assessor |
     * | -------------------------------- | --------- | -------- | -------- | --------- | -------------- |
     * | Application                      | -         | -        | -        | -         | -              |
     * | -> Assessment                    | -         | -        | -        | -         | -              |
     * | -> Match request                 | YES       | YES      | -        | YES       | -              |
     * ```
     */
    @Test
    fun `Withdrawing a match request for original app dates without booking emails CRU`() {
      `Given a User` { applicant, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val (application, assessment) = createApplicationAndAssessment(
            applicant = applicant,
            assignee = applicant,
            offenderDetails = offenderDetails,
            assessmentAllocatedTo = applicant,
          )

          val placementRequest = createPlacementRequest(application)

          assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.STARTED)

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
          val cruEmail = application.apArea!!.emailAddress!!

          emailAsserter.assertEmailsRequestedCount(2)
          assertPlacementRequestWithdrawnEmail(applicantEmail, placementRequest)
          assertMatchRequestWithdrawnEmail(cruEmail, placementRequest)
        }
      }
    }

    /**
     * ```
     * | Entities                         | Withdrawn | Email PP | Email AP | Email CRU | Email Assessor |
     * | -------------------------------- | --------- | -------- | -------- | --------- | -------------- |
     * | Application                      | -         | -        | -        | -         | -              |
     * | -> Assessment                    | -         | -        | -        | -         | -              |
     * | -> Match request                 | YES       | YES      | -        | -         | -              |
     * | ---> Booking arrival pending     | YES       | YES      | YES      | YES       | -              |
     * ```
     */
    @Test
    fun `Withdrawing a match request for original app dates cascades to applicable entities and updates the application status`() {
      `Given a User` { applicant, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val (application, assessment) = createApplicationAndAssessment(
            applicant = applicant,
            assignee = applicant,
            offenderDetails = offenderDetails,
            assessmentAllocatedTo = applicant,
          )

          val placementRequest = createPlacementRequest(application)
          val bookingNoArrival = createBooking(
            application = application,
            hasArrival = false,
            startDate = LocalDate.now().plusDays(1),
            endDate = LocalDate.now().plusDays(6),
          )
          addBookingToPlacementRequest(placementRequest, bookingNoArrival)

          assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.STARTED)

          withdrawPlacementRequest(
            placementRequest,
            WithdrawPlacementRequestReason.duplicatePlacementRequest,
            jwt,
          )

          assertApplicationStatus(application, ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST)

          assertApplicationNotWithdrawn(application)
          assertAssessmentNotWithdrawn(assessment)

          assertPlacementRequestWithdrawn(placementRequest, PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST)
          assertBookingWithdrawn(bookingNoArrival, "Related placement request withdrawn")

          val applicantEmail = applicant.email!!
          val cruEmail = application.apArea!!.emailAddress!!
          val apEmail = bookingNoArrival.premises.emailAddress!!

          emailAsserter.assertEmailsRequestedCount(4)
          assertPlacementRequestWithdrawnEmail(applicantEmail, placementRequest)
          assertBookingWithdrawnEmail(applicantEmail, bookingNoArrival)
          assertBookingWithdrawnEmail(apEmail, bookingNoArrival)
          assertBookingWithdrawnEmail(cruEmail, bookingNoArrival)
        }
      }
    }

    private fun assertAssessmentWithdrawnEmail(emailAddress: String) =
      emailAsserter.assertEmailRequested(
        emailAddress,
        notifyConfig.templates.assessmentWithdrawnV2,
      )

    private fun assertApplicationWithdrawnEmail(emailAddress: String, application: ApplicationEntity) =
      emailAsserter.assertEmailRequested(
        emailAddress,
        notifyConfig.templates.applicationWithdrawnV2,
        mapOf("crn" to application.crn),
      )

    private fun assertBookingWithdrawnEmail(emailAddress: String, booking: BookingEntity) =
      emailAsserter.assertEmailRequested(
        emailAddress,
        notifyConfig.templates.bookingWithdrawn,
        mapOf(
          "startDate" to booking.arrivalDate.toString(),
          "endDate" to booking.departureDate.toString(),
        ),
      )

    private fun assertPlacementRequestWithdrawnEmail(emailAddress: String, placementApplication: PlacementApplicationEntity) =
      emailAsserter.assertEmailRequested(
        emailAddress,
        notifyConfig.templates.placementRequestWithdrawnV2,
        mapOf("startDate" to placementApplication.placementDates[0].expectedArrival.toString()),
      )

    private fun assertPlacementRequestWithdrawnEmail(emailAddress: String, placementRequest: PlacementRequestEntity) =
      emailAsserter.assertEmailRequested(
        emailAddress,
        notifyConfig.templates.placementRequestWithdrawnV2,
        mapOf("startDate" to placementRequest.expectedArrival.toString()),
      )

    private fun assertMatchRequestWithdrawnEmail(emailAddress: String, placementRequest: PlacementRequestEntity) =
      emailAsserter.assertEmailRequested(
        emailAddress,
        notifyConfig.templates.matchRequestWithdrawnV2,
        mapOf("startDate" to placementRequest.expectedArrival.toString()),
      )
  }

  private fun addBookingToPlacementRequest(placementRequest: PlacementRequestEntity, booking: BookingEntity) {
    placementRequest.booking = booking
    placementRequestRepository.save(placementRequest)
  }

  private fun withdrawApplication(application: ApprovedPremisesApplicationEntity, jwt: String) {
    webTestClient.post()
      .uri("/applications/${application.id}/withdrawal")
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
      .uri("/placement-applications/${placementApplication.id}/withdraw")
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
      .uri("/placement-requests/${placementRequest.id}/withdrawal")
      .bodyValue(WithdrawPlacementRequest(reason))
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun assertAssessmentNotWithdrawn(assessment: AssessmentEntity) {
    val updatedAssessment = approvedPremisesAssessmentRepository.findByIdOrNull(assessment.id)!!
    assertThat(updatedAssessment.isWithdrawn).isFalse
  }

  private fun assertAssessmentWithdrawn(assessment: AssessmentEntity) {
    val updatedAssessment = approvedPremisesAssessmentRepository.findByIdOrNull(assessment.id)!!
    assertThat(updatedAssessment.isWithdrawn).isTrue
  }

  private fun assertPlacementApplicationWithdrawn(
    placementApplication: PlacementApplicationEntity,
    decision: PlacementApplicationDecision,
    reason: PlacementApplicationWithdrawalReason,
  ) {
    val updatedPlacementApplication = placementApplicationRepository.findByIdOrNull(placementApplication.id)!!
    assertThat(updatedPlacementApplication.decision).isEqualTo(decision)
    assertThat(updatedPlacementApplication.withdrawalReason).isEqualTo(reason)
  }

  private fun assertPlacementApplicationNotWithdrawn(placementApplication: PlacementApplicationEntity) {
    val updatedPlacementApplication = placementApplicationRepository.findByIdOrNull(placementApplication.id)!!
    assertThat(updatedPlacementApplication.isWithdrawn()).isFalse()
    assertThat(updatedPlacementApplication.withdrawalReason).isNull()
  }

  private fun assertApplicationStatus(application: ApprovedPremisesApplicationEntity, expectedStatus: ApprovedPremisesApplicationStatus) {
    val updatedApplication = approvedPremisesApplicationRepository.findByIdOrNull(application.id)!!
    assertThat(updatedApplication.status).isEqualTo(expectedStatus)
  }

  private fun assertApplicationNotWithdrawn(application: ApprovedPremisesApplicationEntity) {
    val updatedApplication = approvedPremisesApplicationRepository.findByIdOrNull(application.id)!!
    assertThat(updatedApplication.isWithdrawn).isFalse
  }

  private fun assertApplicationWithdrawn(application: ApprovedPremisesApplicationEntity) {
    val updatedApplication = approvedPremisesApplicationRepository.findByIdOrNull(application.id)!!
    assertThat(updatedApplication.isWithdrawn).isTrue
  }

  private fun assertPlacementRequestWithdrawn(placementRequest: PlacementRequestEntity, reason: PlacementRequestWithdrawalReason) {
    val updatedPlacementRequest = placementRequestRepository.findByIdOrNull(placementRequest.id)!!
    assertThat(updatedPlacementRequest.isWithdrawn).isEqualTo(true)
    assertThat(updatedPlacementRequest.withdrawalReason).isEqualTo(reason)
  }

  private fun assertPlacementRequestNotWithdrawn(placementRequest: PlacementRequestEntity) {
    val updatedPlacementRequest = placementRequestRepository.findByIdOrNull(placementRequest.id)!!
    assertThat(updatedPlacementRequest.isWithdrawn).isEqualTo(false)
  }

  private fun assertBookingWithdrawn(booking: BookingEntity, cancellationReason: String) {
    val updatedBooking = bookingRepository.findByIdOrNull(booking.id)!!
    assertThat(updatedBooking.isCancelled).isTrue()
    assertThat(updatedBooking.cancellation!!.reason.name).isEqualTo(cancellationReason)
  }

  private fun assertBookingNotWithdrawn(booking: BookingEntity) {
    val updatedBooking2WithArrival = bookingRepository.findByIdOrNull(booking.id)!!
    assertThat(updatedBooking2WithArrival.isCancelled).isFalse()
  }

  private fun createApplicationAndAssessment(
    applicant: UserEntity,
    assignee: UserEntity,
    offenderDetails: OffenderDetailSummary,
    assessmentSubmitted: Boolean = true,
    assessmentAllocatedTo: UserEntity? = null,
  ): Pair<ApprovedPremisesApplicationEntity, AssessmentEntity> {
    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
      withAddedAt(OffsetDateTime.now())
    }

    val apArea = apAreaEntityFactory.produceAndPersist {
      withEmailAddress("apAreaEmail@test.com")
    }

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(applicant)
      withApplicationSchema(applicationSchema)
      withSubmittedAt(OffsetDateTime.now())
      withApArea(apArea)
      withReleaseType("licence")
    }

    val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
      withAllocatedToUser(assignee)
      withApplication(application)
      withAssessmentSchema(assessmentSchema)
      withSubmittedAt(if (assessmentSubmitted) OffsetDateTime.now() else null)
      withAllocatedToUser(assessmentAllocatedTo)
    }

    assessment.schemaUpToDate = true
    application.assessments.add(assessment)

    return Pair(application, assessment)
  }

  private fun produceAndPersistBasicApplication(
    crn: String,
    userEntity: UserEntity,
    managingTeamCode: String,
  ): ApplicationEntity {
    val jsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
      withSchema(
        """
        {
          "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
          "${"\$id"}": "https://example.com/product.schema.json",
          "title": "Thing",
          "description": "A thing",
          "type": "object",
          "properties": {
            "thingId": {
              "description": "The unique identifier for a thing",
              "type": "integer"
            }
          },
          "required": [ "thingId" ]
        }
        """,
      )
    }

    val application =
      approvedPremisesApplicationEntityFactory.produceAndPersist {
        withApplicationSchema(jsonSchema)
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

  private fun createPlacementApplication(
    application: ApprovedPremisesApplicationEntity,
    placementDates: List<Pair<LocalDate, Int>> = emptyList(),
    isSubmitted: Boolean = true,
    configuration: (PlacementApplicationEntityFactory.() -> Unit)? = null,
  ): PlacementApplicationEntity {
    val placementApplication = placementApplicationFactory.produceAndPersist {
      withCreatedByUser(application.createdByUser)
      withApplication(application)
      withSchemaVersion(
        approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        },
      )
      withSubmittedAt(if (isSubmitted) OffsetDateTime.now() else null)
      withDecision(if (isSubmitted) PlacementApplicationDecision.ACCEPTED else null)
      withPlacementType(PlacementType.ADDITIONAL_PLACEMENT)
      configuration?.invoke(this)
    }

    if (isSubmitted) {
      val dates = placementDates.map { (start, duration) ->
        placementDateFactory.produceAndPersist {
          withPlacementApplication(placementApplication)
          withExpectedArrival(start)
          withDuration(duration)
        }
      }
      placementApplication.placementDates.addAll(dates)
    }

    return placementApplication
  }

  private fun createPlacementRequest(
    application: ApprovedPremisesApplicationEntity,
    configuration: (PlacementRequestEntityFactory.() -> Unit)? = null,
  ) =
    placementRequestFactory.produceAndPersist {
      val assessment = application.assessments[0]

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

      withAllocatedToUser(application.createdByUser)
      withApplication(application)
      withAssessment(assessment)
      withPlacementRequirements(placementRequirements)
      configuration?.invoke(this)
    }

  private fun createBooking(
    application: ApprovedPremisesApplicationEntity,
    startDate: LocalDate,
    endDate: LocalDate,
    hasArrival: Boolean = false,
    adhoc: Boolean? = false,
  ): BookingEntity {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion { probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } } }
    }

    val booking = bookingEntityFactory.produceAndPersist {
      withApplication(application)
      withPremises(premises)
      withCrn(application.crn)
      withServiceName(ServiceName.approvedPremises)
      withArrivalDate(startDate)
      withDepartureDate(endDate)
      withAdhoc(adhoc)
    }

    if (hasArrival) {
      arrivalEntityFactory.produceAndPersist {
        withBooking(booking)
      }
    }

    return booking
  }

  private fun datePeriodForDuration(start: LocalDate, duration: Int) = DatePeriod(start, start.plusDays(duration.toLong()))
}
