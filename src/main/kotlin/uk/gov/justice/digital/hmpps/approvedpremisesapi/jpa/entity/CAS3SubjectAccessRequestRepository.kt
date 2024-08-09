package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class CAS3SubjectAccessRequestRepository(
  jdbcTemplate: NamedParameterJdbcTemplate,
) : SubjectAccessRequestRepositoryBase(jdbcTemplate) {

  fun temporaryAccommodationApplications(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    val result = jdbcTemplate.queryForMap(
      """
     select json_agg(applications) as json from ( 
        select
             a.crn,
             a.noms_number,
             taa."name" as offender_name,
             a."data",
             a."document",
             a.created_at,
             a.submitted_at,
             u."name" as applications_user_name ,
             taa.conviction_id,
             taa.event_number,
             taa.offence_id,
             pr."name" as probation_region,
             taa.risk_ratings,
             taa.arrival_date,
             taa.is_duty_to_refer_submitted,
             taa.duty_to_refer_submission_date,
             taa.duty_to_refer_local_authority_area_name,
             taa.duty_to_refer_outcome,
             taa.is_eligible,
             taa.eligibility_reason,
             taa.prison_name_on_creation,
             taa.prison_release_types,
             taa.person_release_date,
             taa.pdu,
             taa.needs_accessible_property,
             taa.has_history_of_arson,
             taa.is_registered_sex_offender,
             taa.is_history_of_sexual_offence,
             taa.is_concerning_sexual_behaviour,
             taa.is_concerning_arson_behaviour
        from
             temporary_accommodation_applications taa
        inner join applications a on
        	a.id = taa.id
        inner join users u on
        	u.id = a.created_by_user_id
        inner join probation_regions pr on pr.id = taa.probation_region_id
        where
        	(a.crn = :crn
        		or a.noms_number = :noms_number )
        and (:start_date is null or a.created_at >= :start_date) 
        and (:end_date is null or a.created_at <= :end_date)
     ) applications 
      """.trimIndent(),
      MapSqlParameterSource()
        .addSarParameters(crn, nomsNumber, startDate, endDate),
    )

    return toJsonString(result)
  }
  fun temporaryAccommodationAssessments(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    val result = jdbcTemplate.queryForMap(
      """
        select json_agg(assessments) as json 
        from (
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
                taa.summary_data,
                taa.completed_at,
                rrr."name" as referral_rejection_reason_category,
                taa.referral_rejection_reason_detail,
                taa.release_date,
                taa.accommodation_required_from_date
              from
                temporary_accommodation_assessments taa
              inner join assessments assess on
                assess.id = taa.assessment_id
              inner join applications app on
                app.id = assess.application_id
              inner join users u on 
                u.id = assess.allocated_to_user_id 
              left join referral_rejection_reasons rrr on
                rrr.id = taa.referral_rejection_reason_id
              where 
                (app.crn = :crn
                  or app.noms_number = :noms_number )
              and (:start_date is null or app.created_at >= :start_date) 
              and (:end_date is null or app.created_at <= :end_date)
            ) assessments
      
      """.trimIndent(),
      MapSqlParameterSource()
        .addSarParameters(crn, nomsNumber, startDate, endDate),
    )
    return toJsonString(result)
  }

  fun assessmentReferralHistoryNotes(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    val result = jdbcTemplate.queryForMap(
      """
      select json_agg(referral_notes) as json 
      from ( 
        select
            app.crn,
            app.noms_number,
            app.id as application_id,
            assess.id  as assessment_id,
            arhn.message,
            arhn.created_at,
            u."name" as created_by_user,
            case
                when arhsn.id is not null then 'System'
                when arhun.id is not null then 'User'
            end as note_type,
            arhsn.type as system_note_type
        from
            assessment_referral_history_notes arhn
        inner join assessments assess on
            assess.id = arhn.assessment_id
        inner join applications app on
            app.id = assess.application_id
        inner join users u 
            on u.id = arhn.created_by_user_id 
        left join assessment_referral_history_system_notes arhsn 
            on arhsn.id = arhn.id
        left join assessment_referral_history_user_notes arhun 
            on arhun.id = arhn.id 
        where 
            (app.crn = :crn
            or app.noms_number = :noms_number )
        and (:start_date is null or app.created_at >= :start_date) 
        and (:end_date is null or app.created_at <= :end_date)
     ) referral_notes
      """.trimIndent(),
      MapSqlParameterSource()
        .addSarParameters(crn, nomsNumber, startDate, endDate),
    )
    return toJsonString(result)
  }
}
