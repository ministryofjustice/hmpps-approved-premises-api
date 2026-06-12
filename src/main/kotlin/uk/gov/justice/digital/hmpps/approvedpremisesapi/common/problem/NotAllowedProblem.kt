package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

class NotAllowedProblem(detail: String) : ApiProblem(detail) {

  override fun toProblemDetail(): ProblemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.METHOD_NOT_ALLOWED, message).apply {
    title = "Not Allowed"
  }
}
