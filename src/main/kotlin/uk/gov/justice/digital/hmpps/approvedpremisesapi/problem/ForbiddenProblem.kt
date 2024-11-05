package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.zalando.problem.AbstractThrowableProblem
import org.zalando.problem.Exceptional
import org.zalando.problem.Status

class ForbiddenProblem(detail: String? = null) :
  AbstractThrowableProblem(
    null,
    "Forbidden",
    Status.FORBIDDEN,
    "You are not authorized to access this endpoint" + if (detail != null) {
      " $detail"
    } else {
      ""
    },
  ) {
  override fun getCause(): Exceptional? {
    return null
  }
}
