package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting

import org.springframework.stereotype.Repository

@Repository
class Cas1DataScienceReportRepository(
  val reportJdbcTemplate: ReportJdbcTemplate,
) {

  fun generate(
    jdbcResultSetConsumer: JdbcResultSetConsumer,
  ) = reportJdbcTemplate.query(
    generateSql(),
    emptyMap<String, Any>(),
    jdbcResultSetConsumer,
  )

  private fun generateSql(): String = """
      with base_applications as (
          select
              a.id as app_id,
              a.crn as app_crn,
              a.created_at :: date as app_created_at,
              a.created_by_user_id as app_created_by_user_id,
              a.submitted_at :: date as app_submitted_at,
              a.service as app_service,
              a.deleted_at :: date as app_deleted_at,
              apa.arrival_date :: date as apa_arrival_date,
              apa.status as apa_status,
              apa.probation_region_id as apa_probation_region_id,
              pr.name as pr_probation_region_name,
              apa.ap_area_id as apa_ap_area_id,
              ap_areas.name as ap_area_name,
              apa.is_withdrawn as apa_is_withdrawn,
              c.tier_v2 ->> 'tierScore' as tier_v2,
              c.tier_v3 ->> 'tierScore' as tier_v3,
              'CAS 1' as source_query
          from
              applications a
              inner join approved_premises_applications apa on a.id = apa.id
              left join probation_regions pr on apa.probation_region_id = pr.id
              left join ap_areas on apa.ap_area_id = ap_areas.id
              left join cases c on a.crn = c.crn
          where
              a.submitted_at is not null
              and a.submitted_at :: date >= '2026-01-01'
      ),
      -- Request for placements from the placeholder table
      -- where the original application is still pending acceptance
      rfp_from_placeholder as (
          select
              pap.application_id as app_id,
              pap.id as placement_id,
              pap.submitted_at :: date as placement_submitted_at,
              pap.expected_arrival_date :: date as placement_expected_arrival_date,
              null :: boolean as placement_is_withdrawn,
              null :: text as placement_decision,
              'original_application' as placement_source
          from
              placement_applications_placeholder pap
          where
              pap.submitted_at is not null
              and pap.archived is not true
              and pap.submitted_at :: date >= '2026-01-01'
      ),
      -- Request for placements from the main placement applications table
      -- where the original application has been accepted or an additional request has been made
      rfp_from_placement_applications as (
          select
              pa.application_id as app_id,
              pa.id as placement_id,
              pa.submitted_at :: date as placement_submitted_at,
              pa.expected_arrival :: date as placement_expected_arrival_date,
              pa.is_withdrawn as placement_is_withdrawn,
              pa.decision as placement_decision,
              case
                  when pa.automatic is true then 'original_application'
                  else 'additional_request'
              end as placement_source
          from
              placement_applications pa
          where
              pa.submitted_at is not null
              and pa.reallocated_at is null
              and pa.submitted_at :: date >= '2026-01-01'
      ),
      all_placements as (
          select
              *
          from
              rfp_from_placeholder
          union
          all
          select
              *
          from
              rfp_from_placement_applications
      )
      select
          ba.*,
          p.placement_id,
          p.placement_submitted_at,
          p.placement_expected_arrival_date,
          p.placement_is_withdrawn,
          p.placement_decision,
          p.placement_source
      from
          base_applications ba
          left join all_placements p on ba.app_id = p.app_id
      order by
          ba.app_id;
  """.trimIndent()
}
