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
  ): String? {
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
        	cas_2_applications ca
        inner join cas_2_users nu on
        	nu.id = ca.created_by_cas2_user_id
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

  override fun domainEvents(
    crn: String?,
    nomsNumber: String?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    serviceName: String,
  ): String? {
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
               left join cas_2_users u on 
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
