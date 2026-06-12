package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

class ServiceUnavailableProblem(detail: String) : ApiProblem(detail) {

  override fun toProblemDetail(): ProblemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, message).apply {
    title = "Service Unavailable"
  }
}
