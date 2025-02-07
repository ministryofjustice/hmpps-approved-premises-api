package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJobService

class Cas1IsArsonSuitableBackfillJobTest : IntegrationTestBase() {
  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @Test
  fun success() {
    val characteristicArsonDesignated = characteristicRepository.findCas1ByPropertyName("isArsonDesignated")!!
    val characteristicArsonOffences = characteristicRepository.findCas1ByPropertyName("arsonOffences")!!
    val characteristicArsonSuitable = characteristicRepository.findCas1ByPropertyName("isArsonSuitable")!!
    val characteristicEnSuite = characteristicRepository.findCas1ByPropertyName("hasEnSuite")!!
    val characteristicPipe = characteristicRepository.findCas1ByPropertyName("isPIPE")!!

    val noCriteria = givenAPlacementRequirements(
      desirableCharacteristics = emptyList(),
      essentialCharacteristics = emptyList(),
    )

    val noAffectedCriteria = givenAPlacementRequirements(
      desirableCharacteristics = listOf(characteristicPipe, characteristicEnSuite),
      essentialCharacteristics = listOf(characteristicEnSuite, characteristicPipe),
    )

    val hasArsonOffencesDesirable = givenAPlacementRequirements(
      desirableCharacteristics = listOf(characteristicArsonOffences),
      essentialCharacteristics = emptyList(),
    )

    val hasArsonOffencesEssential = givenAPlacementRequirements(
      desirableCharacteristics = emptyList(),
      essentialCharacteristics = listOf(characteristicArsonOffences),
    )

    val hasArsonDesignatedDesirable = givenAPlacementRequirements(
      desirableCharacteristics = listOf(characteristicArsonDesignated),
      essentialCharacteristics = emptyList(),
    )

    val hasArsonDesignatedEssential = givenAPlacementRequirements(
      desirableCharacteristics = emptyList(),
      essentialCharacteristics = listOf(characteristicArsonDesignated),
    )

    val hasArsonOffencesAndArsonDesignatedDesirable = givenAPlacementRequirements(
      desirableCharacteristics = listOf(characteristicArsonOffences, characteristicArsonDesignated),
      essentialCharacteristics = emptyList(),
    )

    val hasArsonOffencesAndArsonDesignatedEssential = givenAPlacementRequirements(
      desirableCharacteristics = emptyList(),
      essentialCharacteristics = listOf(characteristicArsonOffences, characteristicArsonDesignated),
    )

    val hasAllArsonCharacteristicsAlreadyDesirable = givenAPlacementRequirements(
      desirableCharacteristics = listOf(characteristicArsonOffences, characteristicArsonDesignated, characteristicArsonSuitable),
      essentialCharacteristics = emptyList(),
    )

    val hasAllArsonCharacteristicsAlreadyEssential = givenAPlacementRequirements(
      desirableCharacteristics = emptyList(),
      essentialCharacteristics = listOf(characteristicArsonOffences, characteristicArsonDesignated, characteristicArsonSuitable),
    )

    migrationJobService.runMigrationJob(MigrationJobType.cas1BackfillArsonSuitable, 1)

    val noCriteriaUpdated = placementRequirementsRepository.findById(noCriteria.id).get()
    assertThat(noCriteriaUpdated.desirableCriteria).isEmpty()
    assertThat(noCriteriaUpdated.essentialCriteria).isEmpty()

    val noEffectedCriteriaUpdated = placementRequirementsRepository.findById(noAffectedCriteria.id).get()
    assertThat(noEffectedCriteriaUpdated.desirableCriteria).containsExactlyInAnyOrder(characteristicPipe, characteristicEnSuite)
    assertThat(noEffectedCriteriaUpdated.essentialCriteria).containsExactlyInAnyOrder(characteristicPipe, characteristicEnSuite)

    val hasArsonOffencesEssentialUpdated = placementRequirementsRepository.findById(hasArsonOffencesEssential.id).get()
    assertThat(hasArsonOffencesEssentialUpdated.desirableCriteria).isEmpty()
    assertThat(hasArsonOffencesEssentialUpdated.essentialCriteria).containsExactlyInAnyOrder(characteristicArsonSuitable, characteristicArsonOffences)

    val hasArsonOffencesDesirableUpdated = placementRequirementsRepository.findById(hasArsonOffencesDesirable.id).get()
    assertThat(hasArsonOffencesDesirableUpdated.desirableCriteria).containsExactlyInAnyOrder(characteristicArsonSuitable, characteristicArsonOffences)
    assertThat(hasArsonOffencesDesirableUpdated.essentialCriteria).isEmpty()

    val hasArsonDesignatedEssentialUpdated = placementRequirementsRepository.findById(hasArsonDesignatedEssential.id).get()
    assertThat(hasArsonDesignatedEssentialUpdated.desirableCriteria).isEmpty()
    assertThat(hasArsonDesignatedEssentialUpdated.essentialCriteria).containsExactlyInAnyOrder(characteristicArsonSuitable, characteristicArsonDesignated)

    val hasArsonDesignatedDesirableUpdated = placementRequirementsRepository.findById(hasArsonDesignatedDesirable.id).get()
    assertThat(hasArsonDesignatedDesirableUpdated.desirableCriteria).containsExactlyInAnyOrder(characteristicArsonSuitable, characteristicArsonDesignated)
    assertThat(hasArsonDesignatedDesirableUpdated.essentialCriteria).isEmpty()

    val hasArsonOffencesAndArsonDesignatedEssentialUpdated = placementRequirementsRepository.findById(hasArsonOffencesAndArsonDesignatedEssential.id).get()
    assertThat(hasArsonOffencesAndArsonDesignatedEssentialUpdated.desirableCriteria).isEmpty()
    assertThat(hasArsonOffencesAndArsonDesignatedEssentialUpdated.essentialCriteria).containsExactlyInAnyOrder(characteristicArsonSuitable, characteristicArsonOffences, characteristicArsonDesignated)

    val hasArsonOffencesAndArsonDesignatedDesirableUpdated = placementRequirementsRepository.findById(hasArsonOffencesAndArsonDesignatedDesirable.id).get()
    assertThat(hasArsonOffencesAndArsonDesignatedDesirableUpdated.desirableCriteria).containsExactlyInAnyOrder(characteristicArsonSuitable, characteristicArsonOffences, characteristicArsonDesignated)
    assertThat(hasArsonOffencesAndArsonDesignatedDesirableUpdated.essentialCriteria).isEmpty()

    val hasAllArsonCharacteristicsAlreadyDesirableUpdated = placementRequirementsRepository.findById(hasAllArsonCharacteristicsAlreadyDesirable.id).get()
    assertThat(hasAllArsonCharacteristicsAlreadyDesirableUpdated.desirableCriteria).containsExactlyInAnyOrder(characteristicArsonSuitable, characteristicArsonOffences, characteristicArsonDesignated)
    assertThat(hasAllArsonCharacteristicsAlreadyDesirableUpdated.essentialCriteria).isEmpty()

    val hasAllArsonCharacteristicsAlreadyEssentialUpdated = placementRequirementsRepository.findById(hasAllArsonCharacteristicsAlreadyEssential.id).get()
    assertThat(hasAllArsonCharacteristicsAlreadyEssentialUpdated.desirableCriteria).isEmpty()
    assertThat(hasAllArsonCharacteristicsAlreadyEssentialUpdated.essentialCriteria).containsExactlyInAnyOrder(characteristicArsonSuitable, characteristicArsonOffences, characteristicArsonDesignated)
  }
}
