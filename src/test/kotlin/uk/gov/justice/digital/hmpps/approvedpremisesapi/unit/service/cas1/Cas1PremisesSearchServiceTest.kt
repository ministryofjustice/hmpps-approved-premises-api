package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.CandidatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1SpaceSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesSearchService
import java.util.UUID
import java.util.stream.Stream
import kotlin.enums.enumEntries

@ExtendWith(MockKExtension::class)
class Cas1PremisesSearchServiceTest {
  @MockK
  private lateinit var characteristicService: CharacteristicService

  @MockK
  private lateinit var spaceSearchRepository: Cas1SpaceSearchRepository

  @InjectMockKs
  private lateinit var service: Cas1PremisesSearchService

  @Test
  fun `returns premises ordered by distance with the correct space availability`() {
    val candidatePremises1 = CandidatePremises(
      UUID.randomUUID(),
      1.0f,
      ApprovedPremisesType.NORMAL,
      "Some AP",
      fullAddress = "the full address",
      "1 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
      "Area1",
      characteristics = emptyList(),
    )

    val candidatePremises2 = CandidatePremises(
      UUID.randomUUID(),
      2.0f,
      ApprovedPremisesType.ESAP,
      "Some Other AP",
      fullAddress = "the full address",
      "2 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
      "Area2",
      characteristics = emptyList(),
    )

    val candidatePremises3 = CandidatePremises(
      UUID.randomUUID(),
      3.0f,
      ApprovedPremisesType.PIPE,
      "Some AP",
      fullAddress = "the full address",
      "3 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
      "Area3",
      characteristics = emptyList(),
    )

    val spaceCharacteristics = Cas1SpaceCharacteristic.entries.map { characteristicWithRandomModelScopeCalled(it.value) }

    every {
      characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises)
    } returnsMany listOf(
      spaceCharacteristics,
    )

    every {
      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        any(),
        any(),
        any(),
        any(),
      )
    } returns listOf(candidatePremises1, candidatePremises2, candidatePremises3)

    val result = service.findPremises(
      Cas1PremisesSearchService.Cas1PremisesSearchCriteria(
        gender = ApprovedPremisesGender.MAN,
        targetPostcodeDistrict = "TB1",
        spaceCharacteristics = Cas1SpaceCharacteristic.entries.toSet(),
      ),
    )

    assertThat(result).hasSize(3)
    assertThat(result).containsExactly(
      candidatePremises1,
      candidatePremises2,
      candidatePremises3,
    )

    verify(exactly = 1) {
      characteristicService.getCharacteristicsByPropertyNames(
        Cas1SpaceCharacteristic.entries.map { it.value },
        ServiceName.approvedPremises,
      )
    }

    verify(exactly = 1) {
      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        "TB1",
        gender = ApprovedPremisesGender.MAN,
        spaceCharacteristics.filter { it.modelMatches("premises") }.map { it.id },
        spaceCharacteristics.filter { it.modelMatches("room") }.map { it.id },
      )
    }

    confirmVerified()
  }

  @ParameterizedTest
  @CsvSource("MAN", "WOMAN", "NULL", nullValues = ["NULL"])
  fun `pass provided gender, if defined`(gender: ApprovedPremisesGender?) {
    val candidatePremises1 = CandidatePremises(
      UUID.randomUUID(),
      1.0f,
      ApprovedPremisesType.NORMAL,
      "Some AP",
      fullAddress = "the full address",
      "1 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
      "Area1",
      characteristics = emptyList(),
    )

    val candidatePremises2 = CandidatePremises(
      UUID.randomUUID(),
      2.0f,
      ApprovedPremisesType.ESAP,
      "Some Other AP",
      fullAddress = "the full address",
      "2 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
      "Area2",
      characteristics = emptyList(),
    )

    val candidatePremises3 = CandidatePremises(
      UUID.randomUUID(),
      3.0f,
      ApprovedPremisesType.PIPE,
      "Some AP",
      fullAddress = "the full address",
      "3 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
      "Area3",
      characteristics = emptyList(),
    )

    val spaceCharacteristics = Cas1SpaceCharacteristic.entries.map { characteristicWithRandomModelScopeCalled(it.value) }

    every {
      characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises)
    } returnsMany listOf(
      spaceCharacteristics,
    )

    every {
      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        any(),
        any(),
        any(),
        any(),
      )
    } returns listOf(candidatePremises1, candidatePremises2, candidatePremises3)

    service.findPremises(
      Cas1PremisesSearchService.Cas1PremisesSearchCriteria(
        gender = gender,
        spaceCharacteristics = Cas1SpaceCharacteristic.entries.toSet(),
        targetPostcodeDistrict = "TB1",
      ),
    )

    verify(exactly = 1) {
      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        any(),
        gender = gender,
        any(),
        any(),
      )
    }

    verify {
      characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises)
    }
    confirmVerified()
  }

  @ParameterizedTest
  @MethodSource("spaceCharacteristicArgs")
  fun `uses the correct space characteristics`(spaceCharacteristics: List<Cas1SpaceCharacteristic>) {
    val candidatePremises1 = CandidatePremises(
      UUID.randomUUID(),
      1.0f,
      ApprovedPremisesType.NORMAL,
      "Some AP",
      fullAddress = "the full address",
      "1 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
      "Area1",
      characteristics = emptyList(),
    )

    val candidatePremises2 = CandidatePremises(
      UUID.randomUUID(),
      2.0f,
      ApprovedPremisesType.ESAP,
      "Some Other AP",
      fullAddress = "the full address",
      "2 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
      "Area2",
      characteristics = emptyList(),
    )

    val candidatePremises3 = CandidatePremises(
      UUID.randomUUID(),
      3.0f,
      ApprovedPremisesType.PIPE,
      "Some AP",
      fullAddress = "the full address",
      "3 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
      "Area3",
      characteristics = emptyList(),
    )

    val spaceCharacteristicEntities = spaceCharacteristics.map { characteristicWithRandomModelScopeCalled(it.value) }

    every {
      characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises)
    } returnsMany listOf(
      spaceCharacteristicEntities,
    )

    every {
      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        any(),
        any(),
        any(),
        any(),
      )
    } returns listOf(candidatePremises1, candidatePremises2, candidatePremises3)

    service.findPremises(
      Cas1PremisesSearchService.Cas1PremisesSearchCriteria(
        gender = ApprovedPremisesGender.MAN,
        targetPostcodeDistrict = "TB1",
        spaceCharacteristics = spaceCharacteristics.toSet(),
      ),
    )

    verify(exactly = 1) {
      characteristicService.getCharacteristicsByPropertyNames(
        spaceCharacteristics.map { it.value },
        ServiceName.approvedPremises,
      )

      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        any(),
        ApprovedPremisesGender.MAN,
        spaceCharacteristicEntities.filter { it.modelMatches("premises") }.map { it.id },
        spaceCharacteristicEntities.filter { it.modelMatches("room") }.map { it.id },
      )
    }

    verify {
      characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises)
    }

    confirmVerified()
  }

  private fun characteristicWithRandomModelScopeCalled(name: String) = CharacteristicEntityFactory()
    .withName(name)
    .withPropertyName(name)
    .withServiceScope(ServiceName.approvedPremises.value)
    .produce()

  companion object {
    @JvmStatic
    @BeforeAll
    fun setup() {
      clearAllMocks()
    }

    @JvmStatic
    fun apTypeArgs(): Stream<Arguments> = generateListEnumArgs<ApType>()

    @JvmStatic
    fun spaceCharacteristicArgs(): Stream<Arguments> = generateListEnumArgs<Cas1SpaceCharacteristic>()

    private inline fun <reified T : Enum<T>> generateListEnumArgs(): Stream<Arguments> = Stream.concat(
      enumEntries<T>().map { Arguments.of(listOf(it)) }.stream(),
      Stream.of(Arguments.of(enumEntries<T>())),
    )
  }
}
