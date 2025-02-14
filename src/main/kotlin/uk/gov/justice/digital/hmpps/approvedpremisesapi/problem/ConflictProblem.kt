package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.zalando.problem.AbstractThrowableProblem
import org.zalando.problem.Exceptional
import org.zalando.problem.Status

class ConflictProblem(id: Any, conflictReason: String) : AbstractThrowableProblem(null, "Conflict", Status.CONFLICT, "$conflictReason: $id") {
  override fun getCause(): Exceptional? = null
}
