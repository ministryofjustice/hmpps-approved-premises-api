package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity

sealed class TypedTask {
  data class Assessment(val entity: ApprovedPremisesAssessmentEntity) : TypedTask()
  data class PlacementRequest(val entity: PlacementRequestEntity) : TypedTask()
  data class PlacementApplication(val entity: PlacementApplicationEntity) : TypedTask()
}
