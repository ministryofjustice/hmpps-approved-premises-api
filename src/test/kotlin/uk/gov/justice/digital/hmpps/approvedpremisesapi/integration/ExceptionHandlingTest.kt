package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.exception.JDBCConnectionException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.InvalidParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.assertJsonEquals
import java.sql.SQLException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.security.access.AccessDeniedException as SpringAccessDeniedException

class ExceptionHandlingTest : InitialiseDatabasePerClassTestBase() {

  @Nested
  inner class DeserializationTests {

    @Test
    fun `An invalid request body will return a 400 when the expected body root is an object and an array is provided`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      val validationResult = webTestClient.post()
        .uri("/deserialization-test/object")
        .header("Authorization", "Bearer $jwt")
        .header("Content-Type", "application/json")
        .bodyValue("[]")
        .exchange()
        .expectStatus()
        .isBadRequest
        .returnResult<ValidationError>()
        .responseBody
        .blockFirst()!!

      assertThat(validationResult.detail).isEqualTo("Expected an object but got an array")
    }

    @Test
    fun `An invalid request body will return a 400 when the expected body root is an array and an object is provided`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      val validationResult = webTestClient.post()
        .uri("/deserialization-test/array")
        .header("Authorization", "Bearer $jwt")
        .header("Content-Type", "application/json")
        .bodyValue("{}")
        .exchange()
        .expectStatus()
        .isBadRequest
        .returnResult<ValidationError>()
        .responseBody
        .blockFirst()!!

      assertThat(validationResult.detail).isEqualTo("Expected an array but got an object")
    }

    @Test
    fun `An invalid request body will return a 400 with details of all problems when the expected body root is an object and an object is provided`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      val validationResult = webTestClient.post()
        .uri("/deserialization-test/object")
        .header("Authorization", "Bearer $jwt")
        .header("Content-Type", "application/json")
        .bodyValue(
          """
          {
             "requiredInt": null,
             "optionalInt": 123,
             "optionalObject": [],
             "requiredObject": {
                "requiredString": null,
                "optionalBoolean": 1234,
                "optionalLocalDate": false
             },
             "requiredListOfInts": ["not", "ints", false],
             "requiredListOfObjects": null,
             "optionalListOfObjects": [{
                  "requiredString": null,
                  "optionalBoolean": 1234,
                  "optionalLocalDate": false
               },
               null],
             "aLocalDate": "not a date",
             "aLocalDateTime": "not a date time",
             "anOffsetDateTime": "not an offset date time",
             "anInstant": "not an instant",
             "aUUID": "not a uuid"
          }
        """,
        )
        .exchange()
        .expectStatus()
        .isBadRequest
        .returnResult<ValidationError>()
        .responseBody
        .blockFirst()

      assertThat(validationResult!!.invalidParams).containsAll(
        listOf(
          InvalidParam(propertyName = "$.optionalListOfObjects[0].optionalBoolean", errorType = "expectedBoolean"),
          InvalidParam(propertyName = "$.optionalListOfObjects[0].optionalLocalDate", errorType = "expectedString"),
          InvalidParam(propertyName = "$.optionalListOfObjects[0].requiredString", errorType = "empty"),
          InvalidParam(propertyName = "$.optionalListOfObjects[1]", errorType = "expectedObject"),
          InvalidParam(propertyName = "$.optionalObject", errorType = "expectedObject"),
          InvalidParam(propertyName = "$.requiredInt", errorType = "empty"),
          InvalidParam(propertyName = "$.requiredListOfInts[0]", errorType = "expectedNumber"),
          InvalidParam(propertyName = "$.requiredListOfInts[1]", errorType = "expectedNumber"),
          InvalidParam(propertyName = "$.requiredListOfInts[2]", errorType = "expectedNumber"),
          InvalidParam(propertyName = "$.requiredListOfObjects", errorType = "empty"),
          InvalidParam(propertyName = "$.requiredObject.optionalBoolean", errorType = "expectedBoolean"),
          InvalidParam(propertyName = "$.requiredObject.optionalLocalDate", errorType = "expectedString"),
          InvalidParam(propertyName = "$.requiredObject.requiredString", errorType = "empty"),
          InvalidParam(propertyName = "$.aLocalDate", errorType = "invalid"),
          InvalidParam(propertyName = "$.aLocalDateTime", errorType = "invalid"),
          InvalidParam(propertyName = "$.anOffsetDateTime", errorType = "invalid"),
          InvalidParam(propertyName = "$.anInstant", errorType = "invalid"),
          InvalidParam(propertyName = "$.aUUID", errorType = "invalid"),
        ),
      )
    }

    @Test
    fun `An invalid request body will return a 400 with details of all problems when the expected body root is an array and an array is provided`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      val validationResult = webTestClient.post()
        .uri("/deserialization-test/array")
        .header("Authorization", "Bearer $jwt")
        .header("Content-Type", "application/json")
        .bodyValue(
          """
          [{
             "requiredInt": null,
             "optionalInt": 123,
             "optionalObject": [],
             "requiredObject": {
                "requiredString": null,
                "optionalBoolean": 1234,
                "optionalLocalDate": false,
                "aLocalDate": "not a date",
                "aLocalDateTime": "not a date time",
                "anOffsetDateTime": "not an offset date time",
                "anInstant": "not an instant",
                "aUUID": "not a uuid"
             },
             "requiredListOfInts": ["not", "ints", false],
             "requiredListOfObjects": null,
             "optionalListOfObjects": [{
                  "requiredString": null,
                  "optionalBoolean": 1234,
                  "optionalLocalDate": false
               },
               null],
             "aLocalDate": "not a date",
             "aLocalDateTime": "not a date time",
             "anOffsetDateTime": "not an offset date time",
             "anInstant": "not an instant",
             "aUUID": "not a uuid"
          }]
        """,
        )
        .exchange()
        .expectStatus()
        .isBadRequest
        .returnResult<ValidationError>()
        .responseBody
        .blockFirst()

      assertThat(validationResult!!.invalidParams).containsAll(
        listOf(
          InvalidParam(propertyName = "$[0].optionalListOfObjects[0].optionalBoolean", errorType = "expectedBoolean"),
          InvalidParam(propertyName = "$[0].optionalListOfObjects[0].optionalLocalDate", errorType = "expectedString"),
          InvalidParam(propertyName = "$[0].optionalListOfObjects[0].requiredString", errorType = "empty"),
          InvalidParam(propertyName = "$[0].optionalListOfObjects[1]", errorType = "expectedObject"),
          InvalidParam(propertyName = "$[0].optionalObject", errorType = "expectedObject"),
          InvalidParam(propertyName = "$[0].requiredInt", errorType = "empty"),
          InvalidParam(propertyName = "$[0].requiredListOfInts[0]", errorType = "expectedNumber"),
          InvalidParam(propertyName = "$[0].requiredListOfInts[1]", errorType = "expectedNumber"),
          InvalidParam(propertyName = "$[0].requiredListOfInts[2]", errorType = "expectedNumber"),
          InvalidParam(propertyName = "$[0].requiredListOfObjects", errorType = "empty"),
          InvalidParam(propertyName = "$[0].requiredObject.optionalBoolean", errorType = "expectedBoolean"),
          InvalidParam(propertyName = "$[0].requiredObject.optionalLocalDate", errorType = "expectedString"),
          InvalidParam(propertyName = "$[0].requiredObject.requiredString", errorType = "empty"),
          InvalidParam(propertyName = "$[0].aLocalDate", errorType = "invalid"),
          InvalidParam(propertyName = "$[0].aLocalDateTime", errorType = "invalid"),
          InvalidParam(propertyName = "$[0].anOffsetDateTime", errorType = "invalid"),
          InvalidParam(propertyName = "$[0].anInstant", errorType = "invalid"),
          InvalidParam(propertyName = "$[0].aUUID", errorType = "invalid"),
        ),
      )
    }

    @Test
    fun `Valid special JSON primitive properties are not listed as errors when appearing alongside an invalid property`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      val validationResult = webTestClient.post()
        .uri("/deserialization-test/special-json-primitives")
        .header("Authorization", "Bearer $jwt")
        .header("Content-Type", "application/json")
        .bodyValue(
          """
          {
            "missingString": null,
            "localDate": "2023-04-12",
            "localDateTime": "2023-04-12T16:52:00",
            "offsetDateTime": "2023-04-12T16:52:00+01:00",
            "instant": "2023-04-12T16:52:00+01:00",
            "uuid": "61f22c65-4d42-4cc1-8955-e4ea89088194"
          }
        """,
        )
        .exchange()
        .expectStatus()
        .isBadRequest
        .returnResult<ValidationError>()
        .responseBody
        .blockFirst()

      assertThat(validationResult!!.invalidParams).containsExactly(
        InvalidParam(propertyName = "$.missingString", errorType = "empty"),
      )
    }
  }

  @Nested
  inner class RawJsonTests {

    @Test
    fun `Returns 401 unauthenticated when AuthenticationCredentialsNotFoundException is thrown`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      val validationResult = webTestClient.get()
        .uri("/authentication-credentials-not-found-exception")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .returnResult<String>()

      assertJsonEquals(
        actual = validationResult.responseBody.blockFirst(),
        expected = """
          {
            "title" : "Unauthenticated",
            "status" : 401,
            "detail" : "A valid HMPPS Auth JWT must be supplied via bearer authentication to access this endpoint",
            "instance" : "/authentication-credentials-not-found-exception"
          }
          """,
      )

      assertThat(validationResult.responseHeaders.contentType?.toString()).isEqualTo("application/problem+json")
    }

    @Test
    fun `Returns 403 Forbidden when AccessDeniedException is thrown`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      val validationResult = webTestClient.get()
        .uri("/access-denied-exception")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .returnResult<String>()

      assertJsonEquals(
        actual = validationResult.responseBody.blockFirst(),
        expected = """
          {
            "title" : "Forbidden",
            "status" : 403,
            "detail" : "You are not authorized to access this endpoint",
            "instance" : "/access-denied-exception"
          }
          """,
      )
      assertThat(validationResult.responseHeaders.contentType?.toString()).isEqualTo("application/problem+json")
    }

    @Test
    fun `Returns 404 Not Found when NoResourceFoundException is thrown`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      val validationResult = webTestClient.get()
        .uri("/no-resource-found-exception")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .returnResult<String>()

      assertJsonEquals(
        actual = validationResult.responseBody.blockFirst(),
        expected = """
          {
            "title" : "Not Found",
            "status" : 404,
            "detail" : "Resource not found",
            "instance" : "/no-resource-found-exception"
          }
          """,
      )
      assertThat(validationResult.responseHeaders.contentType?.toString()).isEqualTo("application/problem+json")
    }

    @Test
    fun `Returns 400 Bad Request when MissingRequestHeaderException is thrown`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      val validationResult = webTestClient.get()
        .uri("/missing-request-header-exception")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .returnResult<String>()

      assertJsonEquals(
        actual = validationResult.responseBody.blockFirst(),
        expected = """
          {
            "status" : 400,
            "detail" : "Missing required header X-Required-Header",
            "title" : "Bad Request",
            "instance" : "/missing-request-header-exception"
          }
          """,
      )
      assertThat(validationResult.responseHeaders.contentType?.toString()).isEqualTo("application/problem+json")
    }

    @Test
    fun `Returns 400 Bad Request when MissingServletRequestParameterException is thrown`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      val validationResult = webTestClient.get()
        .uri("/missing-servlet-request-parameter-exception")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .returnResult<String>()

      assertJsonEquals(
        actual = validationResult.responseBody.blockFirst(),
        expected = """
            {              
              "status" : 400,
              "detail" : "Missing required query parameter requiredProperty",
              "title" : "Bad Request",
              "instance" : "/missing-servlet-request-parameter-exception"
            }
          """,
      )
      assertThat(validationResult.responseHeaders.contentType?.toString()).isEqualTo("application/problem+json")
    }

    @Test
    fun `Returns 400 Bad Request when MethodArgumentTypeMismatchException is thrown`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      val validationResult = webTestClient.get()
        .uri("/method-argument-type-mismatch-exception")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .returnResult<String>()

      assertJsonEquals(
        actual = validationResult.responseBody.blockFirst(),
        expected = """
          {
              "detail": "Invalid type for query parameter id expected int",
              "instance": "/method-argument-type-mismatch-exception",
              "status": 400,
              "title": "Bad Request"
          }
        """,
      )

      assertThat(validationResult.responseHeaders.contentType?.toString()).isEqualTo("application/problem+json")
    }

    @Test
    fun `Returns 503 Service Unavailable when JDBCConnectionException is thrown`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      val validationResult = webTestClient.get()
        .uri("/jdbc-connection-exception")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .returnResult<String>()

      assertJsonEquals(
        actual = validationResult.responseBody.blockFirst(),
        expected = """
          {        
            "title" : "Service Unavailable",
            "status" : 503,
            "detail" : "Error acquiring a database connection",
            "instance" : "/jdbc-connection-exception"
          }
          """,
      )
      assertThat(validationResult.responseHeaders.contentType?.toString()).isEqualTo("application/problem+json")
    }

    @Test
    fun `Returns 500 Internal Server Error when IllegalArgumentException is thrown`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      val validationResult = webTestClient.get()
        .uri("/illegal-argument-exception")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .returnResult<String>()

      assertJsonEquals(
        actual = validationResult.responseBody.blockFirst(),
        expected = """
           {        
            "title" : "Internal Server Error",
            "status" : 500,
            "detail" : "There was an unexpected problem",
            "instance" : "/illegal-argument-exception"
           }
          """,
      )
      assertThat(validationResult.responseHeaders.contentType?.toString()).isEqualTo("application/problem+json")
    }

    @Test
    fun `Returns 400 Bad Request when expected body root is an array and an object is provided`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      val validationResult = webTestClient.post()
        .uri("/deserialization-test/array")
        .header("Authorization", "Bearer $jwt")
        .header("Content-Type", "application/json")
        .bodyValue("{}")
        .exchange()
        .returnResult<String>()

      assertJsonEquals(
        actual = validationResult.responseBody.blockFirst(),
        expected = """
          {        
            "title" : "Bad Request",
            "status" : 400,
            "detail" : "Expected an array but got an object",
            "instance" : "/deserialization-test/array"
          }
          """,
      )
      assertThat(validationResult.responseHeaders.contentType?.toString()).isEqualTo("application/problem+json")
    }

    @Test
    fun `Returns 400 Bad Request when expected body root is an object and an array is provided`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      val validationResult = webTestClient.post()
        .uri("/deserialization-test/object")
        .header("Authorization", "Bearer $jwt")
        .header("Content-Type", "application/json")
        .bodyValue("[]")
        .exchange()
        .returnResult<String>()

      assertJsonEquals(
        actual = validationResult.responseBody.blockFirst(),
        expected = """
          {        
            "title" : "Bad Request",
            "status" : 400,
            "detail" : "Expected an object but got an array",
            "instance" : "/deserialization-test/object"
          }
          """,
      )
      assertThat(validationResult.responseHeaders.contentType?.toString()).isEqualTo("application/problem+json")
    }

    @Test
    fun `Returns 400 Bad Request when expected body root is an array and request body has an invalid property`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      val validationResult = webTestClient.post()
        .uri("/deserialization-test/array")
        .header("Authorization", "Bearer $jwt")
        .header("Content-Type", "application/json")
        .bodyValue(
          """
          [{
             "requiredInt": "not an int",
             "requiredObject": {
                "requiredString": "a string",
                "aLocalDate": "2026-04-26",
                "aLocalDateTime": "2026-04-26T16:03:56.871",
                "anOffsetDateTime": "2026-04-26T16:03:56.871+00:00",
                "anInstant": "2026-04-26T16:03:56.871Z",
                "aUUID": "f55976f6-fd15-4c07-8044-51f806cf4c7c"
             },
             "requiredListOfInts": [1, 2, 3],
             "requiredListOfObjects": [],
             "aLocalDate": "2026-04-26",
             "aLocalDateTime": "2026-04-26T16:03:56.871",
             "anOffsetDateTime": "2026-04-26T16:03:56.871+00:00",
             "anInstant": "2026-04-26T16:03:56.871Z",
             "aUUID": "f55976f6-fd15-4c07-8044-51f806cf4c7c"
          }]
        """,
        )
        .exchange()
        .returnResult<String>()

      assertJsonEquals(
        actual = validationResult.responseBody.blockFirst(),
        expected = """
          {        
            "status" : 400,
            "detail" : "There is a problem with your request",
            "title" : "Bad Request",
            "instance" : "/deserialization-test/array",
            "invalid-params" : [{
              "propertyName" : "$[0].requiredInt",
              "errorType" : "expectedNumber",
              "entityId" : null, 
              "value" : null
            }]
          }
          """,
      )

      assertThat(validationResult.responseHeaders.contentType?.toString()).isEqualTo("application/problem+json")
    }

    @Test
    fun `Returns 400 Bad Request when expected body root is an object and request body has an invalid property`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      val validationResult = webTestClient.post()
        .uri("/deserialization-test/object")
        .header("Authorization", "Bearer $jwt")
        .header("Content-Type", "application/json")
        .bodyValue(
          """
          {
             "requiredInt": "not an int",
             "requiredObject": {
                "requiredString": "a string",
                "aLocalDate": "2026-04-26",
                "aLocalDateTime": "2026-04-26T16:03:56.871",
                "anOffsetDateTime": "2026-04-26T16:03:56.871+00:00",
                "anInstant": "2026-04-26T16:03:56.871Z",
                "aUUID": "f55976f6-fd15-4c07-8044-51f806cf4c7c"
             },
             "requiredListOfInts": [1, 2, 3],
             "requiredListOfObjects": [],
             "aLocalDate": "2026-04-26",
             "aLocalDateTime": "2026-04-26T16:03:56.871",
             "anOffsetDateTime": "2026-04-26T16:03:56.871+00:00",
             "anInstant": "2026-04-26T16:03:56.871Z",
             "aUUID": "f55976f6-fd15-4c07-8044-51f806cf4c7c"
          }
        """,
        )
        .exchange()
        .returnResult<String>()

      assertJsonEquals(
        actual = validationResult.responseBody.blockFirst(),
        expected = """
          {        
            "status" : 400,
            "detail" : "There is a problem with your request",
            "title" : "Bad Request",
            "instance":"/deserialization-test/object",
            "invalid-params" : [{
              "propertyName" : "$.requiredInt",
              "errorType" : "expectedNumber",
              "entityId" : null, 
              "value" : null
            }]
          }
          """,
      )
      assertThat(validationResult.responseHeaders.contentType?.toString()).isEqualTo("application/problem+json")
    }

    @Test
    fun `Returns 400 Bad Request when cause is not MismatchedInputException`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      val validationResult = webTestClient.post()
        .uri("/deserialization-test/object")
        .header("Authorization", "Bearer $jwt")
        .header("Content-Type", "application/json")
        .bodyValue("{ invalid json }")
        .exchange()
        .returnResult<String>()

      assertJsonEquals(
        actual = validationResult.responseBody.blockFirst(),
        expected = """
          {        
            "title" : "Bad Request",
            "status" : 400,
            "detail" : "JSON parse error: Unexpected character ('i' (code 105)): was expecting double-quote to start property name",
            "instance" : "/deserialization-test/object"
          }
          """,
      )
      assertThat(validationResult.responseHeaders.contentType?.toString()).isEqualTo("application/problem+json")
    }
  }

  @Test
  fun `An unhandled exception will not return a problem response with the exception message in the detail property`() {
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    val validationResult = webTestClient.get()
      .uri("/unhandled-exception")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .is5xxServerError
      .returnResult<ValidationError>()
      .responseBody
      .blockFirst()

    assertThat(validationResult!!.detail).isEqualTo("There was an unexpected problem")
    assertThat(mockSentryService.getRaisedExceptions()).hasSize(1)
    assertThat(mockSentryService.getRaisedExceptions()[0]).isInstanceOf(RuntimeException::class.java)
  }

  @Test
  fun `A missing required query parameter will return a problem response with detail`() {
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    val validationResult = webTestClient.get()
      .uri("/deserialization-test/query-params")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isBadRequest
      .returnResult<ValidationError>()
      .responseBody
      .blockFirst()

    assertThat(validationResult!!.detail).isEqualTo("Missing required query parameter requiredProperty")
  }

  @Test
  fun `A query parameter of the wrong type will return a problem response with detail`() {
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    val validationResult = webTestClient.get()
      .uri("/deserialization-test/query-params?requiredProperty=notanint")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isBadRequest
      .returnResult<ValidationError>()
      .responseBody
      .blockFirst()

    assertThat(validationResult!!.detail).isEqualTo("Invalid type for query parameter requiredProperty expected int")
  }

  @Test
  fun `JDBC Connection Exception returns a 503`() {
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    val validationResult = webTestClient.get()
      .uri("/jdbc-connection-exception")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus().isEqualTo(503)
      .returnResult<ValidationError>()
      .responseBody
      .blockFirst()

    assertThat(validationResult!!.detail).isEqualTo("Error acquiring a database connection")
    assertThat(mockSentryService.getRaisedExceptions()).hasSize(1)
    assertThat(mockSentryService.getRaisedExceptions()[0]).isInstanceOf(JDBCConnectionException::class.java)
  }

  @Test
  fun `Exception with cause of JDBC Connection Exception returns a 503`() {
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    val validationResult = webTestClient.get()
      .uri("/jdbc-connection-exception-in-cause")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus().isEqualTo(503)
      .returnResult<ValidationError>()
      .responseBody
      .blockFirst()

    assertThat(validationResult!!.detail).isEqualTo("Error acquiring a database connection")
    assertThat(mockSentryService.getRaisedExceptions()).hasSize(1)
    assertThat(mockSentryService.getRaisedExceptions()[0]).isInstanceOf(RuntimeException::class.java)
  }
}

@SuppressWarnings("UnusedParameter", "TooGenericExceptionThrown")
@RestController
class ExceptionHandlingTestController {
  @PostMapping(path = ["deserialization-test/object"], consumes = ["application/json"])
  fun testDeserializationForObject(@RequestBody body: DeserializationTestBody): ResponseEntity<Unit> = ResponseEntity.ok(Unit)

  @PostMapping(path = ["deserialization-test/array"], consumes = ["application/json"])
  fun testDeserializationForList(@RequestBody body: List<DeserializationTestBody>): ResponseEntity<Unit> = ResponseEntity.ok(Unit)

  @PostMapping(path = ["deserialization-test/special-json-primitives"], consumes = ["application/json"])
  fun testDeserializationForPrimitives(@RequestBody body: AllSpecialJSONPrimitives): ResponseEntity<Unit> = ResponseEntity.ok(Unit)

  @GetMapping(path = ["deserialization-test/query-params"])
  fun testQueryParams(@RequestParam(value = "requiredProperty", required = true) requiredProperty: Int): ResponseEntity<Unit> = ResponseEntity.ok(Unit)

  @GetMapping(path = ["unhandled-exception"])
  fun unhandledException(): ResponseEntity<Unit> = throw RuntimeException("I am an unhandled exception")

  @GetMapping(path = ["jdbc-connection-exception"])
  fun jdbcConnectionException(): ResponseEntity<Unit> = throw JDBCConnectionException("Oh dear", SQLException(""))

  @GetMapping(path = ["jdbc-connection-exception-in-cause"])
  fun jdbcConnectionExceptionInCause(): ResponseEntity<Unit> = throw RuntimeException(
    JDBCConnectionException("Oh dear", SQLException("")),
  )

  @GetMapping(path = ["authentication-credentials-not-found-exception"])
  fun authenticationCredentialsNotFoundException(): ResponseEntity<Unit> = throw AuthenticationCredentialsNotFoundException(
    "Credentials not found",
    SQLException(""),
  )

  @GetMapping(path = ["access-denied-exception"])
  fun accessDeniedException(): ResponseEntity<Unit> = throw SpringAccessDeniedException("Forbidden")

  @GetMapping(path = ["no-resource-found-exception"])
  fun noResourceFoundException(): ResponseEntity<Unit> = throw NoResourceFoundException(HttpMethod.GET, "localhost", "/path")

  @GetMapping(path = ["missing-request-header-exception"])
  fun missingRequestHeaderException(@RequestHeader("X-Required-Header") header: String): ResponseEntity<Unit> = ResponseEntity.ok().build()

  @GetMapping(path = ["missing-servlet-request-parameter-exception"])
  fun missingServletRequestParameterException(): ResponseEntity<Unit> = throw MissingServletRequestParameterException("requiredProperty", "int")

  @GetMapping(path = ["method-argument-type-mismatch-exception"])
  fun methodArgumentTypeMismatchException(): ResponseEntity<Unit> {
    val methodParameter = mockk<MethodParameter>()
    every { methodParameter.parameterName } returns "id"
    every { methodParameter.parameterType } returns Int::class.java
    throw MethodArgumentTypeMismatchException("notanint", Int::class.java, "requiredProperty", methodParameter, null)
  }

  @GetMapping(path = ["illegal-argument-exception"])
  fun illegalArgumentException(): ResponseEntity<Unit> = throw IllegalArgumentException()
}

data class DeserializationTestBody(
  val requiredInt: Int,
  val optionalInt: Int?,
  val optionalObject: DeserializationTestBodyNested?,
  val requiredObject: DeserializationTestBodyNested,
  val requiredListOfInts: List<Int>,
  val requiredListOfObjects: List<DeserializationTestBodyNested>,
  val optionalListOfObjects: List<DeserializationTestBodyNested>?,
  val aLocalDate: LocalDate,
  val aLocalDateTime: LocalDateTime,
  val anOffsetDateTime: OffsetDateTime,
  val anInstant: Instant,
  val aUUID: UUID,
)

data class DeserializationTestBodyNested(
  val requiredString: String,
  val optionalBoolean: Boolean?,
  val optionalLocalDate: LocalDate?,
)

data class AllSpecialJSONPrimitives(
  val missingString: String,
  val localDate: LocalDate,
  val localDateTime: LocalDateTime,
  val offsetDateTime: OffsetDateTime,
  val instant: Instant,
  val uuid: UUID,
)
