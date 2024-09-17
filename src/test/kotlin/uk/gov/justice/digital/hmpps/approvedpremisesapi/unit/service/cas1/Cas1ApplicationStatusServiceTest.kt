package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Called
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationStatusService

@ExtendWith(MockKExtension::class)
class Cas1ApplicationStatusServiceTest {
  @MockK
  private lateinit var applicationRepository: ApplicationRepository

  @InjectMockKs
  private lateinit var service: Cas1ApplicationStatusService

  @Nested
  inner class BookingMade {
    @Test
    fun `if not linked to application (manual booking), do nothing`() {
      val booking = BookingEntityFactory()
        .withDefaults()
        .withApplication(null)
        .produce()

      service.bookingMade(booking)

      verify { applicationRepository wasNot Called }
    }

    @Test
    fun `if linked to application set status to PLACEMENT_ALLOCATED`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
        .produce()

      val booking = BookingEntityFactory()
        .withDefaults()
        .withApplication(application)
        .produce()

      every { applicationRepository.save(any()) } returns application

      service.bookingMade(booking)

      verify { applicationRepository.save(application) }

      assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED)
    }
  }
}
