package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.postgresql.util.PGobject
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class SubjectAccessRequestRepository(
  val jdbcTemplate: NamedParameterJdbcTemplate,
) {

  fun getApprovedPremisesApplicationsJson(
    crn: String?,
    nomsNumber: String?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
  ): String {
    val result = jdbcTemplate.queryForMap(
      """
select 
	json_agg(app) as json
from
	(
	select
		a.id,
		apa.name,
		a.crn,
		a.noms_number,
		a."data",
		a."document",
		a.created_at,
		a.submitted_at,
		created_by_user.name as created_by_user,
		app_user."name" as application_user_name ,
		apa.event_number,
		apa.is_womens_application,
		apa.offence_id,
		apa.conviction_id,
		apa.risk_ratings,
		apa.release_type,
		apa.arrival_date,
		apa.is_withdrawn,
		apa.withdrawal_reason,
		apa.other_withdrawal_reason ,
		apa.is_emergency_application,
		apa.target_location ,
		apa.status,
		apa.inmate_in_out_status_on_submission,
		apa.sentence_type,
		apa.notice_type,
		apa.ap_type,
		case_manager."name" as case_manager_name,
		apa.case_manager_is_not_applicant
	from
		approved_premises_applications apa
	join 
        applications a on
		a.id = apa.id
	left join 
        cas_1_application_user_details case_manager on
		    case_manager.id = apa.case_manager_cas1_application_user_details_id
	left join 
        cas_1_application_user_details app_user on
		    app_user.id = apa.applicant_cas1_application_user_details_id
	left join 
        users created_by_user on
		    created_by_user.id = a.created_by_user_id
	where
		(a.crn = :crn
			or a.noms_number = :noms_number )
		and (:start_date is null
			or a.created_at >= :start_date)
		and (:end_date is null
			or a.created_at <= :end_date) 
) app;
      """.trimIndent(),
      MapSqlParameterSource().addSarParameters(
        crn,
        nomsNumber,
        startDate,
        endDate,
      ),
    )
    return toJsonString(result)
  }
  fun getApprovedPremisesApplicationTimeLineJson(
    crn: String?,
    nomsNumber: String?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
  ): String {
    val result = jdbcTemplate.queryForMap(
      """
  
  select 
  	json_agg(apptimeline) as json
  from(
      select
          a.id as application_id,
          a.service,
          a.crn,
          a.noms_number,
          atn.body,
          atn.created_at,
          u."name" as user_name
      from
      application_timeline_notes atn
      inner join users u on
          u.id = atn.created_by_user_id
      inner join applications a on
          atn.application_id = a.id
      where
      (a.crn = :crn
          or a.noms_number = :noms_number )
      and (:start_date is null or a.created_at >= :start_date)
      and (:end_date is null or a.created_at <= :end_date)
  ) apptimeline
      """.trimIndent(),
      MapSqlParameterSource().addSarParameters(
        crn,
        nomsNumber,
        startDate,
        endDate,
      ),
    )
    return toJsonString(result)
  }

  private fun toJsonString(result: Map<String, Any>) = (result["json"] as PGobject?)?.value ?: "[]"

  private fun MapSqlParameterSource.addSarParameters(
    crn: String?,
    nomsNumber: String?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
  ): MapSqlParameterSource {
    // note this might not be the most ideal way - happy for a challenge on it.
    this.addValue("crn", crn)
    this.addValue("noms_number", nomsNumber)
    this.addValue("start_date", startDate)
    this.addValue("end_date", endDate)
    return this
  }
}
