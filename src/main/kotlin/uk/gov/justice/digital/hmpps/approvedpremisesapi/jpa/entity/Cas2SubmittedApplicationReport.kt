package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
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
        CAST(
          CAST(events.data -> 'eventDetails' ->> 'submittedAt' as DATE) 
         as TEXT) as submittedAt
      FROM domain_events events
      WHERE events.type = 'CAS2_APPLICATION_SUBMITTED'
      ORDER BY submittedAt DESC;
    """,
    nativeQuery = true,
  )
  fun generateSubmittedApplicationReportRows(): List<Cas2SubmittedApplicationReportRow>
}

interface Cas2SubmittedApplicationReportRow {
  fun getId(): String
  fun getApplicationId(): String
  fun getSubmittedBy(): String
  fun getSubmittedAt(): String
  fun getPersonNoms(): String
  fun getPersonCrn(): String
  fun getReferringPrisonCode(): String
}
