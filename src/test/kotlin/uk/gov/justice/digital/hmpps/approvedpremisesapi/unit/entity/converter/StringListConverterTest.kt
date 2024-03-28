package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.entity.converter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.converter.StringListConverter

class StringListConverterTest {

  @Test
  fun `convertToDatabaseColumn null`() {
    val result = StringListConverter().convertToDatabaseColumn(null)
    assertThat(result).isNull()
  }

  @Test
  fun `convertToDatabaseColumn Empty`() {
    val result = StringListConverter().convertToDatabaseColumn(listOf())
    assertThat(result).isEqualTo("")
  }

  @Test
  fun `convertToDatabaseColumn OneValue`() {
    val result = StringListConverter().convertToDatabaseColumn(listOf("first value"))
    assertThat(result).isEqualTo("first value")
  }

  @Test
  fun `convertToDatabaseColumn MultipleValues`() {
    val result = StringListConverter().convertToDatabaseColumn(listOf("first value", "SECOND VALUE", "third"))
    assertThat(result).isEqualTo("first value;SECOND VALUE;third")
  }

  @Test
  fun `convertToEntityAttribute null`() {
    val result = StringListConverter().convertToEntityAttribute(null)
    assertThat(result).isNull()
  }

  @Test
  fun `convertToEntityAttribute Empty`() {
    val result = StringListConverter().convertToEntityAttribute("")
    assertThat(result).isEmpty()
  }

  @Test
  fun `convertToEntityAttribute OneValue`() {
    val result = StringListConverter().convertToEntityAttribute("first value")
    assertThat(result).isEqualTo(listOf("first value"))
  }

  @Test
  fun `convertToEntityAttribute MultipleValues`() {
    val result = StringListConverter().convertToEntityAttribute("first value;SECOND VALUE;third")
    assertThat(result).isEqualTo(listOf("first value", "SECOND VALUE", "third"))
  }
}
