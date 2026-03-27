package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import com.fasterxml.jackson.annotation.JsonAnyGetter
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

class FlattenedProblemDetail(status: HttpStatus, detail: String) : ProblemDetail(status.value()) {
  init {
    this.detail = detail
  }

  @JsonAnyGetter
  fun getFlattenedProperties(): Map<String, Any>? = properties
}
