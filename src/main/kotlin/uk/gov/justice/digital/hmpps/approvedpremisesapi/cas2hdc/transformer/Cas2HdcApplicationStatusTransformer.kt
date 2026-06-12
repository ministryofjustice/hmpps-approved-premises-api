package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2StatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2ApplicationStatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcApplicationStatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2PersistedApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2PersistedApplicationStatusDetail

@Component("Cas2ApplicationStatusTransformer")
class Cas2HdcApplicationStatusTransformer {
  fun transformModelToApi(status: Cas2PersistedApplicationStatus): Cas2HdcApplicationStatus = Cas2HdcApplicationStatus(
    id = status.id,
    name = status.name,
    label = status.label,
    description = status.description,
    statusDetails = status.statusDetails?.map { statusDetail -> transformStatusDetailModelToApi(statusDetail) }
      ?: emptyList(),
  )

  fun transformV2ModelToApi(status: Cas2PersistedApplicationStatus): Cas2v2ApplicationStatus = Cas2v2ApplicationStatus(
    id = status.id,
    name = status.name,
    label = status.label,
    description = status.description,
    statusDetails = status.statusDetails?.map { statusDetail -> transformV2StatusDetailModelToApi(statusDetail) }
      ?: emptyList(),
  )

  fun transformStatusDetailModelToApi(statusDetail: Cas2PersistedApplicationStatusDetail): Cas2HdcApplicationStatusDetail = Cas2HdcApplicationStatusDetail(
    id = statusDetail.id,
    name = statusDetail.name,
    label = statusDetail.label,
  )

  fun transformV2StatusDetailModelToApi(statusDetail: Cas2PersistedApplicationStatusDetail): Cas2v2ApplicationStatusDetail = Cas2v2ApplicationStatusDetail(
    id = statusDetail.id,
    name = statusDetail.name,
    label = statusDetail.label,
  )

  fun transformStatusDetailListToDetailItemList(statusDetailsList: List<Cas2PersistedApplicationStatusDetail>): List<Cas2StatusDetail> = statusDetailsList.map { status ->
    transformStatusDetailToStatusDetailItem(status)
  }

  fun transformStatusDetailToStatusDetailItem(statusDetail: Cas2PersistedApplicationStatusDetail): Cas2StatusDetail = Cas2StatusDetail(
    name = statusDetail.name,
    label = statusDetail.label,
  )
}
