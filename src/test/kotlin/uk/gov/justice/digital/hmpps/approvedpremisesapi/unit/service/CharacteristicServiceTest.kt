package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceCharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3PremisesCharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspaceCharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import java.util.UUID

class CharacteristicServiceTest {
  private val cas1CharacteristicRepository = mockk<Cas1CharacteristicRepository>()
  private val cas3BedspaceCharacteristicRepository = mockk<Cas3BedspaceCharacteristicRepository>()
  private val cas3PremisesCharacteristicRepository = mockk<Cas3PremisesCharacteristicRepository>()
  private val characteristicService = CharacteristicService(
    cas1CharacteristicRepository,
    cas3BedspaceCharacteristicRepository,
    cas3PremisesCharacteristicRepository,
  )

  @Test
  fun `getCas1CharacteristicsByPropertyNames delegates to the cas1 repository and returns the matching characteristics`() {
    val propertyNames = listOf("isPIPE", "isESAP")
    val expected = listOf(
      Cas1CharacteristicEntityFactory().withPropertyName("isPIPE").produce(),
      Cas1CharacteristicEntityFactory().withPropertyName("isESAP").produce(),
    )

    every { cas1CharacteristicRepository.findAllWherePropertyNameIn(propertyNames) } returns expected

    val result = characteristicService.getCas1CharacteristicsByPropertyNames(propertyNames)

    assertThat(result).isEqualTo(expected)
    verify(exactly = 1) { cas1CharacteristicRepository.findAllWherePropertyNameIn(propertyNames) }
  }

  @Test
  fun `getCas1CharacteristicsByPropertyNames returns an empty list when no characteristics match`() {
    val propertyNames = listOf("doesNotExist")

    every { cas1CharacteristicRepository.findAllWherePropertyNameIn(propertyNames) } returns emptyList()

    val result = characteristicService.getCas1CharacteristicsByPropertyNames(propertyNames)

    assertThat(result).isEmpty()
  }

  @Test
  fun `getCas3BedspaceCharacteristic returns the characteristic when it exists`() {
    val id = UUID.randomUUID()
    val expected = Cas3BedspaceCharacteristicEntityFactory().withId(id).produce()

    every { cas3BedspaceCharacteristicRepository.findByIdOrNull(id) } returns expected

    val result = characteristicService.getCas3BedspaceCharacteristic(id)

    assertThat(result).isEqualTo(expected)
    verify(exactly = 1) { cas3BedspaceCharacteristicRepository.findByIdOrNull(id) }
  }

  @Test
  fun `getCas3BedspaceCharacteristic returns null when the characteristic does not exist`() {
    val id = UUID.randomUUID()

    every { cas3BedspaceCharacteristicRepository.findByIdOrNull(id) } returns null

    val result = characteristicService.getCas3BedspaceCharacteristic(id)

    assertNull(result)
  }

  @Test
  fun `getCas3BedspaceCharacteristics returns only active bedspace characteristics`() {
    val expected = listOf(
      Cas3BedspaceCharacteristicEntityFactory().withIsActive(true).produce(),
      Cas3BedspaceCharacteristicEntityFactory().withIsActive(true).produce(),
    )

    every { cas3BedspaceCharacteristicRepository.findByActive(active = true) } returns expected

    val result = characteristicService.getCas3BedspaceCharacteristics()

    assertThat(result).isEqualTo(expected)
    verify(exactly = 1) { cas3BedspaceCharacteristicRepository.findByActive(active = true) }
  }

  @Test
  fun `getCas3PremisesCharacteristics returns only active premises characteristics`() {
    val expected = listOf(
      Cas3PremisesCharacteristicEntityFactory().isActive(true).produce(),
      Cas3PremisesCharacteristicEntityFactory().isActive(true).produce(),
    )

    every { cas3PremisesCharacteristicRepository.findByActive(active = true) } returns expected

    val result = characteristicService.getCas3PremisesCharacteristics()

    assertThat(result).isEqualTo(expected)
    verify(exactly = 1) { cas3PremisesCharacteristicRepository.findByActive(active = true) }
  }
}
