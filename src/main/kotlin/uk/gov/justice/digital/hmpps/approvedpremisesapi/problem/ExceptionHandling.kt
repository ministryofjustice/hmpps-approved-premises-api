package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.sentry.Sentry
import org.hibernate.exception.JDBCConnectionException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.util.ContentCachingRequestWrapper
import org.zalando.problem.Problem
import org.zalando.problem.Status
import org.zalando.problem.StatusType
import org.zalando.problem.ThrowableProblem
import org.zalando.problem.spring.web.advice.ProblemHandling
import org.zalando.problem.spring.web.advice.io.MessageNotReadableAdviceTrait
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DeserializationValidationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isTypeInThrowableChain

@ControllerAdvice
class ExceptionHandling(
  private val objectMapper: ObjectMapper,
  private val deserializationValidationService: DeserializationValidationService,
  private val sentryService: SentryService,
) : ProblemHandling, MessageNotReadableAdviceTrait {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun toProblem(throwable: Throwable, status: StatusType): ThrowableProblem? {
    Sentry.captureException(throwable)

    if (throwable is AuthenticationCredentialsNotFoundException) {
      return UnauthenticatedProblem()
    }

    if (throwable is AccessDeniedException) {
      return ForbiddenProblem()
    }

    if (throwable is MissingRequestHeaderException) {
      return BadRequestProblem(
        errorDetail = "Missing required header ${throwable.headerName}",
      )
    }

    if (throwable is MissingServletRequestParameterException) {
      return BadRequestProblem(
        errorDetail = "Missing required query parameter ${throwable.parameterName}",
      )
    }

    if (throwable is MethodArgumentTypeMismatchException) {
      return BadRequestProblem(
        errorDetail = "Invalid type for query parameter ${throwable.parameter.parameterName} expected ${throwable.parameter.parameterType.name}",
      )
    }

    if (isTypeInThrowableChain(throwable, JDBCConnectionException::class.java)) {
      sentryService.captureException(throwable)
      return ServiceUnavailableProblem(
        detail = "Error acquiring a database connection",
      )
    }

    log.error("Unhandled exception type, returning generic 500 response", throwable)

    sentryService.captureException(throwable)

    // We throw this instead of an InternalServerErrorProblem to avoid the Sentry Alert
    // being raised again by [ExceptionHandling.logInternalServerErrorProblem]
    return UnhandledExceptionProblem(
      detail = "There was an unexpected problem",
    )
  }

  override fun handleMessageNotReadableException(
    exception: HttpMessageNotReadableException,
    request: NativeWebRequest,
  ): ResponseEntity<Problem> {
    val responseBuilder = Problem.builder()
      .withStatus(Status.BAD_REQUEST)
      .withTitle("Bad Request")

    when (exception.cause) {
      is MismatchedInputException -> {
        val mismatchedInputException = exception.cause as MismatchedInputException

        val requestBody = request.getNativeRequest(ContentCachingRequestWrapper::class.java)
        val jsonTree = objectMapper.readTree(String(requestBody.contentAsByteArray))

        if (expectedArrayButGotObject(jsonTree, mismatchedInputException)) {
          responseBuilder.withDetail("Expected an array but got an object")
          return ResponseEntity<Problem>(responseBuilder.build(), HttpStatus.BAD_REQUEST)
        }

        if (expectedObjectButGotArray(jsonTree, mismatchedInputException)) {
          responseBuilder.withDetail("Expected an object but got an array")
          return ResponseEntity<Problem>(responseBuilder.build(), HttpStatus.BAD_REQUEST)
        }

        val badRequestProblem = if (rootIsArray(mismatchedInputException)) {
          val arrayItemsType = (mismatchedInputException.path[1].from as Class<*>).kotlin

          BadRequestProblem(
            invalidParams = deserializationValidationService.validateArray(
              targetType = arrayItemsType,
              jsonArray = jsonTree as ArrayNode,
            ),
          )
        } else {
          val objectType = (mismatchedInputException.path[0].from as Class<*>).kotlin

          BadRequestProblem(
            invalidParams = deserializationValidationService.validateObject(
              targetType = objectType,
              jsonObject = jsonTree as ObjectNode,
            ),
          )
        }

        logBadRequestProblem(badRequestProblem)

        return ResponseEntity(badRequestProblem, HttpStatus.BAD_REQUEST)
      }
      else ->
        responseBuilder.withDetail(exception.message)
    }

    return ResponseEntity<Problem>(responseBuilder.build(), HttpStatus.BAD_REQUEST)
  }

  private fun isInputTypeArray(mismatchedInputException: MismatchedInputException): Boolean {
    if (mismatchedInputException.path.isEmpty()) {
      return deserializationValidationService.isArrayType(mismatchedInputException.targetType)
    }

    if (mismatchedInputException.path.first().from is Class<*>) {
      return deserializationValidationService.isArrayType(mismatchedInputException.path.first().from as Class<*>)
    }

    return true
  }

  override fun log(throwable: Throwable, problem: Problem, request: NativeWebRequest, status: HttpStatus) {
    when (problem) {
      is BadRequestProblem -> logBadRequestProblem(problem)
      is InternalServerErrorProblem -> logInternalServerErrorProblem(problem)
      is NotImplementedProblem -> logNotImplementedProblem(problem)
      else -> {
        super<ProblemHandling>.log(throwable, problem, request, status)
      }
    }
  }

  private fun logBadRequestProblem(problem: BadRequestProblem) = log.error("Bad Request. Error Detail: ${problem.errorDetail ?: "None"}, Invalid Params: [${problem.invalidParams?.entries?.joinToString(",") { "${it.key}=${it.value}" } ?: "None"}]")

  private fun logInternalServerErrorProblem(problem: InternalServerErrorProblem) {
    log.error("Internal Server Error. Error Detail: ${problem.detail ?: "None"}")
    sentryService.captureException(problem)
  }

  private fun logNotImplementedProblem(problem: NotImplementedProblem) {
    log.error("Not Implemented. Error Detail: ${problem.detail ?: "None"}")
    sentryService.captureException(problem)
  }

  private fun expectedArrayButGotObject(jsonNode: JsonNode, mismatchedInputException: MismatchedInputException) = jsonNode is ObjectNode && isInputTypeArray(mismatchedInputException)
  private fun expectedObjectButGotArray(jsonNode: JsonNode, mismatchedInputException: MismatchedInputException) = jsonNode is ArrayNode && !isInputTypeArray(mismatchedInputException)
  private fun rootIsArray(mismatchedInputException: MismatchedInputException) = mismatchedInputException.path[0].from !is Class<*>
}
