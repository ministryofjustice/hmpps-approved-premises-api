package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2StatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2ApplicationStatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatusDetail

@Component("Cas2v2ApplicationStatusTransformer")
class Cas2v2ApplicationStatusTransformer {
  fun transformModelToApi(status: Cas2PersistedApplicationStatus): Cas2v2ApplicationStatus {
    return Cas2v2ApplicationStatus(
      id = status.id,
      name = status.name,
      label = status.label,
      description = status.description,
      statusDetails = status.statusDetails?.map { statusDetail -> transformStatusDetailModelToApi(statusDetail) }
        ?: emptyList(),
    )
  }

  fun transformStatusDetailModelToApi(statusDetail: Cas2PersistedApplicationStatusDetail): Cas2v2ApplicationStatusDetail {
    return Cas2v2ApplicationStatusDetail(
      id = statusDetail.id,
      name = statusDetail.name,
      label = statusDetail.label,
    )
  }

  fun transformStatusDetailListToDetailItemList(statusDetailsList: List<Cas2PersistedApplicationStatusDetail>): List<Cas2StatusDetail> {
    return statusDetailsList.map { status -> transformStatusDetailToStatusDetailItem(status) }
  }

  fun transformStatusDetailToStatusDetailItem(statusDetail: Cas2PersistedApplicationStatusDetail): Cas2StatusDetail {
    return Cas2StatusDetail(
      name = statusDetail.name,
      label = statusDetail.label,
    )
  }
}
