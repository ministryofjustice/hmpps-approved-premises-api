package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.capture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PostCodeDistrictEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostcodeDistrictRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequirementsService

@ExtendWith(MockKExtension::class)
class Cas1PlacementRequirementsServiceTest {

  @MockK
  private lateinit var postcodeDistrictRepository: PostcodeDistrictRepository

  @MockK
  private lateinit var characteristicRepository: CharacteristicRepository

  @MockK
  private lateinit var placementRequirementsRepository: PlacementRequirementsRepository

  @InjectMockKs
  private lateinit var service: Cas1PlacementRequirementsService

  @Nested
  inner class CreatePlacementRequirements {

    private val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withDefaults()
      .produce()

    private val savedRequirementsCaptor = slot<PlacementRequirementsEntity>()

    private val characteristicIsArsonDesignated = createCas1Characteristic("isArsonDesignated")
    private val characteristicIsArsonSuitable = createCas1Characteristic("isArsonSuitable")
    private val characteristicIsSingle = createCas1Characteristic("isSingle")
    private val characteristicIsPipe = createCas1Characteristic("isPIPE")
    private val characteristicIsEsap = createCas1Characteristic("isESAP")
    private val characteristicIsCatered = createCas1Characteristic("isCatered")

    @BeforeEach
    fun setup() {
      every { postcodeDistrictRepository.findByOutcode("location") } returns PostCodeDistrictEntityFactory().produce()

      every {
        placementRequirementsRepository.save(capture(savedRequirementsCaptor))
      } returns PlacementRequirementsEntityFactory().withDefaults().produce()
    }

    @Test
    fun success() {
      every {
        characteristicRepository.findAllWherePropertyNameIn(
          listOf(
            "isSingle", "isPIPE",
          ),
          "approved-premises",
        )
      } returns listOf(characteristicIsSingle, characteristicIsPipe)

      every {
        characteristicRepository.findAllWherePropertyNameIn(
          listOf(
            "isESAP", "isCatered",
          ),
          "approved-premises",
        )
      } returns listOf(characteristicIsEsap, characteristicIsCatered)

      service.createPlacementRequirements(
        assessment = assessment,
        requirements = PlacementRequirements(
          gender = Gender.male,
          type = ApType.normal,
          location = "location",
          radius = 5,
          essentialCriteria = listOf(PlacementCriteria.isSingle, PlacementCriteria.isPIPE),
          desirableCriteria = listOf(PlacementCriteria.isESAP, PlacementCriteria.isCatered),
        ),
      )

      val savedRequirements = savedRequirementsCaptor.captured

      assertThat(savedRequirements.essentialCriteria)
        .containsExactlyInAnyOrder(characteristicIsSingle, characteristicIsPipe)

      assertThat(savedRequirements.desirableCriteria)
        .containsExactlyInAnyOrder(characteristicIsEsap, characteristicIsCatered)
    }

    @Test
    fun `add isArsonSuitable if isArsonDesignated is specified`() {
      every {
        characteristicRepository.findAllWherePropertyNameIn(
          listOf(
            "isSingle", "isArsonDesignated", "isArsonSuitable",
          ),
          "approved-premises",
        )
      } returns listOf(characteristicIsSingle, characteristicIsArsonDesignated, characteristicIsArsonSuitable)

      every {
        characteristicRepository.findAllWherePropertyNameIn(
          listOf(
            "isArsonDesignated", "isCatered", "isArsonSuitable",
          ),
          "approved-premises",
        )
      } returns listOf(characteristicIsArsonDesignated, characteristicIsArsonSuitable, characteristicIsCatered)

      service.createPlacementRequirements(
        assessment = assessment,
        requirements = PlacementRequirements(
          gender = Gender.male,
          type = ApType.normal,
          location = "location",
          radius = 5,
          essentialCriteria = listOf(PlacementCriteria.isSingle, PlacementCriteria.isArsonDesignated),
          desirableCriteria = listOf(PlacementCriteria.isArsonDesignated, PlacementCriteria.isCatered),
        ),
      )

      val savedRequirements = savedRequirementsCaptor.captured

      assertThat(savedRequirements.essentialCriteria)
        .containsExactlyInAnyOrder(characteristicIsSingle, characteristicIsArsonSuitable, characteristicIsArsonDesignated)

      assertThat(savedRequirements.desirableCriteria)
        .containsExactlyInAnyOrder(characteristicIsArsonSuitable, characteristicIsArsonDesignated, characteristicIsCatered)
    }

    @Test
    fun `dont add isArsonSuitable if isArsonDesignated is specified and isArsonSuitable is already specified`() {
      every {
        characteristicRepository.findAllWherePropertyNameIn(
          listOf(
            "isSingle", "isArsonDesignated", "isArsonSuitable",
          ),
          "approved-premises",
        )
      } returns listOf(characteristicIsSingle, characteristicIsArsonDesignated, characteristicIsArsonSuitable)

      every {
        characteristicRepository.findAllWherePropertyNameIn(
          listOf(
            "isArsonSuitable", "isArsonDesignated", "isCatered",
          ),
          "approved-premises",
        )
      } returns listOf(characteristicIsArsonDesignated, characteristicIsArsonSuitable, characteristicIsCatered)

      service.createPlacementRequirements(
        assessment = assessment,
        requirements = PlacementRequirements(
          gender = Gender.male,
          type = ApType.normal,
          location = "location",
          radius = 5,
          essentialCriteria = listOf(PlacementCriteria.isSingle, PlacementCriteria.isArsonDesignated, PlacementCriteria.isArsonSuitable),
          desirableCriteria = listOf(PlacementCriteria.isArsonSuitable, PlacementCriteria.isArsonDesignated, PlacementCriteria.isCatered),
        ),
      )

      val savedRequirements = savedRequirementsCaptor.captured

      assertThat(savedRequirements.essentialCriteria)
        .containsExactlyInAnyOrder(characteristicIsSingle, characteristicIsArsonSuitable, characteristicIsArsonDesignated)

      assertThat(savedRequirements.desirableCriteria)
        .containsExactlyInAnyOrder(characteristicIsArsonSuitable, characteristicIsArsonDesignated, characteristicIsCatered)
    }

    private fun createCas1Characteristic(propertyName: String) = CharacteristicEntityFactory()
      .withServiceScope("approved-premises")
      .withPropertyName(propertyName)
      .produce()
  }
}
