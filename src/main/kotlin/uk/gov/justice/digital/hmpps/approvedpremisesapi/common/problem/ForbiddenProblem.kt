package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

class ForbiddenProblem(detail: String? = null) : ApiProblem(detail ?: "You are not authorized to access this endpoint") {

  val title = "Forbidden"

  override fun toProblemDetail(): ProblemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, message).apply {
    title = this@ForbiddenProblem.title
    status = HttpStatus.FORBIDDEN.value()
    detail = message
  }
}
