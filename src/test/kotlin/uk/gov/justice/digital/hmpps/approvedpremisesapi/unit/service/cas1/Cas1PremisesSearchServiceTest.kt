package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
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

  @MockK
  private lateinit var applicationRepository: ApprovedPremisesApplicationRepository

  @InjectMockKs
  private lateinit var service: Cas1PremisesSearchService

  @Test
  fun `throws error if the associated application cannot be found`() {
    val applicationId = UUID.randomUUID()

    every { applicationRepository.findByIdOrNull(any()) } returns null

    assertThatThrownBy {
      service.findPremises(
        Cas1PremisesSearchService.Cas1PremisesSearchCriteria(
          applicationId = applicationId,
          targetPostcodeDistrict = "TB1",
          spaceCharacteristics = Cas1SpaceCharacteristic.entries,
        ),
      )
    }.hasMessage(
      "Not Found: No Application with an ID of $applicationId could be found",
    )
  }

  @Test
  fun `returns premises ordered by distance with the correct space availability`() {
    val application = ApprovedPremisesApplicationEntityFactory()
      .withDefaults()
      .withIsWomensApplication(false)
      .produce()

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

    every { applicationRepository.findByIdOrNull(application.id) } returns application

    val result = service.findPremises(
      Cas1PremisesSearchService.Cas1PremisesSearchCriteria(
        applicationId = application.id,
        targetPostcodeDistrict = "TB1",
        spaceCharacteristics = Cas1SpaceCharacteristic.entries,
      ),
    )

    assertThat(result).hasSize(3)
    assertThat(result).containsExactly(
      candidatePremises1,
      candidatePremises2,
      candidatePremises3,
    )

    verify(exactly = 1) {
      applicationRepository.findByIdOrNull(application.id)
    }

    verify(exactly = 1) {
      characteristicService.getCharacteristicsByPropertyNames(
        Cas1SpaceCharacteristic.entries.map { it.value },
        ServiceName.approvedPremises,
      )
    }

    verify(exactly = 1) {
      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        "TB1",
        isWomensPremises = false,
        spaceCharacteristics.filter { it.modelMatches("premises") }.map { it.id },
        spaceCharacteristics.filter { it.modelMatches("room") }.map { it.id },
      )
    }

    confirmVerified()
  }

  @ParameterizedTest
  @CsvSource("true", "false")
  fun `uses the associated gender from the application`(isWomensApplication: Boolean) {
    val application = ApprovedPremisesApplicationEntityFactory()
      .withDefaults()
      .withIsWomensApplication(isWomensApplication)
      .produce()

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
      characteristics = emptyList(),
    )

    val spaceCharacteristics = Cas1SpaceCharacteristic.entries.map { characteristicWithRandomModelScopeCalled(it.value) }

    every { applicationRepository.findByIdOrNull(application.id) } returns application

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
        applicationId = application.id,
        spaceCharacteristics = Cas1SpaceCharacteristic.entries,
        targetPostcodeDistrict = "TB1",
      ),
    )

    verify(exactly = 1) {
      applicationRepository.findByIdOrNull(application.id)

      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        any(),
        isWomensPremises = isWomensApplication,
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
    val application = ApprovedPremisesApplicationEntityFactory()
      .withDefaults()
      .withIsWomensApplication(false)
      .produce()

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
      characteristics = emptyList(),
    )

    val spaceCharacteristicEntities = spaceCharacteristics.map { characteristicWithRandomModelScopeCalled(it.value) }

    every { applicationRepository.findByIdOrNull(application.id) } returns application

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
        applicationId = application.id,
        targetPostcodeDistrict = "TB1",
        spaceCharacteristics = spaceCharacteristics,
      ),
    )

    verify(exactly = 1) {
      applicationRepository.findByIdOrNull(application.id)

      characteristicService.getCharacteristicsByPropertyNames(
        spaceCharacteristics.map { it.value },
        ServiceName.approvedPremises,
      )

      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        any(),
        any(),
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
