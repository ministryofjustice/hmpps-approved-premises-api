package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.migration

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
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

    migrationLogger.info("Have updated $updateCount space bookings")
  }
}

@Repository
interface Cas1BackfillKeyWorkerUserAssignmentsJobRepository : JpaRepository<Cas1SpaceBookingEntity, UUID> {
  @Modifying
  @Query(
    """
    UPDATE cas1_space_bookings b
    SET key_worker_user_id = (SELECT id FROM users u WHERE UPPER(u.delius_staff_code) = UPPER(b.key_worker_staff_code))
    WHERE b.key_worker_staff_code IS NOT NULL AND b.key_worker_user_id IS NULL
  """,
    nativeQuery = true,
  )
  fun addMissingKeyWorkerUserIds(): Int
}
