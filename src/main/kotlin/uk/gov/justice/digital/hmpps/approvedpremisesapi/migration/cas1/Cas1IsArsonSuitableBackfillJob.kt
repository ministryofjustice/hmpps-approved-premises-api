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
 * Adds 'isArsonSuitable' to any placement request that has either 'isArsonDesignated' or 'arsonOffences'
 */
@Component
class Cas1IsArsonSuitableBackfillJob(
  private val repository: Cas1ArsonIsArsonSuitableBackfillJobRepository,
  private val migrationLogger: MigrationLogger,
  override val shouldRunInTransaction: Boolean = true,
) : MigrationJob() {

  override fun process(pageSize: Int) {
    val essentialAdded = repository.addIsArsonSuitableToEssentialWhereRequired()
    migrationLogger.info("Have added is arson suitable to $essentialAdded essential criteria")

    val desirableAdded = repository.addIsArsonSuitableToDesirableWhereRequired()
    migrationLogger.info("Have added is arson suitable to $desirableAdded desirable criteria")
  }
}

/*
isArsonSuitable = a4ba038b-f762-4f19-ae94-2e308637a5ed
*/
@Repository
interface Cas1ArsonIsArsonSuitableBackfillJobRepository : JpaRepository<ApplicationEntity, UUID> {

  @Query(
    """
    with placement_requirements AS (
      select 
      p.id as placement_requirement_id,
      array_remove(array_agg(c.property_name),null) essential 
      from placement_requirements p
      left outer join placement_requirements_essential_criteria link on link.placement_requirement_id = p.id 
      left outer join characteristics c on c.id = link.characteristic_id
      GROUP BY p.id
    )
    insert into placement_requirements_essential_criteria
    select
      placement_requirement_id,
      'a4ba038b-f762-4f19-ae94-2e308637a5ed'
    from placement_requirements
    WHERE 
    ('isArsonDesignated'=ANY(essential) OR 'arsonOffences'=ANY(essential)) AND NOT('isArsonSuitable'=ANY(essential)) 
    """,
    nativeQuery = true,
  )
  @Modifying
  fun addIsArsonSuitableToEssentialWhereRequired(): Int

  @Query(
    """
    with placement_requirements AS (
      select 
      p.id as placement_requirement_id,
      array_remove(array_agg(c.property_name),null) desirable 
      from placement_requirements p
      left outer join placement_requirements_desirable_criteria link on link.placement_requirement_id = p.id 
      left outer join characteristics c on c.id = link.characteristic_id
      GROUP BY p.id
    )
    insert into placement_requirements_desirable_criteria
    select
      placement_requirement_id,
      'a4ba038b-f762-4f19-ae94-2e308637a5ed'
    from placement_requirements
    WHERE 
    ('isArsonDesignated'=ANY(desirable) OR 'arsonOffences'=ANY(desirable)) AND NOT('isArsonSuitable'=ANY(desirable)) 
    """,
    nativeQuery = true,
  )
  @Modifying
  fun addIsArsonSuitableToDesirableWhereRequired(): Int
}
