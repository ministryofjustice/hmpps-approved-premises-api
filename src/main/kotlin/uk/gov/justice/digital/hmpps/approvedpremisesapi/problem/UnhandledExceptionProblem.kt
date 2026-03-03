package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

class UnhandledExceptionProblem(detail: String) : RuntimeException(detail) {

  fun toProblemDetail(): ProblemDetail {
    return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, message ?: "There was an unexpected problem").apply {
      title = "Internal Server Error"
    }
  }
}
