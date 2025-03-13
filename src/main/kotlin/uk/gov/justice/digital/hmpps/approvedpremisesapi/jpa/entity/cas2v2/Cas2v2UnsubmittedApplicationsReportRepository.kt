package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import java.util.UUID

@Repository
interface Cas2v2UnsubmittedApplicationsReportRepository : JpaRepository<Cas2v2ApplicationEntity, UUID> {
  @Query(
    """
      SELECT
        CAST(applications.id AS TEXT) AS applicationId,
        applications.crn AS personCrn,
        applications.noms_number AS personNoms,
        to_char(applications.created_at, 'YYYY-MM-DD"T"HH24:MI:SS') AS startedAt,
        users.username AS startedBy,
        applications.application_origin AS applicationOrigin

      FROM cas_2_v2_applications applications
      LEFT JOIN cas_2_v2_users users ON users.id = applications.created_by_user_id
      WHERE applications.submitted_at IS NULL
        AND applications.created_at  > CURRENT_DATE - 365
      ORDER BY startedAt DESC;
    """,
    nativeQuery = true,
  )
  fun generateUnsubmittedApplicationsReportRows(): List<Cas2v2UnsubmittedApplicationReportRow>
}

interface Cas2v2UnsubmittedApplicationReportRow {
  fun getApplicationId(): String
  fun getApplicationOrigin(): ApplicationOrigin
  fun getPersonNoms(): String
  fun getPersonCrn(): String
  fun getStartedBy(): String
  fun getStartedAt(): String
}
