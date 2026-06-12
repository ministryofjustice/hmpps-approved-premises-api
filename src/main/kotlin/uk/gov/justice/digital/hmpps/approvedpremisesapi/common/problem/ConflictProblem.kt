package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

class ConflictProblem(id: Any, conflictReason: String) : ApiProblem("$conflictReason: $id") {

  override fun toProblemDetail(): ProblemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, message).apply {
    title = "Conflict"
  }
}
