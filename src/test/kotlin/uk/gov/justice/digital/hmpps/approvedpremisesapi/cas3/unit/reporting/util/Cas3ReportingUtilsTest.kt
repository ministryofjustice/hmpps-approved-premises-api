package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.reporting.util

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.util.toShortBase58
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.util.toYesNo
import java.util.UUID

class Cas3ReportingUtilsTest {
  @Test
  fun `toShortBase58 should return different hashed values for different UUIDs`() {
    val uuid1 = UUID.randomUUID()
    val uuid2 = UUID.randomUUID()

    Assertions.assertThat(uuid1.toShortBase58()).isNotBlank()
    Assertions.assertThat(uuid2.toShortBase58()).isNotBlank()
    Assertions.assertThat(uuid1.toShortBase58()).isNotEqualTo(uuid2.toShortBase58())
  }

  @Test
  fun `toShortBase58 should return the same hashed value for the same UUID`() {
    val uuid1 = UUID.fromString("e11e2d61-a84e-4c49-8955-a259ee178fbd")
    val uuid2 = UUID.fromString("e11e2d61-a84e-4c49-8955-a259ee178fbd")

    Assertions.assertThat(uuid1.toShortBase58()).isNotBlank()
    Assertions.assertThat(uuid2.toShortBase58()).isNotBlank()
    Assertions.assertThat(uuid1.toShortBase58()).isEqualTo(uuid2.toShortBase58())
  }

  @Test
  fun `toShortBase58 should return the correct hashed value for a specific UUID`() {
    val specificUUID = UUID.fromString("57fbee87-03c0-4696-b3af-cc18f12d7034")
    val specificHash = "fC5HnCZA11T"

    Assertions.assertThat(specificUUID.toShortBase58()).isNotBlank()
    Assertions.assertThat(specificUUID.toShortBase58()).isEqualTo(specificHash)
  }

  @Test
  fun `toYesNo should return null when boolean value is null`() {
    val boolean: Boolean? = null
    Assertions.assertThat(boolean.toYesNo()).isNull()
  }

  @Test
  fun `toYesNo should return Yes when boolean value is true`() {
    val boolean = true
    Assertions.assertThat(boolean.toYesNo()).isEqualTo("Yes")
  }

  @Test
  fun `toYesNo should return No when boolean value is false`() {
    val boolean = false
    Assertions.assertThat(boolean.toYesNo()).isEqualTo("No")
  }
}
