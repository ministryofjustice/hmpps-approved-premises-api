package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

class NotFoundResourceProblem : RuntimeException("Resource not found") {

  fun toProblemDetail(): ProblemDetail {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Resource not found").apply {
      title = "Not Found"
    }
  }
}
