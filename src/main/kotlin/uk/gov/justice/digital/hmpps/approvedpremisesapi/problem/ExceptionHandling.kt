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
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import org.springframework.web.util.ContentCachingRequestWrapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DeserializationValidationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isTypeInThrowableChain

@ControllerAdvice
class ExceptionHandling(
  private val objectMapper: ObjectMapper,
  private val deserializationValidationService: DeserializationValidationService,
  private val sentryService: SentryService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @ExceptionHandler(AuthenticationCredentialsNotFoundException::class)
  fun handleAuthenticationCredentialsNotFoundException(ex: AuthenticationCredentialsNotFoundException): ResponseEntity<ProblemDetail> {
    Sentry.captureException(ex)
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(UnauthenticatedProblem().toProblemDetail())
  }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(ex: AccessDeniedException): ResponseEntity<ProblemDetail> {
    Sentry.captureException(ex)
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ForbiddenProblem().toProblemDetail())
  }

  @ExceptionHandler(NoResourceFoundException::class)
  fun handleNoResourceFoundException(ex: NoResourceFoundException): ResponseEntity<ProblemDetail> {
    Sentry.captureException(ex)
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(NotFoundResourceProblem().toProblemDetail())
  }

  @ExceptionHandler(MissingRequestHeaderException::class)
  fun handleMissingRequestHeaderException(ex: MissingRequestHeaderException): ResponseEntity<ProblemDetail> {
    Sentry.captureException(ex)
    val problem = BadRequestProblem(errorDetail = "Missing required header ${ex.headerName}")
    logBadRequestProblem(problem)
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem.toProblemDetail())
  }

  @ExceptionHandler(MissingServletRequestParameterException::class)
  fun handleMissingServletRequestParameterException(ex: MissingServletRequestParameterException): ResponseEntity<ProblemDetail> {
    Sentry.captureException(ex)
    val problem = BadRequestProblem(errorDetail = "Missing required query parameter ${ex.parameterName}")
    logBadRequestProblem(problem)
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem.toProblemDetail())
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  fun handleMethodArgumentTypeMismatchException(ex: MethodArgumentTypeMismatchException): ResponseEntity<ProblemDetail> {
    Sentry.captureException(ex)
    val problem = BadRequestProblem(
      errorDetail = "Invalid type for query parameter ${ex.parameter.parameterName} expected ${ex.parameter.parameterType.name}"
    )
    logBadRequestProblem(problem)
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem.toProblemDetail())
  }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleMessageNotReadableException(
    exception: HttpMessageNotReadableException,
    request: NativeWebRequest,
  ): ResponseEntity<ProblemDetail> {
    when (exception.cause) {
      is MismatchedInputException -> {
        val mismatchedInputException = exception.cause as MismatchedInputException

        val requestBody = request.getNativeRequest(ContentCachingRequestWrapper::class.java)
        val jsonTree = objectMapper.readTree(String(requestBody!!.contentAsByteArray))

        if (expectedArrayButGotObject(jsonTree, mismatchedInputException)) {
          val problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Expected an array but got an object")
          problemDetail.title = "Bad Request"
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail)
        }

        if (expectedObjectButGotArray(jsonTree, mismatchedInputException)) {
          val problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Expected an object but got an array")
          problemDetail.title = "Bad Request"
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail)
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

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(badRequestProblem.toProblemDetail())
      }
      else -> {
        val problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.message ?: "Bad Request")
        problemDetail.title = "Bad Request"
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail)
      }
    }
  }

  @ExceptionHandler(BadRequestProblem::class)
  fun handleBadRequestProblem(ex: BadRequestProblem): ResponseEntity<ProblemDetail> {
    Sentry.captureException(ex)
    logBadRequestProblem(ex)
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.toProblemDetail())
  }

  @ExceptionHandler(NotFoundProblem::class)
  fun handleNotFoundProblem(ex: NotFoundProblem): ResponseEntity<ProblemDetail> {
    Sentry.captureException(ex)
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.toProblemDetail())
  }

  @ExceptionHandler(ForbiddenProblem::class)
  fun handleForbiddenProblem(ex: ForbiddenProblem): ResponseEntity<ProblemDetail> {
    Sentry.captureException(ex)
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.toProblemDetail())
  }

  @ExceptionHandler(ConflictProblem::class)
  fun handleConflictProblem(ex: ConflictProblem): ResponseEntity<ProblemDetail> {
    Sentry.captureException(ex)
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.toProblemDetail())
  }

  @ExceptionHandler(NotAllowedProblem::class)
  fun handleNotAllowedProblem(ex: NotAllowedProblem): ResponseEntity<ProblemDetail> {
    Sentry.captureException(ex)
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ex.toProblemDetail())
  }

  @ExceptionHandler(NotFoundResourceProblem::class)
  fun handleNotFoundResourceProblem(ex: NotFoundResourceProblem): ResponseEntity<ProblemDetail> {
    Sentry.captureException(ex)
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.toProblemDetail())
  }

  @ExceptionHandler(ServiceUnavailableProblem::class)
  fun handleServiceUnavailableProblem(ex: ServiceUnavailableProblem): ResponseEntity<ProblemDetail> {
    Sentry.captureException(ex)
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ex.toProblemDetail())
  }

  @ExceptionHandler(NotImplementedProblem::class)
  fun handleNotImplementedProblem(ex: NotImplementedProblem): ResponseEntity<ProblemDetail> {
    Sentry.captureException(ex)
    logNotImplementedProblem(ex)
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(ex.toProblemDetail())
  }

  @ExceptionHandler(UnhandledExceptionProblem::class)
  fun handleUnhandledExceptionProblem(ex: UnhandledExceptionProblem): ResponseEntity<ProblemDetail> {
    logInternalServerErrorProblem(ex)
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.toProblemDetail())
  }

  @ExceptionHandler(InternalServerErrorProblem::class)
  fun handleInternalServerErrorProblem(ex: InternalServerErrorProblem): ResponseEntity<ProblemDetail> {
    logInternalServerErrorProblem(ex)
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.toProblemDetail())
  }

  @ExceptionHandler(JDBCConnectionException::class)
  fun handleJDBCConnectionException(ex: JDBCConnectionException): ResponseEntity<ProblemDetail> {
    sentryService.captureException(ex)
    val problem = ServiceUnavailableProblem(detail = "Error acquiring a database connection")
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem.toProblemDetail())
  }

  @ExceptionHandler(Throwable::class)
  fun handleGenericException(ex: Throwable): ResponseEntity<ProblemDetail> {
    // Check if JDBCConnectionException is in the cause chain
    if (isTypeInThrowableChain(ex, JDBCConnectionException::class.java)) {
      sentryService.captureException(ex)
      val problem = ServiceUnavailableProblem(detail = "Error acquiring a database connection")
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem.toProblemDetail())
    }

    log.error("Unhandled exception type, returning generic 500 response", ex)
    sentryService.captureException(ex)

    val problem = UnhandledExceptionProblem(detail = "There was an unexpected problem")
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem.toProblemDetail())
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

  private fun logBadRequestProblem(problem: BadRequestProblem) = log.error("Bad Request. Error Detail: ${problem.errorDetail ?: "None"}, Invalid Params: [${problem.invalidParams?.entries?.joinToString(",") { "${it.key}=${it.value}" } ?: "None"}]")

  private fun logInternalServerErrorProblem(ex: RuntimeException) {
    log.error("Internal Server Error. Error Detail: ${ex.message ?: "None"}")
    sentryService.captureException(ex)
  }

  private fun logNotImplementedProblem(problem: NotImplementedProblem) {
    log.error("Not Implemented. Error Detail: ${problem.message ?: "None"}")
    sentryService.captureException(problem)
  }

  private fun expectedArrayButGotObject(jsonNode: JsonNode, mismatchedInputException: MismatchedInputException) = jsonNode is ObjectNode && isInputTypeArray(mismatchedInputException)
  private fun expectedObjectButGotArray(jsonNode: JsonNode, mismatchedInputException: MismatchedInputException) = jsonNode is ArrayNode && !isInputTypeArray(mismatchedInputException)
  private fun rootIsArray(mismatchedInputException: MismatchedInputException) = mismatchedInputException.path[0].from !is Class<*>
}
