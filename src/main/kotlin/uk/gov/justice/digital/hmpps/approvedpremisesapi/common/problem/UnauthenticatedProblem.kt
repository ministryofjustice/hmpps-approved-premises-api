package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

class UnauthenticatedProblem(detail: String = "A valid HMPPS Auth JWT must be supplied via bearer authentication to access this endpoint") : ApiProblem(detail) {

  override fun toProblemDetail(): ProblemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, message).apply {
    title = "Unauthenticated"
  }
}
