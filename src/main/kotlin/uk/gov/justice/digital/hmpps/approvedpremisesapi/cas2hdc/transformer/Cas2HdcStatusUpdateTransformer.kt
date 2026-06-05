package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcLatestStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcStatusUpdateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2StatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2StatusUpdateEntity
import java.util.UUID

@Component("Cas2StatusUpdateTransformer")
class Cas2HdcStatusUpdateTransformer(
  private val cas2HdcExternalUserTransformer: Cas2HdcExternalUserTransformer,
) {

  fun transformJpaToApi(
    jpa: Cas2StatusUpdateEntity,
  ): Cas2HdcStatusUpdate = Cas2HdcStatusUpdate(
    id = jpa.id,
    name = jpa.status().name,
    label = jpa.label,
    description = jpa.description,
    updatedBy = cas2HdcExternalUserTransformer.transformJpaToApi(jpa.assessor),
    updatedAt = jpa.createdAt.toInstant(),
    statusUpdateDetails = jpa.statusUpdateDetails?.map { detail -> transformStatusUpdateDetailsJpaToApi(detail) },
  )

  fun transformStatusUpdateDetailsJpaToApi(jpa: Cas2StatusUpdateDetailEntity): Cas2HdcStatusUpdateDetail = Cas2HdcStatusUpdateDetail(
    id = jpa.id,
    name = jpa.statusDetail(jpa.statusUpdate.statusId, jpa.statusDetailId).name,
    label = jpa.label,
  )

  fun transformJpaSummaryToLatestStatusUpdateApi(jpa: Cas2ApplicationSummaryEntity): Cas2HdcLatestStatusUpdate? {
    if (jpa.latestStatusUpdateStatusId !== null) {
      return Cas2HdcLatestStatusUpdate(
        statusId = UUID.fromString(jpa.latestStatusUpdateStatusId!!),
        label = jpa.latestStatusUpdateLabel!!,
      )
    } else {
      return null
    }
  }
}
