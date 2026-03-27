package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

class ServiceUnavailableProblem(val detail: String) : RuntimeException(detail) {

  fun toProblemDetail(): ProblemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, detail).apply {
    title = "Service Unavailable"
  }
}
