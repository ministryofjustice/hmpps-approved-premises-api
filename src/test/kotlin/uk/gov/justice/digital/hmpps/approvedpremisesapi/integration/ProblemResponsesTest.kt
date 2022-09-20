package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.InvalidParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import java.time.LocalDate

class ProblemResponsesTest : IntegrationTestBase() {
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
               null]
          }
        """
      )
      .exchange()
      .expectStatus()
      .isBadRequest
      .returnResult<ValidationError>()
      .responseBody
      .blockFirst()

    assertThat(validationResult!!.invalidParams).containsAll(
      listOf(
        InvalidParam(propertyName = "$.optionalListOfObjects[0].optionalBoolean", errorType = "Expected a boolean"),
        InvalidParam(propertyName = "$.optionalListOfObjects[0].optionalLocalDate", errorType = "Expected a string"),
        InvalidParam(propertyName = "$.optionalListOfObjects[0].requiredString", errorType = "A value must be provided for this property"),
        InvalidParam(propertyName = "$.optionalListOfObjects[1]", errorType = "Expected an object"),
        InvalidParam(propertyName = "$.optionalObject", errorType = "Expected an object"),
        InvalidParam(propertyName = "$.requiredInt", errorType = "A value must be provided for this property"),
        InvalidParam(propertyName = "$.requiredListOfInts[0]", errorType = "Expected a number"),
        InvalidParam(propertyName = "$.requiredListOfInts[1]", errorType = "Expected a number"),
        InvalidParam(propertyName = "$.requiredListOfInts[2]", errorType = "Expected a number"),
        InvalidParam(propertyName = "$.requiredListOfObjects", errorType = "A value must be provided for this property"),
        InvalidParam(propertyName = "$.requiredObject.optionalBoolean", errorType = "Expected a boolean"),
        InvalidParam(propertyName = "$.requiredObject.optionalLocalDate", errorType = "Expected a string"),
        InvalidParam(propertyName = "$.requiredObject.requiredString", errorType = "A value must be provided for this property")
      )
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
                "optionalLocalDate": false
             },
             "requiredListOfInts": ["not", "ints", false],
             "requiredListOfObjects": null,
             "optionalListOfObjects": [{
                  "requiredString": null,
                  "optionalBoolean": 1234,
                  "optionalLocalDate": false
               }, 
               null]
          }]
        """
      )
      .exchange()
      .expectStatus()
      .isBadRequest
      .returnResult<ValidationError>()
      .responseBody
      .blockFirst()

    assertThat(validationResult!!.invalidParams).containsAll(
      listOf(
        InvalidParam(propertyName = "$[0].optionalListOfObjects[0].optionalBoolean", errorType = "Expected a boolean"),
        InvalidParam(propertyName = "$[0].optionalListOfObjects[0].optionalLocalDate", errorType = "Expected a string"),
        InvalidParam(propertyName = "$[0].optionalListOfObjects[0].requiredString", errorType = "A value must be provided for this property"),
        InvalidParam(propertyName = "$[0].optionalListOfObjects[1]", errorType = "Expected an object"),
        InvalidParam(propertyName = "$[0].optionalObject", errorType = "Expected an object"),
        InvalidParam(propertyName = "$[0].requiredInt", errorType = "A value must be provided for this property"),
        InvalidParam(propertyName = "$[0].requiredListOfInts[0]", errorType = "Expected a number"),
        InvalidParam(propertyName = "$[0].requiredListOfInts[1]", errorType = "Expected a number"),
        InvalidParam(propertyName = "$[0].requiredListOfInts[2]", errorType = "Expected a number"),
        InvalidParam(propertyName = "$[0].requiredListOfObjects", errorType = "A value must be provided for this property"),
        InvalidParam(propertyName = "$[0].requiredObject.optionalBoolean", errorType = "Expected a boolean"),
        InvalidParam(propertyName = "$[0].requiredObject.optionalLocalDate", errorType = "Expected a string"),
        InvalidParam(propertyName = "$[0].requiredObject.requiredString", errorType = "A value must be provided for this property")
      )
    )
  }
}

@RestController
class DeserializationTestController {
  @PostMapping(path = ["deserialization-test/object"], consumes = ["application/json"])
  fun testDeserialization(@RequestBody body: DeserializationTestBody): ResponseEntity<Unit> {
    return ResponseEntity.ok(Unit)
  }

  @PostMapping(path = ["deserialization-test/array"], consumes = ["application/json"])
  fun testDeserialization(@RequestBody body: List<DeserializationTestBody>): ResponseEntity<Unit> {
    return ResponseEntity.ok(Unit)
  }
}

data class DeserializationTestBody(
  val requiredInt: Int,
  val optionalInt: Int?,
  val optionalObject: DeserializationTestBodyNested?,
  val requiredObject: DeserializationTestBodyNested,
  val requiredListOfInts: List<Int>,
  val requiredListOfObjects: List<DeserializationTestBodyNested>,
  val optionalListOfObjects: List<DeserializationTestBodyNested>?
)

data class DeserializationTestBodyNested(
  val requiredString: String,
  val optionalBoolean: Boolean?,
  val optionalLocalDate: LocalDate?
)
