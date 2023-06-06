package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementType as ApiPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType as JpaPlacementType

@Component
class TaskTransformer(
  private val personTransformer: PersonTransformer,
  private val userTransformer: UserTransformer,
  private val risksTransformer: RisksTransformer,
  private val placementRequestTransformer: PlacementRequestTransformer,
) {
  fun transformAssessmentToTask(assessment: AssessmentEntity, offenderDetailSummary: OffenderDetailSummary, inmateDetail: InmateDetail) = AssessmentTask(
    applicationId = assessment.application.id,
    person = personTransformer.transformModelToApi(offenderDetailSummary, inmateDetail),
    dueDate = assessment.createdAt.plusDays(10).toLocalDate(),
    allocatedToStaffMember = userTransformer.transformJpaToApi(assessment.allocatedToUser, ServiceName.approvedPremises) as ApprovedPremisesUser,
    status = getAssessmentStatus(assessment),
    taskType = TaskType.assessment,
  )

  fun transformPlacementRequestToTask(placementRequest: PlacementRequestEntity, offenderDetailSummary: OffenderDetailSummary, inmateDetail: InmateDetail) = PlacementRequestTask(
    id = placementRequest.id,
    applicationId = placementRequest.application.id,
    person = personTransformer.transformModelToApi(offenderDetailSummary, inmateDetail),
    dueDate = placementRequest.createdAt.plusDays(10).toLocalDate(),
    allocatedToStaffMember = userTransformer.transformJpaToApi(placementRequest.allocatedToUser, ServiceName.approvedPremises) as ApprovedPremisesUser,
    status = getPlacementRequestStatus(placementRequest),
    taskType = TaskType.placementRequest,
    risks = risksTransformer.transformDomainToApi(placementRequest.application.riskRatings!!, placementRequest.application.crn),
    expectedArrival = placementRequest.expectedArrival,
    duration = placementRequest.duration,
    releaseType = placementRequestTransformer.getReleaseType(placementRequest.application.releaseType)!!,
  )

  fun transformPlacementApplicationToTask(placementApplication: PlacementApplicationEntity, offenderDetailSummary: OffenderDetailSummary, inmateDetail: InmateDetail) = PlacementApplicationTask(
    id = placementApplication.id,
    applicationId = placementApplication.application.id,
    person = personTransformer.transformModelToApi(offenderDetailSummary, inmateDetail),
    dueDate = placementApplication.createdAt.plusDays(10).toLocalDate(),
    allocatedToStaffMember = userTransformer.transformJpaToApi(placementApplication.allocatedToUser!!, ServiceName.approvedPremises) as ApprovedPremisesUser,
    status = getPlacementApplicationStatus(placementApplication),
    taskType = TaskType.placementApplication,
    risks = risksTransformer.transformDomainToApi(placementApplication.application.riskRatings!!, placementApplication.application.crn),
    placementDates = placementApplication.placementDates.map {
      PlacementDates(
        expectedArrival = it.expectedArrival,
        duration = it.duration,
      )
    },
    releaseType = placementRequestTransformer.getReleaseType(placementApplication.application.releaseType)!!,
    placementType = getPlacementType(placementApplication.placementType!!),
  )

  private fun getPlacementType(placementType: JpaPlacementType): ApiPlacementType = when (placementType) {
    JpaPlacementType.ROTL -> ApiPlacementType.rotl
    JpaPlacementType.ADDITIONAL_PLACEMENT -> ApiPlacementType.additionalPlacement
    JpaPlacementType.RELEASE_FOLLOWING_DECISION -> ApiPlacementType.releaseFollowingDecision
  }

  private fun getPlacementApplicationStatus(entity: PlacementApplicationEntity): TaskStatus = when {
    entity.data.isNullOrEmpty() -> TaskStatus.notStarted
    entity.decision !== null -> TaskStatus.complete
    else -> TaskStatus.inProgress
  }

  private fun getAssessmentStatus(entity: AssessmentEntity): TaskStatus = when {
    entity.data.isNullOrEmpty() -> TaskStatus.notStarted
    entity.decision !== null -> TaskStatus.complete
    else -> TaskStatus.inProgress
  }

  private fun getPlacementRequestStatus(entity: PlacementRequestEntity): TaskStatus = when {
    entity.booking !== null -> TaskStatus.complete
    else -> TaskStatus.notStarted
  }
}
