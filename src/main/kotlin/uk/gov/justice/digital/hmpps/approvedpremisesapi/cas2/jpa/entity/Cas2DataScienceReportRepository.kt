package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.JdbcResultSetConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.ReportJdbcTemplate

@Repository
class Cas2DataScienceReportRepository(
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
      with latest_status_update as (
          select
              su.*,
              row_number() over (
                  partition by su.application_id
                  order by
                      su.created_at desc
              ) as rn
          from
              cas_2_status_updates su
      )
      select
          cas2.id as cas2_id,
          cas2.crn as cas2_crn,
          cas2.created_at :: date as cas2_created_at,
          cas2.created_by_user_id as cas2_created_by_user_id,
          cas2.created_by_cas2_user_id as cas2_created_by_cas2_user_id,
          cas2.submitted_at :: date as cas2_submitted_at,
          cas2.hdc_eligibility_date :: date as cas2_hdc_eligibility_date,
          cas2.conditional_release_date :: date as cas2_conditional_release_date,
          cas2.bail_hearing_date :: date as cas2_bail_hearing_date,
          cas2.service_origin as cas2_service_origin,
          cas2.application_origin as cas2_application_origin,
          cas2.cohort as cas2_cohort,
          cas2.abandoned_at :: date as cas2_abandoned_at,
          su.id as status_update_id,
          su.status_id as status_update_status_id,
          su.description as status_update_description,
          su.label as status_update_label,
          su.created_at as status_update_created_at,
          c.tier_v2 ->> 'tierScore' as tier_v2,
          c.tier_v3 ->> 'tierScore' as tier_v3,
          'CAS 2' as source_query
      from
          cas_2_applications cas2
          left join latest_status_update su on su.application_id = cas2.id
          and su.rn = 1
          left join cases c on cas2.crn = c.crn
      where
          cas2.submitted_at is not null
          and cas2.submitted_at :: date >= '2026-10-01'
          and cas2.created_at :: date >= '2026-01-01' -- For consistency with the CAS 1 query date filter
  """.trimIndent()
}
