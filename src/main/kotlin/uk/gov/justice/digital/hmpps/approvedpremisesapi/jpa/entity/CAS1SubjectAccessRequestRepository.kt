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
  ): String? {
    val result = jdbcTemplate.queryForMap(
      """
select 
	json_agg(app) as json
from
	(
	select
		apa.name,
		a.noms_number,
		a."document",
		a.created_at,
		a.submitted_at,
		app_user.delius_username as application_user_name,
		apa.event_number,
		apa.is_womens_application,
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
		apa.case_manager_is_not_applicant,
    apa.situation,
    apa.is_inapplicable,
    apa.licence_expiry_date,
    apa.expired_reason
	from
		approved_premises_applications apa
	join 
        applications a on
		a.id = apa.id
	left join 
        cas_1_application_user_details case_manager on
		    case_manager.id = apa.case_manager_cas1_application_user_details_id
	left join 
        users app_user on
		    app_user.id = a.created_by_user_id
	left join 
        users created_by_user on
		    created_by_user.id = a.created_by_user_id
	where
		(a.crn = :crn
			or a.noms_number = :noms_number )
		and (:start_date::date is null
			or a.created_at >= :start_date)
		and (:end_date::date is null
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
  ): String? {
    val result = jdbcTemplate.queryForMap(
      """
  
  select 
  	json_agg(apptimeline) as json
  from(
      select
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
      and (:start_date::date is null or a.created_at >= :start_date)
      and (:end_date::date is null or a.created_at <= :end_date)
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

  fun getApprovedPremisesAssessments(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String? {
    val result = jdbcTemplate.queryForMap(
      """
       select json_agg(assess) as json from (
           select
               u."name" as assessor_name,
               assess."document",
               assess.created_at,
               assess.allocated_at,
               assess.submitted_at,
               assess.reallocated_at,
               assess.due_at,
               assess.decision,
               assess.rejection_rationale,
               assess.is_withdrawn,
               apa.created_from_appeal,
               apa.agree_with_short_notice_reason,
               apa.agree_with_short_notice_reason_comments,
               apa.reason_for_late_application
           from
               approved_premises_assessments apa
           inner join
               assessments assess
           on
               apa.assessment_id = assess.id
           inner join 
               applications app
           on
               app.id = assess.application_id
           inner join 
               users u 
           on
               u.id = assess.allocated_to_user_id
          where
              (app.crn = :crn or app.noms_number = :noms_number )
          and 
              (:start_date::date is null or app.created_at >= :start_date)
          and 
              (:end_date::date is null or app.created_at <= :end_date)
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

  fun getApprovedPremisesAssessmentClarificationNotes(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String? {
    val result = jdbcTemplate.queryForMap(
      """
    select json_agg(assess) as json 
    from (    
      select
        acn.created_at,
        acn.query,
        acn.response,
        acn.response_received_on,
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
          (:start_date::date is null or app.created_at >= :start_date)
      and 
          (:end_date::date is null or app.created_at <= :end_date)
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
  ): String? {
    var result = jdbcTemplate.queryForMap(
      """
       select json_agg(placement_applications) 
       as json from (
          select
            pa."document",
            pa.created_at,
            pa.submitted_at ,
            pa.allocated_at,
            pa.reallocated_at,
            pa.due_at,
            pa.decision,
            pa.decision_made_at,
            pa.sentence_type,
            pa.release_type,
            pa.requested_duration,
            pa.authorised_duration,
            pa.expected_arrival,
            pa.expected_arrival_flexible,
            pa.situation,
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
          and (:start_date::date is null or a.created_at >= :start_date)
          and (:end_date::date is null or a.created_at <= :end_date)
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

  fun placementRequests(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String? {
    var result = jdbcTemplate.queryForMap(
      """
        select json_agg(placement_requests) 
        as json from (
        select 
              pr.expected_arrival,
              pr.duration, 
              pr.created_at,
              pr.notes,
              pr.is_parole,
              pr.is_withdrawn,
              pr.withdrawal_reason
          from placement_requests pr 
          inner join applications app on
            app.id = pr.application_id
          where
            (app.crn = :crn
              or 
              app.noms_number = :noms_number)
          and 
            (:start_date::date is null or app.created_at >= :start_date)
          and 
            (:end_date::date is null or app.created_at <= :end_date)
        ) placement_requests  
      """.trimIndent(),
      MapSqlParameterSource().addSarParameters(crn, nomsNumber, startDate, endDate),
    )
    return toJsonString(result)
  }

  fun placementRequirements(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String? {
    var result = jdbcTemplate.queryForMap(
      """
     select json_agg(placement_requirements) 
     as json from (
        select 
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
             (:start_date::date is null or app.created_at >= :start_date)
        and 
             (:end_date::date is null or app.created_at <= :end_date)
      ) placement_requirements  
      """.trimIndent(),
      MapSqlParameterSource().addSarParameters(crn, nomsNumber, startDate, endDate),
    )
    return toJsonString(result)
  }

  fun placementRequirementsCriteria(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String? {
    var result = jdbcTemplate.queryForMap(
      """
       select json_agg(placement_requirements_criteria) as json 
       from (
          select 
            c."name" as criteria_name,
            c.property_name,
            'DESIRABLE' as criteria_type
          from placement_requirements pr
          inner join applications app on
       	    app.id = pr.application_id
       	  left join placement_requirements_desirable_criteria prdc on 
            prdc.placement_requirement_id  = pr.id 
       	  inner join cas1_characteristics c on 
            c.id = prdc.characteristic_id
          where 
       	    (app.crn = :crn
                or 
                app.noms_number = :noms_number)
          and 
            (:start_date::date is null or app.created_at >= :start_date)
          and 
            (:end_date::date is null or app.created_at <= :end_date)
          union all 
          select 
            c."name" as criteria_name,
            c.property_name,
            'ESSENTIAL' as criteria_type
          from placement_requirements pr
          inner join applications app on
       	    app.id = pr.application_id
       	  left join placement_requirements_essential_criteria prec 
            on prec.placement_requirement_id  = pr.id 
       	  inner join cas1_characteristics c 
            on c.id = prec.characteristic_id
          where 
       	    (app.crn = :crn
              or 
              app.noms_number = :noms_number)
          and 
            (:start_date::date is null or app.created_at >= :start_date)
          and 
            (:end_date::date is null or app.created_at <= :end_date)
       	) placement_requirements_criteria
      """.trimIndent(),
      MapSqlParameterSource().addSarParameters(crn, nomsNumber, startDate, endDate),
    )
    return toJsonString(result)
  }

  fun offlineApplications(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String? {
    var result = jdbcTemplate.queryForMap(
      """
        select json_agg(offline_applications) as json
        from ( 
            select 
                oa.created_at 
            from offline_applications oa 
            where oa.crn = :crn
            and (
                  (:start_date::date is null or oa.created_at >= :start_date)
              and 
                  (:end_date::date is null or oa.created_at <= :end_date)
                )
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
  ): String? {
    var result = jdbcTemplate.queryForMap(
      """
         select json_agg(booking_not_mades) as json from ( 
             select 
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
                (:start_date::date is null or a.created_at >= :start_date)
            and 
                (:end_date::date is null or a.created_at <= :end_date)
          )booking_not_mades
      """.trimIndent(),
      MapSqlParameterSource().addSarParameters(crn, nomsNumber, startDate, endDate),
    )
    return toJsonString(result)
  }

  fun appeals(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String? {
    var result = jdbcTemplate.queryForMap(
      """
      select json_agg(appeals) as json
      from ( 
            select
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
            and (:start_date::date is null or app.created_at >= :start_date)
            and (:end_date::date is null or app.created_at <= :end_date)
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

  fun spaceBookings(
    crn: String?,
    nomsNumber: String?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
  ): String? {
    val result = jdbcTemplate.queryForMap(
      """
    select json_agg(spaceBooking) as json 
    from (
          select
            a.noms_number,            
            b.expected_arrival_date,
            b.expected_departure_date,
            b.actual_arrival_date,
            b.actual_arrival_time,
            b.actual_departure_date,
            b.actual_departure_time,
            b.non_arrival_confirmed_at,
            b.non_arrival_notes,
            nar.name as non_arrival_reason,
            apa.risk_ratings -> 'tier' -> 'value' ->> 'level' as tier,
            b.created_at,
            b.key_worker_staff_code,
            b.key_worker_assigned_at,
            b.key_worker_name,
            p."name" as premises_name,
            b.delius_event_number,
            u.name as created_by_user_name,
            dr.name as departure_reason,
            b.departure_notes,
            moc.name as move_on_category,
            b.cancellation_reason_notes,
            cr.name as cancellation_reason,
            b.cancellation_occurred_at, 
            b.cancellation_recorded_at,
            b.transfer_type,
            b.additional_information,
            b.transfer_reason,
            ( 
              SELECT STRING_AGG (cas1_characteristics.property_name, ',')
              FROM cas1_space_bookings_criteria sbc
              LEFT OUTER JOIN cas1_characteristics ON cas1_characteristics.id = sbc.characteristic_id
              WHERE sbc.space_booking_id = b.id 
              GROUP by sbc.space_booking_id
            ) AS characteristics_property_names,    
            CASE 
              WHEN apa.id IS NOT NULL THEN apa.name
              ELSE offline_app.name
            END as person_name
            FROM 
              cas1_space_bookings b
            LEFT JOIN non_arrival_reasons nar ON 
              b.non_arrival_reason_id = nar.id           
            LEFT JOIN cas1_premises_base p ON
              b.premises_id = p.id            
            LEFT OUTER JOIN approved_premises_applications apa ON 
              b.approved_premises_application_id = apa.id
            LEFT OUTER JOIN offline_applications offline_app ON 
            b.offline_application_id = offline_app.id              
            LEFT OUTER JOIN   
              applications a on 
              a.id = apa.id
            LEFT JOIN departure_reasons dr ON 
              b.departure_reason_id = dr.id
            LEFT JOIN move_on_categories moc ON
              b.departure_move_on_category_id = moc.id
            LEFT JOIN cancellation_reasons cr ON
              b.cancellation_reason_id = cr.id
            LEFT JOIN users u ON
              b.created_by_user_id = u.id
          where
              (b.crn = :crn
              or a.noms_number = :noms_number )
          and (:start_date::date is null or b.created_at >= :start_date) 
          and (:end_date::date is null or b.created_at <= :end_date)         
  ) spaceBooking
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
