package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.JdbcResultSetConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.ReportJdbcTemplate

@Repository
class Cas3DataScienceReportRepository(
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
      with latest_assessment as (
          select
              assess.*,
              row_number() over (
                  partition by assess.application_id
                  order by assess.created_at desc
              ) as rn
          from
              assessments assess
          where
              assess.reallocated_at is null
      )
      select
          a.id as app_id,
          a.crn as app_crn,
          a.created_at :: date as app_created_at,
          a.created_by_user_id as app_created_by_user_id,
          a.submitted_at :: date as app_submitted_at,
          a.service as app_service,
          a.deleted_at :: date as app_deleted_at,
          taa.arrival_date :: date as taa_arrival_date,
          taa.person_release_date :: date as taa_person_release_date,
          la.id as assess_id,
          la.created_at :: date as assess_created_at,
          la.submitted_at :: date as assess_submitted_at,
          la.decision as assess_decision,
          la.service as assess_service,
          la.is_withdrawn as assess_is_withdrawn,
          taa.probation_region_id as taa_probation_region_id,
          pr.name as pr_probation_region_name,
          taa.probation_delivery_unit_id as taa_probation_delivery_unit_id,
          pdu.name as pdu_probation_delivery_unit_name,
          c.tier_v2 ->> 'tierScore' as tier_v2,
          c.tier_v3 ->> 'tierScore' as tier_v3,
          'CAS 3' as source_query
      from
          applications a
          inner join temporary_accommodation_applications taa on a.id = taa.id
          left join latest_assessment la on a.id = la.application_id and la.rn = 1
          left join probation_regions pr on taa.probation_region_id = pr.id
          left join probation_delivery_units pdu on taa.probation_delivery_unit_id = pdu.id
          left join cases c on a.crn = c.crn
      where
          a.submitted_at is not null
          and a.submitted_at :: date >= '2026-10-01'
          and a.created_at :: date >= '2026-01-01' -- For consistency with the CAS 1 query date filter
      order by
          a.id;
  """.trimIndent()
}
