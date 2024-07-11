package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class CAS3SubjectAccessRequestRepository (
  jdbcTemplate: NamedParameterJdbcTemplate,
) : SubjectAccessRequestRepositoryBase(jdbcTemplate) {

  fun temporaryAccommodationApplications(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    val result = jdbcTemplate.queryForMap(
      """
     select json_agg(applications) as json from ( 
        select
             a.crn,
             a.noms_number,
             taa."name" as offender_name,
             a."data",
             a."document",
             a.created_at,
             a.submitted_at,
             u."name" as applications_user_name ,
             taa.conviction_id,
             taa.event_number,
             taa.offence_id,
             pr."name" as probation_region,
             taa.risk_ratings,
             taa.arrival_date,
             taa.is_duty_to_refer_submitted,
             taa.duty_to_refer_submission_date,
             taa.duty_to_refer_local_authority_area_name,
             taa.duty_to_refer_outcome,
             taa.is_eligible,
             taa.eligibility_reason,
             taa.prison_name_on_creation,
             taa.prison_release_types,
             taa.person_release_date,
             taa.pdu,
             taa.needs_accessible_property,
             taa.has_history_of_arson,
             taa.is_registered_sex_offender,
             taa.is_history_of_sexual_offence,
             taa.is_concerning_sexual_behaviour,
             taa.is_concerning_arson_behaviour
        from
             temporary_accommodation_applications taa
        inner join applications a on
        	a.id = taa.id
        inner join users u on
        	u.id = a.created_by_user_id
        inner join probation_regions pr on pr.id = taa.probation_region_id
        where
        	(a.crn = :crn
        		or a.noms_number = :noms_number )
        and (:start_date is null or a.created_at >= :start_date) 
        and (:end_date is null or a.created_at <= :end_date)
     ) applications 
      """.trimIndent(),
      MapSqlParameterSource()
        .addSarParameters(crn, nomsNumber, startDate, endDate),
    )

    return toJsonString(result)
  }


}