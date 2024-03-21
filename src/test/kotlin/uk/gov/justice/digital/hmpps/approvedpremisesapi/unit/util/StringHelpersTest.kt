package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.javaConstantNameToSentence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.kebabCaseToPascalCase

class StringHelpersTest {
  @Test
  fun kebabCaseToPascalCase() {
    assertThat("kebab-case".kebabCaseToPascalCase()).isEqualTo("KebabCase")
  }

  @Test
  fun javaConstantNameToSentence() {
    assertThat("MY_CONSTANT_NAME".javaConstantNameToSentence()).isEqualTo("My constant name")
  }
}
