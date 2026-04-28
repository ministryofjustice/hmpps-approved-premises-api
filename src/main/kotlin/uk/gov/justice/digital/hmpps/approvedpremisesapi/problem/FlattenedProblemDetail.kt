package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import com.fasterxml.jackson.annotation.JsonAnyGetter
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

/**
 * FlattenedProblemDetail adds an extra @JsonAnyGetter exposing properties, but ProblemDetail still serialises the properties
 * field itself. That currently leads to duplicated data in responses (e.g. both a top-level invalid-params and also
 * properties.invalid-params).  Therefore, invalid-params appears twice in the JSON returned to the client.
 *
 * The most appropriate place for it is in properties.invalid-params and correct according to the spec, however we currently include
 * invalid-params in the root of the JSON response tree and the UI client will presumably expect it to be in this format.  Consequently
 * leaving it in twice seems the most appropriate thing to do - this meets the spec and guidance but also means we don't need to change
 * existing logic and potentially client (UI) logic.
 */
class FlattenedProblemDetail(status: HttpStatus, detail: String) : ProblemDetail(status.value()) {
  init {
    this.detail = detail
  }

  @JsonAnyGetter
  fun getFlattenedProperties(): Map<String, Any>? = properties
}
