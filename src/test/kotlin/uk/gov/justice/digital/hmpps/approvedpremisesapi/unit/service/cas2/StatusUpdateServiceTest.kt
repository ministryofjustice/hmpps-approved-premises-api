package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2ApplicationStatusSeeding
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.StatusUpdateService

class StatusUpdateServiceTest {
  private val statusUpdateService = StatusUpdateService()

  @Nested
  inner class IsValidStatus {

    @Test
    fun `returns true when the given newStatus is valid`() {
      Cas2ApplicationStatusSeeding.statusList().forEach { status ->
        val validUpdate = Cas2ApplicationStatusUpdate(newStatus = status.name)
        assertThat(statusUpdateService.isValidStatus(validUpdate)).isTrue()
      }
    }

    @Test
    fun `returns false when the given newStatus is NOT valid`() {
      val invalidUpdate = Cas2ApplicationStatusUpdate(newStatus = "invalidStatus")

      assertThat(statusUpdateService.isValidStatus(invalidUpdate)).isFalse()
    }
  }
}
