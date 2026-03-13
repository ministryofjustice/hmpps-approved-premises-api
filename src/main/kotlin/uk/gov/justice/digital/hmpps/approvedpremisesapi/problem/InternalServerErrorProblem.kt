package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

class InternalServerErrorProblem(val detail: String) : RuntimeException(detail) {

  val title = "Internal Server Error"

  fun toProblemDetail(): ProblemDetail {
    return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, detail).apply {
      title = title
    }
  }

  override val message: String?
    get() = "$title: $detail"
}
