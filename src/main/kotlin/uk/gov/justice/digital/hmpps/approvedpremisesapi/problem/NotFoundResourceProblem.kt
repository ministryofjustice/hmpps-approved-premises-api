package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.zalando.problem.AbstractThrowableProblem
import org.zalando.problem.Exceptional
import org.zalando.problem.Status

class NotFoundResourceProblem : AbstractThrowableProblem(null, "Not Found", Status.NOT_FOUND, "Resource not found") {
  override fun getCause(): Exceptional? = null
}
