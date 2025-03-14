package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.seed.cas1

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.MoveOnCategoryEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NonArrivalReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository.Constants.MOVE_ON_CATEGORY_LOCAL_AUTHORITY_RENTED_ID
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

  @Nested
  inner class GetMoveOnCategory {

    @Test
    fun `use hardcoded move on category reason for MC02`() {
      val category = MoveOnCategoryEntityFactory().produce()

      every { moveOnCategoryRepository.findByIdOrNull(MOVE_ON_CATEGORY_LOCAL_AUTHORITY_RENTED_ID) } returns category

      val result = service.getMoveOnCategory("MC02")

      assertThat(result).isEqualTo(category)
    }

    @Test
    fun `use mapped move on category reason if not MC02`() {
      val category1 = MoveOnCategoryEntityFactory().withLegacyDeliusCategoryCode("CAT1").produce()
      val category2 = MoveOnCategoryEntityFactory().withLegacyDeliusCategoryCode("CAT2").produce()
      val category3 = MoveOnCategoryEntityFactory().withLegacyDeliusCategoryCode("CAT3").produce()

      every { moveOnCategoryRepository.findAllByServiceScope(ServiceName.approvedPremises.value) } returns listOf(
        category1,
        category2,
        category3,
      )

      val result = service.getMoveOnCategory("CAT2")

      assertThat(result).isEqualTo(category2)
    }
  }

  @Nested
  inner class GetNonArrivalReason {

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
}
