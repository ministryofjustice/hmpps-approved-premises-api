package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.zalando.problem.AbstractThrowableProblem
import org.zalando.problem.Exceptional
import org.zalando.problem.Status

class NotAllowedProblem(detail: String) : AbstractThrowableProblem(null, "Not Allowed", Status.METHOD_NOT_ALLOWED, detail) {
  override fun getCause(): Exceptional? = null
}
