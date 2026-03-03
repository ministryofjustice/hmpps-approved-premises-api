package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

class ConflictProblem(id: Any, conflictReason: String) :
  RuntimeException("$conflictReason: $id") {

  val msg = "$conflictReason: $id"

  fun toProblemDetail(): ProblemDetail {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, message ?: msg).apply {
      title = "Conflict"
    }
  }
}
