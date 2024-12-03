package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawableType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason

@Component
class PlacementApplicationTransformer(
  private val objectMapper: ObjectMapper,
) {
  fun transformJpaToApi(jpa: PlacementApplicationEntity): PlacementApplication {
    val assessment = jpa.application.getLatestAssessment()!!
    val application = jpa.application

    return PlacementApplication(
      id = jpa.id,
      applicationId = application.id,
      applicationCompletedAt = application.submittedAt!!.toInstant(),
      assessmentId = assessment.id,
      assessmentCompletedAt = assessment.submittedAt!!.toInstant(),
      createdByUserId = jpa.createdByUser.id,
      schemaVersion = jpa.schemaVersion.id,
      createdAt = jpa.createdAt.toInstant(),
      data = if (jpa.data != null) objectMapper.readTree(jpa.data) else null,
      document = if (jpa.document != null) objectMapper.readTree(jpa.document) else null,
      outdatedSchema = !jpa.schemaUpToDate,
      submittedAt = jpa.submittedAt?.toInstant(),
      canBeWithdrawn = jpa.isInWithdrawableState(),
      isWithdrawn = jpa.isWithdrawn,
      withdrawalReason = getWithdrawalReason(jpa.withdrawalReason),
      type = PlacementApplicationType.ADDITIONAL,
      placementDates = jpa.placementDates.map { PlacementDates(it.expectedArrival, it.duration) },
    )
  }

  fun transformToWithdrawable(placementApplication: PlacementApplicationEntity): Withdrawable = Withdrawable(
    placementApplication.id,
    WithdrawableType.PLACEMENT_APPLICATION,
    placementApplication.placementDates.map {
      DatePeriod(it.expectedArrival, it.expectedDeparture())
    },
  )

  fun getWithdrawalReason(withdrawalReason: PlacementApplicationWithdrawalReason?): WithdrawPlacementRequestReason? = when (withdrawalReason) {
    PlacementApplicationWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST -> WithdrawPlacementRequestReason.duplicatePlacementRequest
    PlacementApplicationWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED -> WithdrawPlacementRequestReason.alternativeProvisionIdentified
    PlacementApplicationWithdrawalReason.WITHDRAWN_BY_PP -> WithdrawPlacementRequestReason.withdrawnByPP
    PlacementApplicationWithdrawalReason.CHANGE_IN_CIRCUMSTANCES -> WithdrawPlacementRequestReason.changeInCircumstances
    PlacementApplicationWithdrawalReason.CHANGE_IN_RELEASE_DECISION -> WithdrawPlacementRequestReason.changeInReleaseDecision
    PlacementApplicationWithdrawalReason.NO_CAPACITY_DUE_TO_LOST_BED -> WithdrawPlacementRequestReason.noCapacityDueToLostBed
    PlacementApplicationWithdrawalReason.NO_CAPACITY_DUE_TO_PLACEMENT_PRIORITISATION -> WithdrawPlacementRequestReason.noCapacityDueToPlacementPrioritisation
    PlacementApplicationWithdrawalReason.NO_CAPACITY -> WithdrawPlacementRequestReason.noCapacity
    PlacementApplicationWithdrawalReason.ERROR_IN_PLACEMENT_REQUEST -> WithdrawPlacementRequestReason.errorInPlacementRequest
    PlacementApplicationWithdrawalReason.RELATED_APPLICATION_WITHDRAWN -> WithdrawPlacementRequestReason.relatedApplicationWithdrawn
    null -> null
  }
}
