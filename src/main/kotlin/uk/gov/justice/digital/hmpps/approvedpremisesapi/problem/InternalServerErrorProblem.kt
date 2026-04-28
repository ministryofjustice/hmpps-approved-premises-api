package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

class InternalServerErrorProblem(detail: String) : ApiProblem(detail) {

  val title = "Internal Server Error"

  override fun toProblemDetail(): ProblemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, message).apply {
    title = this@InternalServerErrorProblem.title
  }
}
