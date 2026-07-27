package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import java.util.UUID

@Repository
interface DepartureReasonTestRepository : JpaRepository<DepartureReasonEntity, UUID> {
  @Modifying
  @Query(
    value = """
    WITH RECURSIVE tree AS (
      SELECT id FROM departure_reasons
      UNION
      SELECT d.id
      FROM departure_reasons d
      JOIN tree t ON d.parent_reason_id = t.id
    )
    DELETE FROM departure_reasons WHERE id IN (SELECT id FROM tree)
  """,
    nativeQuery = true,
  )
  @Transactional
  fun deleteAllRecursively()
}
