package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

class NotFoundProblem(id: Any, entityType: String, identifier: String? = null) : ApiProblem("No $entityType with ${identifier ?: "an ID"} of $id could be found") {

  val title = "Not Found"

  override fun toProblemDetail(): ProblemDetail = ProblemDetail.forStatusAndDetail(
    HttpStatus.NOT_FOUND,
    message,
  ).apply {
    title = this@NotFoundProblem.title
    status = HttpStatus.NOT_FOUND.value()
    detail = message
  }
}
