package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

class NotFoundResourceProblem : ApiProblem("Resource not found") {

  override fun toProblemDetail(): ProblemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Resource not found").apply {
    title = "Not Found"
  }
}
