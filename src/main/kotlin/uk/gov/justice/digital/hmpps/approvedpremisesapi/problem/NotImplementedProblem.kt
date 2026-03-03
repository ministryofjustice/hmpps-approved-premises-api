package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

class NotImplementedProblem(val detail: String) : RuntimeException(detail) {

  fun toProblemDetail(): ProblemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_IMPLEMENTED, detail).apply {
    title = "Not Implemented"
  }
}
