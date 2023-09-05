package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Placement Request`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Approved Premises Bed`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.ApplicationReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ApplicationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.ApplicationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import java.time.OffsetDateTime

class ApplicationReportsTest : IntegrationTestBase() {
  @Autowired
  lateinit var realPlacementRequestRepository: PlacementRequestRepository

  @Autowired
  lateinit var realApplicationRepository: ApplicationRepository

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
  fun `Get application report returns OK with correct body`() {
    `Given a User`(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { userEntity, jwt ->
      `Given an Approved Premises Bed` { bed ->
        createApplicationWithBooking(OffsetDateTime.parse("2023-01-01T12:00:00Z"), userEntity, bed)

        val (applicationWithBooking, _) = createApplicationWithBooking(OffsetDateTime.parse("2023-04-01T12:00:00Z"), userEntity, bed)
        val (applicationWithDepartedBooking, departedBooking) = createApplicationWithBooking(OffsetDateTime.parse("2023-04-01T12:00:00Z"), userEntity, bed)
        val (applicationWithCancelledBooking, cancelledBooking) = createApplicationWithBooking(OffsetDateTime.parse("2023-04-01T12:00:00Z"), userEntity, bed)
        val (applicationWithNonArrivedBooking, nonArrivedBooking) = createApplicationWithBooking(OffsetDateTime.parse("2023-04-01T12:00:00Z"), userEntity, bed)

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

        val expectedApplications = listOf(
          realApplicationRepository.findByIdOrNull(applicationWithBooking.id) as ApprovedPremisesApplicationEntity,
          realApplicationRepository.findByIdOrNull(applicationWithDepartedBooking.id) as ApprovedPremisesApplicationEntity,
          realApplicationRepository.findByIdOrNull(applicationWithCancelledBooking.id) as ApprovedPremisesApplicationEntity,
          realApplicationRepository.findByIdOrNull(applicationWithNonArrivedBooking.id) as ApprovedPremisesApplicationEntity,
        )

        val expectedDataFrame = ApplicationReportGenerator(realOffenderService)
          .createReport(expectedApplications, ApplicationReportProperties(ServiceName.approvedPremises, 2023, 4, userEntity.deliusUsername))

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

            Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
          }
      }
    }
  }

  private fun createApplicationWithBooking(submittedAt: OffsetDateTime, userEntity: UserEntity, bed: BedEntity): Pair<ApprovedPremisesApplicationEntity, BookingEntity> {
    val (placementRequest, application) = `Given a Placement Request`(
      placementRequestAllocatedTo = userEntity,
      assessmentAllocatedTo = userEntity,
      createdByUser = userEntity,
      applicationSubmittedAt = submittedAt,
      mappa = "CAT M2/LEVEL M2",
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
