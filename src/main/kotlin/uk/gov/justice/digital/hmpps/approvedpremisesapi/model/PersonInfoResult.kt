package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail

sealed interface PersonInfoResult {
  val crn: String

  sealed interface Success : PersonInfoResult {
    data class Full(override val crn: String, val offenderDetailSummary: OffenderDetailSummary, val inmateDetail: InmateDetail?) : Success
    data class Restricted(override val crn: String, val nomsNumber: String?) : Success
  }

  data class NotFound(override val crn: String) : PersonInfoResult
}
