package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import java.util.UUID

/**
 * Replaces all occurrences of 'isArsonSuitable' in placement requests with 'arsonOffences'
 */
@Component
class Cas1ArsonSuitableToArsonOffencesJob(
  private val repository: Cas1ArsonSuitableToArsonOffencesRepository,
  private val migrationLogger: MigrationLogger,
  override val shouldRunInTransaction: Boolean = true,
) : MigrationJob() {

  override fun process(pageSize: Int) {
    val desirableAdded = repository.addArsonOffencesToDesirableCriteriaWhereHasArsonSuitable()
    migrationLogger.info("Have added arson offences to $desirableAdded desirable criteria")

    val essentialAdded = repository.addArsonOffencesToEssentialCriteriaWhereHasArsonSuitable()
    migrationLogger.info("Have added arson offences to $essentialAdded essential criteria")

    val desirableRemoved = repository.removeArsonSuitableFromDesirableCriteria()
    migrationLogger.info("Have remove arson suitable from $desirableRemoved desirable criteria")

    val essentialRemoved = repository.removeArsonOffencesFromEssentialCriteria()
    migrationLogger.info("Have remove arson suitable from $essentialRemoved essential criteria")
  }
}

/*
arsonOffence = 8655cb07-3419-4f24-a579-f57d1d5495b4
isArsonSuitable = a4ba038b-f762-4f19-ae94-2e308637a5ed
*/
@Repository
interface Cas1ArsonSuitableToArsonOffencesRepository : JpaRepository<ApplicationEntity, UUID> {

  @Query(
    """
    insert into placement_requirements_desirable_criteria
    select
      placement_requirement_id,
      '8655cb07-3419-4f24-a579-f57d1d5495b4'
    from placement_requirements_desirable_criteria
    where characteristic_id = 'a4ba038b-f762-4f19-ae94-2e308637a5ed'
    """,
    nativeQuery = true,
  )
  @Modifying
  fun addArsonOffencesToDesirableCriteriaWhereHasArsonSuitable(): Int

  @Query(
    """
    insert into placement_requirements_essential_criteria
    select
      placement_requirement_id,
      '8655cb07-3419-4f24-a579-f57d1d5495b4'
    from placement_requirements_essential_criteria
    where characteristic_id = 'a4ba038b-f762-4f19-ae94-2e308637a5ed'
    """,
    nativeQuery = true,
  )
  @Modifying
  fun addArsonOffencesToEssentialCriteriaWhereHasArsonSuitable(): Int

  @Query(
    """
      delete from placement_requirements_desirable_criteria 
      where characteristic_id = 'a4ba038b-f762-4f19-ae94-2e308637a5ed'
    """,
    nativeQuery = true,
  )
  @Modifying
  fun removeArsonSuitableFromDesirableCriteria(): Int

  @Query(
    """
      delete from placement_requirements_essential_criteria 
      where characteristic_id = 'a4ba038b-f762-4f19-ae94-2e308637a5ed'
    """,
    nativeQuery = true,
  )
  @Modifying
  fun removeArsonOffencesFromEssentialCriteria(): Int
}
