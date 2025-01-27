package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
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

    @Test
    fun success() {
      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withDefaults()
        .produce()

      fun createCas1Characteristic(propertyName: String) = CharacteristicEntityFactory()
        .withServiceScope("approved-premises")
        .withPropertyName(propertyName)
        .produce()

      val characteristicIsSingle = createCas1Characteristic("isSingle")
      val characteristicIsPipe = createCas1Characteristic("isPIPE")
      val characteristicIsEsap = createCas1Characteristic("isESAP")
      val characteristicIsCatered = createCas1Characteristic("isCatered")

      every { postcodeDistrictRepository.findByOutcode("location") } returns PostCodeDistrictEntityFactory().produce()

      val savedRequirementsCaptor = slot<PlacementRequirementsEntity>()
      every {
        placementRequirementsRepository.save(capture(savedRequirementsCaptor))
      } returns PlacementRequirementsEntityFactory().withDefaults().produce()

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
  }
}
