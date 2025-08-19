package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.migration

import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import java.util.UUID

@Service
class Cas1BackfillKeyWorkerUserAssignmentsJob(
  private val repository: Cas1BackfillKeyWorkerUserAssignmentsJobRepository,
  private val migrationLogger: MigrationLogger,
) : MigrationJob() {
  override val shouldRunInTransaction = true
  override fun process(pageSize: Int) {
    val updateCount = repository.addMissingKeyWorkerUserIds()
    migrationLogger.info("Have updated $updateCount space bookings using direct mapping")

    val updateCountLookup = repository.addMissingKeyWorkerUserIdsUsingLookup()
    migrationLogger.info("Have updated $updateCountLookup space bookings using lookup")
  }
}

@Repository
interface Cas1BackfillKeyWorkerUserAssignmentsJobRepository : JpaRepository<Cas1SpaceBookingEntity, UUID> {
  @QueryHints(QueryHint(name = "javax.persistence.query.timeout", value = "240000"))
  @Modifying
  @Query(
    """
    UPDATE cas1_space_bookings b
    SET key_worker_user_id = (
        SELECT u.id FROM users u 
        INNER JOIN user_role_assignments role on role.role = 'CAS1_FUTURE_MANAGER' and role.user_id = u.id
        WHERE UPPER(u.delius_staff_code) = UPPER(b.key_worker_staff_code)
    )
    WHERE b.key_worker_staff_code IS NOT NULL AND b.key_worker_user_id IS NULL
  """,
    nativeQuery = true,
  )
  fun addMissingKeyWorkerUserIds(): Int

  @QueryHints(QueryHint(name = "javax.persistence.query.timeout", value = "240000"))
  @Modifying
  @Query(
    """
    UPDATE cas1_space_bookings b
    SET key_worker_user_id = (
      SELECT u.id FROM users u 
      INNER JOIN user_role_assignments role on role.role = 'CAS1_FUTURE_MANAGER' and role.user_id = u.id
      INNER JOIN cas1_key_worker_staff_code_lookup lookup ON 
        UPPER(lookup.staff_code_1) = UPPER(b.key_worker_staff_code) AND 
        UPPER(u.delius_staff_code) = UPPER(lookup.staff_code_2)
    )
    WHERE b.key_worker_staff_code IS NOT NULL AND b.key_worker_user_id IS NULL
  """,
    nativeQuery = true,
  )
  fun addMissingKeyWorkerUserIdsUsingLookup(): Int
}
