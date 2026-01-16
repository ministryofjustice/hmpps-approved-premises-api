package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller.Cas3RefDataType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceCharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3PremisesCharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3VoidBedspaceReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspaceCharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ReferenceData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.ReferenceData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3ReferenceDataService

@ExtendWith(MockKExtension::class)
class Cas3ReferenceDataServiceTest {
  @MockK
  lateinit var bedspaceCharacteristicRepository: Cas3BedspaceCharacteristicRepository

  @MockK
  lateinit var premisesCharacteristicRepository: Cas3PremisesCharacteristicRepository

  @MockK
  lateinit var cas3VoidBedspaceReasonRepository: Cas3VoidBedspaceReasonRepository

  @InjectMockKs
  lateinit var referenceDataService: Cas3ReferenceDataService

  private fun toCas3ReferenceData(refData: ReferenceData) = listOf(
    Cas3ReferenceData(
      refData.id,
      refData.description,
      refData.name,
    ),
  )

  @Test
  fun `BEDSPACE_CHARACTERISTICS calls correct repository and maps correctly`() {
    val characteristic = Cas3BedspaceCharacteristicEntityFactory().produce()
    every { bedspaceCharacteristicRepository.findAllByActiveIsTrue() } returns listOf(characteristic)
    val result = referenceDataService.getReferenceData(Cas3RefDataType.BEDSPACE_CHARACTERISTICS)
    verify(exactly = 1) { bedspaceCharacteristicRepository.findAllByActiveIsTrue() }
    assertThat(result).isEqualTo(toCas3ReferenceData(characteristic))
  }

  @Test
  fun `PREMISES_CHARACTERISTICS calls correct repository and maps correctly`() {
    val characteristic = Cas3PremisesCharacteristicEntityFactory().produce()
    every { premisesCharacteristicRepository.findAllByActiveIsTrue() } returns listOf(characteristic)
    val result = referenceDataService.getReferenceData(Cas3RefDataType.PREMISES_CHARACTERISTICS)
    verify(exactly = 1) { premisesCharacteristicRepository.findAllByActiveIsTrue() }
    assertThat(result).isEqualTo(toCas3ReferenceData(characteristic))
  }

  @Test
  fun `VOID_BEDSPACE_REASONS calls correct repository and maps correctly`() {
    val reason = Cas3VoidBedspaceReasonEntityFactory().produce()
    every { cas3VoidBedspaceReasonRepository.findAllActive() } returns listOf(reason)
    val result = referenceDataService.getReferenceData(Cas3RefDataType.VOID_BEDSPACE_REASONS)
    verify(exactly = 1) { cas3VoidBedspaceReasonRepository.findAllActive() }
    assertThat(result).isEqualTo(toCas3ReferenceData(reason))
  }
}
