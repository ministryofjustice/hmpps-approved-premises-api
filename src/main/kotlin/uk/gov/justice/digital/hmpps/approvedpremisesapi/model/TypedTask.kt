package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import java.time.OffsetDateTime

sealed class TypedTask(
  val createdAt: OffsetDateTime,
  val crn: String,
) {
  data class Assessment(val entity: ApprovedPremisesAssessmentEntity) : TypedTask(entity.createdAt, entity.application.crn)
  data class PlacementRequest(val entity: PlacementRequestEntity) : TypedTask(entity.createdAt, entity.application.crn)
  data class PlacementApplication(val entity: PlacementApplicationEntity) : TypedTask(entity.createdAt, entity.application.crn)
}
