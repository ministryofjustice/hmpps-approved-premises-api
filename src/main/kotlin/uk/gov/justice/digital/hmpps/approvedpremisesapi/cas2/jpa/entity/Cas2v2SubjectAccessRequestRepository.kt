package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity

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
  ): String? {
    val result = jdbcTemplate.queryForMap(
      """
      select json_agg(applications) as json
      from ( 
        select
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
          CAST( ca.bail_hearing_date as DATE),
          CASE ca.cohort
            WHEN 'HDC' THEN 'Home Detention Curfew'
            WHEN 'PRISON_BAIL' THEN 'Prison Bail'
            WHEN 'COURT_BAIL' THEN 'Court Bail'
            WHEN 'ATCR' THEN 'Alternative to Custodial Recall (ATCR)'
            WHEN 'HCRD' THEN 'Homeless at Conditional Release Date (HCRD)'
            WHEN 'HEFR' THEN 'Homeless at End of Fixed-term Recall'
            WHEN 'ISC' THEN 'Intensive Supervision Courts (ISC)'
            WHEN 'RARR' THEN 'Risk Assessed Recall Review (RARR)'
            WHEN 'FROM_AP' THEN 'Move on from Approved Premises'
            END as cohort_long_display_name
        from
        	cas_2_applications ca
        inner join cas_2_users nu on 
        	nu.id = ca.created_by_cas2_user_id and nu.service_origin = 'BAIL'
        where 
        	(ca.crn = :crn
        		or ca.noms_number = :noms_number ) 
        	and (:start_date::date is null or ca.created_at >= :start_date) 
        	and (:end_date::date is null or ca.created_at <= :end_date)
         and ca.service_origin = 'BAIL'
      ) applications
      """.trimIndent(),
      MapSqlParameterSource()
        .addSarParameters(crn, nomsNumber, startDate, endDate),
    )
    return toJsonString(result)
  }

  fun getAssessments(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String? {
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
          	ca.id = caa.application_id and ca.service_origin = 'BAIL'
          where 
          	(ca.crn = :crn
          		or ca.noms_number = :noms_number )
          and (:start_date::date is null or ca.created_at >= :start_date) 
          and (:end_date::date is null or ca.created_at <= :end_date)
          and caa.service_origin = 'BAIL'
      ) assessments
      """.trimIndent(),
      MapSqlParameterSource().addSarParameters(crn, nomsNumber, startDate, endDate),
    )

    return toJsonString(result)
  }

  fun getApplicationNotes(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String? {
    val result = jdbcTemplate.queryForMap(
      """
      select json_agg(cas_2_application_notes) as json 
      from (
          select
          	cu."name" as created_by_user,
            can.body
          from cas_2_application_notes can 
          inner join cas_2_applications ca on
          	ca.id  = can.application_id and ca.service_origin = 'BAIL'
          left join cas_2_users cu on 
            cu.id = ca.created_by_cas2_user_id and cu.service_origin = 'BAIL'
          where 
          	(ca.crn = :crn
          		or ca.noms_number = :noms_number )
          and (:start_date::date is null or ca.created_at >= :start_date) 
          and (:end_date::date is null or ca.created_at <= :end_date)
      ) cas_2_application_notes
      """.trimIndent(),
      MapSqlParameterSource().addSarParameters(crn, nomsNumber, startDate, endDate),
    )
    return toJsonString(result)
  }
}
