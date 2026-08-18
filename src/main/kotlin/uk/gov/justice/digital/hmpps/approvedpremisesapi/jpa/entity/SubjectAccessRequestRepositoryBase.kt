package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
open class SubjectAccessRequestRepositoryBase(val jdbcTemplate: NamedParameterJdbcTemplate) {

  fun domainEvents(
    crn: String?,
    nomsNumber: String?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    serviceName: String = "CAS1",
  ): String? {
    val result = jdbcTemplate.queryForMap(
      """
           select json_agg(domain_events) as json from ( 
               select 
                 de."type",
                 de.occurred_at,
                 de.created_at,
                 de."data",
                 CASE
                   WHEN cas1_3_app.id IS NOT NULL THEN cas1_3_app.submitted_at
                   WHEN cas2_app.id IS NOT NULL THEN cas2_app.submitted_at
                   ELSE NULL
                 END as application_submitted_at,
                 u.delius_username as triggered_by_username,
                 cas3_booking.arrival_date as cas3_booking_arrival_date,
                 cas3_premises.name as cas3_booking_premises_name
               from
                     domain_events de 
               left join users u on 
                     u.id = de.triggered_by_user_id
               left join applications cas1_3_app on de.application_id = cas1_3_app.id     
               left join cas_2_applications cas2_app on de.application_id = cas2_app.id
               left join cas3_bookings cas3_booking on de.booking_id = cas3_booking.id
               left join cas3_premises cas3_premises on cas3_booking.premises_id = cas3_premises.id
               where
                  de.service = :service_name and
                  (de.crn = :crn
                        or de.noms_number = :noms_number )
               and (:start_date::date is null or de.created_at >= :start_date)
               and (:end_date::date is null or de.created_at <= :end_date) 
               order by de.created_at
           ) domain_events
      """.trimIndent(),
      MapSqlParameterSource()
        .addSarParameters(crn, nomsNumber, startDate, endDate)
        .addValue("service_name", serviceName),
    )
    return toJsonString(result)
  }

  protected fun toJsonString(result: Map<String, Any?>): String? = result["json"]?.toString()
  protected fun MapSqlParameterSource.addSarParameters(
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
