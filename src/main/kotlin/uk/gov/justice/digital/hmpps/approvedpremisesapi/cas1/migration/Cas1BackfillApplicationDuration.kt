package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.migration

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import java.util.UUID

@Component
class Cas1BackfillApplicationDuration(
  val repository: Cas1BackfillApplicationDurationRepository,
  private val migrationLogger: MigrationLogger,
) : MigrationJob() {
  override val shouldRunInTransaction = true

  override fun process(pageSize: Int) {
    repository.updateWhereDurationSetManually().let {
      migrationLogger.info("Have set duration for $it applications with an overridden duration")
    }

    repository.updatePipeApplications().let {
      migrationLogger.info("Have set duration for $it PIPE applications")
    }

    repository.updateEsapApplications().let {
      migrationLogger.info("Have set duration for $it ESAP applications")
    }

    repository.updateRemainingApplications().let {
      migrationLogger.info("Have set duration for $it remaining applications")
    }
  }
}

@Repository
interface Cas1BackfillApplicationDurationRepository : JpaRepository<ApprovedPremisesApplicationEntity, UUID> {
  @Query(
    value = """
      with to_update as (
          select 
              apa.id as id,
              (a.data -> 'move-on' -> 'placement-duration' ->> 'duration')::integer as duration
          from approved_premises_applications apa inner join applications a on a.id = apa.id
          where  
          apa.duration IS NULL AND 
          a.data -> 'move-on' -> 'placement-duration' ->> 'differentDuration' = 'yes'
      )
      UPDATE approved_premises_applications
      SET duration = to_update.duration
      FROM to_update
      WHERE approved_premises_applications.id = to_update.id;
  """,
    nativeQuery = true,
  )
  @Modifying
  fun updateWhereDurationSetManually(): Int

  @Query(
    value = """
      UPDATE approved_premises_applications
      SET duration = (26 * 7)
      WHERE duration IS NULL AND ap_type = 'PIPE'
  """,
    nativeQuery = true,
  )
  @Modifying
  fun updatePipeApplications(): Int

  @Query(
    value = """
      UPDATE approved_premises_applications
      SET duration = (52 * 7)
      WHERE duration IS NULL AND ap_type = 'ESAP'
  """,
    nativeQuery = true,
  )
  @Modifying
  fun updateEsapApplications(): Int

  @Query(
    value = """
      UPDATE approved_premises_applications
      SET duration = (12 * 7) 
      WHERE duration IS NULL
  """,
    nativeQuery = true,
  )
  @Modifying
  fun updateRemainingApplications(): Int
}
