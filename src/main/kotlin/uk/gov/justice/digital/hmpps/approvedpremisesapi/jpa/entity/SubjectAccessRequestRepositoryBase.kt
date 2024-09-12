package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.postgresql.util.PGobject
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import java.time.LocalDateTime

@Repository
open class SubjectAccessRequestRepositoryBase(val jdbcTemplate: NamedParameterJdbcTemplate) {

  fun bookings(
    crn: String?,
    nomsNumber: String?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    serviceName: ServiceName = ServiceName.approvedPremises,
  ): String {
    val result = jdbcTemplate.queryForMap(
      """
    select json_agg(booking) as json 
    from (
          select
               b.crn ,
              b.noms_number,
              b.arrival_date,
              b.departure_date,
              b.original_arrival_date,
              b.original_departure_date,
              b.created_at,
              b.status,
              p."name" as premises_name,
              b.adhoc,
              b.key_worker_staff_code,
              b.service,
              b.application_id, 
              b.offline_application_id,
              b."version"
          from
              bookings b
          left join premises p on
              b.premises_id = p.id
          where
              b.service = :service_name
          and 
              (b.crn = :crn
              or b.noms_number = :noms_number )
          and (:start_date::date is null or b.created_at >= :start_date) 
          and (:end_date::date is null or b.created_at <= :end_date)
  ) booking
      """.trimIndent(),
      MapSqlParameterSource().addSarParameters(
        crn,
        nomsNumber,
        startDate,
        endDate,
      ).addValue("service_name", serviceName.value),
    )
    return toJsonString(result)
  }

  fun bookingExtensions(
    crn: String?,
    nomsNumber: String?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    serviceName: ServiceName = ServiceName.approvedPremises,
  ): String {
    var result = jdbcTemplate.queryForMap(
      """
        select json_agg(booking_ext) as json 
        from (
      select
            a.id as application_id,
            oa.id  as offline_application_id,
            b.crn,
            b.noms_number,
            e.previous_departure_date,
            e.new_departure_date,
            e.notes,
            e.created_at
        from
            extensions e
        join bookings b on
            b.id = e.booking_id
        left join applications a on
            a.id = b.application_id
        left join offline_applications oa on
            oa.id = b.offline_application_id
      where
      b.service = :service_name and
        (b.crn = :crn
          or b.noms_number = :noms_number )
      and (:start_date::date is null or b.created_at >= :start_date)
      and (:end_date::date is null or b.created_at <= :end_date)
        )booking_ext
      """.trimIndent(),
      MapSqlParameterSource().addSarParameters(
        crn,
        nomsNumber,
        startDate,
        endDate,
      ).addValue("service_name", serviceName.value),
    )
    return toJsonString(result)
  }

  fun cancellations(
    crn: String?,
    nomsNumber: String?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    serviceName: ServiceName = ServiceName.approvedPremises,
  ): String {
    var result = jdbcTemplate.queryForMap(
      """
          select json_agg(cancellation) as json
          from (
              select
                  b.crn,
                  b.noms_number,
                  c.notes,
                  c."date" as cancellation_date,
                  cr."name" as cancellation_reason,
                  c.other_reason,
                  c.created_at
              from
                  cancellations c
                  inner join bookings b on
                      b.id = c.booking_id
                  inner join cancellation_reasons cr on
                      c.cancellation_reason_id = cr.id
              where
                  b.service = :service_name and
                  (b.crn = :crn
                      or b.noms_number = :noms_number )
                and (:start_date::date is null or b.created_at >= :start_date)
                and (:end_date::date is null or b.created_at <= :end_date)        
          ) cancellation
      """.trimIndent(),
      MapSqlParameterSource().addSarParameters(
        crn,
        nomsNumber,
        startDate,
        endDate,
      ).addValue("service_name", serviceName.value),
    )
    return toJsonString(result)
  }

  fun domainEvents(
    crn: String?,
    nomsNumber: String?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    serviceName: String = "CAS1",
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
               left join users u on 
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

  fun domainEventMetadata(
    crn: String?,
    nomsNumber: String?,
    startDate: LocalDateTime?,
    endDate: LocalDateTime?,
    serviceName: String = "CAS1",
  ): String {
    val result = jdbcTemplate.queryForMap(
      """
             select json_agg(domain_events_metadata) as json
             from ( 
                 select 
                     de.crn,
                     de.noms_number,
                     de.created_at,
                     dem.domain_event_id,
                     dem."name",
                     dem.value
                 from 
                     domain_events_metadata dem 
                 inner join domain_events de on 
                     de.id = dem.domain_event_id
                 where
                    de.service = :service_name and
                    (de.crn = :crn
                         or de.noms_number = :noms_number )
                 and (:start_date::date is null or de.created_at >= :start_date)
                 and (:end_date::date is null or de.created_at <= :end_date) 
             ) domain_events_metadata
      """.trimIndent(),
      MapSqlParameterSource()
        .addSarParameters(crn, nomsNumber, startDate, endDate)
        .addValue("service_name", serviceName),
    )
    return toJsonString(result)
  }

  protected fun toJsonString(result: Map<String, Any>) = (result["json"] as PGobject?)?.value ?: "[]"
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
