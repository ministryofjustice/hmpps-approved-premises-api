package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.problem

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.exception.JDBCConnectionException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.core.MethodParameter
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import org.springframework.web.util.ContentCachingRequestWrapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ExceptionHandling
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundResourceProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ParamDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ServiceUnavailableProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.UnauthenticatedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.UnhandledExceptionProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DeserializationValidationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.LoggerExtension
import java.sql.SQLException
import org.springframework.security.access.AccessDeniedException as SpringAccessDeniedException

class ExceptionHandlingTest {
  private val mockJsonMapper = mockk<JsonMapper>()
  private val mockDeserializationValidationService = mockk<DeserializationValidationService>()
  private val mockSentryService = mockk<SentryService>(relaxed = true)

  val exceptionHandling = ExceptionHandling(mockJsonMapper, mockDeserializationValidationService, mockSentryService)

  @RegisterExtension
  var loggerExtension: LoggerExtension = LoggerExtension()

  @Test
  fun `Returns UnauthenticatedProblem when exception is AuthenticationCredentialsNotFoundException and exception captured in Sentry`() {
    val throwable = AuthenticationCredentialsNotFoundException("")

    val problem = exceptionHandling.handleAuthenticationCredentialsNotFoundException(throwable).body!!

    val expectedProblem = UnauthenticatedProblem().toProblemDetail()

    assertThat(problem.type).isEqualTo(expectedProblem.type)
    assertThat(problem.title).isEqualTo(expectedProblem.title)
    assertThat(problem.status).isEqualTo(expectedProblem.status)
    assertThat(problem.detail).isEqualTo(expectedProblem.detail)

    verify { mockSentryService.captureException(throwable) }
  }

  @Test
  fun `Returns ForbiddenProblem problem when exception is AccessDeniedException and exception captured in Sentry`() {
    val throwable = SpringAccessDeniedException("")

    val problem = exceptionHandling.handleAccessDeniedException(throwable).body!!

    val expectedProblem = ForbiddenProblem().toProblemDetail()

    assertThat(problem.type).isEqualTo(expectedProblem.type)
    assertThat(problem.title).isEqualTo(expectedProblem.title)
    assertThat(problem.status).isEqualTo(expectedProblem.status)
    assertThat(problem.detail).isEqualTo(expectedProblem.detail)

    verify { mockSentryService.captureException(throwable) }
  }

  @Test
  fun `Returns NotFoundResourceProblem problem when exception is NoResourceFoundException and exception captured in Sentry`() {
    val throwable = NoResourceFoundException(HttpMethod.GET, "", "")

    val problem = exceptionHandling.handleNoResourceFoundException(throwable).body!!

    val expectedProblem = NotFoundResourceProblem().toProblemDetail()

    assertThat(problem!!.type).isEqualTo(expectedProblem.type)
    assertThat(problem.title).isEqualTo(expectedProblem.title)
    assertThat(problem.status).isEqualTo(expectedProblem.status)
    assertThat(problem.detail).isEqualTo(expectedProblem.detail)

    verify { mockSentryService.captureException(throwable) }
  }

  @Test
  fun `Returns BadRequestProblem problem when exception is MissingRequestHeaderException and exception captured in Sentry`() {
    val throwable = MissingRequestHeaderException("Authorisation", mockk<MethodParameter>())

    val problem = exceptionHandling.handleMissingRequestHeaderException(throwable).body!!

    val expectedProblem = BadRequestProblem().toProblemDetail()

    assertThat(problem.type).isEqualTo(expectedProblem.type)
    assertThat(problem.title).isEqualTo(expectedProblem.title)
    assertThat(problem.status).isEqualTo(expectedProblem.status)
    assertThat(problem.detail).isEqualTo("Missing required header Authorisation")

    verify { mockSentryService.captureException(throwable) }
  }

  @Test
  fun `Returns BadRequestProblem problem when exception is MissingServletRequestParameterException and exception captured in Sentry`() {
    val throwable = MissingServletRequestParameterException("id", "")

    val problem = exceptionHandling.handleMissingServletRequestParameterException(throwable).body!!

    val expectedProblem = BadRequestProblem().toProblemDetail()

    assertThat(problem.type).isEqualTo(expectedProblem.type)
    assertThat(problem.title).isEqualTo(expectedProblem.title)
    assertThat(problem.status).isEqualTo(expectedProblem.status)
    assertThat(problem.detail).isEqualTo("Missing required query parameter id")

    verify { mockSentryService.captureException(throwable) }
  }

  @Test
  fun `Returns BadRequestProblem problem when exception is MethodArgumentTypeMismatchException and exception captured in Sentry`() {
    val methodParameter = mockk<MethodParameter>()
    every { methodParameter.parameterName } returns "id"
    every { methodParameter.parameterType } returns Int::class.java

    val throwable = MethodArgumentTypeMismatchException(null, null, "", methodParameter, null)

    val problem = exceptionHandling.handleMethodArgumentTypeMismatchException(throwable).body!!

    val expectedProblem = BadRequestProblem().toProblemDetail()

    assertThat(problem.type).isEqualTo(expectedProblem.type)
    assertThat(problem.title).isEqualTo(expectedProblem.title)
    assertThat(problem.status).isEqualTo(expectedProblem.status)
    assertThat(problem.detail).isEqualTo("Invalid type for query parameter id expected int")

    verify { mockSentryService.captureException(throwable) }
  }

  @Test
  fun `Returns ServiceUnavailableProblem problem when exception is cause chain of JDBCConnectionException and exception captured in Sentry`() {
    val throwable = RuntimeException("wrapper", JDBCConnectionException("", SQLException()))

    val problem = exceptionHandling.handleGenericException(throwable).body!!

    val expectedProblem = ServiceUnavailableProblem("Error acquiring a database connection").toProblemDetail()

    assertThat(problem.type).isEqualTo(expectedProblem.type)
    assertThat(problem.title).isEqualTo(expectedProblem.title)
    assertThat(problem.status).isEqualTo(expectedProblem.status)
    assertThat(problem.detail).isEqualTo("Error acquiring a database connection")

    verify { mockSentryService.captureException(throwable) }
  }

  @Test
  fun `Returns UnhandledExceptionProblem problem when exception is unhandled (IllegalStateException), logs error and exception captured in Sentry`() {
    val throwable = IllegalStateException()

    val problem = exceptionHandling.handleGenericException(throwable).body!!

    val expectedProblem = UnhandledExceptionProblem("There was an unexpected problem").toProblemDetail()

    assertThat(problem.type).isEqualTo(expectedProblem.type)
    assertThat(problem.title).isEqualTo(expectedProblem.title)
    assertThat(problem.status).isEqualTo(expectedProblem.status)
    assertThat(problem.detail).isEqualTo("There was an unexpected problem")

    loggerExtension.assertContains("Unhandled exception type, returning generic 500 response")

    verify { mockSentryService.captureException(throwable) }
  }

  @Test
  fun `Returns ResponseEntity_BadRequestProblem_ problem when exception is HttpMessageNotReadableException caused by MismatchedInputException and JSON node is object and should be array`() {
    val cause = mockk<MismatchedInputException>()
    every { cause.path } returns emptyList()
    every { cause.targetType } returns List::class.java
    every { mockDeserializationValidationService.isArrayType(List::class.java) } returns true

    val throwable = HttpMessageNotReadableException("", cause, mockk<HttpInputMessage>())

    val mockContentCachingRequestWrapper = mockk<ContentCachingRequestWrapper>()
    every { mockContentCachingRequestWrapper.contentAsByteArray } returns "{}".toByteArray()

    val mockRequest = mockk<NativeWebRequest>()
    every { mockRequest.getNativeRequest(ContentCachingRequestWrapper::class.java) } returns mockContentCachingRequestWrapper

    every { mockJsonMapper.readTree(any<String>()) } returns mockk<ObjectNode>()

    val response = exceptionHandling.handleMessageNotReadableException(throwable, mockRequest)

    assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    assertThat(response.body!!.title).isEqualTo("Bad Request")
    assertThat(response.body!!.detail).isEqualTo("Expected an array but got an object")
  }

  @Test
  fun `Returns ResponseEntity_BadRequestProblem_ problem when exception is HttpMessageNotReadableException caused by MismatchedInputException and JSON node is array and should be object`() {
    val cause = mockk<MismatchedInputException>()
    every { cause.path } returns emptyList()
    every { cause.targetType } returns List::class.java
    every { mockDeserializationValidationService.isArrayType(List::class.java) } returns false

    val throwable = HttpMessageNotReadableException("", cause, mockk<HttpInputMessage>())

    val mockContentCachingRequestWrapper = mockk<ContentCachingRequestWrapper>()
    every { mockContentCachingRequestWrapper.contentAsByteArray } returns "{}".toByteArray()

    val mockRequest = mockk<NativeWebRequest>()
    every { mockRequest.getNativeRequest(ContentCachingRequestWrapper::class.java) } returns mockContentCachingRequestWrapper

    every { mockJsonMapper.readTree(any<String>()) } returns mockk<ArrayNode>()

    val response = exceptionHandling.handleMessageNotReadableException(throwable, mockRequest)

    assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    assertThat(response.body?.title).isEqualTo("Bad Request")
    assertThat(response.body?.detail).isEqualTo("Expected an object but got an array")
  }

  @Test
  fun `Returns ResponseEntity_BadRequestProblem_ problem when exception is HttpMessageNotReadableException caused by MismatchedInputException and root is array`() {
    val cause = mockk<MismatchedInputException>()
    val mockReference0 = mockk<JsonMappingException.Reference>()
    every { mockReference0.from } returns "notAClass"
    val mockReference1 = mockk<JsonMappingException.Reference>()
    every { mockReference1.from } returns String::class.java
    every { cause.path } returns listOf(mockReference0, mockReference1)
    every { cause.targetType } returns List::class.java
    every { mockDeserializationValidationService.isArrayType(any<Class<*>>()) } returns false

    every { mockDeserializationValidationService.validateObject(any(), any(), any()) } returns emptyMap<String, ParamDetails>()
    every { mockDeserializationValidationService.validateArray(any(), any(), any()) } returns mapOf<String, ParamDetails>("key" to ParamDetails("arrayErrorType", "arrayEntityId", "arrayValue"))

    val throwable = HttpMessageNotReadableException("", cause, mockk<HttpInputMessage>())

    val mockContentCachingRequestWrapper = mockk<ContentCachingRequestWrapper>()
    every { mockContentCachingRequestWrapper.contentAsByteArray } returns "{}".toByteArray()

    val mockRequest = mockk<NativeWebRequest>()
    every { mockRequest.getNativeRequest(ContentCachingRequestWrapper::class.java) } returns mockContentCachingRequestWrapper

    every { mockJsonMapper.readTree(any<String>()) } returns mockk<ArrayNode>()

    val response = exceptionHandling.handleMessageNotReadableException(throwable, mockRequest)

    loggerExtension.assertContains("Bad Request. Error Detail: None, Invalid Params: [key=ParamDetails(errorType=arrayErrorType, entityId=arrayEntityId, value=arrayValue)]")

    assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    assertThat(response.body?.title).isEqualTo("Bad Request")
    assertThat(response.body?.detail).isEqualTo("There is a problem with your request")
  }

  @Test
  fun `Returns ResponseEntity_BadRequestProblem_ problem when exception is HttpMessageNotReadableException caused by MismatchedInputException and root is object`() {
    val cause = mockk<MismatchedInputException>()
    val mockReference0 = mockk<JsonMappingException.Reference>()
    every { mockReference0.from } returns String::class.java
    every { cause.path } returns listOf(mockReference0)
    every { cause.targetType } returns List::class.java
    every { mockDeserializationValidationService.isArrayType(any<Class<*>>()) } returns false

    every { mockDeserializationValidationService.validateObject(any(), any(), any()) } returns mapOf<String, ParamDetails>("key" to ParamDetails("objectErrorType", "objectEntityId", "objectValue"))
    every { mockDeserializationValidationService.validateArray(any(), any(), any()) } returns mapOf<String, ParamDetails>("key" to ParamDetails("errorType", "entityId", "value"))

    val throwable = HttpMessageNotReadableException("", cause, mockk<HttpInputMessage>())

    val mockContentCachingRequestWrapper = mockk<ContentCachingRequestWrapper>()
    every { mockContentCachingRequestWrapper.contentAsByteArray } returns "{}".toByteArray()

    val mockRequest = mockk<NativeWebRequest>()
    every { mockRequest.getNativeRequest(ContentCachingRequestWrapper::class.java) } returns mockContentCachingRequestWrapper

    every { mockJsonMapper.readTree(any<String>()) } returns mockk<ObjectNode>()

    val response = exceptionHandling.handleMessageNotReadableException(throwable, mockRequest)

    loggerExtension.assertContains("Bad Request. Error Detail: None, Invalid Params: [key=ParamDetails(errorType=objectErrorType, entityId=objectEntityId, value=objectValue)]")

    assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    assertThat(response.body?.title).isEqualTo("Bad Request")
    assertThat(response.body?.detail).isEqualTo("There is a problem with your request")
  }

  @Test
  fun `Returns ResponseEntity_BadRequestProblem_ problem when exception is HttpMessageNotReadableException caused by IllegalArgumentException`() {
    val cause = mockk<IllegalArgumentException>()
    val throwable = HttpMessageNotReadableException("Exception caused by IllegalArgumentException", cause, mockk<HttpInputMessage>())
    val mockRequest = mockk<NativeWebRequest>()
    val response = exceptionHandling.handleMessageNotReadableException(throwable, mockRequest)

    assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    assertThat(response.body?.title).isEqualTo("Bad Request")
    assertThat(response.body?.detail).isEqualTo("Exception caused by IllegalArgumentException")
  }
}
