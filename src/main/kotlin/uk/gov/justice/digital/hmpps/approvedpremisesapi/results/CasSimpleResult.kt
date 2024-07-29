package uk.gov.justice.digital.hmpps.approvedpremisesapi.results

sealed interface CasSimpleResult<SuccessType> {
  data class Success<SuccessType>(val value: SuccessType) : CasSimpleResult<SuccessType>
  data class Failure<SuccessType>(val message: String) : CasSimpleResult<SuccessType>
}
