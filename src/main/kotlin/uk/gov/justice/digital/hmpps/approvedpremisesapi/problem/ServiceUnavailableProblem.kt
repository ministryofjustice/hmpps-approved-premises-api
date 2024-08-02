package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.zalando.problem.AbstractThrowableProblem
import org.zalando.problem.Exceptional
import org.zalando.problem.Status

class ServiceUnavailableProblem(detail: String) : AbstractThrowableProblem(null, "Service Unavailable", Status.SERVICE_UNAVAILABLE, detail) {
  override fun getCause(): Exceptional? {
    return null
  }
}
