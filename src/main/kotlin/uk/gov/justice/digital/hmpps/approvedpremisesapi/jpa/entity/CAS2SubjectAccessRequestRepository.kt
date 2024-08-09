package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class CAS2SubjectAccessRequestRepository(
  jdbcTemplate: NamedParameterJdbcTemplate,
) : SubjectAccessRequestRepositoryBase(jdbcTemplate) {

  fun getApplicationsJson(
    crn: String?,
    nomsNumber: String?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
  ): String {
    val result = jdbcTemplate.queryForMap(
      """
      select json_agg(applications) as json
      from ( 
        select
          ca.id,
        	ca.crn,
        	ca.noms_number,
        	ca."data",
        	ca."document",
        	nu."name" as created_by_user,
        	ca.created_at,
        	ca.submitted_at,
        	ca.referring_prison_code,
        	ca.preferred_areas,
        	ca.telephone_number,
        	ca.hdc_eligibility_date,
        	ca.conditional_release_date,
        	ca.abandoned_at 
        from
        	cas_2_applications ca
        inner join nomis_users nu on
        	nu.id = ca.created_by_user_id
        where 
        	(ca.crn = :crn
        		or ca.noms_number = :noms_number ) 
        	and (:start_date is null or ca.created_at >= :start_date) 
        	and (:end_date is null or ca.created_at <= :end_date)
      ) applications
      """.trimIndent(),
      MapSqlParameterSource()
        .addSarParameters(crn, nomsNumber, startDate, endDate),
    )
    return toJsonString(result)
  }

  fun getAssessments(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    val result = jdbcTemplate.queryForMap(
      """
      select json_agg(assessments) as json
      from(
          select
          	caa.id,
          	ca.crn,
          	ca.noms_number,
          	ca.id as application_id,
          	caa.created_at,
          	caa.assessor_name,
          	caa.nacro_referral_id
          from
          	cas_2_assessments caa
          inner join cas_2_applications ca 
          on
          	ca.id = caa.application_id
          where 
          	(ca.crn = :crn
          		or ca.noms_number = :noms_number )
          and (:start_date is null or ca.created_at >= :start_date) 
          and (:end_date is null or ca.created_at <= :end_date)
      ) assessments
      """.trimIndent(),
      MapSqlParameterSource().addSarParameters(crn, nomsNumber, startDate, endDate),
    )

    return toJsonString(result)
  }

  fun getApplicationNotes(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    val result = jdbcTemplate.queryForMap(
      """
      select json_agg(application_notes) as json 
      from (
          select
          	can.id,
          	ca.crn,
          	ca.noms_number,
          	can.application_id,
          	can.assessment_id, 
          	case 
          		when created_by_external_user_id is not null then eu."name"
          		when created_by_nomis_user_id is not null then nu."name"
          		else 'unknown'
          	end as created_by_user,
          	case 
          		when created_by_external_user_id is not null then 'external'
          		when created_by_nomis_user_id is not null then 'nomis'
          		else 'unknown'
          	end as created_by_user_type,
          	can.body
          from cas_2_application_notes can 
          inner join cas_2_applications ca on
          	ca.id  = can.application_id 
          left join external_users eu on 
          	eu.id = can.created_by_external_user_id
          left join nomis_users nu on nu.id = can.created_by_nomis_user_id 
          where 
          	(ca.crn = :crn
          		or ca.noms_number = :noms_number )
          and (:start_date is null or ca.created_at >= :start_date) 
          and (:end_date is null or ca.created_at <= :end_date)
      ) application_notes
      """.trimIndent(),
      MapSqlParameterSource().addSarParameters(crn, nomsNumber, startDate, endDate),
    )
    return toJsonString(result)
  }

  fun getStatusUpdates(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    val result = jdbcTemplate.queryForMap(
      """
      select json_agg(application_status_updates) as json 
      from (
          select
              csu.id,
              ca.crn,
              ca.noms_number, 
              csu.application_id,
              csu.assessment_id,
              eu."name" as assessor_name,
              eu.origin as assessor_origin,
              to_char(csu.created_at,'YYYY-MM-DD HH24:MI:SS') as created_at,
              csu.description ,
              csu."label"
          from cas_2_status_updates csu 
          inner join cas_2_applications ca
              on ca.id =csu.application_id
          inner join external_users eu 
              on eu.id = csu.assessor_id
          where 
          	(ca.crn = :crn
          		or ca.noms_number = :noms_number )
          and (:start_date is null or ca.created_at >= :start_date) 
          and (:end_date is null or ca.created_at <= :end_date)
      ) application_status_updates
      """.trimIndent(),
      MapSqlParameterSource()
        .addSarParameters(crn, nomsNumber, startDate, endDate),
    )
    return toJsonString(result)
  }

  fun getStatusUpdateDetails(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    val result = jdbcTemplate.queryForMap(
      """
      select json_agg(application_status_update_details) as json 
      from (
        select
        	ca. crn,
        	ca. noms_number, 
        	csud.status_update_id,
        	csu.application_id,
        	csu.assessment_id,
        	csu."label" as status_label,
        	csud."label" as detail_label,
        	to_char(csud.created_at, 'YYYY-MM-DD HH24:MI:SS') as created_at 
        from cas_2_status_updates csu 
        inner join cas_2_status_update_details csud  
        	on csu.id  = csud.status_update_id 
        inner join cas_2_applications ca
        	on ca.id =csu.application_id
        inner join external_users eu 
        	on eu.id = csu.assessor_id 
        where 
        	(ca.crn = :crn
        		or ca.noms_number = :noms_number )
        and (:start_date is null or ca.created_at >= :start_date) 
        and (:end_date is null or ca.created_at <= :end_date)
        ) application_status_update_details
      """.trimIndent(),
      MapSqlParameterSource()
        .addSarParameters(crn, nomsNumber, startDate, endDate),
    )
    return toJsonString(result)
  }
}
