package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.zalando.problem.AbstractThrowableProblem
import org.zalando.problem.Exceptional
import org.zalando.problem.Status

class ConflictProblem(id: Any, entityType: String, conflictReason: String) : AbstractThrowableProblem(null, "Conflict", Status.CONFLICT, "A $entityType already exists for $conflictReason: $id") {
  override fun getCause(): Exceptional? {
    return null
  }
}
