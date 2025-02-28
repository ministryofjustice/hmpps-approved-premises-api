package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail

/**
 * Provides offender details retrieved from delius and inmate details retrieved from NOMIS
 */
sealed interface PersonInfoResult {
  val crn: String

  sealed interface Success : PersonInfoResult {
    data class Full(override val crn: String, val offenderDetailSummary: OffenderDetailSummary, val inmateDetail: InmateDetail?) : Success

    /**
     * Indicates that the person is a Limited Access Offender, and the calling user
     * does not have access to their record
     */
    data class Restricted(override val crn: String, val nomsNumber: String?) : Success
  }

  data class NotFound(override val crn: String) : PersonInfoResult
  data class Unknown(override val crn: String, val throwable: Throwable? = null) : PersonInfoResult
}
