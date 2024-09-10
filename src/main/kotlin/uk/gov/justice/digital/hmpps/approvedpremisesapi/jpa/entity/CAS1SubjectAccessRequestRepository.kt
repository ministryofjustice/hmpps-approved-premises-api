package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class CAS1SubjectAccessRequestRepository(
  jdbcTemplate: NamedParameterJdbcTemplate,
) : SubjectAccessRequestRepositoryBase(jdbcTemplate) {

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

  fun placementApplications(
    crn: String?,
    nomsNumber: String?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
  ): String {
    var result = jdbcTemplate.queryForMap(
      """
       select json_agg(placement_applications) 
       as json from (
          select
            a.crn,
            a.noms_number,
            pa.application_id,
            pa."data",
            pa."document",
            pa.created_at,
            pa.submitted_at ,
            pa.allocated_at,
            pa.reallocated_at,
            pa.due_at,
            pa.decision,
            pa.decision_made_at,
            case
               when pa.placement_type = '0' then 'ROTL'
               when pa.placement_type = '1' then 'RELEASE_FOLLOWING_DECISION'
               when pa.placement_type = '2' then 'ADDITIONAL_PLACEMENT' 
               else ''
            end as placement_type,
            pa.is_withdrawn,
            pa.withdrawal_reason,
            cu."name" as created_by_user,
            au."name" as allocated_user
          from
            placement_applications pa
          inner join applications a on
            pa.application_id = a.id
          inner join users cu on
            cu.id = pa.created_by_user_id
          left join users au on
            au.id = pa.allocated_to_user_id
          where
            (a.crn = :crn
              or a.noms_number = :noms_number )
          and (:start_date is null or a.created_at >= :start_date)
          and (:end_date is null or a.created_at <= :end_date)
       ) placement_applications
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

  fun placementRequests(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    var result = jdbcTemplate.queryForMap(
      """
        select json_agg(placement_requests) 
        as json from (
        select 
              app.crn,
              app.noms_number, 
              pr.expected_arrival,
              pr.duration, 
              pr.created_at,
              pr.placement_application_id, 
              pr.booking_id,
              pr.application_id,
              pr.assessment_id,
              pr.notes,
              pr.is_parole,
              pr.is_withdrawn,
              pr.withdrawal_reason,
              pr.due_at
          from placement_requests pr 
          inner join applications app on
            app.id = pr.application_id
          where
            (app.crn = :crn
              or 
              app.noms_number = :noms_number)
          and 
            (:start_date is null or app.created_at >= :start_date)
          and 
            (:end_date is null or app.created_at <= :end_date)
        ) placement_requests  
      """.trimIndent(),
      MapSqlParameterSource().addSarParameters(crn, nomsNumber, startDate, endDate),
    )
    return toJsonString(result)
  }

  fun placementRequirements(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    var result = jdbcTemplate.queryForMap(
      """
     select json_agg(placement_requirements) 
     as json from (
        select 
             app.crn,
             app.noms_number,
             pr.application_id,
             pr.assessment_id,
             pr.id as placement_requirements_id,
             case 
                 when pr.gender = '0' then 'MALE'
                 when pr.gender = '1' then 'FEMALE'
                 else 'OTHER'
             end gender,
               case
                 when pr.ap_type = '0' then 'NORMAL'
                 when pr.ap_type = '1' then 'PIPE'
                 when pr.ap_type = '2' then 'ESAP'
                 when pr.ap_type = '3' then 'RFAP'
                 when pr.ap_type = '4' then 'MHAP_ST_JOSEPHS'
                 when pr.ap_type = '5' then 'MHAP_ELLIOTT_HOUSE'
                 else 'other'
             end ap_type,
             pd.outcode,
             pr.radius,
             pr.created_at
        from 
             placement_requirements pr
        left join postcode_districts pd on 
        	   pd.id = pr.postcode_district_id 
        inner join applications app on
        	   app.id = pr.application_id
        where
             (app.crn = :crn
              or 
                 app.noms_number = :noms_number)
        and 
             (:start_date is null or app.created_at >= :start_date)
        and 
             (:end_date is null or app.created_at <= :end_date)
      ) placement_requirements  
      """.trimIndent(),
      MapSqlParameterSource().addSarParameters(crn, nomsNumber, startDate, endDate),
    )
    return toJsonString(result)
  }

  fun placementRequirementsCriteria(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    var result = jdbcTemplate.queryForMap(
      """
       select json_agg(placement_requirements_criteria) as json 
       from (
          select 
    
            app.crn,
            app.noms_number,
            pr.id as placement_requirement_id,
            c."name" as criteria_name,
            c.service_scope,
            c.model_scope,
            c.property_name,
            c.is_active,
            'DESIRABLE' as criteria_type
          from placement_requirements pr
          inner join applications app on
       	    app.id = pr.application_id
       	  left join placement_requirements_desirable_criteria prdc on 
            prdc.placement_requirement_id  = pr.id 
       	  inner join "characteristics" c on 
            c.id = prdc.characteristic_id
          where 
       	    (app.crn = :crn
                or 
                app.noms_number = :noms_number)
          and 
            (:start_date is null or app.created_at >= :start_date)
          and 
            (:end_date is null or app.created_at <= :end_date)
          union all 
          select 
            app.crn as crn,
            app.noms_number as noms_number,
            pr.id as placement_requirement_id,
            c."name" as criteria_name,
            c.service_scope,
            c.model_scope,
            c.property_name,
            c.is_active,
            'ESSENTIAL' as criteria_type
          from placement_requirements pr
          inner join applications app on
       	    app.id = pr.application_id
       	  left join placement_requirements_essential_criteria prec 
            on prec.placement_requirement_id  = pr.id 
       	  inner join "characteristics" c 
            on c.id = prec.characteristic_id
          where 
       	    (app.crn = :crn
              or 
              app.noms_number = :noms_number)
          and 
            (:start_date is null or app.created_at >= :start_date)
          and 
            (:end_date is null or app.created_at <= :end_date)
       	) placement_requirements_criteria
      """.trimIndent(),
      MapSqlParameterSource().addSarParameters(crn, nomsNumber, startDate, endDate),
    )
    return toJsonString(result)
  }

  fun offlineApplications(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    var result = jdbcTemplate.queryForMap(
      """
        select json_agg(offline_applications) as json
        from ( 
            select 
                B.CRN,
                B.noms_number ,
                oa.id as offline_application_id, 
                b.id as booking_id,
                oa.created_at 
            from offline_applications oa 
            inner join bookings b on b.offline_application_id = oa.id 
            where 
                (b.crn = :crn
                    or 
                b.noms_number = :noms_number)
            and 
                (:start_date is null or b.created_at >= :start_date)
            and 
                (:end_date is null or b.created_at <= :end_date)
            ) offline_applications
      """.trimIndent(),
      MapSqlParameterSource()
        .addSarParameters(crn, nomsNumber, startDate, endDate),
    )
    return toJsonString(result)
  }

  fun bookingNotMades(
    crn: String?,
    nomsNumber: String?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
  ): String {
    var result = jdbcTemplate.queryForMap(
      """
         select json_agg(booking_not_mades) as json from ( 
             select 
                 a.crn,
                 a.noms_number,
                 B.id as booking_not_made_id,
                 a.id as application_id,
                 b.placement_request_id,
                 b.created_at,
                 b.notes
             from booking_not_mades b
             inner join placement_requests pr  on 
                 b.placement_request_id = pr.id
             inner join applications a on 
                 a.id = pr.application_id
             where 
                (a.crn = :crn
                    or 
                a.noms_number = :noms_number)
            and 
                (:start_date is null or a.created_at >= :start_date)
            and 
                (:end_date is null or a.created_at <= :end_date)
          )booking_not_mades
      """.trimIndent(),
      MapSqlParameterSource().addSarParameters(crn, nomsNumber, startDate, endDate),
    )
    return toJsonString(result)
  }

  fun bedMoves(
    crn: String?,
    nomsNumber: String?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
  ): String {
    var result = jdbcTemplate.queryForMap(
      """
       select json_agg(bed_moves) as json
       from (
          select
              b.crn ,
              b.noms_number,
              bm.notes,
              previous_bed."name" as previous_bed_name,
              previous_bed.code as previous_bed_code,
              new_bed."name" as new_bed_name,
              new_bed.code as new_bed_code,
              bm.created_at
          from
              bed_moves bm
          inner join bookings b on
              b.id = bm.booking_id
          inner join beds previous_bed on 
            bm.previous_bed_id  = previous_bed.id 
          inner join beds new_bed on 
            bm.new_bed_id  = new_bed.id 
                  
          where
              (b.crn = :crn
                  or b.noms_number = :noms_number )
            and (:start_date is null or b.created_at >= :start_date)
            and (:end_date is null or b.created_at <= :end_date)
        ) bed_moves
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

  fun appeals(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    var result = jdbcTemplate.queryForMap(
      """
      select json_agg(appeals) as json
      from ( 
            select
              app.crn,
              app.noms_number,
              a.id as appeal_id,
              a.application_id,
              a.assessment_id,
              a.appeal_date,
              a.appeal_detail,
              a.decision ,
              a.decision_detail,
              a.created_at as appeal_created_at,
              u."name" as created_by_user     
            from appeals a
              inner join users u on
              u.id = a.created_by_user_id
              inner join applications app on
              app.id = a.application_id
              inner join assessments assess on
              assess.id = a.assessment_id 
            where
              (app.crn = :crn
                or app.noms_number = :noms_number )
            and (:start_date is null or app.created_at >= :start_date)
            and (:end_date is null or app.created_at <= :end_date)
        ) appeals
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
}
