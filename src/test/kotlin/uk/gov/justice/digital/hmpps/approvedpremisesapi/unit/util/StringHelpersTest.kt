package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.kebabCaseToPascalCase

class StringHelpersTest {
  @Test
  fun `Converts kebab-case to PascalCase`() {
    assertThat("kebab-case".kebabCaseToPascalCase()).isEqualTo("KebabCase")
  }
}
