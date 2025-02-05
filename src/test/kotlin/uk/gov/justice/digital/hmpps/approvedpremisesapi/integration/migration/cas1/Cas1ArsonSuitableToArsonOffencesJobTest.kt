package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJobService

class Cas1ArsonSuitableToArsonOffencesJobTest : IntegrationTestBase() {
  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @Test
  fun success() {
    val characteristicArsonDesignated = characteristicRepository.findCas1ByPropertyName("isArsonDesignated")!!
    val characteristicArsonOffences = characteristicRepository.findCas1ByPropertyName("arsonOffences")!!
    val characteristicArsonSuitable = characteristicRepository.findCas1ByPropertyName("isArsonSuitable")!!
    val characteristicEnSuite = characteristicRepository.findCas1ByPropertyName("hasEnSuite")!!

    val noCriteria = givenAPlacementRequirements(
      desirableCharacteristics = emptyList(),
      essentialCharacteristics = emptyList(),
    )

    val noEffectedCriteria = givenAPlacementRequirements(
      desirableCharacteristics = listOf(characteristicArsonDesignated, characteristicEnSuite),
      essentialCharacteristics = listOf(characteristicArsonDesignated, characteristicEnSuite),
    )

    val updateDesirableOnly = givenAPlacementRequirements(
      desirableCharacteristics = listOf(characteristicEnSuite, characteristicArsonSuitable),
      essentialCharacteristics = listOf(characteristicArsonDesignated),
    )

    val updateEssentialOnly = givenAPlacementRequirements(
      desirableCharacteristics = listOf(characteristicEnSuite),
      essentialCharacteristics = listOf(characteristicEnSuite, characteristicArsonSuitable, characteristicArsonDesignated),
    )

    val updateDesirableAndEssential = givenAPlacementRequirements(
      desirableCharacteristics = listOf(characteristicEnSuite, characteristicArsonSuitable, characteristicArsonDesignated),
      essentialCharacteristics = listOf(characteristicEnSuite, characteristicArsonSuitable),
    )

    migrationJobService.runMigrationJob(MigrationJobType.cas1ArsonSuitableToArsonOffences, 1)

    val noCriteriaUpdated = placementRequirementsRepository.findById(noCriteria.id).get()
    assertThat(noCriteriaUpdated.desirableCriteria).isEmpty()
    assertThat(noCriteriaUpdated.essentialCriteria).isEmpty()

    val noEffectedCriteriaUpdated = placementRequirementsRepository.findById(noEffectedCriteria.id).get()
    assertThat(noEffectedCriteriaUpdated.desirableCriteria).containsExactlyInAnyOrder(characteristicArsonDesignated, characteristicEnSuite)
    assertThat(noEffectedCriteriaUpdated.essentialCriteria).containsExactlyInAnyOrder(characteristicArsonDesignated, characteristicEnSuite)

    val updateDesirableOnlyUpdated = placementRequirementsRepository.findById(updateDesirableOnly.id).get()
    assertThat(updateDesirableOnlyUpdated.desirableCriteria).containsExactlyInAnyOrder(characteristicEnSuite, characteristicArsonOffences)
    assertThat(updateDesirableOnlyUpdated.essentialCriteria).containsExactlyInAnyOrder(characteristicArsonDesignated)

    val updateEssentialOnlyUpdated = placementRequirementsRepository.findById(updateEssentialOnly.id).get()
    assertThat(updateEssentialOnlyUpdated.desirableCriteria).containsExactlyInAnyOrder(characteristicEnSuite)
    assertThat(updateEssentialOnlyUpdated.essentialCriteria).containsExactlyInAnyOrder(characteristicEnSuite, characteristicArsonOffences, characteristicArsonDesignated)

    val updateDesirableAndEssentialUpdated = placementRequirementsRepository.findById(updateDesirableAndEssential.id).get()
    assertThat(updateDesirableAndEssentialUpdated.desirableCriteria).containsExactlyInAnyOrder(characteristicEnSuite, characteristicArsonOffences, characteristicArsonDesignated)
    assertThat(updateDesirableAndEssentialUpdated.essentialCriteria).containsExactlyInAnyOrder(characteristicEnSuite, characteristicArsonOffences)
  }
}
