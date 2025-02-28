package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary

/**
 * Provides offender details retrieved from delius
 */
sealed interface PersonSummaryInfoResult {
  val crn: String

  sealed interface Success : PersonSummaryInfoResult {
    data class Full(override val crn: String, val summary: CaseSummary) : Success

    /**
     * Indicates that the person is a Limited Access Offender, and the calling user
     * does not have access to their record
     */
    data class Restricted(override val crn: String, val nomsNumber: String?) : Success
  }

  data class NotFound(override val crn: String) : PersonSummaryInfoResult
  data class Unknown(override val crn: String, val throwable: Throwable? = null) : PersonSummaryInfoResult
}

fun List<PersonSummaryInfoResult>.forCrn(crn: String) = this.first { it.crn == crn }
