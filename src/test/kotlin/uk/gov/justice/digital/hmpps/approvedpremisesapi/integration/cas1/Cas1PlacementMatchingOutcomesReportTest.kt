package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.api.toList
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.Cas1PlacementMatchingOutcomesReportTest.Constants.DATE_FORMAT
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.Cas1PlacementMatchingOutcomesReportTest.Constants.REPORT_MONTH
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.Cas1PlacementMatchingOutcomesReportTest.Constants.REPORT_YEAR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Placement Application`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Placement Request`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUtcOffsetDateTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class Cas1PlacementMatchingOutcomesReportTest : IntegrationTestBase() {

  object Constants {
    const val REPORT_MONTH = 1
    const val REPORT_YEAR = 2020

    val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu")
  }

  @Test
  fun `Get placement matching outcomes report returns 403 Forbidden if user does not have correct role`() {
    `Given a User` { _, jwt ->
      webTestClient.get()
        .uri("/reports/placement-applications?year=2023&month=4")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get placement matching outcomes report returns 400 if month is provided and not within 1-12`() {
    `Given a User`(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { _, jwt ->
      webTestClient.get()
        .uri("/reports/placement-applications?year=2023&month=-1")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.detail").isEqualTo("month must be between 1 and 12")
    }
  }

  @Test
  fun `Get placement matching outcomes report if no data return no rows`() {
    `Given a User`(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { userEntity, jwt ->
      getReport(jwt) { rows ->
        assertThat(rows).isEmpty()
      }
    }
  }

  @Nested
  inner class ForInitialRequestForPlacements {

    @Test
    fun `Get placement matching outcomes report maps all fields for initial request without placement`() {
      `Given a User`(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { user, jwt ->
        val (placementRequest, _) = `Given a Placement Request`(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
          expectedArrival = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 1),
          duration = 52,
          reallocated = false,
          applicationSubmittedAt = LocalDate.of(2019, 10, 11).toUtcOffsetDateTime(),
          assessmentSubmittedAt = LocalDate.of(2019, 11, 12).toUtcOffsetDateTime(),
        )

        getReport(jwt) { rows ->
          assertThat(rows).hasSize(1)

          val row1 = rows[0]

          assertThat(row1.crn).isEqualTo(placementRequest.application.crn)
          assertThat(row1.applicationId).isEqualTo(placementRequest.application.id.toString())
          assertThat(row1.requestForPlacementId).isEqualTo("placement_request:${placementRequest.id}")
          assertThat(row1.matchRequestId).isEqualTo(placementRequest.id.toString())
          assertThat(row1.requestForPlacementType).isEqualTo("STANDARD")
          assertThat(row1.requestedArrivalDate).isEqualTo("01/0$REPORT_MONTH/$REPORT_YEAR")
          assertThat(row1.requestedDurationDays).isEqualTo(52)
          assertThat(row1.requestForPlacementSubmittedAt).isEqualTo("11/10/2019")
          assertThat(row1.requestForPlacementWithdrawalReason).isNull()
          assertThat(row1.requestForPlacementAssessedDate).isEqualTo("12/11/2019")
          assertThat(row1.placementId).isNull()
          assertThat(row1.placementCancellationReason).isNull()
        }
      }
    }

    @Test
    fun `Get placement matching outcomes report maps all fields for initial request with withdrawn request and cancelled placement`() {
      `Given a User`(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { user, jwt ->
        val (placementRequest, _) = `Given a Placement Request`(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
          expectedArrival = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 1),
          reallocated = false,
          isWithdrawn = true,
          withdrawalReason = PlacementRequestWithdrawalReason.NO_CAPACITY,
          tier = "C",
        )

        val booking = bookingEntityFactory.produceAndPersist {
          withPremises(premises())
          withApplication(placementRequest.application)
        }

        cancellationEntityFactory.produceAndPersist {
          withBooking(booking)
          withYieldedReason {
            cancellationReasonEntityFactory.produceAndPersist() {
              withName("the cancellation reason!")
            }
          }
        }

        placementRequest.booking = booking
        placementRequestRepository.save(placementRequest)

        getReport(jwt) { rows ->
          assertThat(rows).hasSize(1)

          val row1 = rows[0]
          assertThat(row1.crn).isEqualTo(placementRequest.application.crn)
          assertThat(row1.tier).isEqualTo("C")
          assertThat(row1.applicationId).isEqualTo(placementRequest.application.id.toString())
          assertThat(row1.requestForPlacementId).isEqualTo("placement_request:${placementRequest.id}")
          assertThat(row1.requestForPlacementWithdrawalReason).isEqualTo("NO_CAPACITY")
          assertThat(row1.placementId).isEqualTo(booking.id.toString())
          assertThat(row1.placementCancellationReason).isEqualTo("the cancellation reason!")
        }
      }
    }

    @Test
    fun `Get placement matching outcomes report ignores initial requests outside of date range or reallocated`() {
      `Given a User`(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { user, jwt ->
        val toInclude = `Given a Placement Request`(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
          expectedArrival = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 1),
        ).first

        `Given a Placement Request`(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
          expectedArrival = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 1).minusMonths(1),
        )

        `Given a Placement Request`(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
          expectedArrival = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 1).plusMonths(1),
        )

        `Given a Placement Request`(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
          expectedArrival = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 1),
          reallocated = true,
        )

        getReport(jwt) {
            rows ->
          assertThat(rows).hasSize(1)

          assertThat(rows[0].applicationId).isEqualTo(toInclude.application.id.toString())
        }
      }
    }
  }

  @Nested
  inner class ForOtherRequestForPlacements {

    @ParameterizedTest
    @EnumSource(PlacementType::class)
    fun `Get placement matching outcomes report maps all fields for other request without placement`(placementType: PlacementType) {
      val expectedArrival = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 1)
      val duration = 52

      `Given a User`(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { user, jwt ->
        val placementApplication = `Given a Placement Application`(
          createdByUser = user,
          placementType = placementType,
          submittedAt = LocalDate.of(2019, 8, 9).toUtcOffsetDateTime(),
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          decision = PlacementApplicationDecision.ACCEPTED,
          decisionMadeAt = LocalDate.of(2019, 10, 11).toUtcOffsetDateTime(),
        )

        placementDateFactory.produceAndPersist {
          withPlacementApplication(placementApplication)
          withExpectedArrival(expectedArrival)
          withDuration(duration)
        }

        val (placementRequest, _) = `Given a Placement Request`(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
          expectedArrival = expectedArrival,
          duration = duration,
          reallocated = false,
          applicationSubmittedAt = LocalDate.of(2019, 10, 11).toUtcOffsetDateTime(),
          assessmentSubmittedAt = LocalDate.of(2019, 11, 12).toUtcOffsetDateTime(),
          placementApplication = placementApplication,
          tier = "D",
        )

        getReport(jwt) { rows ->
          assertThat(rows).hasSize(1)

          val row1 = rows[0]
          assertThat(row1.crn).isEqualTo(placementRequest.application.crn)
          assertThat(row1.tier).isEqualTo("D")
          assertThat(row1.applicationId).isEqualTo(placementRequest.application.id.toString())
          assertThat(row1.requestForPlacementId).isEqualTo("placement_application:${placementApplication.id}")
          assertThat(row1.matchRequestId).isEqualTo(placementRequest.id.toString())
          assertThat(row1.requestForPlacementType).isEqualTo(
            when (placementType) {
              PlacementType.ROTL -> "ROTL"
              PlacementType.RELEASE_FOLLOWING_DECISION -> "RELEASE_FOLLOWING_DECISION"
              PlacementType.ADDITIONAL_PLACEMENT -> "ADDITIONAL_PLACEMENT"
            },
          )
          assertThat(row1.requestedArrivalDate).isEqualTo(expectedArrival.format(DATE_FORMAT))
          assertThat(row1.requestedDurationDays).isEqualTo(duration)
          assertThat(row1.requestForPlacementSubmittedAt).isEqualTo("09/08/2019")
          assertThat(row1.requestForPlacementWithdrawalReason).isNull()
          assertThat(row1.requestForPlacementAssessedDate).isEqualTo("11/10/2019")
          assertThat(row1.placementId).isNull()
          assertThat(row1.placementCancellationReason).isNull()
        }
      }
    }

    @Test
    fun `Get placement matching outcomes report maps legacy placement application with multiple dates`() {
      val expectedArrival1 = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 1)
      val duration1 = 2

      val expectedArrival2 = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 5)
      val duration2 = 3

      val expectedArrival3 = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 15)
      val duration3 = 4

      `Given a User`(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { user, jwt ->
        val placementApplication = `Given a Placement Application`(
          createdByUser = user,
          placementType = PlacementType.ROTL,
          submittedAt = LocalDate.of(2019, 8, 9).toUtcOffsetDateTime(),
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          decision = PlacementApplicationDecision.ACCEPTED,
          decisionMadeAt = LocalDate.of(2019, 10, 11).toUtcOffsetDateTime(),
        )

        placementDateFactory.produceAndPersist {
          withPlacementApplication(placementApplication)
          withExpectedArrival(expectedArrival1)
          withDuration(duration1)
        }

        placementDateFactory.produceAndPersist {
          withPlacementApplication(placementApplication)
          withExpectedArrival(expectedArrival2)
          withDuration(duration2)
        }

        placementDateFactory.produceAndPersist {
          withPlacementApplication(placementApplication)
          withExpectedArrival(expectedArrival3)
          withDuration(duration3)
        }

        fun createPlacementRequest(arrival: LocalDate, duration: Int): PlacementRequestEntity =
          `Given a Placement Request`(
            placementRequestAllocatedTo = user,
            assessmentAllocatedTo = user,
            createdByUser = user,
            expectedArrival = arrival,
            duration = duration,
            reallocated = false,
            applicationSubmittedAt = LocalDate.of(2019, 10, 11).toUtcOffsetDateTime(),
            assessmentSubmittedAt = LocalDate.of(2019, 11, 12).toUtcOffsetDateTime(),
            placementApplication = placementApplication,
          ).first

        val placementRequest1 = createPlacementRequest(
          expectedArrival1,
          duration1,
        )

        val placementRequest2 = createPlacementRequest(
          expectedArrival2,
          duration2,
        )

        val placementRequest3 = createPlacementRequest(
          expectedArrival3,
          duration3,
        )

        getReport(jwt) { rows ->
          assertThat(rows).hasSize(3)

          val row1 = rows[0]
          assertThat(row1.crn).isEqualTo(placementRequest1.application.crn)
          assertThat(row1.requestForPlacementId).isEqualTo("placement_application:${placementApplication.id}")
          assertThat(row1.matchRequestId).isEqualTo(placementRequest1.id.toString())
          assertThat(row1.requestedArrivalDate).isEqualTo(expectedArrival1.format(DATE_FORMAT))
          assertThat(row1.requestedDurationDays).isEqualTo(duration1)

          val row2 = rows[1]
          assertThat(row2.crn).isEqualTo(placementRequest2.application.crn)
          assertThat(row2.requestForPlacementId).isEqualTo("placement_application:${placementApplication.id}")
          assertThat(row2.matchRequestId).isEqualTo(placementRequest2.id.toString())
          assertThat(row2.requestedArrivalDate).isEqualTo(expectedArrival2.format(DATE_FORMAT))
          assertThat(row2.requestedDurationDays).isEqualTo(duration2)

          val row3 = rows[2]
          assertThat(row3.crn).isEqualTo(placementRequest3.application.crn)
          assertThat(row3.requestForPlacementId).isEqualTo("placement_application:${placementApplication.id}")
          assertThat(row3.matchRequestId).isEqualTo(placementRequest3.id.toString())
          assertThat(row3.requestedArrivalDate).isEqualTo(expectedArrival3.format(DATE_FORMAT))
          assertThat(row3.requestedDurationDays).isEqualTo(duration3)
        }
      }
    }

    @Test
    fun `Get placement matching outcomes report ignores other requests outside of date range`() {
      `Given a User`(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { user, jwt ->

        fun createValidRequestForGivenExpectedArrival(
          expectedArrival: LocalDate,
          decision: PlacementApplicationDecision?,
        ): PlacementRequestEntity {
          val duration = 52

          val placementApplication = `Given a Placement Application`(
            createdByUser = user,
            placementType = PlacementType.ADDITIONAL_PLACEMENT,
            submittedAt = LocalDate.of(2019, 8, 9).toUtcOffsetDateTime(),
            schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
            decision = decision,
            decisionMadeAt = LocalDate.of(2019, 10, 11).toUtcOffsetDateTime(),
          )

          placementDateFactory.produceAndPersist {
            withPlacementApplication(placementApplication)
            withExpectedArrival(expectedArrival)
            withDuration(duration)
          }

          val (placementRequest) = `Given a Placement Request`(
            placementRequestAllocatedTo = user,
            assessmentAllocatedTo = user,
            createdByUser = user,
            expectedArrival = expectedArrival,
            duration = duration,
            reallocated = false,
            applicationSubmittedAt = LocalDate.of(2019, 10, 11).toUtcOffsetDateTime(),
            assessmentSubmittedAt = LocalDate.of(2019, 11, 12).toUtcOffsetDateTime(),
            placementApplication = placementApplication,
          )

          return placementRequest
        }

        val onlyMatchingRequest = createValidRequestForGivenExpectedArrival(
          expectedArrival = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 1),
          decision = PlacementApplicationDecision.ACCEPTED,
        )

        createValidRequestForGivenExpectedArrival(
          expectedArrival = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 1),
          decision = null,
        )

        createValidRequestForGivenExpectedArrival(
          expectedArrival = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 1),
          decision = PlacementApplicationDecision.WITHDRAW,
        )

        createValidRequestForGivenExpectedArrival(
          expectedArrival = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 1).plusMonths(1),
          decision = PlacementApplicationDecision.ACCEPTED,
        )

        createValidRequestForGivenExpectedArrival(
          expectedArrival = LocalDate.of(REPORT_YEAR, REPORT_MONTH, 1).minusMonths(1),
          decision = PlacementApplicationDecision.ACCEPTED,
        )

        getReport(jwt) {
            rows ->
          assertThat(rows).hasSize(1)

          assertThat(rows[0].applicationId).isEqualTo(onlyMatchingRequest.application.id.toString())
        }
      }
    }
  }

  private fun getReport(jwt: String, inlineAssertion: (input: List<ExpectedRows>) -> Unit) {
    webTestClient.get()
      .uri("/reports/placement-matching-outcomes?year=$REPORT_YEAR&month=$REPORT_MONTH")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.approvedPremises.value)
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().valueEquals("content-disposition", "attachment; filename=\"placement-matching-outcomes-2020-01.xlsx\"")
      .expectBody()
      .consumeWith {
        val actualRows = DataFrame
          .readExcel(it.responseBody!!.inputStream())
          .convertTo<ExpectedRows>(ExcessiveColumns.Remove)
          .toList()

        inlineAssertion.invoke(actualRows)
      }
  }

  data class ExpectedRows(
    val crn: String?,
    val tier: String?,
    val applicationId: String?,
    val requestForPlacementId: String?,
    val matchRequestId: String?,
    val requestForPlacementType: String?,
    val requestedArrivalDate: String?,
    val requestedDurationDays: Int?,
    val requestForPlacementSubmittedAt: String?,
    val requestForPlacementWithdrawalReason: String?,
    val requestForPlacementAssessedDate: String?,
    val placementId: String?,
    val placementCancellationReason: String?,
  )

  private fun premises() = approvedPremisesEntityFactory.produceAndPersist {
    withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
    withYieldedProbationRegion {
      probationRegionEntityFactory.produceAndPersist {
        withId(UUID.randomUUID())
        withYieldedApArea {
          apAreaEntityFactory.produceAndPersist()
        }
      }
    }
  }
}
