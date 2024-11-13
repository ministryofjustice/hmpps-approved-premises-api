package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ActiveOffence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Conviction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenceDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseDetail

@Component
class OffenseTransformer {
  fun transformToApi(conviction: Conviction) = conviction.offences?.map {
    ActiveOffence(
      deliusEventNumber = conviction.index,
      offenceDescription = nonRedundantDescription(it.detail),
      offenceId = it.offenceId,
      convictionId = conviction.convictionId,
      offenceDate = it.offenceDate?.toLocalDate(),
    )
  } ?: emptyList()

  fun transformToApi(caseDetail: CaseDetail) = caseDetail.offences.map {
    ActiveOffence(
      deliusEventNumber = it.eventNumber,
      offenceDescription = it.description,
      offenceId = it.id.toString(),
      convictionId = it.eventId,
      offenceDate = it.date,
    )
  }

  private fun nonRedundantDescription(offenceDetail: OffenceDetail) = if (offenceDetail.mainCategoryDescription != offenceDetail.subCategoryDescription) {
    "${offenceDetail.mainCategoryDescription} - ${offenceDetail.subCategoryDescription}"
  } else {
    offenceDetail.mainCategoryDescription
  }
}
