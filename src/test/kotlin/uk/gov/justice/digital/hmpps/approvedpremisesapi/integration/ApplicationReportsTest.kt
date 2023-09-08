package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.api.toList
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Placement Request`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Approved Premises Bed`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntityReportRowRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ApplicationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.Period
import java.util.UUID

class ApplicationReportsTest : IntegrationTestBase() {
  @Autowired
  lateinit var realPlacementRequestRepository: PlacementRequestRepository

  @Autowired
  lateinit var realApplicationRepository: ApplicationRepository

  @Autowired
  lateinit var realApplicationEntityReportRowRepository: ApplicationEntityReportRowRepository

  @Autowired
  lateinit var realOffenderService: OffenderService

  @Test
  fun `Get application report returns 403 Forbidden if user does not have all regions access`() {
    `Given a User` { _, jwt ->
      webTestClient.get()
        .uri("/reports/applications?year=2023&month=4")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get application report returns 400 if month is provided and not within 1-12`() {
    `Given a User`(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { _, jwt ->
      webTestClient.get()
        .uri("/reports/applications?year=2023&month=-1")
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
  fun `Get application report returns OK with correct applications`() {
    `Given a User` { referrer, _ ->
      `Given a User` { assessor, _ ->
        `Given a User`(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { userEntity, jwt ->
          `Given an Approved Premises Bed` { bed ->
            createApplicationWithBooking(OffsetDateTime.parse("2023-01-01T12:00:00Z"), referrer, assessor, bed)

            val (applicationWithBooking, _) = createApplicationWithBooking(OffsetDateTime.parse("2023-04-01T12:00:00Z"), referrer, assessor, bed)
            val (applicationWithDepartedBooking, departedBooking) = createApplicationWithBooking(OffsetDateTime.parse("2023-04-01T12:00:00Z"), referrer, assessor, bed)
            val (applicationWithCancelledBooking, cancelledBooking) = createApplicationWithBooking(OffsetDateTime.parse("2023-04-01T12:00:00Z"), referrer, assessor, bed)
            val (applicationWithNonArrivedBooking, nonArrivedBooking) = createApplicationWithBooking(OffsetDateTime.parse("2023-04-01T12:00:00Z"), referrer, assessor, bed)

            arrivalEntityFactory.produceAndPersist {
              withBooking(departedBooking)
            }

            departureEntityFactory.produceAndPersist {
              withBooking(departedBooking)
              withYieldedReason {
                departureReasonEntityFactory.produceAndPersist()
              }
              withYieldedMoveOnCategory {
                moveOnCategoryEntityFactory.produceAndPersist()
              }
            }

            cancellationEntityFactory.produceAndPersist {
              withBooking(cancelledBooking)
              withReason(cancellationReasonEntityFactory.produceAndPersist())
            }

            nonArrivalEntityFactory.produceAndPersist {
              withBooking(nonArrivedBooking)
              withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
            }

            webTestClient.get()
              .uri("/reports/applications?year=2023&month=4")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .consumeWith {
                val actual = DataFrame
                  .readExcel(it.responseBody!!.inputStream())
                  .convertTo<ApplicationReportRow>(ExcessiveColumns.Remove)
                  .toList()

                assertThat(actual.size).isEqualTo(4)

                assertThat(
                  actual.any { reportRow ->
                    assertApplicationRowHasCorrectData(applicationWithBooking.id, reportRow, userEntity, hasBooking = true)
                  },
                )

                assertThat(
                  actual.any { reportRow ->
                    assertApplicationRowHasCorrectData(applicationWithDepartedBooking.id, reportRow, userEntity, hasDeparture = true)
                  },
                )

                assertThat(
                  actual.any { reportRow ->
                    assertApplicationRowHasCorrectData(applicationWithCancelledBooking.id, reportRow, userEntity, hasCancellation = true)
                  },
                )

                assertThat(
                  actual.any { reportRow ->
                    assertApplicationRowHasCorrectData(applicationWithNonArrivedBooking.id, reportRow, userEntity, hasNonArrival = true)
                  },
                )
              }
          }
        }
      }
    }
  }

  private fun assertApplicationRowHasCorrectData(applicationId: UUID, reportRow: ApplicationReportRow, userEntity: UserEntity, hasBooking: Boolean = true, hasCancellation: Boolean = false, hasDeparture: Boolean = false, hasNonArrival: Boolean = false): Boolean {
    val application = realApplicationRepository.findByIdOrNull(applicationId) as ApprovedPremisesApplicationEntity
    val assessment = application.getLatestAssessment()!!
    val placementRequest = application.getLatestPlacementRequest()!!
    val offenderDetailSummary = getOffenderDetailForApplication(application, userEntity.deliusUsername)

    var hasCorrectData = reportRow.id == application.id.toString() &&
      reportRow.crn == application.crn &&
      reportRow.applicationAssessedDate == assessment.submittedAt!!.toLocalDate() &&
      reportRow.assessorCru == assessment.allocatedToUser!!.probationRegion.name &&
      reportRow.assessmentDecision == assessment.rejectionRationale &&
      reportRow.assessmentDecisionRationale == assessment.rejectionRationale &&
      reportRow.ageInYears == Period.between(offenderDetailSummary.dateOfBirth, LocalDate.now()).years &&
      reportRow.gender == offenderDetailSummary.gender &&
      reportRow.mappa == application.riskRatings!!.mappa.value!!.level &&
      reportRow.offenceId == application.offenceId &&
      reportRow.noms == application.nomsNumber &&
      reportRow.premisesType == placementRequest.placementRequirements.apType.toString() &&
      reportRow.releaseType == application.releaseType &&
      reportRow.applicationSubmissionDate == application.submittedAt!!.toLocalDate() &&
      reportRow.referrerRegion == application.createdByUser.probationRegion.name &&
      reportRow.targetLocation == placementRequest.placementRequirements.postcodeDistrict.outcode &&
      reportRow.applicationWithdrawalReason == application.withdrawalReason

    if (hasBooking) {
      val booking = placementRequest.booking!!
      hasCorrectData = hasCorrectData &&
        reportRow.bookingID == booking.id.toString() &&
        reportRow.expectedArrivalDate == booking.arrivalDate &&
        reportRow.expectedDepartureDate == booking.departureDate &&
        reportRow.premisesName == booking.premises.name
    }

    if (hasCancellation) {
      val cancellation = placementRequest.booking!!.cancellation!!
      hasCorrectData = hasCorrectData &&
        reportRow.bookingCancellationDate == cancellation.date
    }

    if (hasDeparture) {
      val arrival = placementRequest.booking!!.arrival!!
      val departure = placementRequest.booking!!.departure!!
      hasCorrectData = hasCorrectData &&
        reportRow.actualArrivalDate == arrival.arrivalDate &&
        reportRow.actualDepartureDate == departure.dateTime.toLocalDate() &&
        reportRow.departureMoveOnCategory == departure.moveOnCategory.name
    }

    if (hasNonArrival) {
      val nonArrival = placementRequest.booking!!.nonArrival!!
      hasCorrectData = hasCorrectData &&
        reportRow.nonArrivalDate == nonArrival.date
    }

    return hasCorrectData
  }

  private fun getOffenderDetailForApplication(application: ApplicationEntity, deliusUsername: String): OffenderDetailSummary {
    return when (val personInfo = realOffenderService.getInfoForPerson(application.crn, deliusUsername, true)) {
      is PersonInfoResult.Success.Full -> personInfo.offenderDetailSummary
      else -> throw Exception("No offender found for CRN ${application.crn}")
    }
  }

  private fun createApplicationWithBooking(submittedAt: OffsetDateTime, referrer: UserEntity, assessor: UserEntity, bed: BedEntity): Pair<ApprovedPremisesApplicationEntity, BookingEntity> {
    val (offenderDetails, _) = `Given an Offender`()
    val (placementRequest, application) = `Given a Placement Request`(
      placementRequestAllocatedTo = assessor,
      assessmentAllocatedTo = assessor,
      createdByUser = referrer,
      applicationSubmittedAt = submittedAt,
      mappa = "CAT M2/LEVEL M2",
      crn = offenderDetails.otherIds.crn,
    )

    val booking = bookingEntityFactory.produceAndPersist {
      withBed(bed)
      withPremises(bed.room.premises)
      withApplication(application)
    }

    placementRequest.booking = booking
    realPlacementRequestRepository.save(placementRequest)

    return Pair(application as ApprovedPremisesApplicationEntity, booking)
  }
}
