package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.zalando.problem.AbstractThrowableProblem
import org.zalando.problem.Exceptional
import org.zalando.problem.Status

class UnauthenticatedProblem() : AbstractThrowableProblem(null, "Unauthenticated", Status.UNAUTHORIZED, "A valid HMPPS Auth JWT must be supplied via bearer authentication to access this endpoint") {
  override fun getCause(): Exceptional? {
    return null
  }
}
