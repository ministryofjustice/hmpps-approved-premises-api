package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.exception.JDBCConnectionException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.InvalidParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import java.sql.SQLException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

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
        .blockFirst()

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
        .blockFirst()

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
  fun testDeserialization(@RequestBody body: DeserializationTestBody): ResponseEntity<Unit> = ResponseEntity.ok(Unit)

  @PostMapping(path = ["deserialization-test/array"], consumes = ["application/json"])
  fun testDeserialization(@RequestBody body: List<DeserializationTestBody>): ResponseEntity<Unit> = ResponseEntity.ok(Unit)

  @PostMapping(path = ["deserialization-test/special-json-primitives"], consumes = ["application/json"])
  fun testDeserialization(@RequestBody body: AllSpecialJSONPrimitives): ResponseEntity<Unit> = ResponseEntity.ok(Unit)

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
