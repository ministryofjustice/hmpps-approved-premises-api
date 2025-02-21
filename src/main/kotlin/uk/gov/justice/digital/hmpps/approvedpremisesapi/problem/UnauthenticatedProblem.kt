package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.zalando.problem.AbstractThrowableProblem
import org.zalando.problem.Exceptional
import org.zalando.problem.Status

class UnauthenticatedProblem(detail: String = "A valid HMPPS Auth JWT must be supplied via bearer authentication to access this endpoint") : AbstractThrowableProblem(null, "Unauthenticated", Status.UNAUTHORIZED, detail) {
  override fun getCause(): Exceptional? = null
}
