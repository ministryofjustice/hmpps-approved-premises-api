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
            createTemporaryAccommodationApplication(OffsetDateTime.parse("2023-04-01T12:00:00Z"))

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

                val applicationRowWithBooking = actual.find { reportRow -> reportRow.id == applicationWithBooking.id.toString() }!!
                val applicationRowWithDepartedBooking = actual.find { reportRow -> reportRow.id == applicationWithDepartedBooking.id.toString() }!!
                val applicationRowWithCancelledBooking = actual.find { reportRow -> reportRow.id == applicationWithCancelledBooking.id.toString() }!!
                val applicationRowWithNonArrivedBooking = actual.find { reportRow -> reportRow.id == applicationWithNonArrivedBooking.id.toString() }!!

                assertApplicationRowHasCorrectData(applicationWithBooking.id, applicationRowWithBooking, userEntity, hasBooking = true)
                assertApplicationRowHasCorrectData(applicationWithDepartedBooking.id, applicationRowWithDepartedBooking, userEntity, hasBooking = true)
                assertApplicationRowHasCorrectData(applicationWithCancelledBooking.id, applicationRowWithCancelledBooking, userEntity, hasBooking = true)
                assertApplicationRowHasCorrectData(applicationWithNonArrivedBooking.id, applicationRowWithNonArrivedBooking, userEntity, hasBooking = true)
              }
          }
        }
      }
    }
  }

  private fun assertApplicationRowHasCorrectData(applicationId: UUID, reportRow: ApplicationReportRow, userEntity: UserEntity, hasBooking: Boolean = true, hasCancellation: Boolean = false, hasDeparture: Boolean = false, hasNonArrival: Boolean = false) {
    val application = realApplicationRepository.findByIdOrNull(applicationId) as ApprovedPremisesApplicationEntity
    val assessment = application.getLatestAssessment()!!
    val placementRequest = application.getLatestPlacementRequest()!!
    val offenderDetailSummary = getOffenderDetailForApplication(application, userEntity.deliusUsername)

    assertThat(reportRow.crn).isEqualTo(application.crn)
    assertThat(reportRow.applicationAssessedDate).isEqualTo(assessment.submittedAt!!.toLocalDate())
    assertThat(reportRow.assessorCru).isEqualTo(assessment.allocatedToUser!!.probationRegion.name)
    assertThat(reportRow.assessmentDecision).isEqualTo(assessment.decision.toString())
    assertThat(reportRow.assessmentDecisionRationale).isEqualTo(assessment.rejectionRationale)
    assertThat(reportRow.ageInYears).isEqualTo(Period.between(offenderDetailSummary.dateOfBirth, LocalDate.now()).years)
    assertThat(reportRow.gender).isEqualTo(offenderDetailSummary.gender)
    assertThat(reportRow.mappa).isEqualTo(application.riskRatings!!.mappa.value!!.level)
    assertThat(reportRow.offenceId).isEqualTo(application.offenceId)
    assertThat(reportRow.noms).isEqualTo(application.nomsNumber)
    assertThat(reportRow.premisesType).isEqualTo(placementRequest.placementRequirements.apType.name)
    assertThat(reportRow.releaseType).isEqualTo(application.releaseType)
    assertThat(reportRow.applicationSubmissionDate).isEqualTo(application.submittedAt!!.toLocalDate())
    assertThat(reportRow.referrerRegion).isEqualTo(application.createdByUser.probationRegion.name)
    assertThat(reportRow.targetLocation).isEqualTo(placementRequest.placementRequirements.postcodeDistrict.outcode)
    assertThat(reportRow.applicationWithdrawalReason).isEqualTo(application.withdrawalReason)

    if (hasBooking) {
      val booking = placementRequest.booking!!
      assertThat(reportRow.bookingID).isEqualTo(booking.id.toString())
      assertThat(reportRow.expectedArrivalDate).isEqualTo(booking.arrivalDate)
      assertThat(reportRow.expectedDepartureDate).isEqualTo(booking.departureDate)
      assertThat(reportRow.premisesName).isEqualTo(booking.premises.name)
    }

    if (hasCancellation) {
      val cancellation = placementRequest.booking!!.cancellation!!
      assertThat(reportRow.bookingCancellationDate).isEqualTo(cancellation.date)
    }

    if (hasDeparture) {
      val arrival = placementRequest.booking!!.arrival!!
      val departure = placementRequest.booking!!.departure!!
      assertThat(reportRow.actualArrivalDate).isEqualTo(arrival.arrivalDate)
      assertThat(reportRow.actualDepartureDate).isEqualTo(departure.dateTime.toLocalDate())
      assertThat(reportRow.departureMoveOnCategory).isEqualTo(departure.moveOnCategory.name)
    }

    if (hasNonArrival) {
      val nonArrival = placementRequest.booking!!.nonArrival!!
      assertThat(reportRow.nonArrivalDate).isEqualTo(nonArrival.date)
    }
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

  private fun createTemporaryAccommodationApplication(submittedAt: OffsetDateTime) {
    `Given a User` { submittingUser, _ ->
      val (offenderDetails, _) = `Given an Offender`()

      val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
        withAddedAt(OffsetDateTime.now())
        withId(UUID.randomUUID())
        withSchema(
          """
              {
                "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
                "${"\$id"}": "https://example.com/product.schema.json",
                "title": "Thing",
                "description": "A thing",
                "type": "object",
                "properties": {},
                "required": []
              }
            """,
        )
      }

      temporaryAccommodationApplicationEntityFactory.produceAndPersist {
        withCrn(offenderDetails.otherIds.crn)
        withApplicationSchema(applicationSchema)
        withCreatedByUser(submittingUser)
        withProbationRegion(submittingUser.probationRegion)
        withSubmittedAt(submittedAt)
      }
    }
  }
}
