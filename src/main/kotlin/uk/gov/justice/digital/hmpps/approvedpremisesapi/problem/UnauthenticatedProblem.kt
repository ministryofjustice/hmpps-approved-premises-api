package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

class UnauthenticatedProblem(val detail: String = "A valid HMPPS Auth JWT must be supplied via bearer authentication to access this endpoint") :
  RuntimeException(detail) {

  fun toProblemDetail(): ProblemDetail {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, message ?: detail).apply {
      title = "Unauthenticated"
    }
  }
}
