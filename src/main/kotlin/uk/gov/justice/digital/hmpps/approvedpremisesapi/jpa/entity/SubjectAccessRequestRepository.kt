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

  fun getApprovedPremisesAssessments(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    val result = jdbcTemplate.queryForMap(
      """
       select json_agg(assess) as json from (
           select
               app.id as application_id,
               assess.id as assessment_id,
               app.crn,
               app.noms_number,
               u."name" as assessor_name,
               assess."data" ,
               assess."document",
               assess.created_at,
               assess.allocated_at,
               assess.submitted_at,
               assess.reallocated_at,
               assess.due_at,
               assess.decision,
               assess.rejection_rationale,
               assess.is_withdrawn,
               assess.service,
               apa.created_from_appeal
           from
               assessments assess
           inner join 
               applications app
           on
               app.id = assess.application_id
           inner join 
               users u 
           on
               u.id = assess.allocated_to_user_id
           left join 
               approved_premises_assessments apa 
           on
               apa.assessment_id = assess.id
          where
              (app.crn = :crn or app.noms_number = :noms_number )
          and 
              (:start_date is null or app.created_at >= :start_date)
          and 
              (:end_date is null or app.created_at <= :end_date)
  ) assess
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

  fun getApprovedPremisesAssessmentClarificationNotes(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    val result = jdbcTemplate.queryForMap(
      """
    select json_agg(assess) as json 
    from (    
      select
        app.id as application_id,
        a.id as assessment_id,
        app.crn,
        app.noms_number,
        acn.created_at,
        acn.query,
        acn.response,
        u."name" as created_by_user
      from
        assessment_clarification_notes acn
      inner join assessments a
        on
        a.id = acn.assessment_id
      inner join applications app on
        app.id = a.application_id
      inner join users u on 
        u.id = acn.created_by_user_id
      where
        a.service = 'approved-premises'
      and
          (app.crn = :crn
          or 
          app.noms_number = :noms_number)
      and 
          (:start_date is null or app.created_at >= :start_date)
      and 
          (:end_date is null or app.created_at <= :end_date)
      ) assess
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
