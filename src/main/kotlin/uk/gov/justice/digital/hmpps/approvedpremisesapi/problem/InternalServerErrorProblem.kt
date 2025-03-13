package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.zalando.problem.AbstractThrowableProblem
import org.zalando.problem.Exceptional
import org.zalando.problem.Status

class InternalServerErrorProblem(detail: String) : AbstractThrowableProblem(null, "Internal Server Error", Status.INTERNAL_SERVER_ERROR, detail) {
  override fun getCause(): Exceptional? = null
}
