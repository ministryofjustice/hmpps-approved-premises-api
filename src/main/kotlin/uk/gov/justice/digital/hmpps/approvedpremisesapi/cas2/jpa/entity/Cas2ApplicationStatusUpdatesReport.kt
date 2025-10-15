package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import java.util.UUID

@Repository
interface Cas2ApplicationStatusUpdatesReportRepository : JpaRepository<DomainEventEntity, UUID> {
  @Query(
    """
    SELECT
        CAST(events.id AS TEXT) AS id,
        CAST(events.application_id AS TEXT) AS applicationId,
        events.data -> 'eventDetails' -> 'personReference' ->> 'noms' AS personNoms,
        events.data -> 'eventDetails' -> 'personReference' ->> 'crn' AS personCrn,
        events.data -> 'eventDetails' -> 'newStatus' ->> 'name' AS newStatus,
        events.data -> 'eventDetails' -> 'updatedBy' ->> 'username' AS updatedBy,
        COALESCE(string_agg(distinct details ->> 'name', '|'), '') as statusDetails,
        TO_CHAR(
                CAST(events.data -> 'eventDetails' ->> 'updatedAt' AS TIMESTAMP),
                'YYYY-MM-DD"T"HH24:MI:SS'
        ) AS updatedAt,
        COUNT(distinct location_assignments.id) as numberOfLocationTransfers,
        CASE WHEN COUNT(distinct pom_assignments.id) = 0 THEN 0 ELSE COUNT(distinct pom_assignments.id) - 1 END as numberOfPomTransfers
    FROM domain_events events
             JOIN cas_2_applications as a on a.id = events.application_id
             LEFT JOIN cas_2_application_assignments as pom_assignments on events.application_id = pom_assignments.application_id and pom_assignments.allocated_pom_user_id is NOT NULL
             LEFT JOIN cas_2_application_assignments as location_assignments on events.application_id = location_assignments.application_id and location_assignments.allocated_pom_user_id is NULL
             LEFT JOIN LATERAL jsonb_array_elements(events.data -> 'eventDetails' -> 'newStatus' -> 'statusDetails') as details ON true
    WHERE events.type = 'CAS2_APPLICATION_STATUS_UPDATED'
      AND events.occurred_at  > CURRENT_DATE - 365
    GROUP BY events.id
    ORDER BY updatedAt DESC;
    """,
    nativeQuery = true,
  )
  fun generateApplicationStatusUpdatesReportRows(): List<Cas2ApplicationStatusUpdatedReportRow>
}

interface Cas2ApplicationStatusUpdatedReportRow {
  fun getId(): String
  fun getApplicationId(): String
  fun getUpdatedBy(): String
  fun getUpdatedAt(): String
  fun getPersonNoms(): String
  fun getPersonCrn(): String
  fun getNewStatus(): String
  fun getStatusDetails(): String
  fun getNumberOfLocationTransfers(): String
  fun getNumberOfPomTransfers(): String
}
