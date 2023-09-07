package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ApplicationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.ApplicationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import java.time.LocalDate
import java.time.Period

class ApplicationReportGenerator(
  private val offenderService: OffenderService,
) : ReportGenerator<ApprovedPremisesApplicationEntity, ApplicationReportRow, ApplicationReportProperties>(ApplicationReportRow::class) {
  override fun filter(properties: ApplicationReportProperties): (ApplicationEntity) -> Boolean = {
    true
  }

  override val convert: ApprovedPremisesApplicationEntity.(properties: ApplicationReportProperties) -> List<ApplicationReportRow> = { properties ->
    val assessment = this.getLatestAssessment()
    val placementRequest = this.getLatestPlacementRequest()
    val personInfoResult = getOffenderDetailForApplication(this, properties.deliusUsername)
    val booking = this.getLatestBooking()

    listOf(
      ApplicationReportRow(
        id = this.id.toString(),
        crn = this.crn,
        applicationAssessedDate = assessment?.submittedAt?.toLocalDate(),
        assessorCru = assessment?.allocatedToUser?.probationRegion?.name,
        assessmentDecision = assessment?.decision?.toString(),
        assessmentDecisionRationale = assessment?.rejectionRationale,
        ageInYears = when (personInfoResult) {
          is PersonInfoResult.Success.Full -> Period.between(personInfoResult.offenderDetailSummary.dateOfBirth, LocalDate.now()).years
          else -> null
        },
        gender = when (personInfoResult) {
          is PersonInfoResult.Success.Full -> personInfoResult.offenderDetailSummary.gender
          else -> null
        },
        mappa = this.riskRatings?.mappa?.value?.level ?: "Not found",
        offenceId = this.offenceId,
        noms = this.nomsNumber,
        premisesType = placementRequest?.placementRequirements?.apType?.toString(),
        releaseType = this.releaseType,
        sentenceLengthInMonths = null,
        applicationSubmissionDate = this.submittedAt?.toLocalDate(),
        referrerLdu = null,
        referrerRegion = this.createdByUser.probationRegion.name,
        referrerTeam = null,
        targetLocation = placementRequest?.placementRequirements?.postcodeDistrict?.outcode,
        applicationWithdrawalReason = this.withdrawalReason,
        applicationWithdrawalDate = null,
        bookingID = booking?.id?.toString(),
        bookingCancellationReason = booking?.cancellation?.reason?.name,
        bookingCancellationDate = booking?.cancellation?.date,
        expectedArrivalDate = booking?.arrivalDate,
        matcherCru = null,
        expectedDepartureDate = booking?.departureDate,
        premisesName = booking?.premises?.name,
        actualArrivalDate = booking?.arrival?.arrivalDate,
        actualDepartureDate = booking?.departure?.dateTime?.toLocalDate(),
        departureMoveOnCategory = booking?.departure?.moveOnCategory?.name,
        nonArrivalDate = booking?.nonArrival?.date,
      ),
    )
  }

  private fun getOffenderDetailForApplication(applicationEntity: ApplicationEntity, deliusUsername: String): PersonInfoResult? {
    return when (val personInfo = offenderService.getInfoForPerson(applicationEntity.crn, deliusUsername, true)) {
      is PersonInfoResult.Success -> personInfo
      else -> null
    }
  }
}
