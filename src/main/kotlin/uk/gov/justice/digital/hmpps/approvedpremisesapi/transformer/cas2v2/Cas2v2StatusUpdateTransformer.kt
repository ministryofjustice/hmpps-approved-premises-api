package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2StatusUpdateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LatestCas2v2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2StatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2StatusUpdateEntity
import java.util.UUID

@Component
class Cas2v2StatusUpdateTransformer(
  private val cas2v2UserTransformer: Cas2v2UserTransformer,
) {

  fun transformJpaToApi(
    jpa: Cas2v2StatusUpdateEntity,
  ): Cas2v2StatusUpdate = Cas2v2StatusUpdate(
    id = jpa.id,
    name = jpa.status().name,
    label = jpa.label,
    description = jpa.description,
    updatedBy = cas2v2UserTransformer.transformJpaToApi(jpa.assessor),
    updatedAt = jpa.createdAt.toInstant(),
    statusUpdateDetails = jpa.statusUpdateDetails?.map { detail -> transformStatusUpdateDetailsJpaToApi(detail) },
  )

  private fun transformStatusUpdateDetailsJpaToApi(jpa: Cas2v2StatusUpdateDetailEntity): Cas2v2StatusUpdateDetail = Cas2v2StatusUpdateDetail(
    id = jpa.id,
    name = jpa.statusDetail(jpa.statusUpdate.statusId, jpa.statusDetailId).name,
    label = jpa.label,
  )

  fun transformJpaSummaryToLatestStatusUpdateApi(jpa: Cas2v2ApplicationSummaryEntity): LatestCas2v2StatusUpdate? {
    if (jpa.latestStatusUpdateStatusId !== null) {
      return LatestCas2v2StatusUpdate(
        statusId = UUID.fromString(jpa.latestStatusUpdateStatusId!!),
        label = jpa.latestStatusUpdateLabel!!,
      )
    } else {
      return null
    }
  }
}
