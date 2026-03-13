package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

class NotFoundProblem(id: Any, entityType: String, identifier: String? = null) :
  RuntimeException("No $entityType with ${identifier ?: "an ID"} of $id could be found") {

  val title = "Not Found"
  val msg = "No $entityType with ${identifier ?: "an ID"} of $id could be found"

  fun toProblemDetail(): ProblemDetail {
    return ProblemDetail.forStatusAndDetail(
      HttpStatus.NOT_FOUND,
      message ?: "Not Found"
    ).apply {
      title = title
      status = HttpStatus.NOT_FOUND.value()
      detail = msg
    }
  }

  override val message: String?
    get() = "$title: $msg"

}
