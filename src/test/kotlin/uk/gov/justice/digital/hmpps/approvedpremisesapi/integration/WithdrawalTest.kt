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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsEntity
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
@SuppressWarnings("LongParameterList")
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
    fun `Get withdrawables returns application only for a sparse application if user is not the applicant`() {
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
    fun `Get withdrawables returns application only for a sparse application if user is applicant`() {
      `Given a User` { applicationCreator, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val application = produceAndPersistBasicApplication(offenderDetails.otherIds.crn, applicationCreator, "TEAM")

          val expected = listOf(toWithdrawable(application))

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
              application,
              hasArrival = false,
              startDate = nowPlusDays(1),
              endDate = nowPlusDays(6),
            )
            addBookingToPlacementRequest(placementRequest, bookingNoArrival)

            createPlacementRequest(application, reallocatedAt = OffsetDateTime.now())

            createPlacementRequest(application, isWithdrawn = true)

            val placementApplication = createPlacementApplication(application, DatePeriod(nowPlusDays(50), duration = 6))
            createPlacementRequest(application, placementApplication = placementApplication)

            val expected = listOf(
              toWithdrawable(application),
              toWithdrawable(placementRequest),
              toWithdrawable(placementApplication),
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

            val submittedPlacementApplication1 = createPlacementApplication(
              application,
              datePeriods = listOf(
                DatePeriod(nowPlusDays(1), duration = 5),
                DatePeriod(nowPlusDays(10), duration = 10),
              ),
            )

            val submittedPlacementApplication2 = createPlacementApplication(
              application,
              DatePeriod(nowPlusDays(50), duration = 6),
            )

            createPlacementApplication(
              application,
              isSubmitted = false,
              decision = null,
            )

            createPlacementApplication(
              application,
              DatePeriod(LocalDate.now(), duration = 2),
              reallocatedAt = OffsetDateTime.now(),
            )

            val applicationWithAcceptedDecision = createPlacementApplication(
              application,
              DatePeriod(nowPlusDays(50), duration = 6),
              decision = PlacementApplicationDecision.ACCEPTED,
            )

            createPlacementApplication(
              application,
              DatePeriod(now(), duration = 2),
              decision = PlacementApplicationDecision.WITHDRAW,
            )

            createPlacementApplication(
              application,
              DatePeriod(now(), duration = 2),
              decision = PlacementApplicationDecision.WITHDRAWN_BY_PP,
            )

            val applicationWithRejectedDecision = createPlacementApplication(
              application,
              DatePeriod(nowPlusDays(50), duration = 6),
              decision = PlacementApplicationDecision.REJECTED,
            )

            val expected = listOf(
              toWithdrawable(application),
              toWithdrawable(submittedPlacementApplication1),
              toWithdrawable(submittedPlacementApplication2),
              toWithdrawable(applicationWithAcceptedDecision),
              toWithdrawable(applicationWithRejectedDecision),
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

              val placementApplication1 = createPlacementApplication(application, DatePeriod(now(), duration = 2))
              val placementRequest1 = createPlacementRequest(application, placementApplication = placementApplication1)
              val booking1NoArrival = createBooking(
                application = application,
                hasArrival = false,
                adhoc = false,
                startDate = nowPlusDays(1),
                endDate = nowPlusDays(6),
              )
              addBookingToPlacementRequest(placementRequest1, booking1NoArrival)

              createPlacementRequest(application, placementApplication = placementApplication1)

              val placementApplication2 = createPlacementApplication(
                application,
                DatePeriod(now(), duration = 2),
                allocatedTo = requestForPlacementAssessor,
              )

              val placementRequest3 = createPlacementRequest(application)
              val booking2HasArrival = createBooking(
                application = application,
                hasArrival = true,
                startDate = LocalDate.now(),
                endDate = nowPlusDays(1),
              )
              addBookingToPlacementRequest(placementRequest3, booking2HasArrival)

              val adhocBooking = createBooking(
                application = application,
                adhoc = true,
                hasArrival = false,
                startDate = nowPlusDays(20),
                endDate = nowPlusDays(26),
              )

              createBooking(
                application = otherApplication,
                adhoc = true,
                hasArrival = false,
                startDate = nowPlusDays(20),
                endDate = nowPlusDays(26),
              )
              createBooking(
                application = otherApplication,
                adhoc = null,
                hasArrival = false,
                startDate = nowPlusDays(20),
                endDate = nowPlusDays(26),
              )

              val expected = listOf(
                toWithdrawable(placementApplication1),
                toWithdrawable(booking1NoArrival),
                toWithdrawable(placementApplication2),
                toWithdrawable(adhocBooking),
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

            val placementApplication1 = createPlacementApplication(application, DatePeriod(now(), duration = 2))
            val placementRequest1 = createPlacementRequest(application, placementApplication = placementApplication1)
            val booking1NoArrival = createBooking(
              application = application,
              hasArrival = false,
              startDate = nowPlusDays(1),
              endDate = nowPlusDays(6),
            )
            addBookingToPlacementRequest(placementRequest1, booking1NoArrival)

            createPlacementRequest(application, placementApplication = placementApplication1)

            val placementApplication2 = createPlacementApplication(
              application,
              DatePeriod(now(), duration = 2),
              allocatedTo = requestForPlacementAssessor,
            )

            val placementRequest3 = createPlacementRequest(application)
            val booking2HasArrival = createBooking(
              application = application,
              hasArrival = true,
              startDate = LocalDate.now(),
              endDate = nowPlusDays(1),
            )
            addBookingToPlacementRequest(placementRequest3, booking2HasArrival)

            createBooking(
              application = application,
              hasArrival = false,
              startDate = nowPlusDays(20),
              endDate = nowPlusDays(26),
            )

            val expected = listOf(
              toWithdrawable(placementApplication1),
              toWithdrawable(placementApplication2),
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
     * |                                  |           |         Receive Email          |
     * | Entities                         | Withdrawn | PP   | Case Manager | Assessor |
     * | -------------------------------- | --------- | ---- | ------------ | -------- |
     * | Application                      | YES       | YES  | YES          | -        |
     * | -> Assessment (pending)          | YES       | -    | -            | YES      |
     * ```
     */
    @Test
    fun `Withdrawing an application cascades to an assessment and sends mail to assessor if pending`() {
      `Given a User` { applicant, jwt ->
        `Given a User` { assessor, _ ->
          `Given an Offender` { offenderDetails, _ ->
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
     * | -> Request for placement 1         | YES       | YES | YES          | -   | -   | -        |
     * | ---> Match request 1               | YES       | -   | -            | -   | -   | -        |
     * | -----> Booking 1 arrival pending   | YES       | YES | YES          | YES | YES | -        |
     * | ---> Match request 2               | YES       | -   | -            | -   | YES | -        |
     * | -> Request for placement 2         | YES       | YES | YES          | -   | -   | YES      |
     * | -> Match request 2                 | YES       | YES | YES          | -   | -   | -        |
     * | ---> Booking 2 arrival pending     | YES       | YES | YES          | YES | YES | -        |
     * | -> Adhoc Booking 1 arrival pending | YES       | YES | YES          | YES | YES | -        |
     * | -> Adhoc Booking 2 arrival pending | YES       | YES | YES          | YES | YES | -        |
     * ```
     */
    @Test
    fun `Withdrawing an application cascades to applicable entities`() {
      `Given a User` { applicant, jwt ->
        `Given a User` { requestForPlacementAssessor, _ ->
          `Given an Offender` { offenderDetails, _ ->
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

            val placementApplication1 = createPlacementApplication(application, DatePeriod(now(), duration = 2))
            val placementRequest1 = createPlacementRequest(application, placementApplication = placementApplication1)
            val booking1NoArrival = createBooking(
              application = application,
              hasArrival = false,
              startDate = nowPlusDays(1),
              endDate = nowPlusDays(6),
            )
            addBookingToPlacementRequest(placementRequest1, booking1NoArrival)

            val placementApplication2NoBookingBeingAssessed = createPlacementApplication(
              application,
              DatePeriod(nowPlusDays(2), duration = 2),
              allocatedTo = requestForPlacementAssessor,
              decision = null,
            )

            val placementRequest2 = createPlacementRequest(application)
            val booking2NoArrival = createBooking(
              application = application,
              hasArrival = false,
              startDate = nowPlusDays(5),
              endDate = nowPlusDays(15),
            )
            addBookingToPlacementRequest(placementRequest2, booking2NoArrival)

            val adhocBooking1NoArrival = createBooking(
              application = application,
              hasArrival = false,
              startDate = nowPlusDays(1),
              endDate = nowPlusDays(6),
              adhoc = true,
            )

            // we don't know the adhoc status for some legacy
            // applications. in this case adhoc is 'null'
            // for these cases we treat them as adhoc bookings
            val adhocBooking2NoArrival = createBooking(
              application = application,
              hasArrival = false,
              startDate = nowPlusDays(1),
              endDate = nowPlusDays(6),
              adhoc = null,
            )

            // regression test to ensure other application's
            // bookings aren't affected
            createBooking(
              application = otherApplication,
              adhoc = true,
              hasArrival = false,
              startDate = nowPlusDays(20),
              endDate = nowPlusDays(26),
            )
            createBooking(
              application = otherApplication,
              adhoc = null,
              hasArrival = false,
              startDate = nowPlusDays(20),
              endDate = nowPlusDays(26),
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

            emailAsserter.assertEmailsRequestedCount(25)
            assertApplicationWithdrawnEmail(applicantEmail, application)
            assertApplicationWithdrawnEmail(caseManagerEmail, application)

            assertPlacementRequestWithdrawnEmail(applicantEmail, placementApplication1)
            assertPlacementRequestWithdrawnEmail(caseManagerEmail, placementApplication1)

            assertBookingWithdrawnEmail(applicantEmail, booking1NoArrival)
            assertBookingWithdrawnEmail(caseManagerEmail, booking1NoArrival)
            assertBookingWithdrawnEmail(booking1NoArrival.premises.emailAddress!!, booking1NoArrival)
            assertBookingWithdrawnEmail(cruEmail, booking1NoArrival)

            assertPlacementRequestWithdrawnEmail(applicantEmail, placementApplication2NoBookingBeingAssessed)
            assertPlacementRequestWithdrawnEmail(caseManagerEmail, placementApplication2NoBookingBeingAssessed)
            assertPlacementRequestWithdrawnEmail(requestForPlacementAssessorEmail, placementApplication2NoBookingBeingAssessed)

            assertPlacementRequestWithdrawnEmail(applicantEmail, placementRequest2)
            assertPlacementRequestWithdrawnEmail(caseManagerEmail, placementRequest2)

            assertBookingWithdrawnEmail(applicantEmail, booking2NoArrival)
            assertBookingWithdrawnEmail(caseManagerEmail, booking2NoArrival)
            assertBookingWithdrawnEmail(booking2NoArrival.premises.emailAddress!!, booking2NoArrival)
            assertBookingWithdrawnEmail(cruEmail, booking2NoArrival)

            assertBookingWithdrawnEmail(applicantEmail, adhocBooking1NoArrival)
            assertBookingWithdrawnEmail(caseManagerEmail, adhocBooking1NoArrival)
            assertBookingWithdrawnEmail(adhocBooking1NoArrival.premises.emailAddress!!, adhocBooking1NoArrival)
            assertBookingWithdrawnEmail(cruEmail, adhocBooking1NoArrival)

            assertBookingWithdrawnEmail(applicantEmail, adhocBooking2NoArrival)
            assertBookingWithdrawnEmail(caseManagerEmail, adhocBooking2NoArrival)
            assertBookingWithdrawnEmail(adhocBooking1NoArrival.premises.emailAddress!!, adhocBooking2NoArrival)
            assertBookingWithdrawnEmail(cruEmail, adhocBooking2NoArrival)
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

          val placementApplication = createPlacementApplication(application, DatePeriod(now(), duration = 2))
          val placementRequest = createPlacementRequest(application, placementApplication = placementApplication)
          val bookingWithArrival = createBooking(
            application = application,
            hasArrival = true,
            startDate = nowPlusDays(1),
            endDate = nowPlusDays(6),
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
     * For this test the applicant is also the case manager
     *
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

          val placementApplication1 = createPlacementApplication(application, DatePeriod(now(), duration = 2))
          val placementRequest1 = createPlacementRequest(application, placementApplication = placementApplication1)
          val booking1NoArrival = createBooking(
            application = application,
            hasArrival = false,
            startDate = nowPlusDays(1),
            endDate = nowPlusDays(6),
          )
          addBookingToPlacementRequest(placementRequest1, booking1NoArrival)

          val placementRequest2 = createPlacementRequest(application, placementApplication = placementApplication1)
          val booking2NoArrival = createBooking(
            application = application,
            hasArrival = false,
            startDate = nowPlusDays(10),
            endDate = nowPlusDays(21),
          )
          addBookingToPlacementRequest(placementRequest2, booking2NoArrival)

          val placementApplication2 = createPlacementApplication(application, DatePeriod(now(), duration = 2))
          val placementRequest3 = createPlacementRequest(application, placementApplication = placementApplication2)
          val booking3NoArrival = createBooking(
            application = application,
            hasArrival = false,
            startDate = nowPlusDays(10),
            endDate = nowPlusDays(21),
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
     * For this test the applicant is also the case manager
     *
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

          val placementApplication1 = createPlacementApplication(application, DatePeriod(now(), duration = 2))
          val placementRequest1 = createPlacementRequest(application, placementApplication = placementApplication1)
          val booking1Adhoc = createBooking(
            application = application,
            adhoc = true,
            hasArrival = false,
            startDate = nowPlusDays(1),
            endDate = nowPlusDays(6),
          )
          addBookingToPlacementRequest(placementRequest1, booking1Adhoc)

          val placementRequest2 = createPlacementRequest(application, placementApplication = placementApplication1)
          val booking2NoArrival = createBooking(
            application = application,
            adhoc = false,
            hasArrival = false,
            startDate = nowPlusDays(10),
            endDate = nowPlusDays(21),
          )
          addBookingToPlacementRequest(placementRequest2, booking2NoArrival)

          val placementRequest3 = createPlacementRequest(application, placementApplication = placementApplication1)
          val booking3PotentiallyAdhoc = createBooking(
            application = application,
            adhoc = null,
            hasArrival = false,
            startDate = nowPlusDays(10),
            endDate = nowPlusDays(21),
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
     * For this test the applicant is also the case manager
     *
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
     * For this test the applicant is also the case manager
     *
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
            startDate = nowPlusDays(1),
            endDate = nowPlusDays(6),
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
        notifyConfig.templates.bookingWithdrawnV2,
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

  @SuppressWarnings("LongParameterList")
  private fun createApplicationAndAssessment(
    applicant: UserEntity,
    assignee: UserEntity,
    offenderDetails: OffenderDetailSummary,
    assessmentSubmitted: Boolean = true,
    assessmentAllocatedTo: UserEntity? = null,
    caseManager: Cas1ApplicationUserDetailsEntity? = null,
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
      withCaseManagerUserDetails(caseManager)
      withCaseManagerIsNotApplicant(caseManager != null)
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

  data class DatePeriod(val start: LocalDate, val duration: Int)

  private fun createPlacementApplication(
    application: ApprovedPremisesApplicationEntity,
    datePeriod: DatePeriod? = null,
    datePeriods: List<DatePeriod> = emptyList(),
    isSubmitted: Boolean = true,
    reallocatedAt: OffsetDateTime? = null,
    decision: PlacementApplicationDecision? = PlacementApplicationDecision.ACCEPTED,
    allocatedTo: UserEntity? = null,
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
      withDecision(decision)
      withPlacementType(PlacementType.ADDITIONAL_PLACEMENT)
      withReallocatedAt(reallocatedAt)
      withAllocatedToUser(allocatedTo)
    }

    if (isSubmitted) {
      val dates = (listOfNotNull(datePeriod) + datePeriods).map {
        placementDateFactory.produceAndPersist {
          withPlacementApplication(placementApplication)
          withExpectedArrival(it.start)
          withDuration(it.duration)
        }
      }
      placementApplication.placementDates.addAll(dates)
    }

    return placementApplication
  }

  private fun createPlacementRequest(
    application: ApprovedPremisesApplicationEntity,
    reallocatedAt: OffsetDateTime? = null,
    isWithdrawn: Boolean = false,
    placementApplication: PlacementApplicationEntity? = null,
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
      withReallocatedAt(reallocatedAt)
      withIsWithdrawn(isWithdrawn)
      withPlacementApplication(placementApplication)
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

  fun toWithdrawable(placementApplication: PlacementApplicationEntity) =
    Withdrawable(
      placementApplication.id,
      WithdrawableType.placementApplication,
      placementApplication.placementDates.map { toDatePeriod(it.expectedArrival, it.duration) },
    )

  fun toWithdrawable(booking: BookingEntity) =
    Withdrawable(
      booking.id,
      WithdrawableType.booking,
      listOf(DatePeriod(booking.arrivalDate, booking.departureDate)),
    )
}
