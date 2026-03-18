package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

class NotAllowedProblem(val detail: String) : RuntimeException(detail) {

  fun toProblemDetail(): ProblemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.METHOD_NOT_ALLOWED, detail).apply {
    title = "Not Allowed"
  }
}
