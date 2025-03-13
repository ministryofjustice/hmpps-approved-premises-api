package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.zalando.problem.AbstractThrowableProblem
import org.zalando.problem.Exceptional
import org.zalando.problem.Status

class NotFoundProblem(id: Any, entityType: String, identifier: String? = null) : AbstractThrowableProblem(null, "Not Found", Status.NOT_FOUND, "No $entityType with ${identifier ?: "an ID"} of $id could be found") {
  override fun getCause(): Exceptional? = null
}
