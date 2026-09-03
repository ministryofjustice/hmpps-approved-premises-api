package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.SubjectAccessRequestRepositoryBase
import java.time.LocalDateTime

@Repository
class Cas2HdcSubjectAccessRequestRepository(
  jdbcTemplate: NamedParameterJdbcTemplate,
) : SubjectAccessRequestRepositoryBase(jdbcTemplate) {

  fun getApplicationsJson(
    crn: String?,
    nomsNumber: String?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    serviceOrigin: String,
  ): String? {
    val result = jdbcTemplate.queryForMap(
      """
      select json_agg(applications) as json
      from ( 
        select
        	ca."document",
        	cu."name" as created_by_user,
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
        	cas_2_applications ca
        inner join cas_2_users cu on
        	cu.id = ca.created_by_cas2_user_id and cu.user_type = 'NOMIS' and cu.service_origin = :service_origin
        where 
        	(ca.crn = :crn
        		or ca.noms_number = :noms_number ) 
        	and (:start_date::date is null or ca.created_at >= :start_date) 
        	and (:end_date::date is null or ca.created_at <= :end_date)
        and ca.service_origin = :service_origin
      ) applications
      """.trimIndent(),
      MapSqlParameterSource()
        .addSarParameters(crn, nomsNumber, startDate, endDate).addValue("service_origin", serviceOrigin),
    )
    return toJsonString(result)
  }

  fun getAssessments(
    crn: String?,
    nomsNumber: String?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    serviceOrigin: String,
  ): String? {
    val result = jdbcTemplate.queryForMap(
      """
      select json_agg(assessments) as json
      from(
          select
          	caa.created_at,
          	REGEXP_REPLACE(TRIM(caa.assessor_name), '^.* ', '') as assessor_name,
          	caa.nacro_referral_id
          from
          	cas_2_assessments caa
          inner join cas_2_applications ca 
          on
          	ca.id = caa.application_id and ca.service_origin = :service_origin
          where 
          	(ca.crn = :crn
          		or ca.noms_number = :noms_number )
          and (:start_date::date is null or ca.created_at >= :start_date) 
          and (:end_date::date is null or ca.created_at <= :end_date)
          and caa.service_origin = :service_origin
      ) assessments
      """.trimIndent(),
      MapSqlParameterSource().addSarParameters(crn, nomsNumber, startDate, endDate).addValue("service_origin", serviceOrigin),
    )

    return toJsonString(result)
  }

  fun getApplicationNotes(
    crn: String?,
    nomsNumber: String?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    serviceOrigin: String,
  ): String? {
    val result = jdbcTemplate.queryForMap(
      """
      select json_agg(application_notes) as json 
      from (
          select
          	case 
          		when can.created_by_cas2_user_id is not null then cu."name"
          		else 'unknown'
          	end as created_by_user,
          	can.body
          from cas_2_application_notes can 
          inner join cas_2_applications ca on
          	ca.id  = can.application_id and ca.service_origin = :service_origin
          left join cas_2_users cu on cu.id = can.created_by_cas2_user_id and cu.service_origin = :service_origin
          where 
          	(ca.crn = :crn
          		or ca.noms_number = :noms_number )
          and (:start_date::date is null or ca.created_at >= :start_date) 
          and (:end_date::date is null or ca.created_at <= :end_date)
      ) application_notes
      """.trimIndent(),
      MapSqlParameterSource().addSarParameters(crn, nomsNumber, startDate, endDate).addValue("service_origin", serviceOrigin),
    )
    return toJsonString(result)
  }
}
