package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem

import org.springframework.http.ProblemDetail

abstract class ApiProblem(override val message: String) : RuntimeException(message) {
  abstract fun toProblemDetail(): ProblemDetail?
}
