package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

class ForbiddenProblem(detail: String? = null) : RuntimeException(detail ?: "You are not authorized to access this endpoint") {

  val msg = detail ?: "You are not authorized to access this endpoint"

  fun toProblemDetail(): ProblemDetail {
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, message ?: msg).apply {
      title = "Forbidden"
    }
  }
}
