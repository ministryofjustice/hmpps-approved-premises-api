package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcLatestStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcStatusUpdateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2StatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2StatusUpdateEntity

@Component("Cas2StatusUpdateTransformer")
class Cas2HdcStatusUpdateTransformer(
  private val cas2HdcExternalUserTransformer: Cas2HdcExternalUserTransformer,
) {

  fun transformJpaToApi(
    jpa: Cas2StatusUpdateEntity,
  ): Cas2HdcStatusUpdate = Cas2HdcStatusUpdate(
    id = jpa.id,
    name = jpa.status.apiName,
    label = jpa.label,
    description = jpa.description,
    updatedBy = cas2HdcExternalUserTransformer.transformJpaToApi(jpa.assessor),
    updatedAt = jpa.createdAt.toInstant(),
    statusUpdateDetails = jpa.statusUpdateDetails?.map { detail -> transformStatusUpdateDetailsJpaToApi(detail) },
  )

  fun transformStatusUpdateDetailsJpaToApi(jpa: Cas2StatusUpdateDetailEntity): Cas2HdcStatusUpdateDetail = Cas2HdcStatusUpdateDetail(
    id = jpa.id,
    name = jpa.statusDetail(jpa.statusUpdate.statusId, ServiceName.cas2).apiName,
    label = jpa.label,
  )

  fun transformJpaSummaryToLatestStatusUpdateApi(jpa: Cas2ApplicationSummary): Cas2HdcLatestStatusUpdate? = jpa.latestStatusUpdateStatusId?.let {
    Cas2HdcLatestStatusUpdate(
      statusId = it,
      label = jpa.latestStatusUpdateLabel!!,
    )
  }
}
