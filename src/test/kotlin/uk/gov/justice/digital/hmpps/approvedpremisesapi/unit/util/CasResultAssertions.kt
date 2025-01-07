package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util

import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions.assertThat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult

fun <T> assertThat(actual: CasResult<T>): CasResultAssertions<T> = CasResultAssertions(actual)

class CasResultAssertions<T>(actual: CasResult<T>) : AbstractAssert<CasResultAssertions<T>, CasResult<T>>(
  actual,
  CasResultAssertions::class.java,
) {

  fun isSuccess(): CasSuccessResultAssertions<T> {
    if (actual !is CasResult.Success) {
      failWithMessage("Expected CasResult.Success but was <%s>", actual.javaClass.simpleName, actual)
    }
    return CasSuccessResultAssertions(actual as CasResult.Success)
  }

  fun isConflictError(): CasResultConflictErrorAssertions<T> {
    if (actual !is CasResult.ConflictError) {
      failWithMessage("Expected CasResult.ConflictError but was <%s>", actual.javaClass.simpleName, actual)
    }
    return CasResultConflictErrorAssertions(actual as CasResult.ConflictError)
  }

  fun isFieldValidationError(): CasResultFieldValidationErrorAssertions<T> {
    if (actual !is CasResult.FieldValidationError) {
      failWithMessage("Expected CasResult.FieldValidationError but was <%s>", actual.javaClass.simpleName, actual)
    }
    return CasResultFieldValidationErrorAssertions(actual as CasResult.FieldValidationError)
  }

  @Deprecated("Use the chained version isFieldValidationError()")
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

  fun isNotFound(expectedEntityType: String, expectedId: Any) {
    if (actual !is CasResult.NotFound) {
      failWithMessage("Expected CasResult.NotFound but was <%s>", actual.javaClass.simpleName, actual)
    }
    val notFound = actual as CasResult.NotFound

    assertThat(notFound.id).isEqualTo(expectedId.toString())
    assertThat(notFound.entityType).isEqualTo(expectedEntityType)
  }
}

class CasSuccessResultAssertions<T>(actual: CasResult.Success<T>) : AbstractAssert<CasSuccessResultAssertions<T>, CasResult.Success<T>>(
  actual,
  CasSuccessResultAssertions::class.java,
) {
  fun hasValueEqualTo(expected: Any): CasSuccessResultAssertions<T> {
    val value = actual.value
    if (value != expected) {
      failWithMessage("Expected CasResult value to be <%s> but was:<%s>", expected, value)
    }
    return this
  }

  fun with(check: (T) -> Unit) {
    check.invoke(actual.value)
  }
}

class CasResultConflictErrorAssertions<T>(actual: CasResult.ConflictError<T>) :
  AbstractAssert<CasResultConflictErrorAssertions<T>, CasResult.ConflictError<T>>(
    actual,
    CasResultConflictErrorAssertions::class.java,
  ) {
  fun hasEntityId(expected: Any): CasResultConflictErrorAssertions<T> {
    val value = actual.conflictingEntityId
    if (value != expected) {
      failWithMessage("Expected ConflictError Entity Id value to be <%s> but was:<%s>", expected, value)
    }
    return this
  }

  fun hasMessage(expected: Any): CasResultConflictErrorAssertions<T> {
    val value = actual.message
    if (value != expected) {
      failWithMessage("Expected ConflictError Message value to be <%s> but was:<%s>", expected, value)
    }
    return this
  }

  fun hasMessageContaining(expected: String): CasResultConflictErrorAssertions<T> {
    val value = actual.message
    assertThat(value).contains(expected)
    return this
  }
}

class CasResultFieldValidationErrorAssertions<T>(actual: CasResult.FieldValidationError<T>) :
  AbstractAssert<CasResultFieldValidationErrorAssertions<T>, CasResult.FieldValidationError<T>>(
    actual,
    CasResultFieldValidationErrorAssertions::class.java,
  ) {
  fun hasMessage(field: String, expectedMessage: String): CasResultFieldValidationErrorAssertions<T> {
    val validationMessages = actual.validationMessages

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
}
