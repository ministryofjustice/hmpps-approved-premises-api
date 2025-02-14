package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import com.fasterxml.jackson.annotation.JsonIgnore
import org.zalando.problem.AbstractThrowableProblem
import org.zalando.problem.Exceptional
import org.zalando.problem.Status

class BadRequestProblem(
  @JsonIgnore val invalidParams: Map<String, String>? = null,
  @JsonIgnore val errorDetail: String? = null,
) : AbstractThrowableProblem(null, "Bad Request", Status.BAD_REQUEST, errorDetail ?: "There is a problem with your request") {
  override fun getCause(): Exceptional? = null

  override val message: String
    get() = listOfNotNull(this.title, this.detail, this.invalidParams).joinToString(": ")

  override fun getParameters(): MutableMap<String, Any> = mutableMapOf(
    "invalid-params" to (
      invalidParams?.map {
        ParamError(
          propertyName = it.key,
          errorType = it.value,
        )
      } ?: emptyList()
      ),
  )
}

data class ParamError(
  val propertyName: String,
  val errorType: String,
)
