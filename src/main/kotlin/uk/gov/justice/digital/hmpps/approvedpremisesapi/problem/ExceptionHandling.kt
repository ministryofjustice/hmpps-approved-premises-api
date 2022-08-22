package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.context.request.NativeWebRequest
import org.zalando.problem.Problem
import org.zalando.problem.Status
import org.zalando.problem.StatusType
import org.zalando.problem.ThrowableProblem
import org.zalando.problem.spring.common.AdviceTrait
import org.zalando.problem.spring.web.advice.ProblemHandling
import org.zalando.problem.spring.web.advice.io.MessageNotReadableAdviceTrait

@ControllerAdvice
class ExceptionHandling : ProblemHandling, MessageNotReadableAdviceTrait {
  override fun toProblem(throwable: Throwable, status: StatusType): ThrowableProblem? {
    if (throwable is AuthenticationCredentialsNotFoundException) {
      return UnauthenticatedProblem()
    }

    if (throwable is AccessDeniedException) {
      return ForbiddenProblem()
    }

    return AdviceTraitDefault().toProblem(throwable, status)
  }

  override fun handleMessageNotReadableException(
    exception: HttpMessageNotReadableException,
    request: NativeWebRequest
  ): ResponseEntity<Problem> {
    val responseBuilder = Problem.builder()
      .withStatus(Status.BAD_REQUEST)
      .withTitle("Bad Request")

    if (exception.cause is InvalidFormatException) {
      val invalidFormatException = exception.cause as InvalidFormatException
      responseBuilder
        .withDetail("There is a problem with your request")
        .with(
          "invalid-params",
          ParamError(
            propertyName = invalidFormatException.path.joinToString(".") { it.fieldName },
            errorType = invalidFormatException.cause?.message ?: "Unknown problem"
          )
        )
    } else {
      responseBuilder
        .withDetail(exception.message)
    }

    return ResponseEntity<Problem>(responseBuilder.build(), HttpStatus.BAD_REQUEST)
  }
}

private class AdviceTraitDefault : AdviceTrait
