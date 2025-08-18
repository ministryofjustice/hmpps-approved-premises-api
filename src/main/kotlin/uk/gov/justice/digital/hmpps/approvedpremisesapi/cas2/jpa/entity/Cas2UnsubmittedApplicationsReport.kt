package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import java.util.UUID

@Repository
interface Cas2UnsubmittedApplicationsReportRepository : JpaRepository<Cas2ApplicationEntity, UUID> {
  // TODO besscerule check it is ok for Cas2v2 reports as removed Z from startedAt for Cas2v2 reports (prev they had them but now they use Cas2)
  @Query(
    """
      SELECT
        CAST(applications.id AS TEXT) AS applicationId,
        applications.crn AS personCrn,
        applications.noms_number AS personNoms,
        to_char(applications.created_at, 'YYYY-MM-DD"T"HH24:MI:SS') AS startedAt,
        users.nomis_username AS startedBy,
        applications.application_origin AS applicationOrigin

      FROM cas_2_applications applications
      JOIN nomis_users users ON users.id = applications.created_by_user_id
      WHERE applications.submitted_at IS NULL
        AND applications.created_at  > CURRENT_DATE - 365
      ORDER BY startedAt DESC;
    """,
    nativeQuery = true,
  )
  fun generateUnsubmittedApplicationsReportRows(): List<Cas2UnsubmittedApplicationReportRow>
}

interface Cas2UnsubmittedApplicationReportRow {
  fun getApplicationId(): String
  fun getApplicationOrigin(): ApplicationOrigin
  fun getPersonNoms(): String
  fun getPersonCrn(): String
  fun getStartedBy(): String
  fun getStartedAt(): String
}
