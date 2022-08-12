package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.zalando.problem.AbstractThrowableProblem
import org.zalando.problem.Exceptional
import org.zalando.problem.Status

class BadRequestProblem(private val invalidParams: Map<String, String>? = null, private val errorDetail: String? = null) :
  AbstractThrowableProblem(null, "Bad Request", Status.BAD_REQUEST, errorDetail ?: "There is a problem with your request") {
  override fun getCause(): Exceptional? {
    return null
  }

  override fun getParameters(): MutableMap<String, Any> {
    return mutableMapOf(
      "invalid-params" to (invalidParams ?: emptyMap<String, Any>())
    )
  }
}
