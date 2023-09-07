package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.reporting.generator

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.api.count
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.MoveOnCategoryEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NonArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NonArrivalReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PostCodeDistrictEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.ApplicationReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ApplicationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.ApplicationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import java.time.LocalDate
import java.time.OffsetDateTime

class ApplicationReportGeneratorTest {
  private val mockOffenderService = mockk<OffenderService>()

  private val applicationReportGenerator = ApplicationReportGenerator(mockOffenderService)

  private val crn = "ABC123"

  private val applicationFactory = ApprovedPremisesApplicationEntityFactory()
    .withCrn(crn)
    .withYieldedCreatedByUser {
      UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()
    }
    .withRiskRatings(
      PersonRisksFactory()
        .withMappa(
          RiskWithStatus(
            status = RiskStatus.Retrieved,
            value = Mappa(
              level = "CAT M2/LEVEL M2",
              lastUpdated = LocalDate.now(),
            ),
          ),
        ).produce(),
    )

  private val offender = PersonInfoResult.Success.Full(
    crn,
    OffenderDetailsSummaryFactory()
      .withCrn(crn)
      .withDateOfBirth(
        LocalDate.now()
          .minusYears(22)
          .minusDays(42),
      )
      .produce(),
    InmateDetailFactory().produce(),
  )

  private val user = UserEntityFactory()
    .withUnitTestControlProbationRegion()
    .produce()

  private val premises = ApprovedPremisesEntityFactory()
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
    }
    .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
    .produce()

  private val room = RoomEntityFactory()
    .withPremises(premises)
    .produce()

  private val bed = BedEntityFactory()
    .withRoom(room)
    .produce()

  @BeforeEach
  fun setup() {
    every { mockOffenderService.getInfoForPerson(crn, "username", true) } returns offender
  }

  @Test
  fun `it returns report data for an unsubmitted application`() {
    val application = applicationFactory.produce()

    val result = applicationReportGenerator
      .createReport(listOf(application), ApplicationReportProperties(ServiceName.approvedPremises, 2023, 4, "username"))

    assertThat(result.count()).isEqualTo(1)

    assertThat(result[0][ApplicationReportRow::id]).isEqualTo(application.id.toString())
    assertThat(result[0][ApplicationReportRow::crn]).isEqualTo(application.crn)
    assertThat(result[0][ApplicationReportRow::applicationAssessedDate]).isNull()
    assertThat(result[0][ApplicationReportRow::assessorCru]).isNull()
    assertThat(result[0][ApplicationReportRow::assessmentDecision]).isNull()
    assertThat(result[0][ApplicationReportRow::assessmentDecisionRationale]).isNull()
    assertThat(result[0][ApplicationReportRow::ageInYears]).isEqualTo(22)
    assertThat(result[0][ApplicationReportRow::gender]).isEqualTo(offender.offenderDetailSummary.gender)
    assertThat(result[0][ApplicationReportRow::mappa]).isEqualTo("CAT M2/LEVEL M2")
    assertThat(result[0][ApplicationReportRow::offenceId]).isEqualTo(application.offenceId)
    assertThat(result[0][ApplicationReportRow::noms]).isEqualTo(application.nomsNumber)
    assertThat(result[0][ApplicationReportRow::premisesType]).isNull()
    assertThat(result[0][ApplicationReportRow::releaseType]).isNull()
    assertThat(result[0][ApplicationReportRow::sentenceLengthInMonths]).isNull()
    assertThat(result[0][ApplicationReportRow::applicationSubmissionDate]).isEqualTo(application.submittedAt)
    assertThat(result[0][ApplicationReportRow::referrerLdu]).isNull()
    assertThat(result[0][ApplicationReportRow::referrerRegion]).isEqualTo(application.createdByUser.probationRegion.name)
    assertThat(result[0][ApplicationReportRow::referrerTeam]).isNull()
    assertThat(result[0][ApplicationReportRow::targetLocation]).isNull()
    assertThat(result[0][ApplicationReportRow::applicationWithdrawalReason]).isNull()
    assertThat(result[0][ApplicationReportRow::applicationWithdrawalDate]).isNull()
    assertThat(result[0][ApplicationReportRow::bookingID]).isNull()
    assertThat(result[0][ApplicationReportRow::bookingCancellationReason]).isNull()
    assertThat(result[0][ApplicationReportRow::bookingCancellationDate]).isNull()
    assertThat(result[0][ApplicationReportRow::expectedArrivalDate]).isNull()
    assertThat(result[0][ApplicationReportRow::matcherCru]).isNull()
    assertThat(result[0][ApplicationReportRow::expectedDepartureDate]).isNull()
    assertThat(result[0][ApplicationReportRow::premisesName]).isNull()
    assertThat(result[0][ApplicationReportRow::actualArrivalDate]).isNull()
    assertThat(result[0][ApplicationReportRow::actualDepartureDate]).isNull()
    assertThat(result[0][ApplicationReportRow::departureMoveOnCategory]).isNull()
    assertThat(result[0][ApplicationReportRow::nonArrivalDate]).isNull()
  }

  @Test
  fun `returns data for an assessed application`() {
    val application = applicationFactory
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withAllocatedToUser(user)
      .withApplication(application)
      .withSubmittedAt(OffsetDateTime.now())
      .withDecision(AssessmentDecision.REJECTED)
      .withRejectionRationale("Some Text")
      .produce()

    application.assessments = mutableListOf(assessment)

    val result = applicationReportGenerator
      .createReport(listOf(application), ApplicationReportProperties(ServiceName.approvedPremises, 2023, 4, "username"))

    assertThat(result.count()).isEqualTo(1)

    assertThat(result[0][ApplicationReportRow::applicationAssessedDate]).isEqualTo(assessment.submittedAt!!.toLocalDate())
    assertThat(result[0][ApplicationReportRow::assessorCru]).isEqualTo(assessment.allocatedToUser!!.probationRegion.name)
    assertThat(result[0][ApplicationReportRow::assessmentDecision]).isEqualTo("REJECTED")
    assertThat(result[0][ApplicationReportRow::assessmentDecisionRationale]).isEqualTo("Some Text")
  }

  @Test
  fun `returns data for an application with a placement request`() {
    val application = applicationFactory
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withAllocatedToUser(user)
      .withApplication(application)
      .produce()

    val placementRequest = PlacementRequestEntityFactory()
      .withPlacementRequirements(
        PlacementRequirementsEntityFactory()
          .withApType(ApType.normal)
          .withPostcodeDistrict(
            PostCodeDistrictEntityFactory()
              .withOutcode("ABC")
              .produce(),
          )
          .withApplication(application)
          .withAssessment(assessment)
          .produce(),
      )
      .withApplication(application)
      .withAssessment(assessment)
      .withAllocatedToUser(user)
      .produce()

    application.placementRequests = mutableListOf(placementRequest)

    val result = applicationReportGenerator
      .createReport(listOf(application), ApplicationReportProperties(ServiceName.approvedPremises, 2023, 4, "username"))

    assertThat(result.count()).isEqualTo(1)

    assertThat(result[0][ApplicationReportRow::premisesType]).isEqualTo("normal")
    assertThat(result[0][ApplicationReportRow::targetLocation]).isEqualTo("ABC")
  }

  @Test
  fun `returns data for an application with a booking`() {
    val application = applicationFactory
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withAllocatedToUser(user)
      .withApplication(application)
      .produce()

    val booking = BookingEntityFactory()
      .withBed(bed)
      .withPremises(premises)
      .produce()

    val placementRequirements = PlacementRequirementsEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .produce()

    val placementRequest = PlacementRequestEntityFactory()
      .withBooking(booking)
      .withApplication(application)
      .withAssessment(assessment)
      .withPlacementRequirements(placementRequirements)
      .withAllocatedToUser(user)
      .produce()

    application.placementRequests = mutableListOf(placementRequest)

    val result = applicationReportGenerator
      .createReport(listOf(application), ApplicationReportProperties(ServiceName.approvedPremises, 2023, 4, "username"))

    assertThat(result.count()).isEqualTo(1)

    assertThat(result[0][ApplicationReportRow::bookingID]).isEqualTo(booking.id.toString())
    assertThat(result[0][ApplicationReportRow::expectedArrivalDate]).isEqualTo(booking.arrivalDate)
    assertThat(result[0][ApplicationReportRow::expectedDepartureDate]).isEqualTo(booking.departureDate)
  }

  @Test
  fun `returns data for an application with a booking with an arrival and departure`() {
    val application = applicationFactory
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withAllocatedToUser(user)
      .withApplication(application)
      .produce()

    val booking = BookingEntityFactory()
      .withBed(bed)
      .withPremises(premises)
      .produce()
      .apply {
        arrival = ArrivalEntityFactory().withBooking(this).produce()
        departures += DepartureEntityFactory()
          .withYieldedReason {
            DepartureReasonEntityFactory()
              .produce()
          }
          .withYieldedMoveOnCategory {
            MoveOnCategoryEntityFactory()
              .produce()
          }
          .withBooking(this)
          .produce()
      }

    val placementRequirements = PlacementRequirementsEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .produce()

    val placementRequest = PlacementRequestEntityFactory()
      .withBooking(booking)
      .withApplication(application)
      .withAssessment(assessment)
      .withPlacementRequirements(placementRequirements)
      .withAllocatedToUser(user)
      .produce()

    application.placementRequests = mutableListOf(placementRequest)

    val result = applicationReportGenerator
      .createReport(listOf(application), ApplicationReportProperties(ServiceName.approvedPremises, 2023, 4, "username"))

    assertThat(result.count()).isEqualTo(1)

    assertThat(result[0][ApplicationReportRow::actualArrivalDate]).isEqualTo(booking.arrival!!.arrivalDate)
    assertThat(result[0][ApplicationReportRow::actualDepartureDate]).isEqualTo(booking.departure!!.dateTime.toLocalDate())
  }

  @Test
  fun `returns data for an application with a booking with a non-arrival`() {
    val application = applicationFactory
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withAllocatedToUser(user)
      .withApplication(application)
      .produce()

    val booking = BookingEntityFactory()
      .withBed(bed)
      .withPremises(premises)
      .produce()
      .apply {
        nonArrival = NonArrivalEntityFactory()
          .withBooking(this)
          .withYieldedReason { NonArrivalReasonEntityFactory().produce() }
          .produce()
      }

    val placementRequirements = PlacementRequirementsEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .produce()

    val placementRequest = PlacementRequestEntityFactory()
      .withBooking(booking)
      .withApplication(application)
      .withAssessment(assessment)
      .withPlacementRequirements(placementRequirements)
      .withAllocatedToUser(user)
      .produce()

    application.placementRequests = mutableListOf(placementRequest)

    val result = applicationReportGenerator
      .createReport(listOf(application), ApplicationReportProperties(ServiceName.approvedPremises, 2023, 4, "username"))

    assertThat(result.count()).isEqualTo(1)

    assertThat(result[0][ApplicationReportRow::nonArrivalDate]).isEqualTo(booking.nonArrival!!.date)
  }

  @Test
  fun `returns data for an application with a booking with a cancellation`() {
    val application = applicationFactory
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withAllocatedToUser(user)
      .withApplication(application)
      .produce()

    val booking = BookingEntityFactory()
      .withBed(bed)
      .withPremises(premises)
      .produce()
      .apply {
        cancellations += CancellationEntityFactory()
          .withBooking(this)
          .withReason(
            CancellationReasonEntityFactory()
              .withName("Some cancellation reason")
              .produce(),
          )
          .produce()
      }

    val placementRequirements = PlacementRequirementsEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .produce()

    val placementRequest = PlacementRequestEntityFactory()
      .withBooking(booking)
      .withApplication(application)
      .withAssessment(assessment)
      .withPlacementRequirements(placementRequirements)
      .withAllocatedToUser(user)
      .produce()

    application.placementRequests = mutableListOf(placementRequest)

    val result = applicationReportGenerator
      .createReport(listOf(application), ApplicationReportProperties(ServiceName.approvedPremises, 2023, 4, "username"))

    assertThat(result.count()).isEqualTo(1)

    assertThat(result[0][ApplicationReportRow::bookingCancellationDate]).isEqualTo(booking.cancellation!!.date)
    assertThat(result[0][ApplicationReportRow::bookingCancellationReason]).isEqualTo("Some cancellation reason")
  }
}
