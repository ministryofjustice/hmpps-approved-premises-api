package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysSuitabilityService
import java.time.OffsetDateTime

@ExtendWith(MockKExtension::class)
class OASysApplicabilityServiceTest {

  @InjectMockKs
  private lateinit var service: OASysSuitabilityService

  companion object {
    const val CRN = "CRN1122"
  }

  @Nested
  inner class AssessmentSummary {
    @Test
    fun `always returns true`() {
      val result = service.isUsable(
        OASysSuitabilityService.OASysAssessmentDates(
          crn = CRN,
          initiationDate = OffsetDateTime.now(),
          dateCompleted = OffsetDateTime.now(),
        ),
      )

      assertThat(result).isTrue
    }
  }
}
