package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.external

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalApplicationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalAssessmentDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPlacementDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPremisesDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalRequestForPlacementDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1PlacementPairDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingShortSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate
import java.time.LocalDate

@Component
class Cas1ExternalApplicationTransformer(
  private val cas1AssessmentTransformer: Cas1AssessmentTransformer,
  private val cas1ApplicationService: Cas1ApplicationService,
  @Value($$"${url-templates.frontend.application}") private val cas1ApplicationUrlTemplate: String,
) {

  fun transformToCas1PlacementPair(
    rfp: RequestForPlacement,
    rejectionReason: String? = null,
    withdrawalDate: LocalDate? = null,
    placement: Cas1SpaceBookingShortSummary? = null,
    premises: ApprovedPremisesEntity? = null,
  ) = Cas1PlacementPairDto(
    dateApplied = placement?.statusSetDate ?: rfp.statusSetDate,
    requestForPlacement = transformToRequestForPlacement(rfp, rejectionReason, withdrawalDate, placement),
    placement = placement?.let { transformToPlacement(it, premises) },
  )

  private fun transformToRequestForPlacement(
    rfp: RequestForPlacement,
    rejectionReason: String?,
    withdrawalDate: LocalDate?,
    placement: Cas1SpaceBookingShortSummary?,
  ) = Cas1ExternalRequestForPlacementDto(
    decision = rfp.decision,
    rejectionReason = rejectionReason,
    submittedBy = rfp.submittedBy,
    submittedAt = rfp.submittedAt?.toLocalDate(),
    withdrawalReason = if (rfp.isWithdrawn) rfp.withdrawalReason else null,
    withdrawalDate = withdrawalDate,
    expectedArrivalDate = placement?.expectedArrivalDate ?: rfp.canonicalPlacementPeriod.arrival,
    durationDays = rfp.canonicalPlacementPeriod.duration,
    status = rfp.status,
  )

  private fun transformToPlacement(
    placement: Cas1SpaceBookingShortSummary,
    premises: ApprovedPremisesEntity?,
  ) = Cas1ExternalPlacementDto(
    actualArrivalDate = placement.actualArrivalDate,
    actualDepartureDate = placement.actualDepartureDate,
    cancellationReason = getCancellationReason(placement),
    premises = premises?.let { transformToPremises(placement, it) },
    status = placement.status,
  )

  private fun transformToPremises(
    placement: Cas1SpaceBookingShortSummary?,
    premises: ApprovedPremisesEntity?,
  ) = premises?.let {
    Cas1ExternalPremisesDto(
      startDate = placement?.actualArrivalDate ?: placement?.expectedArrivalDate,
      endDate = placement?.actualDepartureDate ?: placement?.expectedDepartureDate,
      postcode = it.postcode,
      addressLine1 = it.addressLine1,
      addressLine2 = it.addressLine2,
      town = it.town,
    )
  }

  private fun getCancellationReason(placement: Cas1SpaceBookingShortSummary?): String? = if (placement?.cancellation?.reason?.name == "Other") placement.cancellation.reasonNotes else placement?.cancellation?.reason?.name

  fun transformToCas1SuitableApplication(
    application: ApprovedPremisesApplicationEntity,
    suitablePlacementPair: Cas1PlacementPairDto?,
    placementHistory: List<Cas1PlacementPairDto>,
  ): Cas1SuitableApplication {
    val latestAssessment = application.getLatestAssessment()
    return Cas1SuitableApplication(
      id = application.id,
      uiUrl = cas1ApplicationUrlTemplate.replace("#id", application.id.toString()),
      applicationStatus = application.status,
      requestForPlacementStatus = suitablePlacementPair?.requestForPlacement?.status,
      placementStatus = suitablePlacementPair?.placement?.status,
      premises = suitablePlacementPair?.placement?.premises,
      application = transformToApplication(application),
      assessment = latestAssessment?.let { transformToAssessment(it) },
      requestForPlacement = suitablePlacementPair?.requestForPlacement,
      placement = suitablePlacementPair?.placement,
      placementHistory = placementHistory,
    )
  }

  private fun transformToApplication(
    application: ApprovedPremisesApplicationEntity,
  ) = Cas1ExternalApplicationDto(
    createdAt = application.createdAt,
    createdBy = cas1AssessmentTransformer.transformToStaffDto(application.createdByUser),
    submittedAt = application.submittedAt,
    expiresAt = cas1ApplicationService.getApplicationExpiresAt(application),
    status = application.status,
    id = application.id,
  )

  private fun transformToAssessment(
    latestAssessment: AssessmentEntity,
  ) = Cas1ExternalAssessmentDto(
    decision = latestAssessment.decision,
    rejectionRationale = latestAssessment.rejectionRationale,
  )
}
