package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

class NotImplementedProblem(detail: String) : ApiProblem(detail) {

  override fun toProblemDetail(): ProblemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_IMPLEMENTED, message).apply {
    title = "Not Implemented"
  }
}
