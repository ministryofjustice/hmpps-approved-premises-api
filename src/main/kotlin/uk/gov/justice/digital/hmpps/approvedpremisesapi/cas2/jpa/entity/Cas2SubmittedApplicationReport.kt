package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import java.util.UUID

@Repository
interface Cas2SubmittedApplicationReportRepository : JpaRepository<DomainEventEntity, UUID> {
  @Query(
    """
    SELECT
        CAST(events.id AS TEXT) AS id,
        CAST(events.application_id AS TEXT) AS applicationId,
        events.data -> 'eventDetails' -> 'submittedBy' -> 'staffMember' ->> 'username' AS submittedBy,
        events.data -> 'eventDetails' -> 'personReference' ->> 'noms' AS personNoms,
        events.data -> 'eventDetails' -> 'personReference' ->> 'crn' AS personCrn,
        events.data -> 'eventDetails' ->> 'referringPrisonCode' AS referringPrisonCode,
        events.data -> 'eventDetails' ->> 'preferredAreas' AS preferredAreas,
        CAST(events.data -> 'eventDetails' ->> 'hdcEligibilityDate' as DATE) AS hdcEligibilityDate,
        CAST(events.data -> 'eventDetails' ->> 'conditionalReleaseDate' as DATE) AS conditionalReleaseDate,
        TO_CHAR(events.occurred_at,'YYYY-MM-DD"T"HH24:MI:SS') AS submittedAt,
        TO_CHAR(applications.created_at, 'YYYY-MM-DD"T"HH24:MI:SS') AS startedAt,
        applications.application_origin as applicationOrigin,
        applications.service_origin as serviceOrigin,
        CAST(applications.bail_hearing_date as DATE) as bailHearingDate,
        CASE
            WHEN COUNT(distinct pom_assignments.id) = 0 THEN 0
            ELSE COUNT(distinct pom_assignments.id) - 1
        END as numberOfPomTransfers,
        COUNT(distinct location_assignments.id) as numberOfLocationTransfers
    FROM domain_events events
             JOIN cas_2_applications applications
                  ON events.application_id = applications.id and applications.service_origin = 'HDC'
             LEFT JOIN cas_2_application_assignments as pom_assignments
                       ON events.application_id = pom_assignments.application_id and pom_assignments.allocated_pom_cas_2_user_id is NOT NULL
             LEFT JOIN cas_2_application_assignments as location_assignments
                       ON events.application_id = location_assignments.application_id and location_assignments.allocated_pom_cas_2_user_id is NULL
    WHERE events.type = 'CAS2_APPLICATION_SUBMITTED'
      AND events.occurred_at  > CURRENT_DATE - 365
    GROUP BY events.id, events.application_id, events.data, events.occurred_at, applications.created_at, applications.application_origin, applications.service_origin, applications.bail_hearing_date
    ORDER BY submittedAt DESC;
    """,
    nativeQuery = true,
  )
  fun generateSubmittedApplicationReportRows(): List<Cas2SubmittedApplicationReportRow>
}

@SuppressWarnings("TooManyFunctions")
interface Cas2SubmittedApplicationReportRow {
  fun getId(): String
  fun getApplicationId(): String
  fun getApplicationOrigin(): ApplicationOrigin
  fun getBailHearingDate(): String?
  fun getSubmittedBy(): String
  fun getSubmittedAt(): String
  fun getPersonNoms(): String
  fun getPersonCrn(): String
  fun getReferringPrisonCode(): String
  fun getPreferredAreas(): String?
  fun getHdcEligibilityDate(): String?
  fun getConditionalReleaseDate(): String?
  fun getStartedAt(): String
  fun getNumberOfLocationTransfers(): String
  fun getNumberOfPomTransfers(): String
}
