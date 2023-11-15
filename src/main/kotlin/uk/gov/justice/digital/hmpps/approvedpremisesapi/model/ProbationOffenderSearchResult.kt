package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.probationoffendersearchapi.ProbationOffenderDetail

sealed interface ProbationOffenderSearchResult {
  val nomsNumber: String

  sealed interface Success : ProbationOffenderSearchResult {
    data class Full(override val nomsNumber: String, val probationOffenderDetail: ProbationOffenderDetail, val inmateDetail: InmateDetail?) : Success
  }

  data class NotFound(override val nomsNumber: String) : ProbationOffenderSearchResult
  data class Unknown(override val nomsNumber: String, val throwable: Throwable? = null) : ProbationOffenderSearchResult
  data class Forbidden(override val nomsNumber: String, val throwable: Throwable? = null) : ProbationOffenderSearchResult
}
