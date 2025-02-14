package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.zalando.problem.AbstractThrowableProblem
import org.zalando.problem.Exceptional
import org.zalando.problem.Status

class NotImplementedProblem(detail: String) : AbstractThrowableProblem(null, "Not Implemented", Status.NOT_IMPLEMENTED, detail) {
  override fun getCause(): Exceptional? = null
}
