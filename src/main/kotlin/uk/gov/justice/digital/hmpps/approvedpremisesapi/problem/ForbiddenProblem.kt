package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

class ForbiddenProblem(detail: String? = null) : RuntimeException(detail ?: "You are not authorized to access this endpoint") {

  val msg = detail ?: "You are not authorized to access this endpoint"
  val title = "Forbidden"

  fun toProblemDetail(): ProblemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, message ?: msg).apply {
    title = title
    status = HttpStatus.FORBIDDEN.value()
    detail = msg
  }

  override val message: String?
    get() = "$title: $msg"
}
