package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

class BadRequestProblem(
  @JsonIgnore val invalidParams: Map<String, ParamDetails>? = null,
  @JsonIgnore val errorDetail: String? = null,
) : ApiProblem(listOfNotNull("Bad Request", errorDetail, invalidParams).joinToString(": ")) {

  override fun toProblemDetail(): ProblemDetail {
    val detail = errorDetail ?: "There is a problem with your request"

    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail).apply {
      title = "Bad Request"

      if (invalidParams != null) {
        setProperty(
          "invalid-params",
          invalidParams.map {
            ParamError(
              propertyName = it.key,
              errorType = it.value.errorType,
              entityId = it.value.entityId,
              value = it.value.value,
            )
          },
        )
      }
    }
  }
}

data class ParamDetails(
  val errorType: String,
  val entityId: String? = null,
  val value: String? = null,
)

data class ParamError(
  val propertyName: String,
  val errorType: String,
  val entityId: String? = null,
  val value: String? = null,
)
