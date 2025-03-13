package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.seed.cas1

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NonArrivalReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonRepository.Companion.NON_ARRIVAL_REASON_CUSTODIAL_DISPOSAL_RIC
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1BookingManagementInfoService

@ExtendWith(MockKExtension::class)
@SuppressWarnings("UnusedPrivateProperty")
class Cas1BookingManagementInfoServiceTest {

  @MockK
  private lateinit var departureReasonRepository: DepartureReasonRepository

  @MockK
  private lateinit var moveOnCategoryRepository: MoveOnCategoryRepository

  @MockK
  private lateinit var nonArrivalReasonRepository: NonArrivalReasonRepository

  @InjectMockKs
  private lateinit var service: Cas1BookingManagementInfoService

  @Test
  fun `use hardcoded non arrival reason for 1H`() {
    val reason = NonArrivalReasonEntityFactory().produce()

    every { nonArrivalReasonRepository.findByIdOrNull(NON_ARRIVAL_REASON_CUSTODIAL_DISPOSAL_RIC) } returns reason

    val result = service.getNonArrivalReason("1H")

    assertThat(result).isEqualTo(reason)
  }

  @Test
  fun `use mapped non arrival reason if not 1H`() {
    val reason = NonArrivalReasonEntityFactory().produce()

    every { nonArrivalReasonRepository.findByLegacyDeliusReasonCode("NOT1H") } returns reason

    val result = service.getNonArrivalReason("NOT1H")

    assertThat(result).isEqualTo(reason)
  }
}
