package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CruService

class CruServiceTest {
  private val cruService = CruService()

  @Test
  fun `cruNameFromProbationAreaCode returns CRU mapping`() {
    assertThat(cruService.cruNameFromProbationAreaCode("N50")).isEqualTo("North West")
  }

  @Test
  fun `cruNameFromProbationAreaCode returns Unknown CRU when no mapping exists`() {
    assertThat(cruService.cruNameFromProbationAreaCode("UNKNOWN")).isEqualTo("Unknown CRU")
  }
}
