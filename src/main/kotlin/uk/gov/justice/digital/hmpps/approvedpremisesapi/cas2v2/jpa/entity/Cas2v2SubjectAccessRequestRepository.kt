package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.SubjectAccessRequestRepositoryBase
import java.time.LocalDateTime

@Repository
class Cas2v2SubjectAccessRequestRepository(
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
        	ca.abandoned_at,
          ca.application_origin,
          CAST( ca.bail_hearing_date as DATE) 
        from
        	cas_2_v2_applications ca
        inner join cas_2_v2_users nu on
        	nu.id = ca.created_by_user_id
        where 
        	(ca.crn = :crn
        		or ca.noms_number = :noms_number ) 
        	and (:start_date::date is null or ca.created_at >= :start_date) 
        	and (:end_date::date is null or ca.created_at <= :end_date)
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
          	cas_2_v2_assessments caa
          inner join public.cas_2_v2_applications ca 
          on
          	ca.id = caa.application_id
          where 
          	(ca.crn = :crn
          		or ca.noms_number = :noms_number )
          and (:start_date::date is null or ca.created_at >= :start_date) 
          and (:end_date::date is null or ca.created_at <= :end_date)
      ) assessments
      """.trimIndent(),
      MapSqlParameterSource().addSarParameters(crn, nomsNumber, startDate, endDate),
    )

    return toJsonString(result)
  }

  fun getApplicationNotes(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    val result = jdbcTemplate.queryForMap(
      """
      select json_agg(cas_2_v2_application_notes) as json 
      from (
          select
          	can.id,
          	ca.crn,
          	ca.noms_number,
          	can.application_id,
          	can.assessment_id, 
          	cu."name" as created_by_user,
            cu.user_type as created_by_user_type,
            can.body
          from cas_2_v2_application_notes can 
          inner join cas_2_v2_applications ca on
          	ca.id  = can.application_id 
          left join cas_2_v2_users cu on 
            cu.id = ca.created_by_user_id
          where 
          	(ca.crn = :crn
          		or ca.noms_number = :noms_number )
          and (:start_date::date is null or ca.created_at >= :start_date) 
          and (:end_date::date is null or ca.created_at <= :end_date)
      ) cas_2_v2_application_notes
      """.trimIndent(),
      MapSqlParameterSource().addSarParameters(crn, nomsNumber, startDate, endDate),
    )
    return toJsonString(result)
  }

  fun getStatusUpdates(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    val result = jdbcTemplate.queryForMap(
      """
      select json_agg(cas_2_v2_application_status_updates) as json 
      from (
          select
              csu.id,
              ca.crn,
              ca.noms_number, 
              csu.application_id,
              csu.assessment_id,
              eu."name" as assessor_name,
              to_char(csu.created_at,'YYYY-MM-DD HH24:MI:SS')  as created_at,
              csu.description ,
              csu."label"
          from cas_2_v2_status_updates csu 
          inner join cas_2_v2_applications ca
              on ca.id =csu.application_id
          inner join cas_2_v2_users eu 
              on eu.id = csu.assessor_id
          where 
          	(ca.crn = :crn
          		or ca.noms_number = :noms_number )
          and (:start_date::date is null or ca.created_at >= :start_date) 
          and (:end_date::date is null or ca.created_at <= :end_date)
      ) cas_2_v2_application_status_updates
      """.trimIndent(),
      MapSqlParameterSource()
        .addSarParameters(crn, nomsNumber, startDate, endDate),
    )
    return toJsonString(result)
  }

  fun getStatusUpdateDetails(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    val result = jdbcTemplate.queryForMap(
      """
      select json_agg(cas_2_v2_application_status_update_details) as json 
      from (
        select
        	ca. crn,
        	ca. noms_number, 
        	csud.status_update_id,
        	csu.application_id,
        	csu.assessment_id,
        	csu."label" as status_label,
        	csud."label" as detail_label,
        	to_char(csud.created_at , 'YYYY-MM-DD HH24:MI:SS') as created_at 
        from cas_2_v2_status_updates csu 
        inner join cas_2_v2_status_update_details csud  
        	on csu.id  = csud.status_update_id 
        inner join cas_2_v2_applications ca
        	on ca.id =csu.application_id
        inner join cas_2_v2_users eu 
        	on eu.id = csu.assessor_id 
        where 
        	(ca.crn = :crn
        		or ca.noms_number = :noms_number )
        and (:start_date::date is null or ca.created_at >= :start_date) 
        and (:end_date::date is null or ca.created_at <= :end_date)
        ) cas_2_v2_application_status_update_details
      """.trimIndent(),
      MapSqlParameterSource()
        .addSarParameters(crn, nomsNumber, startDate, endDate),
    )
    return toJsonString(result)
  }

  override fun domainEvents(
    crn: String?,
    nomsNumber: String?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    serviceName: String,
  ): String {
    val result = jdbcTemplate.queryForMap(
      """
           select json_agg(domain_events) as json from ( 
               select 
                 de.id,
                 de.application_id,
                 de.crn,
                 de."type",
                 de.occurred_at,
                 de.created_at,
                 de."data",
                 de.booking_id,
                 de.service,
                 de.assessment_id,
                 u."name" as triggered_by_user,
                 de.noms_number,
                 de.trigger_source
               from
                     domain_events de 
               left join cas_2_v2_users u on 
                     u.id = de.triggered_by_user_id
               where
                  de.service = :service_name and
                  (de.crn = :crn
                        or de.noms_number = :noms_number )
               and (:start_date::date is null or de.created_at >= :start_date)
               and (:end_date::date is null or de.created_at <= :end_date) 
           ) domain_events
      """.trimIndent(),
      MapSqlParameterSource()
        .addSarParameters(crn, nomsNumber, startDate, endDate)
        .addValue("service_name", serviceName),
    )
    return toJsonString(result)
  }
}
