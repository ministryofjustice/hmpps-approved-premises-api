package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.convert

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.convert.EnumConverterFactory

class EnumConverterTest {
  @Suppress("UNUSED") // Should be accessed by the enum converter under test
  private enum class OpenApiLike(val value: String) {
    OPTION_ONE("the-first-option"),
    OPTION_TWO("the-second-option"),
  }
  private enum class Standard {
    OPTION_ONE,
    OPTION_TWO,
  }

  @Test
  fun `The enum converter uses the String value where it is defined`() {
    val converter = EnumConverterFactory().getConverter(OpenApiLike::class.java)

    assertThat(converter.convert("the-first-option")).isEqualTo(OpenApiLike.OPTION_ONE)
    assertThat(converter.convert("the-second-option")).isEqualTo(OpenApiLike.OPTION_TWO)
  }

  @Test
  fun `The enum converter doesn't use the constant names if the String value is defined`() {
    val converter = EnumConverterFactory().getConverter(OpenApiLike::class.java)

    assertThat(converter.convert("OPTION_ONE")).isNull()
    assertThat(converter.convert("OPTION_TWO")).isNull()
  }

  @Test
  fun `The enum converter falls back to the constant names if a String value isn't defined`() {
    val converter = EnumConverterFactory().getConverter(Standard::class.java)

    assertThat(converter.convert("OPTION_ONE")).isEqualTo(Standard.OPTION_ONE)
    assertThat(converter.convert("OPTION_TWO")).isEqualTo(Standard.OPTION_TWO)
  }

  @Test
  fun `The enum converter returns null for all other strings`() {
    val openApiLikeConverter = EnumConverterFactory().getConverter(OpenApiLike::class.java)
    val standardConverter = EnumConverterFactory().getConverter(Standard::class.java)

    assertThat(openApiLikeConverter.convert("the-third-option")).isNull()
    assertThat(standardConverter.convert("the-third-option")).isNull()
  }
}
