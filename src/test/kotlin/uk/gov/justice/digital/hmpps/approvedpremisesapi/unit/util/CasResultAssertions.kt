package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util

import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions.assertThat
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
    assertThat((actual as CasResult.Success).value).isEqualTo(expected)
    return this
  }

  fun isFieldValidationError(field: String, expectedMessage: String): CasResultAssertions<T> {
    if (actual !is CasResult.FieldValidationError) {
      failWithMessage("Expected CasResult.FieldValidationError but was <%s>", actual.javaClass.simpleName, actual)
    } else {
      assertThat((actual as CasResult.FieldValidationError<T>).validationMessages).containsEntry(field, expectedMessage)
    }
    return this
  }

  fun isUnauthorised(): CasResultAssertions<T> {
    if (actual !is CasResult.Unauthorised) {
      failWithMessage("Expected CasResult.Unauthorised but was <%s>", actual.javaClass.simpleName, actual)
    }
    return this
  }

  fun isGeneralValidationError(message: String): CasResultAssertions<T> {
    if (actual !is CasResult.GeneralValidationError) {
      failWithMessage("Expected CasResult.GeneralValidationError but was <%s>", actual.javaClass.simpleName, actual)
    } else {
      assertThat((actual as CasResult.GeneralValidationError).message).isEqualTo(message)
    }
    return this
  }
}
