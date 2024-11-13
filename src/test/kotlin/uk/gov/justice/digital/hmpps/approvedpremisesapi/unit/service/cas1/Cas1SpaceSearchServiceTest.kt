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
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.CandidatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1SpaceSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.SpaceAvailability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.asApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceSearchService
import java.time.LocalDate
import java.util.UUID
import java.util.stream.Stream
import kotlin.enums.enumEntries

@ExtendWith(MockKExtension::class)
class Cas1SpaceSearchServiceTest {
  @MockK
  private lateinit var characteristicService: CharacteristicService

  @MockK
  private lateinit var spaceSearchRepository: Cas1SpaceSearchRepository

  @InjectMockKs
  private lateinit var service: Cas1SpaceSearchService

  @Test
  fun `findSpaces returns premises ordered by distance with the correct space availability`() {
    val candidatePremises1 = CandidatePremises(
      UUID.randomUUID(),
      1.0f,
      "AP1234",
      "QCODE1",
      ApprovedPremisesType.NORMAL,
      "Some AP",
      "1 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
      3,
    )

    val candidatePremises2 = CandidatePremises(
      UUID.randomUUID(),
      2.0f,
      "AP2345",
      "QCODE2",
      ApprovedPremisesType.ESAP,
      "Some Other AP",
      "2 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
      6,
    )

    val candidatePremises3 = CandidatePremises(
      UUID.randomUUID(),
      3.0f,
      "AP3456",
      "QCODE3",
      ApprovedPremisesType.PIPE,
      "Some AP",
      "3 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
      9,
    )

    val genderCharacteristics = Gender.entries.map { characteristicCalled(it.value) }
    val spaceCharacteristics = Cas1SpaceCharacteristic.entries.map { characteristicWithRandomModelScopeCalled(it.value) }

    every {
      characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises)
    } returnsMany listOf(
      genderCharacteristics,
      spaceCharacteristics,
    )

    every {
      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        any(),
        any(),
        any(),
        any(),
        any(),
      )
    } returns listOf(candidatePremises1, candidatePremises2, candidatePremises3)

    val spaceAvailability1 = SpaceAvailability(candidatePremises1.premisesId)
    val spaceAvailability2 = SpaceAvailability(candidatePremises2.premisesId)
    val spaceAvailability3 = SpaceAvailability(candidatePremises3.premisesId)

    every {
      spaceSearchRepository.getSpaceAvailabilityForCandidatePremises(
        any(),
        any(),
        any(),
      )
    } returns listOf(spaceAvailability3, spaceAvailability1, spaceAvailability2)

    val result = service.findSpaces(
      Cas1SpaceSearchParameters(
        startDate = LocalDate.parse("2024-08-01"),
        durationInDays = 14,
        targetPostcodeDistrict = "TB1",
        requirements = Cas1SpaceSearchRequirements(
          apTypes = ApType.entries,
          genders = Gender.entries,
          spaceCharacteristics = Cas1SpaceCharacteristic.entries,
        ),
      ),
    )

    assertThat(result).hasSize(3)
    assertThat(result).containsExactly(
      Cas1SpaceSearchResult(candidatePremises1, spaceAvailability1),
      Cas1SpaceSearchResult(candidatePremises2, spaceAvailability2),
      Cas1SpaceSearchResult(candidatePremises3, spaceAvailability3),
    )

    verify(exactly = 1) {
      characteristicService.getCharacteristicsByPropertyNames(
        listOf(
          // TBD: gender characteristics
        ),
        ServiceName.approvedPremises,
      )

      characteristicService.getCharacteristicsByPropertyNames(
        Cas1SpaceCharacteristic.entries.map { it.value },
        ServiceName.approvedPremises,
      )

      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        "TB1",
        ApType.entries.map { it.asApprovedPremisesType() },
        genderCharacteristics.map { it.id },
        spaceCharacteristics.filter { it.modelMatches("premises") }.map { it.id },
        spaceCharacteristics.filter { it.modelMatches("room") }.map { it.id },
      )

      spaceSearchRepository.getSpaceAvailabilityForCandidatePremises(
        listOf(candidatePremises1.premisesId, candidatePremises2.premisesId, candidatePremises3.premisesId),
        LocalDate.parse("2024-08-01"),
        14,
      )
    }

    confirmVerified()
  }

  @ParameterizedTest
  @MethodSource("apTypeArgs")
  fun `findSpaces uses the correct list of AP types`(apTypes: List<ApType>) {
    val candidatePremises1 = CandidatePremises(
      UUID.randomUUID(),
      1.0f,
      "AP1234",
      "QCODE1",
      ApprovedPremisesType.NORMAL,
      "Some AP",
      "1 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
      3,
    )

    val candidatePremises2 = CandidatePremises(
      UUID.randomUUID(),
      2.0f,
      "AP2345",
      "QCODE2",
      ApprovedPremisesType.ESAP,
      "Some Other AP",
      "2 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
      6,
    )

    val candidatePremises3 = CandidatePremises(
      UUID.randomUUID(),
      3.0f,
      "AP3456",
      "QCODE3",
      ApprovedPremisesType.PIPE,
      "Some AP",
      "3 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
      9,
    )

    val genderCharacteristics = Gender.entries.map { characteristicCalled(it.value) }
    val spaceCharacteristics = Cas1SpaceCharacteristic.entries.map { characteristicWithRandomModelScopeCalled(it.value) }

    every {
      characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises)
    } returnsMany listOf(
      genderCharacteristics,
      spaceCharacteristics,
    )

    every {
      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        any(),
        any(),
        any(),
        any(),
        any(),
      )
    } returns listOf(candidatePremises1, candidatePremises2, candidatePremises3)

    val spaceAvailability1 = SpaceAvailability(candidatePremises1.premisesId)
    val spaceAvailability2 = SpaceAvailability(candidatePremises2.premisesId)
    val spaceAvailability3 = SpaceAvailability(candidatePremises3.premisesId)

    every {
      spaceSearchRepository.getSpaceAvailabilityForCandidatePremises(
        any(),
        any(),
        any(),
      )
    } returns listOf(spaceAvailability3, spaceAvailability1, spaceAvailability2)

    service.findSpaces(
      Cas1SpaceSearchParameters(
        startDate = LocalDate.parse("2024-08-01"),
        durationInDays = 14,
        targetPostcodeDistrict = "TB1",
        requirements = Cas1SpaceSearchRequirements(
          apTypes = apTypes,
          genders = Gender.entries,
          spaceCharacteristics = Cas1SpaceCharacteristic.entries,
        ),
      ),
    )

    verify(exactly = 1) {
      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        "TB1",
        apTypes.map { it.asApprovedPremisesType() },
        any(),
        any(),
        any(),
      )
    }

    verify {
      characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises)

      spaceSearchRepository.getSpaceAvailabilityForCandidatePremises(
        any(),
        any(),
        any(),
      )
    }

    confirmVerified()
  }

  @ParameterizedTest
  @MethodSource("genderArgs")
  fun `findSpaces uses the correct gender characteristics`(genders: List<Gender>) {
    val candidatePremises1 = CandidatePremises(
      UUID.randomUUID(),
      1.0f,
      "AP1234",
      "QCODE1",
      ApprovedPremisesType.NORMAL,
      "Some AP",
      "1 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
      3,
    )

    val candidatePremises2 = CandidatePremises(
      UUID.randomUUID(),
      2.0f,
      "AP2345",
      "QCODE2",
      ApprovedPremisesType.ESAP,
      "Some Other AP",
      "2 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
      6,
    )

    val candidatePremises3 = CandidatePremises(
      UUID.randomUUID(),
      3.0f,
      "AP3456",
      "QCODE3",
      ApprovedPremisesType.PIPE,
      "Some AP",
      "3 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
      9,
    )

    val genderCharacteristics = genders.map { characteristicCalled(it.value) }
    val spaceCharacteristics = Cas1SpaceCharacteristic.entries.map { characteristicWithRandomModelScopeCalled(it.value) }

    every {
      characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises)
    } returnsMany listOf(
      genderCharacteristics,
      spaceCharacteristics,
    )

    every {
      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        any(),
        any(),
        any(),
        any(),
        any(),
      )
    } returns listOf(candidatePremises1, candidatePremises2, candidatePremises3)

    val spaceAvailability1 = SpaceAvailability(candidatePremises1.premisesId)
    val spaceAvailability2 = SpaceAvailability(candidatePremises2.premisesId)
    val spaceAvailability3 = SpaceAvailability(candidatePremises3.premisesId)

    every {
      spaceSearchRepository.getSpaceAvailabilityForCandidatePremises(
        any(),
        any(),
        any(),
      )
    } returns listOf(spaceAvailability3, spaceAvailability1, spaceAvailability2)

    service.findSpaces(
      Cas1SpaceSearchParameters(
        startDate = LocalDate.parse("2024-08-01"),
        durationInDays = 14,
        targetPostcodeDistrict = "TB1",
        requirements = Cas1SpaceSearchRequirements(
          apTypes = ApType.entries,
          genders = genders,
          spaceCharacteristics = Cas1SpaceCharacteristic.entries,
        ),
      ),
    )

    verify(exactly = 1) {
      characteristicService.getCharacteristicsByPropertyNames(
        listOf(
          // TBD: gender characteristics
        ),
        ServiceName.approvedPremises,
      )

      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        any(),
        any(),
        genderCharacteristics.map { it.id },
        any(),
        any(),
      )
    }

    verify {
      characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises)

      spaceSearchRepository.getSpaceAvailabilityForCandidatePremises(
        any(),
        any(),
        any(),
      )
    }

    confirmVerified()
  }

  @ParameterizedTest
  @MethodSource("spaceCharacteristicArgs")
  fun `findSpaces uses the correct space characteristics`(spaceCharacteristics: List<Cas1SpaceCharacteristic>) {
    val candidatePremises1 = CandidatePremises(
      UUID.randomUUID(),
      1.0f,
      "AP1234",
      "QCODE1",
      ApprovedPremisesType.NORMAL,
      "Some AP",
      "1 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
      3,
    )

    val candidatePremises2 = CandidatePremises(
      UUID.randomUUID(),
      2.0f,
      "AP2345",
      "QCODE2",
      ApprovedPremisesType.ESAP,
      "Some Other AP",
      "2 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
      6,
    )

    val candidatePremises3 = CandidatePremises(
      UUID.randomUUID(),
      3.0f,
      "AP3456",
      "QCODE3",
      ApprovedPremisesType.PIPE,
      "Some AP",
      "3 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
      9,
    )

    val genderCharacteristics = Gender.entries.map { characteristicCalled(it.value) }
    val spaceCharacteristicEntities = spaceCharacteristics.map { characteristicWithRandomModelScopeCalled(it.value) }

    every {
      characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises)
    } returnsMany listOf(
      genderCharacteristics,
      spaceCharacteristicEntities,
    )

    every {
      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        any(),
        any(),
        any(),
        any(),
        any(),
      )
    } returns listOf(candidatePremises1, candidatePremises2, candidatePremises3)

    val spaceAvailability1 = SpaceAvailability(candidatePremises1.premisesId)
    val spaceAvailability2 = SpaceAvailability(candidatePremises2.premisesId)
    val spaceAvailability3 = SpaceAvailability(candidatePremises3.premisesId)

    every {
      spaceSearchRepository.getSpaceAvailabilityForCandidatePremises(
        any(),
        any(),
        any(),
      )
    } returns listOf(spaceAvailability3, spaceAvailability1, spaceAvailability2)

    service.findSpaces(
      Cas1SpaceSearchParameters(
        startDate = LocalDate.parse("2024-08-01"),
        durationInDays = 14,
        targetPostcodeDistrict = "TB1",
        requirements = Cas1SpaceSearchRequirements(
          apTypes = ApType.entries,
          genders = Gender.entries,
          spaceCharacteristics = spaceCharacteristics,
        ),
      ),
    )

    verify(exactly = 1) {
      characteristicService.getCharacteristicsByPropertyNames(
        spaceCharacteristics.map { it.value },
        ServiceName.approvedPremises,
      )

      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        any(),
        any(),
        any(),
        spaceCharacteristicEntities.filter { it.modelMatches("premises") }.map { it.id },
        spaceCharacteristicEntities.filter { it.modelMatches("room") }.map { it.id },
      )
    }

    verify {
      characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises)

      spaceSearchRepository.getSpaceAvailabilityForCandidatePremises(
        any(),
        any(),
        any(),
      )
    }

    confirmVerified()
  }

  private fun characteristicCalled(name: String) = CharacteristicEntityFactory()
    .withName(name)
    .withPropertyName(name)
    .withServiceScope(ServiceName.approvedPremises.value)
    .withModelScope("*")
    .produce()

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
    fun genderArgs(): Stream<Arguments> = generateListEnumArgs<Gender>()

    @JvmStatic
    fun spaceCharacteristicArgs(): Stream<Arguments> = generateListEnumArgs<Cas1SpaceCharacteristic>()

    @OptIn(ExperimentalStdlibApi::class)
    private inline fun <reified T : Enum<T>> generateListEnumArgs(): Stream<Arguments> = Stream.concat(
      enumEntries<T>().map { Arguments.of(listOf(it)) }.stream(),
      Stream.of(Arguments.of(enumEntries<T>())),
    )
  }
}
