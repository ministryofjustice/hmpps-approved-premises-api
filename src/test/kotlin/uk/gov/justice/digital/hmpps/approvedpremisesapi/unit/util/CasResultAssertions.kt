package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util

import org.assertj.core.api.AbstractAssert
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult

fun <T> assertThat(actual: CasResult<T>): CasResultAssertions<T> = CasResultAssertions(actual)

class CasResultAssertions<T>(actual: CasResult<T>) : AbstractAssert<CasResultAssertions<T>, CasResult<T>>(
  actual,
  CasResultAssertions::class.java,
) {

  fun isSuccess(): CasResultAssertions<T> {
    if (actual !is CasResult.Success) {
      failWithMessage("Expected CasResult.Success but was <%s>", actual.javaClass.simpleName, actual)
    }
    return this
  }

  fun hasValueEqualTo(expected: Any): CasResultAssertions<T> {
    val value = (actual as CasResult.Success).value
    if (value != expected) {
      failWithMessage("Expected CasResult value to be <%s> but was:<%s>", expected, value)
    }
    return this
  }

  fun isFieldValidationError(field: String, expectedMessage: String): CasResultAssertions<T> {
    if (actual !is CasResult.FieldValidationError) {
      failWithMessage("Expected CasResult.FieldValidationError but was <%s>", actual.javaClass.simpleName, actual)
    }
    val validationMessages = (actual as CasResult.FieldValidationError<T>).validationMessages

    if (!validationMessages.containsKey(field)) {
      failWithMessage("Expected field <%s> not found in validation messages", field)
    } else if (validationMessages[field] != expectedMessage) {
      failWithMessage(
        "Expected field <%s> to have message <%s> but was <%s>",
        field,
        expectedMessage,
        validationMessages[field],
      )
    }
    return this
  }

  fun isUnauthorised(): CasResultAssertions<T> {
    if (actual !is CasResult.Unauthorised) {
      failWithMessage("Expected CasResult.Unauthorised but was <%s>", actual.javaClass.simpleName, actual)
    }
    return this
  }

  fun isGeneralValidationError(expectedMessage: String): CasResultAssertions<T> {
    if (actual !is CasResult.GeneralValidationError) {
      failWithMessage("Expected CasResult.GeneralValidationError but was <%s>", actual.javaClass.simpleName, actual)
    }
    val message = (actual as CasResult.GeneralValidationError<T>).message
    if (message != expectedMessage) {
      failWithMessage(
        "Expected GeneralValidationError message to be <%s> but was <%s>",
        message,
        (actual as CasResult.GeneralValidationError<T>).message,
      )
    }
    return this
  }
}
