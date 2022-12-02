package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ActiveOffence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Conviction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenceDetail

@Component
class ConvictionTransformer {
  fun transformToApi(conviction: Conviction) = conviction.offences?.map {
    ActiveOffence(
      deliusEventNumber = conviction.index,
      offenceDescription = nonRedundantDescription(it.detail),
      offenceId = it.offenceId,
      convictionId = conviction.convictionId
    )
  } ?: emptyList()

  private fun nonRedundantDescription(offenceDetail: OffenceDetail) = if (offenceDetail.mainCategoryDescription != offenceDetail.subCategoryDescription) {
    "${offenceDetail.mainCategoryDescription} - ${offenceDetail.subCategoryDescription}"
  } else {
    offenceDetail.mainCategoryDescription
  }
}
